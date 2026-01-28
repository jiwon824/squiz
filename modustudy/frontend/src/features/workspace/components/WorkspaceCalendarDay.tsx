import { Button } from '@/shared/components';

interface CalendarDayInfo {
  day: number;
  month: 'prev' | 'current' | 'next';
  fullDate: string;
}

interface WorkspaceCalendarDayProps {
  dayInfo: CalendarDayInfo;
  scheduleCount: number;
  isToday: boolean;
  onDateClick?: (date: string) => void;
  onQuickAdd?: (date: string) => void;
  isLeader?: boolean;
}

/**
 * 워크스페이스용 캘린더 날짜 셀 컴포넌트
 * - 빨간 점 + 일정 개수로 심플하게 표시
 */
export const WorkspaceCalendarDay: React.FC<WorkspaceCalendarDayProps> = ({
  dayInfo,
  scheduleCount,
  isToday,
  onDateClick,
  onQuickAdd,
  isLeader = false,
}) => {
  const dayOfWeek = new Date(dayInfo.fullDate).getDay();

  const handleClick = () => {
    onDateClick?.(dayInfo.fullDate);
  };

  const handleQuickAdd = (e: React.MouseEvent) => {
    e.stopPropagation();
    onQuickAdd?.(dayInfo.fullDate);
  };


  // 스타일 클래스
  const baseClasses =
    'md:h-[120px] h-auto aspect-square md:aspect-auto cursor-pointer transition-all duration-200 p-1.5 md:p-2 border-r border-b border-gray-200 rounded-lg relative';
  const monthClasses = dayInfo.month !== 'current' ? 'opacity-40 bg-transparent' : '';
  const todayClasses = isToday
    ? 'bg-blue-50/50 border-blue-200'
    : 'bg-white hover:bg-gray-50/80 hover:shadow-md hover:-translate-y-0.5 hover:z-10';
  const sundayClasses = dayOfWeek === 0 && dayInfo.month === 'current' ? 'text-red-400' : '';
  const saturdayClasses = dayOfWeek === 6 && dayInfo.month === 'current' ? 'text-blue-400' : '';

  return (
    <div
      className={`group ${baseClasses} ${monthClasses} ${todayClasses}`}
      onClick={handleClick}
    >
      {/* 데스크탑 뷰 */}
      <div className="hidden md:flex flex-col h-full">
        {/* 날짜 헤더 */}
        <div className="flex justify-between items-center">
          <span
            className={`text-sm font-bold ${sundayClasses} ${saturdayClasses} ${
              isToday ? 'text-blue-600' : 'text-gray-700'
            }`}
          >
            {dayInfo.day}
          </span>
          {/* 빠른 추가 버튼 (리더만 표시) */}
          {isLeader && (
            <Button
              variant="ghost"
              size="xs"
              className="w-5 h-5 !p-0 opacity-0 group-hover:opacity-100 hover:!bg-blue-600 hover:!text-white text-xs"
              onClick={handleQuickAdd}
              title="빠른 일정 추가"
            >
              +
            </Button>
          )}
        </div>

        {/* 일정 표시 - 라이브 점 + 개수 */}
        {scheduleCount > 0 && (
          <div className="flex items-center gap-1.5 mt-2">
            <div className="relative flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-red-500" />
              <div className="absolute w-1.5 h-1.5 rounded-full bg-red-500 animate-ping" />
            </div>
            <span className="text-xs font-medium text-gray-600">
              {scheduleCount}
            </span>
          </div>
        )}
      </div>

      {/* 모바일 뷰 */}
      <div className="flex md:hidden flex-col items-center justify-center h-full gap-1">
        <span
          className={`text-sm font-bold ${sundayClasses} ${saturdayClasses} ${
            isToday ? 'text-blue-600' : 'text-gray-400'
          }`}
        >
          {dayInfo.day}
        </span>
        {scheduleCount > 0 && (
          <div className="flex items-center gap-1">
            <div className="relative flex items-center justify-center">
              <div className="w-1 h-1 rounded-full bg-red-500" />
              <div className="absolute w-1 h-1 rounded-full bg-red-500 animate-ping" />
            </div>
            <span className="text-[10px] text-gray-500">{scheduleCount}</span>
          </div>
        )}
      </div>
    </div>
  );
};
