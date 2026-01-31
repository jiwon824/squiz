import type { UnifiedSchedule } from '@/features/calendar';

interface CalendarDayInfo {
  day: number;
  month: 'prev' | 'current' | 'next';
  fullDate: string;
}

interface WorkspaceCalendarDayProps {
  dayInfo: CalendarDayInfo;
  scheduleCount: number;
  statusItems?: Array<{ status: string; count?: number; label?: string; color: string }>;
  completedSchedules?: UnifiedSchedule[];
  isToday: boolean;
  onDateClick?: (date: string) => void;
  onQuickAdd?: (date: string) => void;
  isLeader?: boolean;
  onAbsentBadgeClick?: (schedule: UnifiedSchedule) => void;
}

/**
 * 워크스페이스 캘린더의 날짜 셀 컴포넌트
 * - 완료 일정(점 + 시간 + 출석 배지) 표시
 * - 빠른 추가 버튼 제공
 */
export const WorkspaceCalendarDay: React.FC<WorkspaceCalendarDayProps> = ({
  dayInfo,
  scheduleCount,
  statusItems = [],
  completedSchedules = [],
  isToday,
  onDateClick,
  onQuickAdd,
  isLeader = false,
  onAbsentBadgeClick,
}) => {
  const dayOfWeek = new Date(dayInfo.fullDate).getDay();

  const handleClick = () => {
    onDateClick?.(dayInfo.fullDate);
  };

  const handleQuickAdd = (e: React.MouseEvent) => {
    e.stopPropagation();
    onQuickAdd?.(dayInfo.fullDate);
  };

  const sundayClasses = dayOfWeek === 0 && dayInfo.month === 'current' ? 'text-red-400' : '';
  const saturdayClasses = dayOfWeek === 6 && dayInfo.month === 'current' ? 'text-blue-400' : '';

  const resolveAttendanceBadge = (status?: string) => {
    switch (status) {
      case 'PRESENT':
      case 'EXCUSED':
        return { label: '출석', className: 'bg-green-100 text-green-700' };
      case 'LATE':
        return { label: '지각', className: 'bg-yellow-100 text-yellow-700' };
      case 'ABSENT':
        return { label: '결석', className: 'bg-red-100 text-red-700' };
      default:
        return { label: '미확인', className: 'bg-gray-100 text-gray-600' };
    }
  };

  return (
    <div
      className={`
        group workspace-calendar-day
        md:h-[120px] h-auto aspect-square md:aspect-auto
        cursor-pointer transition-all duration-200
        p-1.5 md:p-2 rounded-lg relative
        ${dayInfo.month !== 'current' ? 'opacity-40' : ''}
        ${isToday ? 'workspace-calendar-day--today' : ''}
      `}
      onClick={handleClick}
    >
      
      <div className="hidden md:flex flex-col h-full">
        
        <div className="flex justify-between items-center">
          <span
            className={`text-sm font-bold ${sundayClasses} ${saturdayClasses} ${
              isToday ? 'text-blue-500' : ''
            }`}
          >
            {dayInfo.day}
          </span>
          
          {isLeader && (
            <button
              type="button"
              className="workspace-calendar-day__add-btn w-5 h-5 flex items-center justify-center rounded text-xs font-bold opacity-0 group-hover:opacity-100 transition-all"
              onClick={handleQuickAdd}
              title="빠른 일정 추가"
            >
              +
            </button>
          )}
        </div>

        
        {scheduleCount > 0 && (
          <div className="workspace-calendar-day__items flex flex-col gap-1.5 mt-2">
            {completedSchedules.length > 0 && (
              <div className="flex flex-col gap-1">
                {completedSchedules.map((schedule) => {
                  const badge = resolveAttendanceBadge(schedule.attendanceStatus);
                  const isAbsent = schedule.attendanceStatus === "ABSENT";
                  return (
                    <div key={`completed-${schedule.id}`} className="flex items-center gap-1">
                      <div className="w-1.5 h-1.5 rounded-full bg-green-500" />
                      <span className="text-[10px] text-gray-700">
                        {schedule.startTime || "--:--"}
                      </span>
                      <button
                        type="button"
                        className={`text-[10px] font-medium px-1.5 py-0.5 rounded ${badge.className}`}
                        onClick={(event) => {
                          event.stopPropagation();
                          if (!isAbsent) return;
                          onAbsentBadgeClick?.(schedule);
                        }}
                      >
                        {badge.label}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
            {statusItems.length > 0 && (
              <div className="flex flex-col gap-1">
                {statusItems.map((item, idx) => (
                  <div key={`${item.status}-${idx}`} className="flex items-center gap-0.5">
                    <div className="relative flex items-center justify-center">
                      <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: item.color }} />
                      {idx === 0 && (
                        <div className="absolute w-1.5 h-1.5 rounded-full animate-ping" style={{ backgroundColor: item.color }} />
                      )}
                    </div>
                    <span className="text-[10px] text-gray-600">
                      {item.label ?? item.count}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
      
      <div className="flex md:hidden flex-col items-center justify-center h-full gap-1">
        <span
          className={`text-sm font-bold ${sundayClasses} ${saturdayClasses} ${
            isToday ? 'text-blue-500' : ''
          }`}
        >
          {dayInfo.day}
        </span>
        {scheduleCount > 0 && (
          <div className="workspace-calendar-day__items flex flex-col items-center gap-1">
            {completedSchedules.length > 0 && (
              <div className="flex flex-col items-center gap-0.5">
                {completedSchedules.map((schedule) => {
                  const badge = resolveAttendanceBadge(schedule.attendanceStatus);
                  const isAbsent = schedule.attendanceStatus === "ABSENT";
                  return (
                    <div key={`completed-mobile-${schedule.id}`} className="flex items-center gap-0.5">
                      <div className="w-1 h-1 rounded-full bg-green-500" />
                      <span className="text-[9px] text-gray-700">
                        {schedule.startTime || "--:--"}
                      </span>
                      <button
                        type="button"
                        className={`text-[9px] font-medium px-1 py-0.5 rounded ${badge.className}`}
                        onClick={(event) => {
                          event.stopPropagation();
                          if (!isAbsent) return;
                          onAbsentBadgeClick?.(schedule);
                        }}
                      >
                        {badge.label}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
            {statusItems.length > 0 && (
              <div className="flex flex-col items-center gap-0.5">
                {statusItems.map((item, idx) => (
                  <div key={`${item.status}-${idx}`} className="flex items-center gap-0.5">
                    <div className="relative flex items-center justify-center">
                      <div className="w-1 h-1 rounded-full" style={{ backgroundColor: item.color }} />
                      {idx === 0 && (
                        <div className="absolute w-1 h-1 rounded-full animate-ping" style={{ backgroundColor: item.color }} />
                      )}
                    </div>
                    <span className="text-[9px] text-gray-600">
                      {item.label ?? item.count}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};



