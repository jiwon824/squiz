import { useEffect } from 'react';
import { useCalendarStore } from '../services/calendarStore';
import { formatDate } from '../utils';

/**
 * 캘린더 데이터 페칭 훅
 * - 현재 월/날짜 범위의 일정 자동 로드
 * - 날짜 변경 시 자동 재조회
 */
export const useCalendarData = (currentDate: Date) => {
    const { 
        schedules, 
        loading, 
        error, 
        fetchSchedules,
        checkGoogleStatus 
    } = useCalendarStore();

    // 월 시작/종료일 계산
    const getMonthRange = (date: Date) => {
        const year = date.getFullYear();
        const month = date.getMonth();
        
        // 이전 달 마지막 주 포함
        const firstDay = new Date(year, month, 1);
        const startDate = new Date(firstDay);
        startDate.setDate(startDate.getDate() - firstDay.getDay()); // 일요일부터
        
        // 다음 달 첫 주 포함
        const lastDay = new Date(year, month + 1, 0);
        const endDate = new Date(lastDay);
        endDate.setDate(endDate.getDate() + (6 - lastDay.getDay())); // 토요일까지
        
        return {
            startDate: formatDate(startDate),
            endDate: formatDate(endDate)
        };
    };

    // 현재 월 데이터 로드
    useEffect(() => {
        const { startDate, endDate } = getMonthRange(currentDate);
        fetchSchedules(startDate, endDate);
        checkGoogleStatus();
    }, [currentDate.getFullYear(), currentDate.getMonth()]);

    return {
        schedules,
        loading,
        error
    };
};

/**
 * 특정 날짜 범위의 일정 조회 훅
 */
export const useSchedulesByDateRange = (startDate: string, endDate: string) => {
    const { schedules, loading, error, fetchSchedules } = useCalendarStore();

    useEffect(() => {
        fetchSchedules(startDate, endDate);
    }, [startDate, endDate]);

    return {
        schedules,
        loading,
        error
    };
};

/**
 * 월 변경 시 데이터 프리페치 훅
 */
export const usePrefetchMonth = () => {
    const { fetchSchedules } = useCalendarStore();

    const prefetchMonth = (date: Date) => {
        const year = date.getFullYear();
        const month = date.getMonth();
        const startDate = formatDate(new Date(year, month, 1));
        const endDate = formatDate(new Date(year, month + 1, 0));
        
        // 백그라운드에서 미리 로드
        fetchSchedules(startDate, endDate);
    };

    return { prefetchMonth };
};
