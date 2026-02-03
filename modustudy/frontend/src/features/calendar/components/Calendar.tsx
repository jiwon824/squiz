import { useState, useEffect } from 'react';
import { CalendarProps } from '../types';
import { getDaysInMonth, getWeeklyDays, getTodayString } from '../utils';
import { CalendarHeader } from './CalendarHeader';
import { CalendarDay } from './CalendarDay';
import { CalendarSkeleton } from '@/shared/components/skeleton/composed/CalendarSkeleton';

/**
 * 재사용 가능한 캘린더 메인 컴포넌트 (SQUIZ 고유 디자인 복구)
 * - 월간/주간 뷰 지원
 * - 반응형 (모바일 자동 주간 뷰)
 */
export const Calendar = ({
    currentDate,
    schedules,
    onDateClick,
    onQuickAdd,
    onEventClick,
    viewMode = 'monthly',
    className = '',
    loading = false
}: CalendarProps) => {
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);

    // 윈도우 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const allCalendarDays = getDaysInMonth(currentDate.getFullYear(), currentDate.getMonth());

    // 모바일(768px 이하) 또는 weekly 모드일 경우 주간 뷰
    const isMobile = windowWidth <= 768;
    const shouldShowWeekly = viewMode === 'weekly' || isMobile;
    const calendarDays = shouldShowWeekly
        ? getWeeklyDays(allCalendarDays, currentDate)
        : allCalendarDays;

    const todayStr = getTodayString();

    if (loading) {
        return (
            <div className={className}>
                <CalendarSkeleton isWeeklyView={shouldShowWeekly} />
            </div>
        );
    }

    return (
        <div className={`flex flex-col ${className}`}>
            <CalendarHeader />
            <div className="grid grid-cols-7 gap-0">
                {calendarDays.map((dayInfo, index) => {
                    // 해당 날짜의 일정만 필터링
                    const daySchedules = schedules.filter(s => s.startDate === dayInfo.fullDate);
                    const isToday = dayInfo.fullDate === todayStr;

                    return (
                        <CalendarDay
                            key={`${dayInfo.fullDate}-${index}`}
                            dayInfo={dayInfo}
                            schedules={daySchedules}
                            isToday={isToday}
                            onDateClick={onDateClick}
                            onQuickAdd={onQuickAdd}
                            onEventClick={onEventClick}
                        />
                    );
                })}
            </div>
        </div>
    );
};
