import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { meetingApi } from '../services/meetingApi';
import { MeetingListItemResponse } from '../types';
import '../styles/MeetingHistory.css';
import '../styles/MeetingShared.css';

interface MeetingHistoryPanelProps {
  studyId: number;
}

const MeetingHistoryPanel: React.FC<MeetingHistoryPanelProps> = ({ studyId }) => {
  const navigate = useNavigate();
  const [meetings, setMeetings] = useState<MeetingListItemResponse[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const pageSize = 10;
  const pageGroupSize = 5;

  const fetchMeetings = useCallback(async () => {
    if (!studyId) return;
    setIsLoading(true);
    try {
      const response = await meetingApi.listMeetings(studyId, {
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
  }, [studyId, startDate, endDate, page]);

  useEffect(() => {
    fetchMeetings();
  }, [fetchMeetings]);

  return (
    <div className="meeting-history">
      <div className="meeting-history__header">
        <div className="meeting-history__header-left">
          <div>
            <h1>미팅 기록</h1>
            <p className="meeting-history__subtitle">날짜별로 미팅을 확인해보세요.</p>
          </div>
        </div>
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
                    onClick={() => navigate(`/study/${studyId}/meetings/${meeting.id}`)}
                  >
                    기록 보기
                  </button>
                )}
                <button
                  className="meeting-btn primary"
                  onClick={() => navigate(`/study/${studyId}/meetings/${meeting.id}/room`)}
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
  );
};

export default MeetingHistoryPanel;
