import { useState, useEffect } from 'react';
import { getDaysInMonth, getWeeklyDays, getTodayString } from '@/features/calendar/utils';
import { WorkspaceCalendarDay } from './WorkspaceCalendarDay';
import type { UnifiedSchedule } from '@/features/calendar';

interface WorkspaceCalendarProps {
  currentDate: Date;
  schedules: UnifiedSchedule[];
  onDateClick?: (date: string) => void;
  onQuickAdd?: (date: string) => void;
  viewMode?: 'monthly' | 'weekly';
  loading?: boolean;
  className?: string;
  isLeader?: boolean;
}

// 요일 헤더
const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

/**
 * 워크스페이스용 캘린더 컴포넌트
 */
export const WorkspaceCalendar: React.FC<WorkspaceCalendarProps> = ({
  currentDate,
  schedules,
  onDateClick,
  onQuickAdd,
  viewMode = 'monthly',
  loading = false,
  className = '',
  isLeader = false,
}) => {
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);

  // 윈도우 리사이즈 감지
  useEffect(() => {
    const handleResize = () => setWindowWidth(window.innerWidth);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const allCalendarDays = getDaysInMonth(currentDate.getFullYear(), currentDate.getMonth());

  // 모바일 또는 weekly 모드일 경우 주간 뷰
  const isMobile = windowWidth <= 768;
  const shouldShowWeekly = viewMode === 'weekly' || isMobile;
  const calendarDays = shouldShowWeekly
    ? getWeeklyDays(allCalendarDays, currentDate)
    : allCalendarDays;

  const todayStr = getTodayString();

  const statusColors: Record<string, string> = {
    SCHEDULED: '#EA4335',
    IN_PROGRESS: '#FBBC05',
    COMPLETED: '#34A853',
    CANCELLED: '#9CA3AF',
  };

  const resolveDayStatusItems = (daySchedules: UnifiedSchedule[]) => {
    const orderedStatuses = ['COMPLETED', 'IN_PROGRESS', 'SCHEDULED', 'CANCELLED'] as const;
    const counts = daySchedules.reduce<Record<string, number>>((acc, schedule) => {
      if (!schedule.status) return acc;
      acc[schedule.status] = (acc[schedule.status] ?? 0) + 1;
      return acc;
    }, {});
    return orderedStatuses
      .filter((status) => counts[status] > 0)
      .map((status) => ({
        status,
        count: counts[status],
        color: statusColors[status],
      }));
  };

  if (loading) {
    return (
      <div className={`flex items-center justify-center min-h-[400px] ${className}`}>
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500" />
      </div>
    );
  }

  return (
    <div className={`flex flex-col ${className}`}>
      {/* 요일 헤더 */}
      <div className="grid grid-cols-7 border-b border-gray-200">
        {WEEKDAYS.map((day, idx) => (
          <div
            key={day}
            className={`py-2 text-center text-xs font-semibold ${
              idx === 0 ? 'text-red-400' : idx === 6 ? 'text-blue-400' : 'text-gray-500'
            }`}
          >
            {day}
          </div>
        ))}
      </div>

      {/* 캘린더 그리드 */}
      <div className="grid grid-cols-7 gap-0">
        {calendarDays.map((dayInfo, index) => {
          // 해당 날짜의 일정 개수
          const daySchedules = schedules.filter((s) => s.startDate === dayInfo.fullDate);
          const scheduleCount = daySchedules.length;
          const statusItems = resolveDayStatusItems(daySchedules);
          const isToday = dayInfo.fullDate === todayStr;

          return (
            <WorkspaceCalendarDay
              key={`${dayInfo.fullDate}-${index}`}
              dayInfo={dayInfo}
              scheduleCount={scheduleCount}
              statusItems={statusItems}
              isToday={isToday}
              onDateClick={onDateClick}
              onQuickAdd={onQuickAdd}
              isLeader={isLeader}
            />
          );
        })}
      </div>
    </div>
  );
};
