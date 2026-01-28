import { useState, useEffect, useCallback } from 'react';
import { type UnifiedSchedule } from '@/features/calendar';
import { sessionApi, type StudySessionResponse } from '@/api/endpoints/sessionApi';
import { SessionModal } from './SessionModal';
import { WorkspaceCalendar } from './WorkspaceCalendar';
import { useUIStore } from '@/store/uiStore';
import { Button, Modal } from '@/shared/components';
import { ChevronLeft, ChevronRight, Plus, Calendar as CalendarIcon, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface WorkspaceCalendarAreaProps {
  studyId: number;
}

/**
 * 세션 응답을 UnifiedSchedule로 변환
 */
const toUnifiedSchedule = (session: StudySessionResponse): UnifiedSchedule => {
  // scheduledAt에서 날짜 추출
  const dt = new Date(session.scheduledAt);
  const dateStr = dt.toISOString().split('T')[0];
  const timeStr = dt.toTimeString().slice(0, 5);

  // 상태별 색상
  const statusColors: Record<string, string> = {
    SCHEDULED: '#4285F4', // 파랑
    IN_PROGRESS: '#FBBC05', // 노랑
    COMPLETED: '#34A853', // 초록
    CANCELLED: '#EA4335', // 빨강
  };

  return {
    id: session.id,
    title: session.title || `${session.sessionNumber}회차`,
    description: session.description || undefined,
    startDate: dateStr,
    startTime: timeStr,
    durationMinutes: session.durationMinutes || undefined,
    location: session.location || undefined,
    isOnline: session.isOnline,
    source: 'study',
    status: session.status,
    studyId: session.studyId,
    sessionNumber: session.sessionNumber,
    color: statusColors[session.status] || '#4285F4',
    createdAt: session.createdAt,
  };
};

/**
 * 워크스페이스 캘린더 영역
 */
export const WorkspaceCalendarArea: React.FC<WorkspaceCalendarAreaProps> = ({
  studyId,
}) => {
  const showToast = useUIStore((state) => state.showToast);

  // 상태
  const [currentDate, setCurrentDate] = useState(new Date());
  const [sessions, setSessions] = useState<StudySessionResponse[]>([]);
  const [schedules, setSchedules] = useState<UnifiedSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<StudySessionResponse | null>(null);
  const [isSessionModalOpen, setIsSessionModalOpen] = useState(false);
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);

  // 세션 목록 로드
  const loadSessions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await sessionApi.getSessions(studyId);
      setSessions(data);
      setSchedules(data.map(toUnifiedSchedule));
    } catch (err: any) {
      // 세션이 없는 경우 빈 배열로 처리
      setSessions([]);
      setSchedules([]);
    } finally {
      setLoading(false);
    }
  }, [studyId]);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  // 이전/다음 달 이동
  const goToPrevMonth = () => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      d.setMonth(d.getMonth() - 1);
      return d;
    });
  };

  const goToNextMonth = () => {
    setCurrentDate((prev) => {
      const d = new Date(prev);
      d.setMonth(d.getMonth() + 1);
      return d;
    });
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  // 날짜 클릭 핸들러 - 모달로 표시
  const handleDateClick = (date: string) => {
    setSelectedDate(date);
    setIsDateModalOpen(true);
  };

  // 빠른 추가 핸들러
  const handleQuickAdd = (date: string) => {
    setSelectedDate(date);
    setSelectedSession(null);
    setIsDateModalOpen(false);
    setIsSessionModalOpen(true);
  };

  // 이벤트(세션) 클릭 핸들러
  const handleEventClick = (schedule: UnifiedSchedule) => {
    const session = sessions.find((s) => s.id === schedule.id);
    if (session) {
      setSelectedSession(session);
      setIsDateModalOpen(false);
      setIsSessionModalOpen(true);
    }
  };

  // 새 세션 추가 버튼
  const handleAddSession = () => {
    setSelectedDate(null);
    setSelectedSession(null);
    setIsSessionModalOpen(true);
  };

  // 세션 모달 닫기
  const handleSessionModalClose = () => {
    setIsSessionModalOpen(false);
    setSelectedSession(null);
  };

  // 날짜 모달 닫기
  const handleDateModalClose = () => {
    setIsDateModalOpen(false);
    setSelectedDate(null);
  };

  // 세션 저장 성공 시
  const handleSessionSuccess = () => {
    loadSessions();
  };

  // 월/년 표시
  const monthYear = currentDate.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
  });

  // 선택된 날짜의 일정 목록
  const selectedDateSchedules = selectedDate
    ? schedules.filter((s) => s.startDate === selectedDate)
    : [];

  return (
    <div className="workspace-calendar">
      {/* 캘린더 헤더 */}
      <div className="workspace-calendar__header">
        <div className="workspace-calendar__nav">
          <button
            className="workspace-calendar__nav-btn"
            onClick={goToPrevMonth}
            title="이전 달"
          >
            <ChevronLeft size={20} />
          </button>
          <h2 className="workspace-calendar__title">{monthYear}</h2>
          <button
            className="workspace-calendar__nav-btn"
            onClick={goToNextMonth}
            title="다음 달"
          >
            <ChevronRight size={20} />
          </button>
        </div>

        <div className="workspace-calendar__actions">
          <Button
            variant="ghost"
            size="sm"
            onClick={goToToday}
            className="text-gray-600"
          >
            오늘
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleAddSession}
            className="gap-1"
          >
            세션 추가하기
          </Button>
        </div>
      </div>

      {/* 캘린더 본체 */}
      <div className="workspace-calendar__body">
        <WorkspaceCalendar
          currentDate={currentDate}
          schedules={schedules}
          onDateClick={handleDateClick}
          onQuickAdd={handleQuickAdd}
          loading={loading}
          className="w-full"
        />
      </div>

      {/* 날짜 상세 모달 */}
      <Modal isOpen={isDateModalOpen} onClose={handleDateModalClose} maxWidth="sm">
        <div className="-m-8 p-4">
          {/* 모달 헤더 */}
          <div className="flex items-center justify-between mb-3">
            <h3 className="flex items-center gap-1.5 text-base font-bold text-gray-900">
              <CalendarIcon size={16} className="text-blue-500" />
              {selectedDate &&
                new Date(selectedDate).toLocaleDateString('ko-KR', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  weekday: 'long',
                })}
            </h3>
            <button
              onClick={handleDateModalClose}
              className="p-1 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <X size={16} className="text-gray-500" />
            </button>
          </div>

          {/* 일정 목록 */}
          <div className="space-y-1.5 max-h-[200px] overflow-y-auto">
            {selectedDateSchedules.length > 0 ? (
              selectedDateSchedules.map((schedule) => {
                const session = sessions.find((s) => s.id === schedule.id);
                return (
                  <div
                    key={schedule.id}
                    className={cn(
                      'flex items-center gap-2 p-2 rounded-lg cursor-pointer transition-colors',
                      'bg-gray-50 hover:bg-gray-100'
                    )}
                    onClick={() => session && handleEventClick(schedule)}
                  >
                    <div
                      className="w-2 h-2 rounded-full flex-shrink-0"
                      style={{ backgroundColor: schedule.color }}
                    />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {schedule.title}
                      </p>
                      {schedule.startTime && (
                        <p className="text-xs text-gray-500">{schedule.startTime}</p>
                      )}
                    </div>
                    <span
                      className={cn(
                        'text-[10px] font-medium px-1.5 py-0.5 rounded',
                        schedule.status === 'SCHEDULED' && 'bg-blue-100 text-blue-700',
                        schedule.status === 'IN_PROGRESS' && 'bg-yellow-100 text-yellow-700',
                        schedule.status === 'COMPLETED' && 'bg-green-100 text-green-700',
                        schedule.status === 'CANCELLED' && 'bg-red-100 text-red-700'
                      )}
                    >
                      {schedule.status === 'SCHEDULED' && '예정'}
                      {schedule.status === 'IN_PROGRESS' && '진행 중'}
                      {schedule.status === 'COMPLETED' && '완료'}
                      {schedule.status === 'CANCELLED' && '취소됨'}
                    </span>
                  </div>
                );
              })
            ) : (
              <div className="text-center py-6 text-gray-500">
                <CalendarIcon size={24} className="mx-auto mb-1.5 opacity-50" />
                <p className="text-sm">이 날짜에 예정된 세션이 없습니다.</p>
              </div>
            )}
          </div>

          {/* 세션 추가 버튼 */}
          <div className="mt-3 pt-3 border-t">
            <Button
              variant="primary"
              size="sm"
              className="w-full gap-1.5 text-sm"
              onClick={() => selectedDate && handleQuickAdd(selectedDate)}
            >
              이 날짜에 세션 추가
            </Button>
          </div>
        </div>
      </Modal>

      {/* 세션 생성/편집 모달 */}
      <SessionModal
        isOpen={isSessionModalOpen}
        onClose={handleSessionModalClose}
        studyId={studyId}
        session={selectedSession}
        initialDate={selectedDate || undefined}
        onSuccess={handleSessionSuccess}
      />
    </div>
  );
};
