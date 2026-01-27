import React from 'react';
import { cn } from '@/shared/utils/cn';

interface DifficultyBadgeProps {
    difficulty: string;
    size?: 'sm' | 'md';
}

/**
 * DifficultyBadge - 난이도 표시 공통 뱃지 컴포넌트
 *
 * 사용처: StudyCardContentV2, StudyDetailPageV2
 */
export const DifficultyBadge: React.FC<DifficultyBadgeProps> = ({ difficulty, size = 'sm' }) => {
    const config = getDifficultyConfig(difficulty);

    return (
        <span className={cn(
            "rounded-md font-bold",
            config.color,
            config.bgColor,
            size === 'sm' ? "px-2.5 py-1 text-[11px]" : "px-3 py-1.5 text-xs"
        )}>
            {config.text}
        </span>
    );
};

export const getDifficultyConfig = (difficulty: string) => {
    switch (difficulty) {
        case 'BEGINNER':
        case 'ELEMENTARY':
            return {
                text: '입문',
                color: 'text-[var(--color-success)]',
                bgColor: 'bg-[var(--color-success-light)]'
            };
        case 'INTERMEDIATE':
            return {
                text: '중급',
                color: 'text-[var(--color-primary)]',
                bgColor: 'bg-[var(--color-primary-alpha-10)]'
            };
        case 'ADVANCED':
            return {
                text: '고급',
                color: 'text-[var(--color-error)]',
                bgColor: 'bg-[var(--color-error-light)]'
            };
        default:
            return {
                text: difficulty,
                color: 'text-[var(--color-text-tertiary)]',
                bgColor: 'bg-gray-100'
            };
    }
};

export default DifficultyBadge;
