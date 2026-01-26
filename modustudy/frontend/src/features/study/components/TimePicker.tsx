import { useState, useRef, useEffect } from 'react';
import { Clock } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface TimePickerProps {
    value: string;
    onChange: (time: string) => void;
    className?: string;
}

export const TimePicker = ({ value, onChange, className }: TimePickerProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    const [selectedHour, setSelectedHour] = useState('00');
    const [selectedMinute, setSelectedMinute] = useState('00');

    useEffect(() => {
        if (value && value.includes(':')) {
            const [h, m] = value.split(':');
            setSelectedHour(h);
            setSelectedMinute(m);
        }
    }, [value]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        if (isOpen) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isOpen]);

    // 00-23
    const hours = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
    // 00-55 (5분 단위)
    const minutes = Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0'));

    const handleTimeChange = (type: 'hour' | 'minute', val: string) => {
        let newH = selectedHour;
        let newM = selectedMinute;

        if (type === 'hour') {
            newH = val;
            setSelectedHour(val);
        } else {
            newM = val;
            setSelectedMinute(val);
        }
        onChange(`${newH}:${newM}`);
    };

    return (
        <div className={cn("relative w-full", className)} ref={containerRef}>
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className={cn(
                    "w-full flex items-center justify-between p-3.5 bg-gray-50 border border-gray-200 rounded-xl transition-all text-base",
                    "focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary",
                    "hover:bg-white",
                    isOpen && "border-primary ring-2 ring-primary/20 bg-white"
                )}
            >
                <div className="flex items-center gap-2 text-gray-800">
                    <Clock size={18} className="text-gray-500" />
                    <span className="font-medium">{value || '시간 선택'}</span>
                </div>
            </button>

            {isOpen && (
                <div className="absolute z-50 mt-2 p-2 bg-white border border-gray-100 rounded-xl shadow-xl animate-in fade-in zoom-in-95 duration-100 w-full min-w-[200px]">
                    <div className="flex gap-2 h-64">
                        {/* Hours */}
                        <div className="flex-1 flex flex-col gap-1 overflow-y-auto">
                            <div className="text-xs font-semibold text-gray-400 text-center py-1 sticky top-0 bg-white">시</div>
                            {hours.map(h => (
                                <button
                                    key={h}
                                    type="button"
                                    onClick={() => handleTimeChange('hour', h)}
                                    className={cn(
                                        "py-2 px-1 rounded-lg text-sm text-center transition-colors shrink-0",
                                        selectedHour === h
                                            ? "bg-primary text-white font-bold"
                                            : "text-gray-600 hover:bg-gray-100"
                                    )}
                                >
                                    {h}
                                </button>
                            ))}
                        </div>

                        <div className="w-[1px] bg-gray-100 my-2" />

                        {/* Minutes */}
                        <div className="flex-1 flex flex-col gap-1 overflow-y-auto">
                            <div className="text-xs font-semibold text-gray-400 text-center py-1 sticky top-0 bg-white">분</div>
                            {minutes.map(m => (
                                <button
                                    key={m}
                                    type="button"
                                    onClick={() => handleTimeChange('minute', m)}
                                    className={cn(
                                        "py-2 px-1 rounded-lg text-sm text-center transition-colors shrink-0",
                                        selectedMinute === m
                                            ? "bg-primary text-white font-bold"
                                            : "text-gray-600 hover:bg-gray-100"
                                    )}
                                >
                                    {m}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
