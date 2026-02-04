import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export interface SelectOption {
    value: string | number;
    label: string;
}

interface SelectProps {
    value?: string | number;
    onChange: (value: string) => void;
    options: (SelectOption | string)[];
    placeholder?: string;
    label?: string;
    className?: string;
    buttonClassName?: string;
    disabled?: boolean;
    name?: string;
}

export const Select: React.FC<SelectProps> = ({
    value,
    onChange,
    options,
    placeholder = '선택하세요',
    label,
    className,
    buttonClassName,
    disabled = false,
    name: _name
}) => {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    // Close on click outside
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

    const handleSelect = (optionValue: string | number) => {
        if (disabled) return;
        onChange(String(optionValue));
        setIsOpen(false);
    };

    // Helper to get label and value objects safely
    const normalizedOptions: SelectOption[] = options.map(opt =>
        typeof opt === 'string' ? { value: opt, label: opt } : opt
    );

    const selectedOption = normalizedOptions.find(opt => opt.value === value);

    return (
        <div className={cn("w-full mb-1.5", className)} ref={containerRef}>
            {label && (
                <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    {label}
                </label>
            )}

            <div className="relative">
                <button
                    type="button"
                    onClick={() => !disabled && setIsOpen(!isOpen)}
                    disabled={disabled}
                    className={cn(
                        "w-full text-left flex items-center justify-between transition-all",
                        "focus:outline-none focus:ring-2 focus:ring-primary/20",
                        // 기본 외형 (buttonClassName 제공 시 대체됨)
                        buttonClassName
                            ? buttonClassName
                            : "p-3.5 bg-gray-50 border border-gray-200 rounded-xl text-base hover:bg-white",
                        // 상태별 스타일
                        disabled && "opacity-50 cursor-not-allowed bg-gray-100",
                        !buttonClassName && isOpen && "border-primary ring-2 ring-primary/20 bg-white",
                        !selectedOption && "text-gray-400",
                        selectedOption && "text-gray-800"
                    )}
                >
                    <span className="truncate">
                        {selectedOption ? selectedOption.label : placeholder}
                    </span>
                    <ChevronDown
                        size={16}
                        className={cn(
                            "text-gray-400 transition-transform duration-200",
                            isOpen && "transform rotate-180 text-primary"
                        )}
                    />
                </button>

                {isOpen && (
                    <div className="absolute z-50 w-full min-w-max mt-1.5 bg-white border border-gray-100 rounded-xl shadow-lg max-h-60 overflow-y-auto animate-in fade-in zoom-in-95 duration-100">
                        {normalizedOptions.length > 0 ? (
                            <div className="p-1">
                                {normalizedOptions.map((option) => (
                                    <button
                                        key={option.value}
                                        type="button"
                                        onClick={() => handleSelect(option.value)}
                                        className={cn(
                                            "w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm text-left transition-colors",
                                            option.value === value
                                                ? "bg-primary/5 text-primary font-medium"
                                                : "text-gray-700 hover:bg-gray-50"
                                        )}
                                    >
                                        <span>{option.label}</span>
                                        {option.value === value && (
                                            <Check size={16} className="text-primary" />
                                        )}
                                    </button>
                                ))}
                            </div>
                        ) : (
                            <div className="p-4 text-center text-gray-400 text-sm">
                                선택 가능한 항목이 없습니다
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};
