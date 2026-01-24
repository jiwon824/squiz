import React from 'react';
import { ChevronLeft } from 'lucide-react';
import { Button } from './Button';

interface BackButtonProps {
    onClick: () => void;
    variant?: 'icon-only' | 'with-text' | 'with-text-2';
    className?: string;
}

export const BackButton: React.FC<BackButtonProps> = ({
    onClick,
    variant = 'icon-only',
    className = ''
}) => {
    if (variant === 'icon-only') {
        return (
            <Button
                variant="back-button"
                isCircle
                size="md"
                onClick={onClick}
                className={className}
                aria-label="뒤로 가기"
            >
                <ChevronLeft size={24} />
            </Button>
        );
    }

    if (variant === 'with-text-2') {
        return (
            <Button
                variant="back-button"
                leftIcon={<ChevronLeft size={18} />}
                size="sm"
                onClick={onClick}
                className={className}
            >
                뒤로 가기
            </Button>
        );
    }

    return (
        <Button
            variant="back-button"
            leftIcon={<ChevronLeft size={18} />}
            size="sm"
            onClick={onClick}
            className={className}
        >
            목록으로
        </Button>
    );
};