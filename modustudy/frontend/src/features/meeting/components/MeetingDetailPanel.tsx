import React, { useEffect, useRef, useState } from 'react';
import { useUIStore } from '@/store/uiStore';
import { meetingApi } from '../services/meetingApi';
import {
  MeetingChatMessageResponse,
  MeetingDetailResponse,
  MeetingPhotoResponse,
} from '../types';
import MeetingSummaryPanel from './MeetingSummaryPanel';
import '../styles/MeetingDetail.css';
import '../styles/MeetingShared.css';

interface MeetingDetailPanelProps {
  studyId: number;
  meetingId: number;
  onBack?: () => void;
}

const MeetingDetailPanel: React.FC<MeetingDetailPanelProps> = ({ studyId, meetingId, onBack }) => {
  const [detail, setDetail] = useState<MeetingDetailResponse | null>(null);
  const [chatMessages, setChatMessages] = useState<MeetingChatMessageResponse[]>([]);
  const [photos, setPhotos] = useState<MeetingPhotoResponse[]>([]);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<Set<number>>(new Set());
  const [isSavingSelection, setIsSavingSelection] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const showToast = useUIStore((state) => state.showToast);
  const canSelectPhotos = detail?.status === 'ENDED';
  const selectAllRef = useRef<HTMLInputElement | null>(null);
  const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
  const resolveImageUrl = (url: string) => {
    if (!url) return url;
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    if (!apiBaseUrl) return url;
    if (url.startsWith('/')) return `${apiBaseUrl}${url}`;
    return `${apiBaseUrl}/${url}`;
  };

  useEffect(() => {
    if (!studyId || !meetingId) return;
    const load = async () => {
      const meetingDetail = await meetingApi.getMeetingDetail(studyId, meetingId);
      setDetail(meetingDetail);
      const chatData = await meetingApi
        .getChatHistory(studyId, meetingId, { page: 0, size: 100 })
        .catch(() => null);
      if (chatData) setChatMessages(chatData.content);
      const photoList = await meetingApi.getPhotos(studyId, meetingId).catch(() => []);
      setPhotos(photoList);
    };
    load();
  }, [studyId, meetingId]);

  useEffect(() => {
    const selectedIds = new Set(photos.filter((photo) => photo.isSelected).map((photo) => photo.id));
    setSelectedPhotoIds(selectedIds);
  }, [photos]);

  useEffect(() => {
    if (!selectAllRef.current) return;
    const total = photos.length;
    const selectedCount = selectedPhotoIds.size;
    selectAllRef.current.indeterminate = selectedCount > 0 && selectedCount < total;
  }, [photos.length, selectedPhotoIds]);

  const handleExport = async (format: 'MARKDOWN' | 'PDF') => {
    if (!studyId || !meetingId) return;
    const savedSelection = photos.filter((photo) => photo.isSelected);
    if (photos.length > 0 && savedSelection.length === 0) {
      window.alert('보고서용 이미지로 선택한 사진이 없습니다.');
      return;
    }
    const blob = await meetingApi.exportMeeting(studyId, meetingId, format);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `meeting-${meetingId}.${format === 'MARKDOWN' ? 'md' : 'pdf'}`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleTogglePhoto = (photoId: number) => {
    setSelectedPhotoIds((prev) => {
      const next = new Set(prev);
      if (next.has(photoId)) {
        next.delete(photoId);
      } else {
        next.add(photoId);
      }
      return next;
    });
  };

  const handleToggleAll = () => {
    setSelectedPhotoIds((prev) => {
      if (prev.size === photos.length) {
        return new Set();
      }
      return new Set(photos.map((photo) => photo.id));
    });
  };

  const handleSaveSelection = async () => {
    if (!studyId || !meetingId || !canSelectPhotos || isSavingSelection) return;
    if (selectedPhotoIds.size === 0) {
      showToast('이미지 선택 후 클릭해주세요.', 'warning');
      return;
    }
    setIsSavingSelection(true);
    try {
      const updated = await meetingApi.selectPhotos(
        studyId,
        meetingId,
        Array.from(selectedPhotoIds)
      );
      setPhotos(updated);
      showToast('보고서에 이미지가 첨부되었습니다.', 'success');
    } catch (error) {
      console.error('Failed to save report photo selection', error);
      showToast('이미지 첨부에 실패했습니다.', 'error');
    } finally {
      setIsSavingSelection(false);
    }
  };

  const handleDownloadSelected = async () => {
    if (isDownloading) return;
    const selected = photos.filter((photo) => selectedPhotoIds.has(photo.id));
    if (selected.length === 0) {
      window.alert('다운로드할 이미지를 선택해주세요.');
      return;
    }
    setIsDownloading(true);
    try {
      for (const photo of selected) {
        const url = resolveImageUrl(photo.imageUrl);
        try {
          const response = await fetch(url, { credentials: 'include' });
          const blob = await response.blob();
          const extension = (() => {
            const dotIndex = url.lastIndexOf('.');
            return dotIndex !== -1 ? url.slice(dotIndex) : '.png';
          })();
          const filename = `meeting-${meetingId}-photo-${photo.id}${extension}`;
          const linkUrl = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = linkUrl;
          link.download = filename;
          link.click();
          URL.revokeObjectURL(linkUrl);
        } catch (error) {
          window.open(url, '_blank', 'noopener,noreferrer');
          console.error('Failed to download meeting photo', error);
        }
      }
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="meeting-detail">
      <div className="meeting-detail__header">
        <div>
          <h1>{detail?.title || '미팅 기록'}</h1>
          <p>
            {detail?.startedAt ? new Date(detail.startedAt).toLocaleString() : '시작 시간 미정'}
            {detail?.endedAt && ` ~ ${new Date(detail.endedAt).toLocaleTimeString()}`}
          </p>
        </div>
        <div className="meeting-detail__exports">
          <button className="meeting-btn ghost" onClick={onBack}>
            목록으로
          </button>
          <button className="meeting-btn ghost" onClick={() => handleExport('MARKDOWN')}>
            Markdown 내보내기
          </button>
          <button className="meeting-btn ghost" onClick={() => handleExport('PDF')}>
            PDF 내보내기
          </button>
        </div>
      </div>

      <MeetingSummaryPanel summary={detail?.summary ?? null} />

      <section className="meeting-detail-card">
        <div className="meeting-detail-card__header">
          <h3>미팅 사진</h3>
          {canSelectPhotos && photos.length > 0 && (
            <div className="meeting-photo-actions">
              <label className="meeting-photo-selectall">
                <input
                  ref={selectAllRef}
                  type="checkbox"
                  checked={photos.length > 0 && selectedPhotoIds.size === photos.length}
                  onChange={handleToggleAll}
                />
                전체 선택
              </label>
                            <div className="meeting-tooltip-wrapper">
                <button
                  type="button"
                  className="meeting-btn ghost"
                  onClick={handleSaveSelection}
                  disabled={isSavingSelection}
                  aria-label="보고서용 이미지"
                >
                  보고서용 이미지
                </button>
                <span className="meeting-tooltip" role="tooltip">
                  이미지 체크 후 클릭하면 pdf, markdown 보고서에 첨부됩니다.
                </span>
              </div>
              <button
                type="button"
                className="meeting-btn ghost"
                onClick={handleDownloadSelected}
                disabled={isDownloading || selectedPhotoIds.size === 0}
              >
                이미지 다운로드
              </button>
            </div>
          )}
        </div>
        <div className="meeting-detail-card__body">
          {!canSelectPhotos && (
            <p className="meeting-detail-empty">미팅이 종료된 뒤 사진을 선택할 수 있습니다.</p>
          )}
          {canSelectPhotos && photos.length === 0 && (
            <p className="meeting-detail-empty">저장된 캡쳐가 없습니다.</p>
          )}
          {canSelectPhotos && photos.length > 0 && (
            <div className="meeting-photo-grid">
              {photos.map((photo) => {
                const isSelected = selectedPhotoIds.has(photo.id);
                return (
                  <label key={photo.id} className={`meeting-photo-card ${isSelected ? 'selected' : ''}`}>
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => handleTogglePhoto(photo.id)}
                    />
                    <img src={resolveImageUrl(photo.imageUrl)} alt="미팅 캡쳐" />
                    <div className="meeting-photo-card__footer">
                      <span>{new Date(photo.capturedAt).toLocaleTimeString()}</span>
                      <span>{isSelected ? '선택됨' : '선택'}</span>
                    </div>
                  </label>
                );
              })}
            </div>
          )}
        </div>
      </section>

      <section className="meeting-detail-card">
        <div className="meeting-detail-card__header">
          <h3>채팅 기록</h3>
        </div>
        <div className="meeting-detail-card__body">
          {chatMessages.length === 0 ? (
            <p className="meeting-detail-empty">채팅 기록이 없습니다.</p>
          ) : (
            <div className="meeting-chat-history">
              {chatMessages.map((message) => (
                <div key={message.id} className="meeting-chat-history__item">
                  <span>{message.senderName}</span>
                  <p>{message.content}</p>
                  <time>{new Date(message.sentAt).toLocaleString()}</time>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default MeetingDetailPanel;


