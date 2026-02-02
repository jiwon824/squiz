import React, { InputHTMLAttributes, forwardRef, useState, useId } from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type FloatingSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
type FloatingStatus = 'default' | 'error' | 'success';

interface FloatingInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /** 플로팅 라벨 텍스트 */
  label: string;
  /** 에러 메시지 */
  error?: string;
  /** 성공 메시지 */
  success?: string;
  /** 도움말 텍스트 */
  helperText?: string;
  /** 필수 입력 표시 */
  required?: boolean;
  /** 사이즈 */
  size?: FloatingSize;
  /** 전체 너비 */
  fullWidth?: boolean;
}

// 사이즈별 스타일
const sizeStyles: Record<FloatingSize, { wrapper: string; input: string; label: string; labelFloated: string; message: string }> = {
  xs: {
    wrapper: 'h-9',
    input: 'px-2.5 pt-4 pb-1 text-xs rounded-lg',
    label: 'text-xs left-2.5 top-2.5',
    labelFloated: 'text-[10px] left-2.5 top-1',
    message: 'text-[11px] mt-1',
  },
  sm: {
    wrapper: 'h-10',
    input: 'px-3 pt-4.5 pb-1.5 text-sm rounded-xl',
    label: 'text-sm left-3 top-2.5',
    labelFloated: 'text-[11px] left-3 top-1',
    message: 'text-xs mt-1',
  },
  md: {
    wrapper: 'h-12',
    input: 'px-3.5 pt-5 pb-2 text-base rounded-2xl',
    label: 'text-base left-3.5 top-3',
    labelFloated: 'text-xs left-3.5 top-1.5',
    message: 'text-xs mt-1.5',
  },
  lg: {
    wrapper: 'h-14',
    input: 'px-4 pt-6 pb-2.5 text-lg rounded-2xl',
    label: 'text-lg left-4 top-3.5',
    labelFloated: 'text-xs left-4 top-2',
    message: 'text-sm mt-1.5',
  },
  xl: {
    wrapper: 'h-16',
    input: 'px-4 pt-7 pb-3 text-xl rounded-2xl',
    label: 'text-xl left-4 top-4',
    labelFloated: 'text-sm left-4 top-2',
    message: 'text-sm mt-2',
  },
};

// 상태별 스타일 (배경색 고정, 포커스 효과 없음)
const statusStyles: Record<FloatingStatus, { input: string; label: string; message: string; icon: React.ReactNode }> = {
  default: {
    input: '',
    label: 'text-gray-400',
    message: 'text-gray-500',
    icon: null,
  },
  error: {
    input: 'bg-red-50/30',
    label: 'text-red-400',
    message: 'text-red-500',
    icon: <AlertCircle size={14} className="flex-shrink-0" />,
  },
  success: {
    input: 'bg-green-50/30',
    label: 'text-green-500',
    message: 'text-green-600',
    icon: <CheckCircle2 size={14} className="flex-shrink-0" />,
  },
};

export const FloatingInput = forwardRef<HTMLInputElement, FloatingInputProps>(
  (props, ref) => {
    const {
      label,
      error,
      success,
      helperText,
      required,
      size = 'md',
      fullWidth = true,
      className,
      id: externalId,
      value,
      defaultValue,
      onFocus,
      onBlur,
      onChange,
      ...rest
    } = props;

    const generatedId = useId();
    const inputId = externalId || generatedId;

    const [isFocused, setIsFocused] = useState(false);
    const [internalValue, setInternalValue] = useState(defaultValue ?? '');

    // 실제 값 (controlled / uncontrolled 모두 대응)
    const currentValue = value !== undefined ? value : internalValue;
    const isFloated = isFocused || Boolean(currentValue);

    // 상태 결정
    const status: FloatingStatus = error ? 'error' : success ? 'success' : 'default';
    const message = error || success || helperText;

    const sizeStyle = sizeStyles[size];
    const statusStyle = statusStyles[status];

    const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
      setIsFocused(true);
      onFocus?.(e);
    };

    const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
      setIsFocused(false);
      onBlur?.(e);
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (value === undefined) {
        setInternalValue(e.target.value);
      }
      onChange?.(e);
    };

    return (
      <div className={cn('flex flex-col', fullWidth ? 'w-full' : 'w-auto')}>
        {/* 플로팅 인풋 래퍼 */}
        <div className={cn('relative', sizeStyle.wrapper)}>
          <input
            ref={ref}
            id={inputId}
            className={cn(
              'peer w-full h-full bg-gray-50 border-none outline-none',
              'disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed',
              'placeholder-transparent',
              sizeStyle.input,
              statusStyle.input,
              className,
            )}
            placeholder={label}
            value={value}
            defaultValue={value !== undefined ? undefined : defaultValue}
            onFocus={handleFocus}
            onBlur={handleBlur}
            onChange={handleChange}
            {...rest}
          />
          <label
            htmlFor={inputId}
            className={cn(
              'absolute pointer-events-none',
              'transition-all duration-200 ease-out',
              'origin-left',
              statusStyle.label,
              isFloated
                ? cn(sizeStyle.labelFloated, 'font-medium')
                : cn(sizeStyle.label, 'font-normal'),
            )}
          >
            {label}
            {required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
        </div>

        {/* 메시지 (에러/성공/도움말) */}
        {message && (
          <p className={cn('flex items-center gap-1 ml-1', sizeStyle.message, statusStyle.message)}>
            {statusStyle.icon}
            <span>{message}</span>
          </p>
        )}
      </div>
    );
  },
);

FloatingInput.displayName = 'FloatingInput';

export default FloatingInput;
