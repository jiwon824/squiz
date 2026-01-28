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
          const scheduleCount = schedules.filter((s) => s.startDate === dayInfo.fullDate).length;
          const isToday = dayInfo.fullDate === todayStr;

          return (
            <WorkspaceCalendarDay
              key={`${dayInfo.fullDate}-${index}`}
              dayInfo={dayInfo}
              scheduleCount={scheduleCount}
              isToday={isToday}
              onDateClick={onDateClick}
              onQuickAdd={onQuickAdd}
            />
          );
        })}
      </div>
    </div>
  );
};
