import { useState, useMemo, useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';

interface DateRangePickerProps {
    startDate: string | null;
    endDate: string | null;
    minDate?: string | null;
    onRangeChange: (start: string | null, end: string | null) => void;
}

/**
 * 스터디 기간 선택용 미니 캘린더
 */
export const DateRangePicker = ({ startDate, endDate, minDate, onRangeChange }: DateRangePickerProps) => {
    // 초기 표시 월: endDate가 있으면 endDate 기준, 없으면 startDate 기준, 둘 다 없으면 오늘
    const getInitialMonth = () => {
        if (endDate) {
            return new Date(endDate);
        } else if (startDate) {
            return new Date(startDate);
        }
        return new Date();
    };

    const [currentMonth, setCurrentMonth] = useState(getInitialMonth);
    const [hoveredDate, setHoveredDate] = useState<string | null>(null);

    // endDate가 변경되면 해당 월로 이동
    useEffect(() => {
        if (endDate) {
            const endDateObj = new Date(endDate);
            setCurrentMonth(new Date(endDateObj.getFullYear(), endDateObj.getMonth(), 1));
        } else if (startDate) {
            const startDateObj = new Date(startDate);
            setCurrentMonth(new Date(startDateObj.getFullYear(), startDateObj.getMonth(), 1));
        }
    }, [endDate, startDate]);

    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];

    const formatDate = (date: Date): string => {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    };

    // 해당 월의 날짜 계산
    const calendarDays = useMemo(() => {
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth();
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const days: { date: string; day: number; isCurrentMonth: boolean }[] = [];

        // 이전 달 날짜
        const firstDayIndex = firstDay.getDay();
        for (let i = firstDayIndex; i > 0; i--) {
            const d = new Date(year, month, 1 - i);
            days.push({
                date: formatDate(d),
                day: d.getDate(),
                isCurrentMonth: false
            });
        }

        // 현재 달 날짜
        for (let d = 1; d <= lastDay.getDate(); d++) {
            const date = new Date(year, month, d);
            days.push({
                date: formatDate(date),
                day: d,
                isCurrentMonth: true
            });
        }

        // 다음 달 날짜 (7의 배수로 채움)
        const remaining = 7 - (days.length % 7);
        if (remaining < 7) {
            for (let i = 1; i <= remaining; i++) {
                const d = new Date(year, month + 1, i);
                days.push({
                    date: formatDate(d),
                    day: d.getDate(),
                    isCurrentMonth: false
                });
            }
        }

        return days;
    }, [currentMonth]);

    const handleDateClick = (dateStr: string) => {
        // 비활성화된 날짜 클릭 무시
        if (minDate && dateStr < minDate) return;

        if (!startDate || (startDate && endDate)) {
            // 첫 클릭 또는 리셋
            onRangeChange(dateStr, null);
        } else {
            // 두 번째 클릭
            if (dateStr < startDate) {
                onRangeChange(dateStr, startDate);
            } else {
                onRangeChange(startDate, dateStr);
            }
        }
    };

    const isInRange = (dateStr: string) => {
        if (!startDate) return false;
        const effectiveEnd = endDate || hoveredDate;
        if (!effectiveEnd) return dateStr === startDate;

        const start = startDate < effectiveEnd ? startDate : effectiveEnd;
        const end = startDate < effectiveEnd ? effectiveEnd : startDate;
        return dateStr >= start && dateStr <= end;
    };

    const isStart = (dateStr: string) => dateStr === startDate;
    const isEnd = (dateStr: string) => dateStr === endDate;

    const prevMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
    };

    const nextMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
    };

    const today = formatDate(new Date());

    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm">
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-4">
                <Button type="button" variant="ghost" size="sm" onClick={prevMonth}>
                    <ChevronLeft size={18} />
                </Button>
                <span className="font-bold text-gray-800">
                    {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
                </span>
                <Button type="button" variant="ghost" size="sm" onClick={nextMonth}>
                    <ChevronRight size={18} />
                </Button>
            </div>

            {/* 요일 헤더 */}
            <div className="grid grid-cols-7 mb-2">
                {weekDays.map((day, idx) => (
                    <div
                        key={day}
                        className={cn(
                            "text-center text-xs font-bold py-2",
                            idx === 0 ? "text-red-400" : idx === 6 ? "text-blue-400" : "text-gray-400"
                        )}
                    >
                        {day}
                    </div>
                ))}
            </div>

            {/* 날짜 그리드 */}
            <div className="grid grid-cols-7 gap-0.5">
                {calendarDays.map(({ date, day, isCurrentMonth }) => {
                    const isDisabled = minDate && date < minDate;
                    const inRange = isInRange(date);
                    const isStartDate = isStart(date);
                    const isEndDate = isEnd(date);
                    const isToday = date === today;
                    const dayOfWeek = new Date(date).getDay();
                    const isMiddleRange = inRange && !isStartDate && !isEndDate;

                    return (
                        <button
                            key={date}
                            type="button"
                            onClick={() => !isDisabled && handleDateClick(date)}
                            onMouseEnter={() => !isDisabled && setHoveredDate(date)}
                            onMouseLeave={() => setHoveredDate(null)}
                            disabled={isDisabled || false}
                            className={cn(
                                "relative h-9 text-sm font-medium transition-all",
                                isDisabled && "text-gray-200 cursor-not-allowed",
                                // 기본 텍스트 색상 (범위 밖)
                                !isDisabled && !isCurrentMonth && !inRange && "text-gray-300",
                                !isDisabled && isCurrentMonth && !inRange && "text-gray-700 hover:bg-gray-100 rounded-lg",
                                !isDisabled && isCurrentMonth && dayOfWeek === 0 && !inRange && "text-red-400",
                                !isDisabled && isCurrentMonth && dayOfWeek === 6 && !inRange && "text-blue-400",
                                // 범위 중간 날짜들 - 연한 배경색 (현재 달/다른 달 모두 적용)
                                !isDisabled && isMiddleRange && "bg-primary-100",
                                !isDisabled && isMiddleRange && isCurrentMonth && "text-primary",
                                !isDisabled && isMiddleRange && !isCurrentMonth && "text-primary/60",
                                // 시작일/종료일 - 진한 배경색
                                !isDisabled && isStartDate && "bg-primary text-white font-bold rounded-l-lg",
                                !isDisabled && isEndDate && "bg-primary text-white font-bold rounded-r-lg",
                                !isDisabled && isStartDate && isEndDate && "rounded-lg",
                                !isDisabled && isToday && !inRange && "ring-2 ring-primary/30 ring-inset rounded-lg"
                            )}
                        >
                            {day}
                        </button>
                    );
                })}
            </div>

            {/* 선택된 범위 표시 */}
            <div className="mt-4 pt-3 border-t border-gray-100">
                <div className="flex items-center justify-center gap-6 text-sm">
                    <div className="flex items-center gap-2">
                        <span className="text-gray-500">시작일</span>
                        <span className={cn("font-bold px-3 py-1 rounded-lg", startDate ? "bg-primary/10 text-primary" : "bg-gray-100 text-gray-300")}>
                            {startDate || '선택하세요'}
                        </span>
                    </div>
                    <span className="text-gray-300 text-lg">→</span>
                    <div className="flex items-center gap-2">
                        <span className="text-gray-500">종료일</span>
                        <span className={cn("font-bold px-3 py-1 rounded-lg", endDate ? "bg-primary/10 text-primary" : "bg-gray-100 text-gray-300")}>
                            {endDate || '선택하세요'}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};
