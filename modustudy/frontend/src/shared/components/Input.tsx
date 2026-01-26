import React, { InputHTMLAttributes } from 'react';
import { cn } from '@/shared/utils/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    label?: string;
    error?: string;
}

export const Input: React.FC<InputProps> = ({ label, error, className = '', ...props }) => {
    const styles = {
        container: 'flex flex-col gap-1.5 w-full',
        label: 'text-sm font-bold text-gray-700 ml-1',
        input: cn(
            'w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl',
            'focus:ring-2 focus:ring-study-blue/20 focus:border-study-blue',
            'transition-all outline-none',
            error && 'border-red-500'
        ),
        errorMessage: 'text-xs text-red-500 ml-1',
    };

    return (
        <div className={styles.container}>
            {label && <label className={styles.label}>{label}</label>}
            <input
                className={cn(styles.input, className)}
                {...props}
            />
            {error && <p className={styles.errorMessage}>{error}</p>}
        </div>
    );
};
