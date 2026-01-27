import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
import { meetingApi } from '../services/meetingApi';
import {
    MeetingChatMessageResponse,
    MeetingDetailResponse,
    MeetingRecordingResponse,
} from '../types';
import '../styles/MeetingRecordingPlayback.css';
import '../styles/MeetingShared.css';

interface TimedChatMessage extends MeetingChatMessageResponse {
    offsetSeconds: number;
    timeLabel: string;
}

const formatOffset = (seconds: number) => {
    const safeSeconds = Math.max(0, Math.floor(seconds));
    const mins = Math.floor(safeSeconds / 60);
    const secs = safeSeconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

const MeetingRecordingPlaybackPage: React.FC = () => {
    const { studyId, meetingId } = useParams();
    const numericStudyId = Number(studyId);
    const numericMeetingId = Number(meetingId);
    const navigate = useNavigate();
    const videoRef = useRef<HTMLVideoElement | null>(null);
    const chatScrollRef = useRef<HTMLDivElement | null>(null);
    const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
    const resolveMediaUrl = (url: string) => {
        if (!url) return url;
        if (url.startsWith('http://') || url.startsWith('https://')) return url;
        if (!apiBaseUrl) return url;
        if (url.startsWith('/')) return `${apiBaseUrl}${url}`;
        return `${apiBaseUrl}/${url}`;
    };

    const [detail, setDetail] = useState<MeetingDetailResponse | null>(null);
    const [recording, setRecording] = useState<MeetingRecordingResponse | null>(null);
    const [chatMessages, setChatMessages] = useState<MeetingChatMessageResponse[]>([]);
    const [playheadSeconds, setPlayheadSeconds] = useState(0);

    useEffect(() => {
        if (!numericStudyId || !numericMeetingId) return;
        const load = async () => {
            const [meetingDetail, recordingData, chatData] = await Promise.all([
                meetingApi.getMeetingDetail(numericStudyId, numericMeetingId).catch(() => null),
                meetingApi.getRecording(numericStudyId, numericMeetingId).catch(() => null),
                meetingApi.getChatHistory(numericStudyId, numericMeetingId, { page: 0, size: 500 }).catch(() => null),
            ]);
            if (meetingDetail) setDetail(meetingDetail);
            if (recordingData) setRecording(recordingData);
            if (chatData) setChatMessages(chatData.content);
        };
        load();
    }, [numericStudyId, numericMeetingId]);

    const messageTimeline = useMemo<TimedChatMessage[]>(() => {
        if (chatMessages.length === 0) return [];
        const baseTimestamp = recording?.startedAt || detail?.startedAt || chatMessages[0]?.sentAt;
        const baseMs = baseTimestamp ? new Date(baseTimestamp).getTime() : null;

        return chatMessages
            .map((message) => {
                const sentMs = new Date(message.sentAt).getTime();
                const offsetSeconds = baseMs ? Math.max(0, (sentMs - baseMs) / 1000) : 0;
                return {
                    ...message,
                    offsetSeconds,
                    timeLabel: formatOffset(offsetSeconds),
                };
            })
            .sort((a, b) => a.offsetSeconds - b.offsetSeconds);
    }, [chatMessages, detail?.startedAt, recording?.startedAt]);

    const visibleMessages = useMemo(() => {
        if (!messageTimeline.length) return [];
        return messageTimeline.filter((message) => message.offsetSeconds <= playheadSeconds + 0.4);
    }, [messageTimeline, playheadSeconds]);

    useEffect(() => {
        if (!chatScrollRef.current) return;
        chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }, [visibleMessages.length]);

    const handleTimeUpdate = () => {
        const currentTime = videoRef.current?.currentTime ?? 0;
        setPlayheadSeconds(currentTime);
    };

    const handleSeekTo = (seconds: number) => {
        if (!videoRef.current) return;
        videoRef.current.currentTime = seconds;
        void videoRef.current.play().catch(() => undefined);
    };

    const recordingTitle = detail?.title || `미팅 ${numericMeetingId}`;
    const recordingUrl = resolveMediaUrl(recording?.recordingUrl ?? '');
    const hasRecording = Boolean(recordingUrl);

    return (
        <MainLayout>
            <div className="meeting-recording-page">
                <div className="meeting-recording-header">
                    <div>
                        <p className="meeting-recording-kicker">녹화 다시보기</p>
                        <h1>{recordingTitle}</h1>
                        <span className="meeting-recording-subtitle">
                            {detail?.startedAt ? new Date(detail.startedAt).toLocaleString() : '시작 시간 미정'}
                        </span>
                    </div>
                    <button
                        type="button"
                        className="meeting-btn ghost"
                        onClick={() => navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`)}
                    >
                        요약으로 돌아가기
                    </button>
                </div>

                <div className="meeting-recording-layout">
                    <section className="meeting-recording-video">
                        <div className="meeting-recording-video__frame">
                            {hasRecording ? (
                                <video
                                    ref={videoRef}
                                    controls
                                    preload="metadata"
                                    onTimeUpdate={handleTimeUpdate}
                                    src={recordingUrl || undefined}
                                />
                            ) : (
                                <div className="meeting-recording-video__empty">
                                    <p>녹화 파일이 아직 준비되지 않았어요.</p>
                                </div>
                            )}
                        </div>
                        <div className="meeting-recording-video__meta">
                            <div>
                                <span className="meeting-recording-label">상태</span>
                                <p>{recording?.status ?? '알 수 없음'}</p>
                            </div>
                            <div>
                                <span className="meeting-recording-label">길이</span>
                                <p>
                                    {recording?.durationSeconds
                                        ? formatOffset(recording.durationSeconds)
                                        : '미정'}
                                </p>
                            </div>
                        </div>
                    </section>

                    <section className="meeting-recording-chat">
                        <div className="meeting-recording-chat__header">
                            <div>
                                <h3>채팅 다시보기</h3>
                                <p>
                                    {visibleMessages.length} / {messageTimeline.length} 메시지
                                </p>
                            </div>
                            <button
                                type="button"
                                className="meeting-btn ghost"
                                onClick={() => handleSeekTo(playheadSeconds + 10)}
                                disabled={!hasRecording}
                            >
                                +10초 이동
                            </button>
                        </div>
                        <div className="meeting-recording-chat__messages" ref={chatScrollRef}>
                            {messageTimeline.length === 0 && (
                                <div className="meeting-recording-chat__empty">
                                    <p>녹화된 채팅 메시지가 없어요.</p>
                                </div>
                            )}
                            {messageTimeline.length > 0 && visibleMessages.length === 0 && (
                                <div className="meeting-recording-chat__empty">
                                    <p>재생을 누르면 채팅이 함께 올라옵니다.</p>
                                </div>
                            )}
                            {visibleMessages.map((message) => (
                                <button
                                    key={message.id}
                                    type="button"
                                    className="meeting-recording-chat__item"
                                    onClick={() => handleSeekTo(message.offsetSeconds)}
                                >
                                    <div className="meeting-recording-chat__meta">
                                        <span>{message.senderName}</span>
                                        <span>{message.timeLabel}</span>
                                    </div>
                                    <p>{message.content}</p>
                                </button>
                            ))}
                        </div>
                    </section>
                </div>
            </div>
        </MainLayout>
    );
};

export default MeetingRecordingPlaybackPage;
