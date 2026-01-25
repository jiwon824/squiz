import React, { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'google-primary' | 'google-outline' | 'google-ghost' | 'back-button' | 'arrow-transparent';
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
        primary: 'bg-gradient-to-br from-blue-500 to-blue-700 text-white shadow-lg shadow-blue-500/30 hover:shadow-blue-500/40 hover:-translate-y-0.5 active:translate-y-0',
        secondary: 'bg-surface-200 hover:bg-surface-300 text-on-surface',
        outline: 'border-2 border-primary-500 text-primary-500 hover:bg-primary-50',
        ghost: 'hover:bg-surface-200 text-on-surface-variant',
        danger: 'bg-error-500 hover:bg-error-600 text-on-error shadow-lg shadow-error-500/20',
        // 구글 캘린더 모티브 스타일 (Material 3 컨셉)
        'google-primary': 'bg-primary-500 hover:bg-primary-600 text-on-primary shadow-sm hover:shadow-md active:bg-primary-700',
        'google-outline': 'bg-surface-50 border border-outline text-primary-500 hover:bg-primary-50 hover:border-primary-200 shadow-sm',
        'google-ghost': 'bg-transparent text-on-surface-variant hover:bg-surface-200 font-medium',
        // 전용 네비게이션 버튼
        'back-button': 'bg-transparent text-on-surface hover:bg-surface-200 font-medium',
        'arrow-transparent': 'bg-transparent text-on-surface-variant hover:bg-surface-100 border-none shadow-none',
    };

    // 사이즈 스타일 (원형일 때와 아닐 때 구분)
    // - 모바일: 14-16px (text-sm/text-base)
    // - 태블릿: 14-17px (text-sm/text-base+)
    // - 데스크탑: 15-20px (text-base/text-xl)
    const sizes: Record<ButtonSize, string> = {
        sm: isCircle ? 'w-8 h-8 rounded-full p-0' : 'text-sm md:text-sm lg:text-[15px] rounded-lg gap-1.5',
        md: isCircle ? 'w-10 h-10 rounded-full p-0' : 'text-sm md:text-[15px] lg:text-[17px] rounded-xl gap-2',
        lg: isCircle ? 'w-12 h-12 rounded-full p-0' : 'text-base md:text-[16px] lg:text-[18px] rounded-2xl gap-2.5',
        xl: isCircle ? 'w-14 h-14 rounded-full p-0' : 'text-base md:text-[17px] lg:text-[20px] rounded-3xl gap-3',
    };

    // 패딩 규칙 (아이콘 포함 시 12:20, 글자만 있을 시 20:20)
    const hasIcon = !!leftIcon || !!rightIcon || isLoading;
    const paddingStyles = isCircle ? '' : (hasIcon ? 'pl-[12px] pr-[20px]' : 'px-[20px]');

    return (
        <button
            className={cn(
                baseStyles,
                variants[variant],
                sizes[size],
                paddingStyles,
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
        </button>
    );
};
