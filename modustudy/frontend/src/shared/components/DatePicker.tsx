import { useState, useMemo, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Calendar } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';

interface DatePickerProps {
    value: string;
    onChange: (date: string) => void;
    min?: string;
    max?: string;
    placeholder?: string;
    className?: string;
}

/**
 * 단일 날짜 선택용 팝오버 캘린더 (Shared)
 */
export const DatePicker = ({ value, onChange, min, max, placeholder = "날짜 선택", className }: DatePickerProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const [currentMonth, setCurrentMonth] = useState(value ? new Date(value) : new Date());

    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];

    // 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

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
        onChange(dateStr);
        setIsOpen(false);
    };

    const prevMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
    };

    const nextMonth = () => {
        setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
    };

    const today = formatDate(new Date());

    return (
        <div className={cn("relative w-full", className)} ref={containerRef}>
            {/* 트리거 버튼 (Input 모양) */}
            <div
                onClick={() => setIsOpen(!isOpen)}
                className={cn(
                    "flex items-center w-full px-3.5 py-2.5 h-[50px] bg-gray-50 border border-gray-200 rounded-2xl cursor-pointer transition-all hover:bg-white hover:border-study-blue/30",
                    isOpen && "ring-2 ring-study-blue/10 border-study-blue bg-white",
                    !value && "text-gray-400"
                )}
            >
                <span className="flex-1 text-sm font-bold text-gray-800">
                    {value || placeholder}
                </span>
                <Calendar size={18} className={cn("transition-colors", isOpen ? "text-study-blue" : "text-gray-400")} />
            </div>

            {/* 팝오버 캘린더 */}
            {isOpen && (
                <div className="absolute top-full left-0 mt-2 z-50 bg-white border border-gray-200 rounded-[32px] shadow-[0_20px_50px_rgba(0,0,0,0.1)] w-[320px] p-6 animate-in fade-in zoom-in-95 duration-200 origin-top">
                    {/* 헤더 */}
                    <div className="flex items-center justify-between mb-6">
                        <Button variant="google-ghost" size="sm" isCircle onClick={prevMonth}>
                            <ChevronLeft size={20} />
                        </Button>
                        <span className="font-black text-gray-800 tracking-tight">
                            {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
                        </span>
                        <Button variant="google-ghost" size="sm" isCircle onClick={nextMonth}>
                            <ChevronRight size={20} />
                        </Button>
                    </div>

                    {/* 요일 헤더 */}
                    <div className="grid grid-cols-7 mb-3">
                        {weekDays.map((day, idx) => (
                            <div
                                key={day}
                                className={cn(
                                    "text-center text-[11px] font-black py-2 uppercase tracking-widest",
                                    idx === 0 ? "text-red-400" : idx === 6 ? "text-study-blue" : "text-gray-300"
                                )}
                            >
                                {day}
                            </div>
                        ))}
                    </div>

                    {/* 날짜 그리드 */}
                    <div className="grid grid-cols-7 gap-1">
                        {calendarDays.map(({ date, day, isCurrentMonth }) => {
                            const isDisabled = (min && date < min) || (max && date > max);
                            const isSelected = value === date;
                            const isTodayDate = date === today;
                            const dayOfWeek = new Date(date).getDay();

                            return (
                                <button
                                    key={date}
                                    type="button"
                                    onClick={() => !isDisabled && handleDateClick(date)}
                                    disabled={isDisabled || false}
                                    className={cn(
                                        "h-9 w-9 mx-auto flex items-center justify-center text-sm font-bold rounded-xl transition-all relative group",
                                        isDisabled && "text-gray-200 cursor-not-allowed",
                                        !isDisabled && !isCurrentMonth && "text-gray-300",

                                        // 기본 상태
                                        !isDisabled && isCurrentMonth && !isSelected && "text-text-secondary hover:bg-gray-50 hover:text-study-blue",

                                        // 주말 색상
                                        !isDisabled && isCurrentMonth && dayOfWeek === 0 && !isSelected && "text-red-400",
                                        !isDisabled && isCurrentMonth && dayOfWeek === 6 && !isSelected && "text-study-blue",

                                        // 선택됨 (가장 우선순위 높음)
                                        isSelected && "bg-study-blue text-white shadow-lg shadow-study-blue/20",

                                        // 오늘 날짜 표시 (점)
                                        !isSelected && isTodayDate && !isDisabled && "text-study-blue font-black bg-study-blue/5"
                                    )}
                                >
                                    {day}
                                    {isTodayDate && !isSelected && (
                                        <div className="absolute bottom-1 w-1 h-1 bg-study-blue rounded-full" />
                                    )}
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
};
