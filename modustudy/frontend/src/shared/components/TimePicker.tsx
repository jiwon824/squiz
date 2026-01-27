import { useState, useRef, useEffect } from 'react';
import { Clock } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface TimePickerProps {
    value: string;
    onChange: (time: string) => void;
    placeholder?: string;
    className?: string;
}

/**
 * 시간 선택용 팝오버 픽커 (Shared)
 */
export const TimePicker = ({ value, onChange, placeholder = "시간 선택", className }: TimePickerProps) => {
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
                    "w-full flex items-center justify-between px-3.5 py-2.5 h-[50px] bg-gray-50 border border-gray-200 rounded-2xl transition-all outline-none",
                    "hover:bg-white hover:border-study-blue/30",
                    isOpen && "border-study-blue ring-2 ring-study-blue/10 bg-white"
                )}
            >
                <div className="flex items-center gap-2 text-gray-800">
                    <Clock size={18} className={cn("transition-colors", isOpen ? "text-study-blue" : "text-gray-400")} />
                    <span className={cn("text-sm font-bold", !value && "text-gray-400")}>
                        {value || placeholder}
                    </span>
                </div>
            </button>

            {isOpen && (
                <div className="absolute z-50 mt-2 p-3 bg-white border border-gray-200 rounded-[32px] shadow-[0_20px_50px_rgba(0,0,0,0.1)] w-full min-w-[200px] animate-in fade-in zoom-in-95 duration-200 origin-top">
                    <div className="flex gap-2 h-64">
                        {/* Hours */}
                        <div className="flex-1 flex flex-col gap-1 overflow-y-auto pr-1 custom-scrollbar">
                            <div className="text-[10px] font-black text-gray-300 text-center py-2 sticky top-0 bg-white uppercase tracking-widest">HOUR</div>
                            {hours.map(h => (
                                <button
                                    key={h}
                                    type="button"
                                    onClick={() => handleTimeChange('hour', h)}
                                    className={cn(
                                        "py-2.5 rounded-xl text-sm font-bold text-center transition-all shrink-0",
                                        selectedHour === h
                                            ? "bg-study-blue text-white shadow-md shadow-study-blue/20"
                                            : "text-text-secondary hover:bg-gray-50 hover:text-study-blue"
                                    )}
                                >
                                    {h}
                                </button>
                            ))}
                        </div>

                        <div className="w-[1px] bg-gray-100 my-4" />

                        {/* Minutes */}
                        <div className="flex-1 flex flex-col gap-1 overflow-y-auto pr-1 custom-scrollbar">
                            <div className="text-[10px] font-black text-gray-300 text-center py-2 sticky top-0 bg-white uppercase tracking-widest">MIN</div>
                            {minutes.map(m => (
                                <button
                                    key={m}
                                    type="button"
                                    onClick={() => handleTimeChange('minute', m)}
                                    className={cn(
                                        "py-2.5 rounded-xl text-sm font-bold text-center transition-all shrink-0",
                                        selectedMinute === m
                                            ? "bg-study-blue text-white shadow-md shadow-study-blue/20"
                                            : "text-text-secondary hover:bg-gray-50 hover:text-study-blue"
                                    )}
                                >
                                    {m}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            <style dangerouslySetInnerHTML={{
                __html: `
                .custom-scrollbar::-webkit-scrollbar {
                    width: 4px;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background: #edf2f7;
                    border-radius: 10px;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb:hover {
                    background: #e2e8f0;
                }
            `}} />
        </div>
    );
};
