import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
import { useAuthStore } from '@/store/authStore';
import MeetingStartModal from './MeetingStartModal';
import { meetingApi } from '../services/meetingApi';
import { MeetingListItemResponse, MeetingRequestPayload } from '../types';
import '../styles/MeetingHistory.css';
import '../styles/MeetingShared.css';

const MeetingHistoryPage: React.FC = () => {
    const { studyId } = useParams();
    const numericStudyId = Number(studyId);
    const navigate = useNavigate();
    const { user } = useAuthStore();
    const ownerKey = user?.id ?? user?.name ?? 'guest';
    const [meetings, setMeetings] = useState<MeetingListItemResponse[]>([]);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showStartModal, setShowStartModal] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const pageSize = 10;
    const pageGroupSize = 5;

    const fetchMeetings = useCallback(async () => {
        if (!numericStudyId) return;
        setIsLoading(true);
        try {
            const response = await meetingApi.listMeetings(numericStudyId, {
                startDate: startDate || undefined,
                endDate: endDate || undefined,
                page,
                size: pageSize,
            });
            setMeetings(response.content);
            setTotalPages(Math.max(1, response.totalPages || 1));
        } finally {
            setIsLoading(false);
        }
    }, [numericStudyId, startDate, endDate, page]);

    useEffect(() => {
        fetchMeetings();
    }, [fetchMeetings]);

    const handleStartMeeting = async (payload: MeetingRequestPayload) => {
        if (!numericStudyId) return;
        try {
            const meeting = await meetingApi.startMeeting(numericStudyId, payload);
            localStorage.setItem(`meeting-owner-${meeting.id}`, String(ownerKey));
            setShowStartModal(false);
            navigate(`/study/${numericStudyId}/meetings/${meeting.id}/room`);
        } catch (error) {
            const status = (error as { response?: { status?: number; data?: { message?: string } } })?.response?.status;
            const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
            if (status === 409 && message === 'MEETING_IN_PROGRESS') {
                alert('이미 진행 중인 미팅이 있습니다. 기존 미팅을 종료한 뒤 다시 시도해주세요.');
                return;
            }
            alert('미팅 시작에 실패했습니다. 백엔드 연결과 CORS 설정을 확인해주세요.');
            console.error(error);
        }
    };

    return (
        <MainLayout>
            <div className="meeting-history">
                <div className="meeting-history__header">
                    <div>
                        <h1>미팅 기록</h1>
                        <p className="meeting-history__subtitle">날짜별로 미팅을 확인해보세요.</p>
                    </div>
                    <button className="meeting-btn primary" onClick={() => setShowStartModal(true)}>
                        미팅 시작
                    </button>
                </div>

                <div className="meeting-history__filters">
                    <input
                        type="date"
                        value={startDate}
                        onChange={(event) => {
                            setStartDate(event.target.value);
                            setPage(0);
                        }}
                    />
                    <input
                        type="date"
                        value={endDate}
                        onChange={(event) => {
                            setEndDate(event.target.value);
                            setPage(0);
                        }}
                    />
                    <button
                        className="meeting-btn ghost"
                        onClick={() => {
                            if (page !== 0) {
                                setPage(0);
                                return;
                            }
                            fetchMeetings();
                        }}
                    >
                        필터 적용
                    </button>
                </div>

                {isLoading ? (
                    <p className="meeting-history__empty">불러오는 중...</p>
                ) : (
                    <div className="meeting-history__list">
                        {meetings.map((meeting) => (
                            <div key={meeting.id} className="meeting-history__card">
                                <div>
                                    <h3 className="meeting-history__title">
                                        {meeting.title || '미팅'}
                                        {!meeting.endedAt && (
                                            <span className="meeting-history__badge meeting-history__badge--active">
                                                진행 중
                                            </span>
                                        )}
                                    </h3>
                                    <p>
                                        {meeting.startedAt
                                            ? new Date(meeting.startedAt).toLocaleString()
                                            : '시작 전'}
                                        {meeting.endedAt && ` ~ ${new Date(meeting.endedAt).toLocaleTimeString()}`}
                                    </p>
                                </div>
                                <div className="meeting-history__actions">
                                    {meeting.endedAt && (
                                        <button
                                            className="meeting-btn ghost"
                                            onClick={() => navigate(`/study/${numericStudyId}/meetings/${meeting.id}`)}
                                        >
                                            기록 보기
                                        </button>
                                    )}
                                    <button
                                        className="meeting-btn primary"
                                        onClick={() => navigate(`/study/${numericStudyId}/meetings/${meeting.id}/room`)}
                                        disabled={Boolean(meeting.endedAt)}
                                    >
                                        {meeting.endedAt ? '미팅 종료' : '입장'}
                                    </button>
                                </div>
                            </div>
                        ))}
                        {meetings.length === 0 && <p className="meeting-history__empty">미팅 기록이 없습니다.</p>}
                    </div>
                )}
                {totalPages > 1 && (
                    <div className="meeting-history__pagination">
                        <button
                            className="meeting-page-btn meeting-page-btn--nav"
                            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                            disabled={page === 0}
                        >
                            이전
                        </button>
                        {Array.from({ length: Math.min(pageGroupSize, totalPages) }, (_, offset) => {
                            const groupStart = Math.floor(page / pageGroupSize) * pageGroupSize;
                            const index = groupStart + offset;
                            if (index >= totalPages) return null;
                            const displayPage = index + 1;
                            return (
                                <button
                                    key={displayPage}
                                    className={`meeting-page-btn ${page === index ? 'active' : ''}`}
                                    onClick={() => setPage(index)}
                                    disabled={page === index}
                                >
                                    {displayPage}
                                </button>
                            );
                        })}
                        <button
                            className="meeting-page-btn meeting-page-btn--nav"
                            onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
                            disabled={page >= totalPages - 1}
                        >
                            다음
                        </button>
                    </div>
                )}
            </div>

            <MeetingStartModal
                open={showStartModal}
                onClose={() => setShowStartModal(false)}
                onStart={handleStartMeeting}
            />
        </MainLayout>
    );
};

export default MeetingHistoryPage;



