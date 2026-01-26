import { CalendarHeaderProps } from '../types';
import { getWeekDayNames } from '../utils';

/**
 * 요일 헤더 컴포넌트 - 일~토요일 표시
 */
export const CalendarHeader = ({ weekDays }: CalendarHeaderProps) => {
    const days = weekDays || getWeekDayNames('en-US');

    return (
        <div className="grid grid-cols-7 border-b border-gray-300">
            {days.map((day, idx) => (
                <div
                    key={day}
                    className={`
                        text-xs font-bold uppercase tracking-wider py-6 text-center
                        ${idx === 0 ? 'text-red-500' : idx === 6 ? 'text-blue-500' : 'text-gray-600'}
                    `}
                >
                    {day}
                </div>
            ))}
        </div>
    );
};