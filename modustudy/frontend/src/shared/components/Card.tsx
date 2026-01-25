import React, { ReactNode } from 'react';

type CardVariant = 'default' | 'elevated' | 'outline' | 'flat';

interface CardProps {
    children: ReactNode;
    variant?: CardVariant;
    className?: string;
    onClick?: () => void;
}

export const Card: React.FC<CardProps> = ({
    children,
    variant = 'default',
    className = '',
    onClick
}) => {
    const baseStyles = 'rounded-3xl overflow-hidden transition-all duration-300';

    const variants: Record<CardVariant, string> = {
        default: 'bg-white border border-gray-100 shadow-sm',
        elevated: 'bg-white shadow-xl hover:shadow-2xl hover:-translate-y-1 border border-gray-50',
        outline: 'bg-transparent border-2 border-gray-200 hover:border-study-blue/30',
        flat: 'bg-gray-50 border-none',
    };

    return (
        <div
            className={`${baseStyles} ${variants[variant]} ${className} ${onClick ? 'cursor-pointer' : ''}`}
            onClick={onClick}
        >
            {children}
        </div>
    );
};
