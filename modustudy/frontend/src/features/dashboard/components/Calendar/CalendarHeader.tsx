import { CalendarHeaderProps } from './types';
import './Calendar.css';

// 요일 헤더 컴포넌트 - 일~토요일 표시
export const CalendarHeader = ({ weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] }: CalendarHeaderProps) => {
    return (
        <div className="calendar-weekdays">
            {weekDays.map((day, idx) => (
                <div
                    key={day}
                    className={`weekday large ${idx === 0 ? 'is-sun' : idx === 6 ? 'is-sat' : ''}`}
                >
                    {day}
                </div>
            ))}
        </div>
    );
};
