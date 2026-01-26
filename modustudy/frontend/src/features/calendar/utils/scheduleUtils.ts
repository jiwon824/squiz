import { UnifiedSchedule, ScheduleSource } from '../types';

/*
 일정 소스별 색상 반환
 */
export const getScheduleColor = (source: ScheduleSource): string => {
    const colorMap: Record<ScheduleSource, string> = {
        personal: 'bg-blue-500',
        study: 'bg-green-500',
        google: 'bg-red-500'
    };
    return colorMap[source];
};

/**
 * 일정 소스별 텍스트 색상 반환
 */
export const getScheduleTextColor = (_source: ScheduleSource): string => {
    // 현재는 모두 흰색, 향후 소스별 구분 가능
    return 'text-white';
};

/*
 날짜별로 일정 그룹화
 */
export const groupSchedulesByDate = (
    schedules: UnifiedSchedule[]
): Record<string, UnifiedSchedule[]> => {
    return schedules.reduce((acc, schedule) => {
        const date = schedule.startDate;
        if (!acc[date]) {
            acc[date] = [];
        }
        acc[date].push(schedule);
        return acc;
    }, {} as Record<string, UnifiedSchedule[]>);
};

/*
 일정 정렬 (시간순)
 */
export const sortSchedules = (schedules: UnifiedSchedule[]): UnifiedSchedule[] => {
    return [...schedules].sort((a, b) => {
        // 시간이 있는 경우 시간 기준 정렬
        if (a.startTime && b.startTime) {
            return a.startTime.localeCompare(b.startTime);
        }
        // 시간이 없으면 제목 기준
        return a.title.localeCompare(b.title);
    });
};

/*
 일정 제목 단축 (최대 길이)
 */
export const truncateTitle = (title: string, maxLength: number = 20): string => {
    if (title.length <= maxLength) return title;
    return title.substring(0, maxLength) + '...';
};