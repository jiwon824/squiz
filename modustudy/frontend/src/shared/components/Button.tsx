import React, { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'google-primary' | 'google-outline' | 'google-ghost';
type ButtonSize = 'sm' | 'md' | 'lg' | 'xl';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    size?: ButtonSize;
    isLoading?: boolean;
    leftIcon?: ReactNode;
    rightIcon?: ReactNode;
    fullWidth?: boolean;
    isCircle?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
    children,
    variant = 'google-primary',
    size = 'md',
    isLoading = false,
    leftIcon,
    rightIcon,
    fullWidth = false,
    isCircle = false,
    className = '',
    disabled,
    ...props
}) => {
    const baseStyles = 'inline-flex items-center justify-center font-semibold transition-all duration-200 active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100 disabled:cursor-not-allowed';
    // 변체 스타일
    const variants: Record<ButtonVariant, string> = {
        // 프로젝트 메인 브랜드 스타일 (그라디언트)
        primary: 'bg-gradient-to-br from-[#3b82f6] to-[#1d4ed8] text-white shadow-lg shadow-blue-500/30 hover:shadow-blue-500/40 hover:-translate-y-0.5 active:translate-y-0',
        secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
        outline: 'border-2 border-[#3b82f6] text-[#3b82f6] hover:bg-blue-50',
        ghost: 'hover:bg-gray-100 text-gray-600',
        danger: 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/20',
        // 구글 캘린더 모티브 스타일 (Material 3 컨셉)
        'google-primary': 'bg-[#1a73e8] hover:bg-[#1765cc] text-white shadow-sm hover:shadow-md active:bg-[#1557b0]',
        'google-outline': 'bg-white border border-[#dadce0] text-[#1a73e8] hover:bg-[#f8f9fa] hover:border-[#d2e3fc] shadow-sm',
        'google-ghost': 'bg-transparent text-[#3c4043] hover:bg-[#f1f3f4] font-medium',
    };

    // 사이즈 스타일 (원형일 때와 아닐 때 구분)
    const sizes: Record<ButtonSize, string> = {
        sm: isCircle ? 'w-8 h-8 rounded-full p-0' : 'px-3 py-1.5 text-xs rounded-lg gap-1.5',
        md: isCircle ? 'w-10 h-10 rounded-full p-0' : 'px-6 py-2.5 text-sm rounded-xl gap-2',
        lg: isCircle ? 'w-12 h-12 rounded-full p-0' : 'px-8 py-3.5 text-base rounded-2xl gap-2.5',
        xl: isCircle ? 'w-14 h-14 rounded-full p-0' : 'px-10 py-4 text-lg rounded-3xl gap-3',
    };

    const widthStyle = fullWidth ? 'w-full' : '';

    return (
        <button
            className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${widthStyle} ${className}`}
            disabled={isLoading || disabled}
            {...props}
        >
            {isLoading && <Loader2 className="animate-spin" size={size === 'sm' ? 14 : 18} />}
            {!isLoading && leftIcon && <span className="flex-shrink-0">{leftIcon}</span>}
            {children && <span className="truncate">{children}</span>}
            {!isLoading && rightIcon && <span className="flex-shrink-0">{rightIcon}</span>}
        </button>
    );
};
