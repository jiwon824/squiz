import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { PageNavHeader } from '@/shared/components/layouts/PageNavHeader';
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

const MeetingDetailPage: React.FC = () => {
    const { studyId, meetingId } = useParams();
    const numericStudyId = Number(studyId);
    const numericMeetingId = Number(meetingId);
    const navigate = useNavigate();
    const showToast = useUIStore((state) => state.showToast);
    const [detail, setDetail] = useState<MeetingDetailResponse | null>(null);
    const [chatMessages, setChatMessages] = useState<MeetingChatMessageResponse[]>([]);
    const [photos, setPhotos] = useState<MeetingPhotoResponse[]>([]);
    const [selectedPhotoIds, setSelectedPhotoIds] = useState<Set<number>>(new Set());
    const [isSavingSelection, setIsSavingSelection] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);
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
        if (!numericMeetingId) return;
        const key = `meeting-end-reload-${numericMeetingId}`;
        if (sessionStorage.getItem(key) === '1') {
            sessionStorage.removeItem(key);
            window.location.reload();
        }
    }, [numericMeetingId]);

    useEffect(() => {
        if (!numericStudyId || !numericMeetingId) return;
        const load = async () => {
            const meetingDetail = await meetingApi.getMeetingDetail(numericStudyId, numericMeetingId);
            setDetail(meetingDetail);
            const chatData = await meetingApi
                .getChatHistory(numericStudyId, numericMeetingId, { page: 0, size: 100 })
                .catch(() => null);
            if (chatData) setChatMessages(chatData.content);

            const photoList = await meetingApi.getPhotos(numericStudyId, numericMeetingId).catch(() => []);
            setPhotos(photoList);
        };
        load();
    }, [numericStudyId, numericMeetingId]);

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
        if (!numericStudyId || !numericMeetingId) return;
        const savedSelection = photos.filter((photo) => photo.isSelected);
        if (photos.length > 0 && savedSelection.length === 0) {
            window.alert('보고서용 이미지로 선택한 사진이 없습니다.');
            return;
        }
        const blob = await meetingApi.exportMeeting(numericStudyId, numericMeetingId, format);
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `meeting-${numericMeetingId}.${format === 'MARKDOWN' ? 'md' : 'pdf'}`;
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
        if (!numericStudyId || !numericMeetingId || !canSelectPhotos || isSavingSelection) return;
        if (selectedPhotoIds.size === 0) {
            showToast('이미지 선택 후 클릭해주세요.', 'warning');
            return;
        }
        setIsSavingSelection(true);
        try {
            const updated = await meetingApi.selectPhotos(
                numericStudyId,
                numericMeetingId,
                Array.from(selectedPhotoIds)
            );
            setPhotos(updated);
            showToast('보고서에 이미지가 첨부되었습니다.', 'success');
        } catch (error) {
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
                    const filename = `meeting-${numericMeetingId}-photo-${photo.id}${extension}`;
                    const linkUrl = URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = linkUrl;
                    link.download = filename;
                    link.click();
                    URL.revokeObjectURL(linkUrl);
                } catch (error) {
                    window.open(url, '_blank', 'noopener,noreferrer');
                }
            }
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <UserLayoutV2>
            <div className="meeting-detail">
                <PageNavHeader
                    title={detail?.title || '회의 상세'}
                    breadcrumbs={[
                        { label: '스터디', path: `/study/${numericStudyId}` },
                        { label: '회의 상세' },
                    ]}
                    onBack={() => navigate(`/study/${numericStudyId}/meetings`)}
                    rightActions={
                        <div className="meeting-detail__exports">
                            <button className="meeting-btn ghost" onClick={() => handleExport('MARKDOWN')}>
                                Markdown 내보내기
                            </button>
                            <button className="meeting-btn ghost" onClick={() => handleExport('PDF')}>
                                PDF 내보내기
                            </button>
                        </div>
                    }
                />

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
                                        <label
                                            key={photo.id}
                                            className={`meeting-photo-card ${isSelected ? 'selected' : ''}`}
                                        >
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
        </UserLayoutV2>
    );
};

export default MeetingDetailPage;
