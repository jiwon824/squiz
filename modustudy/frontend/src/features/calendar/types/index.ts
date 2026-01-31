// ============================================
// 캘린더 통합 타입 정의
// ============================================

/**
 * 일정 소스 타입
 */
export type ScheduleSource = 'personal' | 'study' | 'google';

/**
 * 일정 상태 (스터디 세션용)
 */
export type ScheduleStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

/**
 * 통합 일정 인터페이스
 * - 개인 일정, 스터디 세션, Google Calendar를 모두 표현
 */
export interface UnifiedSchedule {
    id: number | string;
    title: string;
    description?: string;
    startDate: string; // YYYY-MM-DD
    startTime?: string; // HH:mm
    endDate?: string;
    endTime?: string;
    durationMinutes?: number;
    location?: string;
    isOnline?: boolean;
    source: ScheduleSource;
    status?: ScheduleStatus;
    attendanceStatus?: 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED';
    excuseStatus?: 'PENDING' | 'APPROVED' | 'REJECTED';
    studyId?: number; // 스터디 세션인 경우
    sessionNumber?: number; // 스터디 세션 회차
    googleEventId?: string; // Google Calendar ID
    isSyncedWithGoogle?: boolean; // Google Calendar 동기화 여부
    lastSyncedAt?: string; // 마지막 동기화 시간
    color?: string; // UI 표시용 색상
    createdAt?: string;
    updatedAt?: string;
}

/**
 * 캘린더 날짜 정보
 */
export interface CalendarDayInfo {
    day: number;
    month: 'prev' | 'current' | 'next';
    fullDate: string; // YYYY-MM-DD
}

/**
 * 캘린더 뷰 모드
 */
export type CalendarViewMode = 'monthly' | 'weekly' | 'daily';

/**
 * Calendar 컴포넌트 Props
 */
export interface CalendarProps {
    currentDate: Date;
    schedules: UnifiedSchedule[];
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
    onEventClick?: (schedule: UnifiedSchedule) => void;
    viewMode?: CalendarViewMode;
    className?: string;
    loading?: boolean;
}

/**
 * CalendarDay 컴포넌트 Props
 */
export interface CalendarDayProps {
    dayInfo: CalendarDayInfo;
    schedules: UnifiedSchedule[];
    isToday: boolean;
    onDateClick?: (date: string) => void;
    onQuickAdd?: (date: string) => void;
    onEventClick?: (schedule: UnifiedSchedule) => void;
}

/**
 * CalendarHeader 컴포넌트 Props
 */
export interface CalendarHeaderProps {
    weekDays?: string[];
}

/**
 * 목표 (학습 목표 위젯용)
 */
export interface Goal {
    id: number;
    text: string;
    completed: boolean;
    createdAt?: string;
}

/**
 * 태그 (학습 메모용)
 */
export interface Tag {
    id: number;
    text: string;
    color?: string;
}

/**
 * 캘린더 필터 옵션
 */
export interface CalendarFilterOptions {
    sources: ScheduleSource[];
    statuses?: ScheduleStatus[];
    studyIds?: number[];
}

/**
 * Google Calendar 동기화 설정
 */
export interface GoogleCalendarSyncConfig {
    enabled: boolean;
    lastSyncAt?: string;
    syncInterval?: number; // 분 단위
    calendarIds: string[];
}
