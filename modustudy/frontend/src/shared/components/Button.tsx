import React, { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'dark' | 'google-primary' | 'google-outline' | 'google-ghost' | 'back-button' | 'arrow-transparent' | 'text';
type ButtonSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    size?: ButtonSize;
    isLoading?: boolean;
    leftIcon?: ReactNode;
    rightIcon?: ReactNode;
    fullWidth?: boolean;
    isCircle?: boolean;
    tooltip?: string;
}

// 버튼 스타일 정의
const styles = {
    base: 'cursor-pointer group relative inline-flex items-center justify-center font-semibold transition-all duration-200 active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100 disabled:cursor-not-allowed',
    variants: {
        // 메인 프라이머리 (파란색 배경)
        primary: 'bg-blue-600 hover:bg-blue-700 text-white shadow-md hover:shadow-lg',
        // 세컨더리 (밝은 배경)
        secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-900 shadow-sm',
        // 아웃라인 (테두리)
        outline: 'border-2 border-gray-300 bg-white text-gray-900 hover:bg-gray-50 hover:border-gray-400',
        // 고스트 (배경 없음)
        ghost: 'bg-transparent hover:bg-gray-100 text-gray-900',
        // 위험 (삭제 등)
        danger: 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/20',
        // 다크 (제공된 스타일 기반)
        dark: 'bg-black bg-opacity-80 text-white hover:bg-opacity-70 shadow-md',
        // 구글 캘린더 모티브 스타일 (Material 3 컨셉)
        'google-primary': 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm hover:shadow-md',
        'google-outline': 'bg-white border border-gray-300 text-blue-600 hover:bg-blue-50 hover:border-blue-200 shadow-sm',
        'google-ghost': 'bg-transparent text-gray-900 hover:bg-gray-100',
        // 전용 네비게이션 버튼
        'back-button': 'bg-transparent text-gray-900 hover:bg-gray-100',
        'arrow-transparent': 'bg-transparent text-gray-700 hover:bg-gray-50 border-none shadow-none',
        // 텍스트 전용 버튼 (배경 없음, 테두리 없음)
        'text': 'bg-transparent border-none text-primary hover:text-primary-dark shadow-none',
    },
};

export const Button: React.FC<ButtonProps> = ({
    children,
    variant = 'primary',
    size = 'md',
    isLoading = false,
    leftIcon,
    rightIcon,
    fullWidth = false,
    isCircle = false,
    tooltip,
    className = '',
    disabled,
    ...props
}) => {
    // 사이즈 스타일 (원형일 때와 아닐 때 구분)
    const getSizeStyles = (isCircle: boolean): Record<ButtonSize, string> => ({
        xs: isCircle ? 'w-6 h-6 rounded-full p-0' : 'text-xs py-1 px-2 rounded-lg gap-1',
        sm: isCircle ? 'w-8 h-8 rounded-full p-0' : 'text-sm py-2 px-4 rounded-2xl gap-1.5',
        md: isCircle ? 'w-10 h-10 rounded-full p-0' : 'text-sm py-3 px-6 rounded-3xl gap-1.5',
        lg: isCircle ? 'w-12 h-12 rounded-full p-0' : 'text-base py-4 px-8 rounded-3xl gap-1.5',
        xl: isCircle ? 'w-14 h-14 rounded-full p-0' : 'text-lg py-5 px-10 rounded-3xl gap-2',
    });

    const sizeStyles = getSizeStyles(isCircle);

    return (
        <button
            className={cn(
                styles.base,
                styles.variants[variant],
                sizeStyles[size],
                {
                    'w-full': fullWidth,
                },
                className
            )}
            disabled={isLoading || disabled}
            {...props}
        >
            {isLoading && <Loader2 className="animate-spin" size={size === 'sm' ? 14 : 18} />}
            {!isLoading && leftIcon && <span className="flex-shrink-0">{leftIcon}</span>}
            {children && <span className="truncate">{children}</span>}
            {!isLoading && rightIcon && <span className="flex-shrink-0">{rightIcon}</span>}

            {/* 툴팁 (hover 시 표시) */}
            {tooltip && (
                <div className="absolute opacity-0 -bottom-full rounded-md py-2 px-2 bg-black bg-opacity-70 left-1/2 -translate-x-1/2 group-hover:opacity-100 transition-opacity shadow-lg text-xs text-white whitespace-nowrap pointer-events-none">
                    {tooltip}
                </div>
            )}
        </button>
    );
};
