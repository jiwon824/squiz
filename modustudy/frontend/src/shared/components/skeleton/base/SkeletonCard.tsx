// 카드형 스켈레톤 컴포넌트
import React from 'react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from './Skeleton';

interface SkeletonCardProps {
    className?: string;
    /** 카드 내부 이미지 영역 높이 */
    imageHeight?: number;
    /** 텍스트 라인 수 */
    lines?: number;
    /** 이미지 표시 여부 */
    showImage?: boolean;
}

/**
 * 카드형 스켈레톤 컴포넌트
 * - 이미지 + 텍스트 조합의 카드 로딩 상태 표시
 */
export const SkeletonCard: React.FC<SkeletonCardProps> = ({
    className,
    imageHeight = 120,
    lines = 2,
    showImage = true,
}) => {
    return (
        <div
            className={cn(
                'bg-[var(--color-surface)] rounded-xl p-4 border border-[var(--color-border)]',
                className
            )}
        >
            {showImage && (
                <Skeleton
                    variant="rect"
                    height={imageHeight}
                    className="rounded-lg mb-3 w-full"
                />
            )}
            {Array.from({ length: lines }).map((_, index) => (
                <Skeleton
                    key={index}
                    variant="text"
                    width={index === 0 ? '70%' : '50%'}
                    height={index === 0 ? 20 : 16}
                    className={index < lines - 1 ? 'mb-2' : ''}
                />
            ))}
        </div>
    );
};
