import { useState, useEffect } from 'react';
import { CalendarProps, CalendarDayInfo } from './types';
import { CalendarHeader } from './CalendarHeader';
import { CalendarDay } from './CalendarDay';
import './Calendar.css';

// 재사용 가능한 캘린더 메인 컴포넌트
export const Calendar = ({
    currentDate,
    schedules,
    onDateClick,
    onQuickAdd,
    viewMode = 'monthly',
    className = ''
}: CalendarProps) => {
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);

    // 윈도우 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // 해당 월의 모든 날짜 계산
    const getDaysInMonth = (year: number, month: number): CalendarDayInfo[] => {
        const date = new Date(year, month, 1);
        const days: CalendarDayInfo[] = [];
        const firstDayIndex = date.getDay();

        // 이전 달 날짜 추가
        for (let i = firstDayIndex; i > 0; i--) {
            const prevDate = new Date(year, month, 1 - i);
            days.push({
                day: prevDate.getDate(),
                month: 'prev',
                fullDate: prevDate.toISOString().split('T')[0]
            });
        }

        // 현재 달 날짜 추가
        while (date.getMonth() === month) {
            days.push({
                day: date.getDate(),
                month: 'current',
                fullDate: new Date(date).toISOString().split('T')[0]
            });
            date.setDate(date.getDate() + 1);
        }

        // 다음 달 날짜 추가 (7의 배수로 맞춤)
        const totalCells = Math.ceil(days.length / 7) * 7;
        const remaining = totalCells - days.length;
        for (let i = 1; i <= remaining; i++) {
            const nextDate = new Date(year, month + 1, i);
            days.push({
                day: nextDate.getDate(),
                month: 'next',
                fullDate: nextDate.toISOString().split('T')[0]
            });
        }

        return days;
    };

    // 오늘 기준 주간 뷰 계산
    const getWeeklyDays = (allDays: CalendarDayInfo[]): CalendarDayInfo[] => {
        const todayStr = currentDate.toISOString().split('T')[0];
        const todayIndex = allDays.findIndex(d => d.fullDate === todayStr);
        if (todayIndex === -1) return allDays.slice(0, 7);

        const startOfWeekIndex = Math.floor(todayIndex / 7) * 7;
        return allDays.slice(startOfWeekIndex, startOfWeekIndex + 7);
    };

    const allCalendarDays = getDaysInMonth(currentDate.getFullYear(), currentDate.getMonth());

    // 모바일(768px 이하) 또는 weekly 모드일 경우 주간 뷰
    const isMobile = windowWidth <= 768;
    const shouldShowWeekly = viewMode === 'weekly' || isMobile;
    const calendarDays = shouldShowWeekly ? getWeeklyDays(allCalendarDays) : allCalendarDays;

    const todayStr = currentDate.toISOString().split('T')[0];

    return (
        <div className={`expanded-calendar-wrapper ${className}`}>
            <CalendarHeader />
            <div className="calendar-grid large">
                {calendarDays.map((dayInfo, index) => {
                    // 해당 날짜의 일정만 필터링
                    const daySchedules = schedules.filter(s => s.date === dayInfo.fullDate);
                    const isToday = dayInfo.fullDate === todayStr;

                    return (
                        <CalendarDay
                            key={index}
                            dayInfo={dayInfo}
                            schedules={daySchedules}
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
