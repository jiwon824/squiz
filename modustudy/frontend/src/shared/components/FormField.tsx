import React, { InputHTMLAttributes, TextareaHTMLAttributes, forwardRef } from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type FieldSize = 'sm' | 'md' | 'lg';
type FieldStatus = 'default' | 'error' | 'success';

interface BaseFieldProps {
    /** 라벨 텍스트 */
    label?: string;
    /** 에러 메시지 */
    error?: string;
    /** 성공 메시지 */
    success?: string;
    /** 도움말 텍스트 */
    helperText?: string;
    /** 필수 입력 표시 */
    required?: boolean;
    /** 사이즈 */
    size?: FieldSize;
    /** 전체 너비 */
    fullWidth?: boolean;
}

interface InputFieldProps extends BaseFieldProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
    as?: 'input';
}

interface TextareaFieldProps extends BaseFieldProps, Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'size'> {
    as: 'textarea';
    /** textarea 줄 수 */
    rows?: number;
}

type FormFieldProps = InputFieldProps | TextareaFieldProps;

// 사이즈별 스타일
const sizeStyles: Record<FieldSize, { input: string; label: string; message: string }> = {
    sm: {
        input: 'p-2.5 text-sm rounded-xl',
        label: 'text-xs font-semibold mb-1',
        message: 'text-[11px] mt-1',
    },
    md: {
        input: 'p-3.5 text-base rounded-2xl',
        label: 'text-sm font-bold mb-1.5',
        message: 'text-xs mt-1.5',
    },
    lg: {
        input: 'p-4 text-lg rounded-2xl',
        label: 'text-base font-bold mb-2',
        message: 'text-sm mt-2',
    },
};

// 상태별 스타일
const statusStyles: Record<FieldStatus, { input: string; message: string; icon: React.ReactNode }> = {
    default: {
        input: 'border-gray-200 focus:border-[var(--color-primary)] focus:ring-[var(--color-primary)]/20',
        message: 'text-gray-500',
        icon: null,
    },
    error: {
        input: 'border-red-400 focus:border-red-500 focus:ring-red-500/20 bg-red-50/30',
        message: 'text-red-500',
        icon: <AlertCircle size={14} className="flex-shrink-0" />,
    },
    success: {
        input: 'border-green-400 focus:border-green-500 focus:ring-green-500/20 bg-green-50/30',
        message: 'text-green-600',
        icon: <CheckCircle2 size={14} className="flex-shrink-0" />,
    },
};

export const FormField = forwardRef<HTMLInputElement | HTMLTextAreaElement, FormFieldProps>(
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
            as = 'input',
            ...rest
        } = props;

        // 현재 상태 결정
        const status: FieldStatus = error ? 'error' : success ? 'success' : 'default';
        const message = error || success || helperText;

        // 스타일 가져오기
        const sizeStyle = sizeStyles[size];
        const statusStyle = statusStyles[status];

        // 공통 input 스타일
        const inputClassName = cn(
            'w-full bg-gray-50 border-2 outline-none transition-all duration-200',
            'focus:ring-2 focus:bg-white',
            'disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed',
            'placeholder:text-gray-400',
            sizeStyle.input,
            statusStyle.input,
            className
        );

        return (
            <div className={cn('flex flex-col', fullWidth ? 'w-full' : 'w-auto')}>
                {/* 라벨 */}
                {label && (
                    <label className={cn('text-gray-700 ml-1', sizeStyle.label)}>
                        {label}
                        {required && <span className="text-red-500 ml-0.5">*</span>}
                    </label>
                )}

                {/* Input 또는 Textarea */}
                {as === 'textarea' ? (
                    <textarea
                        ref={ref as React.Ref<HTMLTextAreaElement>}
                        className={cn(inputClassName, 'resize-none min-h-[100px]')}
                        rows={(rest as TextareaFieldProps).rows || 3}
                        {...(rest as TextareaHTMLAttributes<HTMLTextAreaElement>)}
                    />
                ) : (
                    <input
                        ref={ref as React.Ref<HTMLInputElement>}
                        className={inputClassName}
                        {...(rest as InputHTMLAttributes<HTMLInputElement>)}
                    />
                )}

                {/* 메시지 (에러/성공/도움말) */}
                {message && (
                    <p className={cn('flex items-center gap-1 ml-1', sizeStyle.message, statusStyle.message)}>
                        {statusStyle.icon}
                        <span>{message}</span>
                    </p>
                )}
            </div>
        );
    }
);

FormField.displayName = 'FormField';

export default FormField;
