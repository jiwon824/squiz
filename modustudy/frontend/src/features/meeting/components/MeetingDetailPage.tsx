import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
import { useUIStore } from '@/store/uiStore';
import { meetingApi } from '../services/meetingApi';
import {
    MeetingActionItemRequest,
    MeetingActionItemResponse,
    MeetingAudioRecordingResponse,
    MeetingChatMessageResponse,
    MeetingDetailResponse,
    MeetingPhotoResponse,
    MeetingRecordingResponse,
    MeetingSttFileResponse,
    MeetingSttSummaryResponse,
} from '../types';
import MeetingActionItems from './MeetingActionItems';
import MeetingSummaryPanel from './MeetingSummaryPanel';
import MeetingRecordingPanel from './MeetingRecordingPanel';
import '../styles/MeetingDetail.css';
import '../styles/MeetingShared.css';

const MeetingDetailPage: React.FC = () => {
    const { studyId, meetingId } = useParams();
    const numericStudyId = Number(studyId);
    const numericMeetingId = Number(meetingId);
    const navigate = useNavigate();
    const showToast = useUIStore((state) => state.showToast);
    const [detail, setDetail] = useState<MeetingDetailResponse | null>(null);
    const [actionItems, setActionItems] = useState<MeetingActionItemResponse[]>([]);
    const [recording, setRecording] = useState<MeetingRecordingResponse | null>(null);
    const [audioRecordings, setAudioRecordings] = useState<MeetingAudioRecordingResponse[]>([]);
    const [sttFile, setSttFile] = useState<MeetingSttFileResponse | null>(null);
    const [summaryFile, setSummaryFile] = useState<MeetingSttSummaryResponse | null>(null);
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
            setActionItems(meetingDetail.summary?.actionItems ?? []);

            const [recordingData, audioData, chatData] = await Promise.all([
                meetingApi.getRecording(numericStudyId, numericMeetingId).catch(() => null),
                meetingApi.listAudioRecordings(numericStudyId, numericMeetingId).catch(() => []),
                meetingApi.getChatHistory(numericStudyId, numericMeetingId, { page: 0, size: 100 }).catch(() => null),
            ]);
            if (recordingData) setRecording(recordingData);
            if (audioData) setAudioRecordings(audioData);
            if (chatData) setChatMessages(chatData.content);

            const stt = await meetingApi
                .getSttFile(numericStudyId, numericMeetingId, { trackType: 'MIXED' })
                .catch(() => null);
            const summary = await meetingApi
                .getSummaryFile(numericStudyId, numericMeetingId, { trackType: 'MIXED' })
                .catch(() => null);
            if (stt) setSttFile(stt);
            if (summary) setSummaryFile(summary);

            const photoList = await meetingApi.getPhotos(numericStudyId, numericMeetingId).catch(() => []);
            setPhotos(photoList);
        };
        load();
    }, [numericStudyId, numericMeetingId]);

    const handleAddActionItem = async (payload: MeetingActionItemRequest) => {
        if (!numericStudyId || !numericMeetingId) return;
        const created = await meetingApi.addActionItem(numericStudyId, numericMeetingId, payload);
        setActionItems((prev) => [...prev, created]);
    };

    const handleUpdateActionItem = async (id: number, payload: MeetingActionItemRequest) => {
        if (!numericStudyId || !numericMeetingId) return;
        const updated = await meetingApi.updateActionItem(numericStudyId, numericMeetingId, id, payload);
        setActionItems((prev) => prev.map((item) => (item.id === id ? updated : item)));
    };

    const handleExport = async (format: 'MARKDOWN' | 'PDF') => {
        if (!numericStudyId || !numericMeetingId) return;
        if (photos.length > 0 && !selectedPhotoId) {
            showToast('회의 사진을 선택해 주세요.', 'warning');
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
                            {detail?.startedAt ? new Date(detail.startedAt).toLocaleString() : '시작 전'}
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
                <MeetingActionItems
                    actionItems={actionItems}
                    participants={detail?.participants ?? []}
                    onAdd={handleAddActionItem}
                    onUpdate={handleUpdateActionItem}
                />
                <MeetingRecordingPanel
                    recording={recording}
                    audioRecordings={audioRecordings}
                    sttFile={sttFile}
                    summaryFile={summaryFile}
                />

                <section className="meeting-detail-card">
                    <div className="meeting-detail-card__header">
                        <h3>회의 사진</h3>
                        {selectedPhotoId ? <span className="meeting-status-chip">선택됨</span> : null}
                    </div>
                    <div className="meeting-detail-card__body">
                        {!canSelectPhotos && (
                            <p className="meeting-detail-empty">미팅 종료 후 사진을 선택할 수 있습니다.</p>
                        )}
                        {canSelectPhotos && photos.length === 0 && (
                            <p className="meeting-detail-empty">캡쳐된 회의 사진이 없습니다.</p>
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
                                        title={photo.isSelected ? '선택된 사진' : '이 사진을 선택'}
                                    >
                                        <img src={resolveImageUrl(photo.imageUrl)} alt="회의 사진" />
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
