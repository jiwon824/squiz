import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
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
    const [detail, setDetail] = useState<MeetingDetailResponse | null>(null);
    const [chatMessages, setChatMessages] = useState<MeetingChatMessageResponse[]>([]);
    const [photos, setPhotos] = useState<MeetingPhotoResponse[]>([]);
    const [selectingPhotoId, setSelectingPhotoId] = useState<number | null>(null);
    const selectedPhotoId = photos.find((photo) => photo.isSelected)?.id ?? null;
    const canSelectPhotos = detail?.status === 'ENDED';
    const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
    const resolveImageUrl = (url: string) => {
        if (!url) return url;
        if (url.startsWith('http://') || url.startsWith('https://')) return url;
        if (!apiBaseUrl) return url;
        if (url.startsWith('/')) return `${apiBaseUrl}${url}`;
        return `${apiBaseUrl}/${url}`;
    };

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

    const handleExport = async (format: 'MARKDOWN' | 'PDF') => {
        if (!numericStudyId || !numericMeetingId) return;
        if (photos.length > 0 && !selectedPhotoId) {
            window.alert('미팅 사진을 선택해 주세요.');
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

    const handleSelectPhoto = async (photoId: number) => {
        if (!numericStudyId || !numericMeetingId || selectingPhotoId !== null || !canSelectPhotos) return;
        setSelectingPhotoId(photoId);
        try {
            const selected = await meetingApi.selectPhoto(numericStudyId, numericMeetingId, photoId);
            setPhotos((prev) =>
                prev.map((photo) => ({
                    ...photo,
                    isSelected: photo.id === selected.id,
                }))
            );
        } catch (error) {
            console.error('Failed to select meeting photo', error);
        } finally {
            setSelectingPhotoId(null);
        }
    };

    return (
        <MainLayout>
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
                        <button
                            className="meeting-btn ghost"
                            onClick={() => navigate(`/study/${numericStudyId}/meetings`)}
                        >
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
                        {selectedPhotoId ? <span className="meeting-status-chip">선택됨</span> : null}
                    </div>
                    <div className="meeting-detail-card__body">
                        {!canSelectPhotos && (
                            <p className="meeting-detail-empty">미팅 종료 후에 사진을 선택할 수 있습니다.</p>
                        )}
                        {canSelectPhotos && photos.length === 0 && (
                            <p className="meeting-detail-empty">저장된 캡처가 없습니다.</p>
                        )}
                        {canSelectPhotos && photos.length > 0 && (
                            <div className="meeting-photo-grid">
                                {photos.map((photo) => (
                                    <button
                                        key={photo.id}
                                        type="button"
                                        className={`meeting-photo-card ${photo.isSelected ? 'selected' : ''}`}
                                        onClick={() => handleSelectPhoto(photo.id)}
                                        disabled={selectingPhotoId === photo.id}
                                        title={photo.isSelected ? '선택된 사진' : '사진 선택'}
                                    >
                                        <img src={resolveImageUrl(photo.imageUrl)} alt="미팅 캡처" />
                                        <div className="meeting-photo-card__footer">
                                            <span>{new Date(photo.capturedAt).toLocaleTimeString()}</span>
                                            <span>{photo.isSelected ? '선택됨' : '선택'}</span>
                                        </div>
                                    </button>
                                ))}
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
        </MainLayout>
    );
};

export default MeetingDetailPage;
