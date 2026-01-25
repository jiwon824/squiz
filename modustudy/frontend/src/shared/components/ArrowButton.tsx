import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

interface ArrowButtonProps {
    direction: 'left' | 'right';
    onClick: () => void;
    size?: 'sm' | 'md' | 'lg';
    className?: string;
}

export const ArrowButton: React.FC<ArrowButtonProps> = ({
    direction,
    onClick,
    size = 'sm',
    className = ''
}) => {
    const Icon = direction === 'left' ? ChevronLeft : ChevronRight;
    const iconSize = size === 'sm' ? 20 : size === 'md' ? 24 : 28;

    return (
        <Button
            variant="arrow-transparent"
            isCircle
            size={size}
            onClick={onClick}
            className={className}
            aria-label={direction === 'left' ? '이전' : '다음'}
        >
            <Icon size={iconSize} />
        </Button>
    );
};