import { useState, useEffect, useCallback, useMemo } from 'react';
import { type UnifiedSchedule } from '@/features/calendar';
import { formatDate } from '@/features/calendar/utils';
import { sessionApi, type StudySessionResponse } from '@/api/endpoints/sessionApi';
import { studyApi } from '@/api/endpoints/studyApi';
import { SessionModal } from './SessionModal';
import { WorkspaceCalendar } from './WorkspaceCalendar';
import { useUIStore } from '@/store/uiStore';
import { Button, Modal } from '@/shared/components';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon, X, Trash2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface WorkspaceCalendarAreaProps {
  studyId: number;
  isLeader?: boolean;
  /** 세션 변경 시 부모 컴포넌트에 알림 (사이드바 미팅 버튼 갱신용) */
  onSessionChange?: () => void;
}

/**
 * 현재 시간 기준으로 세션 상태 계산
 */
const calculateSessionStatus = (
  session: StudySessionResponse
): 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' => {
  // 취소된 세션은 그대로 유지
  if (session.status === 'CANCELLED') {
    return 'CANCELLED';
  }

  const now = new Date();
  const startTime = new Date(session.scheduledAt);
  const durationMs = (session.durationMinutes || 60) * 60 * 1000;
  const endTime = new Date(startTime.getTime() + durationMs);

  if (now < startTime) {
    return 'SCHEDULED'; // 예정
  } else if (now >= startTime && now < endTime) {
    return 'IN_PROGRESS'; // 진행 중
  } else {
    return 'COMPLETED'; // 종료
  }
};

/**
 * 세션 응답을 UnifiedSchedule로 변환
 */
const toUnifiedSchedule = (session: StudySessionResponse): UnifiedSchedule => {
  // scheduledAt에서 날짜 추출
  const dt = new Date(session.scheduledAt);
  const dateStr = formatDate(dt);
  const timeStr = dt.toTimeString().slice(0, 5);

  // 현재 시간 기준 상태 계산
  const calculatedStatus = calculateSessionStatus(session);

  // 상태별 색상
  const statusColors: Record<string, string> = {
    SCHEDULED: '#EA4335', // 파랑
    IN_PROGRESS: '#FBBC05', // 노랑
    COMPLETED: '#34A853', // 초록
    CANCELLED: '#9CA3AF', // 빨강
  };

  return {
    id: session.id,
    title: session.title || '스터디 세션',
    description: session.description || undefined,
    startDate: dateStr,
    startTime: timeStr,
    durationMinutes: session.durationMinutes || undefined,
    location: session.location || undefined,
    isOnline: session.isOnline,
    source: 'study',
    status: calculatedStatus,
    studyId: session.studyId,
    sessionNumber: session.sessionNumber,
    color: statusColors[calculatedStatus] || '#EA4335',
    createdAt: session.createdAt,
  };
};

/**
 * 워크스페이스 캘린더 영역
 */
export const WorkspaceCalendarArea: React.FC<WorkspaceCalendarAreaProps> = ({
  studyId,
  isLeader = false,
  onSessionChange,
}) => {
  const showToast = useUIStore((state) => state.showToast);

  // 상태
  const [currentDate, setCurrentDate] = useState(new Date());
  const [sessions, setSessions] = useState<StudySessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<StudySessionResponse | null>(null);
  const [isSessionModalOpen, setIsSessionModalOpen] = useState(false);
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  const [deletingSessionId, setDeletingSessionId] = useState<number | null>(null);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [attendanceBySessionId, setAttendanceBySessionId] = useState<
    Record<number, { status: string; excuseStatus?: string }>
  >({});
  const [isExcuseModalOpen, setIsExcuseModalOpen] = useState(false);
  const [excuseReason, setExcuseReason] = useState('');
  const [excuseTarget, setExcuseTarget] = useState<UnifiedSchedule | null>(null);
  const [excuseSubmitting, setExcuseSubmitting] = useState(false);

  // 세션 목록 로드
  const loadSessions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await sessionApi.getSessions(studyId);
      setSessions(data);
    } catch {
      // 세션이 없는 경우 빈 배열로 처리
      setSessions([]);
    } finally {
      setLoading(false);
    }
  }, [studyId]);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  const loadAttendanceCalendar = useCallback(async () => {
    try {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth() + 1;
      const response = await studyApi.getAttendanceCalendar(studyId, year, month);
      const payload = response?.data || response;
      const items = payload?.items || [];
      const nextMap: Record<number, { status: string; excuseStatus?: string }> = {};
      items.forEach((item: any) => {
        if (item?.sessionId) {
          nextMap[item.sessionId] = {
            status: item.status,
            excuseStatus: item.excuseStatus,
          };
        }
      });
      setAttendanceBySessionId(nextMap);
    } catch (error) {
      console.warn('출석 달력 조회 실패:', error);
      setAttendanceBySessionId({});
    }
  }, [currentDate, studyId]);

  useEffect(() => {
    loadAttendanceCalendar();
  }, [loadAttendanceCalendar]);

  // 10초마다 현재 시간 갱신 (세션 상태 실시간 업데이트)
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTime(new Date());
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  // 세션 상태를 실시간으로 계산 (currentTime이 바뀔 때마다 재계산)
  const schedules = useMemo(() => {
    void currentTime; // 의존성으로 사용
    return sessions.map((session) => {
      const base = toUnifiedSchedule(session);
      const attendanceInfo = attendanceBySessionId[session.id];
      return attendanceInfo
        ? {
            ...base,
            attendanceStatus: attendanceInfo.status as UnifiedSchedule['attendanceStatus'],
            excuseStatus: attendanceInfo.excuseStatus as UnifiedSchedule['excuseStatus'],
          }
        : base;
    });
  }, [sessions, currentTime, attendanceBySessionId]);

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

  const handleAbsentBadgeClick = (schedule: UnifiedSchedule) => {
    if (schedule.excuseStatus === 'APPROVED' || schedule.excuseStatus === 'REJECTED') {
      showToast?.('이미 소명이 처리된 출결입니다.', 'info');
      return;
    }
    setExcuseTarget(schedule);
    setExcuseReason('');
    setIsExcuseModalOpen(true);
  };

  const handleExcuseModalClose = () => {
    if (excuseSubmitting) return;
    setIsExcuseModalOpen(false);
    setExcuseReason('');
    setExcuseTarget(null);
  };

  const handleSubmitExcuse = async () => {
    if (!excuseTarget || excuseSubmitting) return;
    if (!excuseReason.trim()) {
      showToast?.('소명 사유를 입력해주세요.', 'warning');
      return;
    }
    setExcuseSubmitting(true);
    try {
      await studyApi.submitExcuse(studyId, Number(excuseTarget.id), excuseReason.trim());
      showToast?.('결석 소명이 제출되었습니다.', 'success');
      await loadAttendanceCalendar();
      handleExcuseModalClose();
    } catch (error) {
      console.error('결석 소명 제출 실패:', error);
      showToast?.('결석 소명 제출에 실패했습니다.', 'error');
    } finally {
      setExcuseSubmitting(false);
    }
  };

  // 세션 저장 성공 시
  const handleSessionSuccess = () => {
    loadSessions();
    loadAttendanceCalendar();
    onSessionChange?.(); // 부모에게 세션 변경 알림
  };

  // 세션 삭제 핸들러 (리더만 가능)
  const handleDeleteSession = async (e: React.MouseEvent, sessionId: number) => {
    e.stopPropagation();

    if (!isLeader) {
      showToast?.('스터디장만 세션을 삭제할 수 있습니다.', 'warning');
      return;
    }

    if (!window.confirm('정말 이 세션을 삭제하시겠습니까?')) {
      return;
    }

    setDeletingSessionId(sessionId);
    try {
      await sessionApi.deleteSession(studyId, sessionId);
      showToast?.('세션을 삭제했습니다.', 'success');
      loadSessions();
      loadAttendanceCalendar();
      onSessionChange?.(); // 부모에게 세션 변경 알림
    } catch (err: any) {
      const errorMessage = err?.response?.data?.message || '세션 삭제에 실패했습니다.';
      showToast?.(errorMessage, 'error');
    } finally {
      setDeletingSessionId(null);
    }
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
          {/* 세션 추가 버튼 (리더만 표시) */}
          {isLeader && (
            <Button
              variant="primary"
              size="sm"
              onClick={handleAddSession}
              className="gap-1"
            >
              세션 추가하기
            </Button>
          )}
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
          isLeader={isLeader}
          onAbsentBadgeClick={handleAbsentBadgeClick}
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
                      {schedule.status === 'CANCELLED' && '취소'}
                    </span>
                    {/* 삭제 버튼 (리더이고 예정 상태일 때만 표시) */}
                    {isLeader && schedule.status === 'SCHEDULED' && (
                      <button
                        onClick={(e) => handleDeleteSession(e, schedule.id as number)}
                        disabled={deletingSessionId === schedule.id}
                        className={cn(
                          'p-1 rounded hover:bg-red-100 transition-colors',
                          deletingSessionId === schedule.id && 'opacity-50 cursor-not-allowed'
                        )}
                        title="일정 삭제"
                      >
                        <Trash2 size={14} className="text-red-500" />
                      </button>
                    )}
                  </div>
                );
              })
            ) : (
              <div className="text-center py-6 text-gray-500">
                <CalendarIcon size={24} className="mx-auto mb-1.5 opacity-50" />
                해당 날짜에 일정이 없습니다.
              </div>
            )}
          </div>

          {/* 세션 추가 버튼 (리더만 표시) */}
          {isLeader && (
            <div className="mt-3 pt-3 border-t">
              <Button
                variant="primary"
                size="sm"
                className="w-full gap-1.5 text-sm"
                onClick={() => selectedDate && handleQuickAdd(selectedDate)}
              >
                해당 날짜에 세션 추가
              </Button>
            </div>
          )}
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
        isLeader={isLeader}
      />

      {/* 결석 소명 작성 모달 */}
      <Modal isOpen={isExcuseModalOpen} onClose={handleExcuseModalClose} maxWidth="sm">
        <div className="-m-8 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-bold text-gray-900">결석 소명 작성</h3>
            <button
              onClick={handleExcuseModalClose}
              className="p-1 rounded-lg hover:bg-gray-100 transition-colors"
              disabled={excuseSubmitting}
            >
              <X size={16} className="text-gray-500" />
            </button>
          </div>
          <div className="space-y-2">
            <p className="text-sm text-gray-600">
              {excuseTarget?.title ? `${excuseTarget.title} 일정` : ''}
              {excuseTarget?.startTime ? ` · ${excuseTarget.startTime}` : ''}
            </p>
            <textarea
              className="w-full min-h-[120px] rounded-lg border border-gray-200 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
              placeholder="결석 사유를 입력해주세요."
              value={excuseReason}
              onChange={(e) => setExcuseReason(e.target.value)}
              disabled={excuseSubmitting}
            />
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="ghost" size="sm" onClick={handleExcuseModalClose} disabled={excuseSubmitting}>
              취소
            </Button>
            <Button variant="primary" size="sm" onClick={handleSubmitExcuse} disabled={excuseSubmitting}>
              제출
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
