// Calendar 컴포넌트 타입 정의

// 일정 아이템 타입
export interface ScheduleItem {
    id: string | number;
    title: string;
    date: string; // YYYY-MM-DD 형식
    type?: string;
}

// 캘린더 날짜 정보
export interface CalendarDayInfo {
    day: number;
    month: 'prev' | 'current' | 'next';
    fullDate: string; // YYYY-MM-DD 형식
}

// CalendarHeader 컴포넌트 Props
export interface CalendarHeaderProps {
    weekDays?: string[];
}

// CalendarDay 컴포넌트 Props
export interface CalendarDayProps {
    dayInfo: CalendarDayInfo;
    schedules: ScheduleItem[];
    isToday: boolean;
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
}

// Calendar 메인 컴포넌트 Props
export interface CalendarProps {
    currentDate: Date;
    schedules: ScheduleItem[];
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
    viewMode?: 'monthly' | 'weekly';
    className?: string;
}
