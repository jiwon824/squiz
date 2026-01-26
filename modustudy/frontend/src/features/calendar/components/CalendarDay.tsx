import { CalendarDayProps } from '../types';
import { getScheduleColor, truncateTitle } from '../utils';
import { Button } from '@/shared/components';

/**
 * 개별 날짜 셀 컴포넌트
 */
export const CalendarDay = ({
    dayInfo,
    schedules,
    isToday,
    onDateClick,
    onQuickAdd,
    onEventClick
}: CalendarDayProps) => {
    const dayOfWeek = new Date(dayInfo.fullDate).getDay();

    const handleClick = () => {
        onDateClick?.(dayInfo.fullDate);
    };

    const handleQuickAdd = (e: React.MouseEvent) => {
        e.stopPropagation();
        onQuickAdd?.(dayInfo.fullDate);
    };

    const handleEventClick = (e: React.MouseEvent, scheduleId: number | string) => {
        e.stopPropagation();
        const schedule = schedules.find(s => s.id === scheduleId);
        if (schedule && onEventClick) {
            onEventClick(schedule);
        }
    };

    // Tailwind 클래스 동적 구성 (기존 캘린더 스타일 적용)
    // 모바일(md 이하)에서는 정사각형, 데스크탑에서는 고정 높이
    const baseClasses = 'md:h-[140px] h-auto aspect-square md:aspect-auto cursor-pointer transition-all duration-200 p-2 md:p-3 border-r border-b border-gray-200 rounded-lg';
    // 모바일에서는 flex layout으로 중앙 정렬 필요할 수 있음 (부모 div가 아니라 내부 컨텐츠에서 처리)

    const monthClasses = dayInfo.month !== 'current' ? 'opacity-40 bg-transparent' : '';
    const todayClasses = isToday
        ? 'bg-blue-50/50'
        : 'bg-white hover:bg-gray-50 hover:shadow-md hover:-translate-y-0.5 hover:z-10';
    const sundayClasses = dayOfWeek === 0 && dayInfo.month === 'current' ? 'text-red-400' : '';
    const saturdayClasses = dayOfWeek === 6 && dayInfo.month === 'current' ? 'text-blue-400' : '';

    return (
        <div
            className={`group ${baseClasses} ${monthClasses} ${todayClasses}`}
            onClick={handleClick}
        >
            {/* 데스크탑 뷰 (md 이상) */}
            <div className="hidden md:flex flex-col gap-2 h-full">
                {/* 날짜 헤더: 숫자 + 빠른 추가 버튼 */}
                <div className="flex justify-between items-center mb-1.5">
                    <span className={`text-sm font-extrabold ${sundayClasses} ${saturdayClasses} ${isToday ? 'text-blue-600 font-black' : 'text-gray-700'}`}>
                        {dayInfo.day}
                    </span>
                    <Button
                        variant="ghost"
                        size="xs"
                        className="w-5 h-5 !p-0 opacity-0 group-hover:opacity-100 hover:!bg-blue-600 hover:!text-white"
                        onClick={handleQuickAdd}
                        title="빠른 일정 추가"
                    >
                        +
                    </Button>
                </div>

                {/* 해당 날짜의 일정 목록 */}
                <div className="flex flex-col gap-1 overflow-hidden">
                    {schedules.slice(0, 3).map(schedule => (
                        <div
                            key={schedule.id}
                            className={`${getScheduleColor(schedule.source)} text-white text-xs px-2 py-1 rounded cursor-pointer hover:opacity-80 transition-opacity`}
                            onClick={(e) => handleEventClick(e, schedule.id)}
                            title={schedule.title}
                        >
                            {truncateTitle(schedule.title, 15)}
                        </div>
                    ))}
                    {schedules.length > 3 && (
                        <div className="text-xs text-gray-500 font-medium px-1">
                            +{schedules.length - 3} more
                        </div>
                    )}
                </div>
            </div>

            {/* 모바일 뷰 (md 미만) - 점(Dot) 형태로 표시 */}
            <div className="flex md:hidden flex-col items-center justify-center h-full gap-1">
                <span className={`text-sm font-bold ${sundayClasses} ${saturdayClasses} ${isToday ? 'text-blue-600 font-black' : 'text-gray-400'}`}>
                    {dayInfo.day}
                </span>
                <div className="flex gap-1 flex-wrap justify-center">
                    {schedules.slice(0, 4).map(schedule => (
                        <div
                            key={schedule.id}
                            className={`w-1.5 h-1.5 rounded-full ${getScheduleColor(schedule.source)}`}
                        ></div>
                    ))}
                    {schedules.length > 4 && (
                        <div className="w-1.5 h-1.5 rounded-full bg-gray-300"></div>
                    )}
                </div>
            </div>
        </div>
    );
};