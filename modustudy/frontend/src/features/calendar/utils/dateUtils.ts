import { CalendarDayInfo } from '../types';

/**
 * 해당 월의 모든 날짜 계산 (이전달/현재달/다음달 포함)
 */
export const getDaysInMonth = (year: number, month: number): CalendarDayInfo[] => {
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

/**
 * 오늘 기준 주간 뷰 계산
 */
export const getWeeklyDays = (
    allDays: CalendarDayInfo[],
    currentDate: Date
): CalendarDayInfo[] => {
    const todayStr = currentDate.toISOString().split('T')[0];
    const todayIndex = allDays.findIndex(d => d.fullDate === todayStr);
    if (todayIndex === -1) return allDays.slice(0, 7);

    const startOfWeekIndex = Math.floor(todayIndex / 7) * 7;
    return allDays.slice(startOfWeekIndex, startOfWeekIndex + 7);
};

/**
 * 날짜 문자열을 Date 객체로 변환
 */
export const parseDate = (dateStr: string): Date => {
    return new Date(dateStr);
};

/**
 * Date 객체를 YYYY-MM-DD 형식으로 변환
 */
export const formatDate = (date: Date): string => {
    return date.toISOString().split('T')[0];
};

/**
 * 오늘 날짜 문자열 반환 (YYYY-MM-DD)
 */
export const getTodayString = (): string => {
    return formatDate(new Date());
};

/**
 * 두 날짜가 같은지 비교
 */
export const isSameDate = (date1: string, date2: string): boolean => {
    return date1 === date2;
};

/**
 * 날짜가 오늘인지 확인
 */
export const isToday = (dateStr: string): boolean => {
    return isSameDate(dateStr, getTodayString());
};

/**
 * 월 이름 반환 (1-12)
 */
export const getMonthName = (month: number, locale: string = 'ko-KR'): string => {
    const date = new Date(2000, month, 1);
    return date.toLocaleDateString(locale, { month: 'long' });
};

/**
 * 요일 이름 배열 반환
 */
export const getWeekDayNames = (locale: string = 'en-US'): string[] => {
    if (locale === 'en-US') {
        return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    }
    return ['일', '월', '화', '수', '목', '금', '토'];
};