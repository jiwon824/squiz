import React, { ReactNode } from 'react';
import { cn } from '@/shared/utils/cn';

type CardVariant = 'default' | 'elevated' | 'outline' | 'flat';

interface CardProps {
    children: ReactNode;
    variant?: CardVariant;
    className?: string;
    onClick?: () => void;
}

const styles = {
    base: 'rounded-3xl overflow-hidden transition-all duration-300',
    variants: {
        default: 'bg-surface-50 border border-outline shadow-sm',
        elevated: 'bg-surface-50 shadow-xl hover:shadow-2xl hover:-translate-y-1 border border-outline',
        outline: 'bg-transparent border-2 border-outline-variant hover:border-primary-300',
        flat: 'bg-surface-100 border-none',
    },
};

export const Card: React.FC<CardProps> = ({
    children,
    variant = 'default',
    className = '',
    onClick
}) => {
    return (
        <div
            className={cn(
                styles.base,
                styles.variants[variant],
                {
                    'cursor-pointer': !!onClick,
                },
                className
            )}
            onClick={onClick}
        >
            {children}
        </div>
    );
};
