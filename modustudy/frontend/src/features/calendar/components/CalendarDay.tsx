import { CalendarDayProps } from '../types';
import { getScheduleColor } from '../utils';
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

    // Tailwind 클래스 동적 구성 (디자인 이식: 상단/좌측 테두리 강조 및 입체감)
    // border-t, border-l에 밝은 색상을 주어 돌출된 느낌을 주고, active 시 scale을 줄여 눌리는 느낌 구현
    const baseClasses = 'md:h-[140px] h-auto aspect-square md:aspect-auto cursor-pointer transition-all duration-300 p-2 md:p-3 border-r border-b border-gray-200 border-t border-l border-gray-50/50 rounded-xl relative active:scale-[0.98] active:z-50 active:shadow-inner';

    const monthClasses = dayInfo.month !== 'current' ? 'opacity-40 bg-transparent' : '';
    const todayClasses = isToday
        ? 'bg-blue-50/50 border-blue-200 shadow-inner'
        : 'bg-white hover:bg-gray-50/80 hover:shadow-xl hover:-translate-y-1 hover:z-10 hover:border-blue-100';
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

                {/* 해당 날짜의 일정 목록 (디자인 이식: 색상 테마가 적용된 카드 스타일) */}
                <div className="flex flex-col gap-1 overflow-hidden">
                    {schedules.slice(0, 3).map(schedule => {
                        const baseColor = schedule.color || getScheduleColor(schedule.source);

                        return (
                            <div
                                key={schedule.id}
                                className="group/item relative px-2 py-1 rounded-[5px] border-l-[3px] cursor-pointer hover:shadow-sm transition-all w-full active:scale-[0.98]"
                                style={{
                                    backgroundColor: `${baseColor}15`, // 15% 투명도 배경
                                    borderLeftColor: baseColor,
                                    borderTop: '1px solid rgba(255,255,255,0.4)', // 상단 미세 하이라이트
                                    borderRight: '1px solid rgba(0,0,0,0.02)',
                                    borderBottom: '1px solid rgba(0,0,0,0.02)'
                                }}
                                onClick={(e) => handleEventClick(e, schedule.id)}
                                title={schedule.title}
                            >
                                <div className="flex items-center gap-1.5">
                                    {/* 섹션 아이콘 역할을 하는 작은 점 (선택 사항, 필요시 활성 상태 등 표시) */}
                                    <div
                                        className="w-1 h-1 rounded-full shrink-0"
                                        style={{ backgroundColor: baseColor }}
                                    />
                                    <p
                                        className="text-[10px] md:text-[11px] font-bold truncate leading-tight tracking-tight"
                                        style={{ color: baseColor }} // 텍스트도 해당 색상의 진한 버전으로 (Shadow 자동 적용 느낌)
                                    >
                                        {schedule.title}
                                    </p>
                                </div>
                            </div>
                        );
                    })}
                    {schedules.length > 3 && (
                        <div className="text-[9px] md:text-[10px] text-gray-400 font-extrabold px-1.5 mt-0.5">
                            + {schedules.length - 3} more
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
                            className={`w-1.5 h-1.5 rounded-full ${!schedule.color ? getScheduleColor(schedule.source) : ''}`}
                            style={schedule.color ? { backgroundColor: schedule.color } : {}}
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