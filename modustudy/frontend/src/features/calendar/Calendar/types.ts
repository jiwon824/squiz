// 캘린더 관련 타입 정의
import { Schedule } from '../../dashboard/services/scheduleStore';

// CalendarDay에 표시할 날짜 정보
export interface CalendarDayInfo {
    day: number;
    month: 'prev' | 'current' | 'next';
    fullDate: string; // YYYY-MM-DD 형식
}

// Calendar 컴포넌트 Props
export interface CalendarProps {
    currentDate: Date;
    schedules: Schedule[];
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
    viewMode?: 'monthly' | 'weekly';
    className?: string;
}

// CalendarDay 컴포넌트 Props
export interface CalendarDayProps {
    dayInfo: CalendarDayInfo;
    schedules: Schedule[];
    isToday: boolean;
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
}

// CalendarHeader 컴포넌트 Props
export interface CalendarHeaderProps {
    weekDays?: string[];
}
