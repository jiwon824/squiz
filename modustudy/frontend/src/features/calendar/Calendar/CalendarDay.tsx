import { CalendarDayProps } from './types';
import './Calendar.css';

// 개별 날짜 셀 컴포넌트
export const CalendarDay = ({
    dayInfo,
    schedules,
    isToday,
    onDateClick,
    onQuickAdd
}: CalendarDayProps) => {
    const dayOfWeek = new Date(dayInfo.fullDate).getDay();

    // 클래스명 동적 구성
    const className = [
        'calendar-day',
        'large',
        dayInfo.month !== 'current' ? 'other-month' : '',
        isToday ? 'today' : '',
        dayOfWeek === 0 ? 'is-sun' : dayOfWeek === 6 ? 'is-sat' : ''
    ].filter(Boolean).join(' ');

    const handleClick = () => {
        onDateClick?.(dayInfo.fullDate);
    };

    const handleQuickAdd = (e: React.MouseEvent) => {
        e.stopPropagation();
        onQuickAdd?.(dayInfo.fullDate);
    };

    return (
        <div className={className} onClick={handleClick}>
            {/* 날짜 헤더: 숫자 + 빠른 추가 버튼 */}
            <div className="day-header-box">
                <span className="day-number">{dayInfo.day}</span>
                <button className="quick-add-btn" onClick={handleQuickAdd}>
                    +
                </button>
            </div>

            {/* 해당 날짜의 일정 목록 */}
            <div className="day-schedules">
                {schedules.map(schedule => (
                    <div key={schedule.id} className={`schedule-item ${schedule.type} large`}>
                        {schedule.title}
                    </div>
                ))}
            </div>
        </div>
    );
};
