// 텍스트 블록 스켈레톤 컴포넌트
import React from 'react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from './Skeleton';

interface SkeletonTextProps {
    className?: string;
    /** 텍스트 라인 수 */
    lines?: number;
    /** 마지막 라인 너비 (기본값: 60%) */
    lastLineWidth?: string;
    /** 라인 높이 */
    lineHeight?: number;
    /** 라인 간격 */
    gap?: number;
}

/**
 * 텍스트 블록 스켈레톤 컴포넌트
 * - 여러 줄의 텍스트 로딩 상태 표시
 * - 마지막 줄은 짧게 표시하여 자연스러운 느낌
 */
export const SkeletonText: React.FC<SkeletonTextProps> = ({
    className,
    lines = 3,
    lastLineWidth = '60%',
    lineHeight = 16,
    gap = 8,
}) => {
    return (
        <div className={cn('flex flex-col', className)} style={{ gap }}>
            {Array.from({ length: lines }).map((_, index) => (
                <Skeleton
                    key={index}
                    variant="text"
                    width={index === lines - 1 ? lastLineWidth : '100%'}
                    height={lineHeight}
                />
            ))}
        </div>
    );
};
