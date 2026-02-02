import React, { InputHTMLAttributes, forwardRef } from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type IconInputSize = 'sm' | 'md' | 'lg';
type IconInputStatus = 'default' | 'error' | 'success';

interface IconInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /** 왼쪽 아이콘 */
  leftIcon?: React.ReactNode;
  /** 오른쪽 아이콘 */
  rightIcon?: React.ReactNode;
  /** 에러 메시지 */
  error?: string;
  /** 성공 메시지 */
  success?: string;
  /** 도움말 텍스트 */
  helperText?: string;
  /** 사이즈 */
  size?: IconInputSize;
  /** 전체 너비 */
  fullWidth?: boolean;
}

// 사이즈별 스타일
const sizeStyles: Record<IconInputSize, {
  input: string;
  inputWithLeftIcon: string;
  inputWithRightIcon: string;
  icon: string;
  iconRight: string;
  message: string;
}> = {
  sm: {
    input: 'py-2.5 px-3.5 text-sm rounded-xl',
    inputWithLeftIcon: 'ps-10',
    inputWithRightIcon: 'pe-10',
    icon: 'ps-3.5 [&>svg]:size-4',
    iconRight: 'pe-3.5 [&>svg]:size-4',
    message: 'text-[11px] mt-1',
  },
  md: {
    input: 'py-3 px-4 text-sm rounded-2xl',
    inputWithLeftIcon: 'ps-12',
    inputWithRightIcon: 'pe-12',
    icon: 'ps-4 [&>svg]:size-4',
    iconRight: 'pe-4 [&>svg]:size-4',
    message: 'text-xs mt-1.5',
  },
  lg: {
    input: 'py-3.5 px-5 text-base rounded-2xl',
    inputWithLeftIcon: 'ps-14',
    inputWithRightIcon: 'pe-14',
    icon: 'ps-5 [&>svg]:size-5',
    iconRight: 'pe-5 [&>svg]:size-5',
    message: 'text-sm mt-2',
  },
};

// 상태별 스타일 (border 없이 shadow glow + 배경 전환)
const statusStyles: Record<IconInputStatus, {
  input: string;
  icon: string;
  iconFocus: string;
  message: string;
  messageIcon: React.ReactNode;
}> = {
  default: {
    input: 'shadow-sm focus:bg-white focus:shadow-[0_4px_20px_rgba(66,133,244,0.12)]',
    icon: 'text-gray-400',
    iconFocus: 'peer-focus:text-[var(--color-primary)]',
    message: 'text-gray-500',
    messageIcon: null,
  },
  error: {
    input: 'bg-red-50/40 shadow-sm focus:bg-white focus:shadow-[0_4px_20px_rgba(234,67,53,0.12)]',
    icon: 'text-red-400',
    iconFocus: 'peer-focus:text-red-500',
    message: 'text-red-500',
    messageIcon: <AlertCircle size={14} className="flex-shrink-0" />,
  },
  success: {
    input: 'bg-green-50/40 shadow-sm focus:bg-white focus:shadow-[0_4px_20px_rgba(52,168,83,0.12)]',
    icon: 'text-green-500',
    iconFocus: 'peer-focus:text-green-600',
    message: 'text-green-600',
    messageIcon: <CheckCircle2 size={14} className="flex-shrink-0" />,
  },
};

export const IconInput = forwardRef<HTMLInputElement, IconInputProps>(
  (props, ref) => {
    const {
      leftIcon,
      rightIcon,
      error,
      success,
      helperText,
      size = 'md',
      fullWidth = true,
      className,
      disabled,
      ...rest
    } = props;

    const status: IconInputStatus = error ? 'error' : success ? 'success' : 'default';
    const message = error || success || helperText;

    const sizeStyle = sizeStyles[size];
    const statusStyle = statusStyles[status];

    return (
      <div className={cn('flex flex-col', fullWidth ? 'w-full' : 'w-auto')}>
        <div className="relative">
          <input
            ref={ref}
            disabled={disabled}
            className={cn(
              'peer w-full bg-gray-50 border-none outline-none',
              'transition-all duration-300 ease-out',
              'placeholder:text-gray-400',
              'disabled:opacity-50 disabled:cursor-not-allowed',
              sizeStyle.input,
              leftIcon && sizeStyle.inputWithLeftIcon,
              rightIcon && sizeStyle.inputWithRightIcon,
              statusStyle.input,
              className,
            )}
            {...rest}
          />

          {/* 왼쪽 아이콘 */}
          {leftIcon && (
            <div
              className={cn(
                'absolute inset-y-0 start-0 flex items-center pointer-events-none',
                'transition-colors duration-300',
                sizeStyle.icon,
                statusStyle.icon,
                statusStyle.iconFocus,
                disabled && 'opacity-50',
              )}
            >
              {leftIcon}
            </div>
          )}

          {/* 오른쪽 아이콘 */}
          {rightIcon && (
            <div
              className={cn(
                'absolute inset-y-0 end-0 flex items-center pointer-events-none',
                'transition-colors duration-300',
                sizeStyle.iconRight,
                statusStyle.icon,
                statusStyle.iconFocus,
                disabled && 'opacity-50',
              )}
            >
              {rightIcon}
            </div>
          )}
        </div>

        {/* 메시지 (에러/성공/도움말) */}
        {message && (
          <p className={cn('flex items-center gap-1 ml-1', sizeStyle.message, statusStyle.message)}>
            {statusStyle.messageIcon}
            <span>{message}</span>
          </p>
        )}
      </div>
    );
  },
);

IconInput.displayName = 'IconInput';

export default IconInput;
