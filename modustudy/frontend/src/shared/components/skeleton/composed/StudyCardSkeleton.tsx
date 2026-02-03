// 스터디 카드 스켈레톤 컴포넌트
import React from 'react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from '../base/Skeleton';

interface StudyCardSkeletonProps {
    className?: string;
}

/**
 * 스터디 카드 로딩 시 표시되는 스켈레톤
 * - 스터디 목록 페이지에서 사용
 */
export const StudyCardSkeleton: React.FC<StudyCardSkeletonProps> = ({
    className,
}) => {
    return (
        <div
            className={cn(
                'bg-white rounded-2xl overflow-hidden shadow-sm border border-[var(--color-border)]',
                className
            )}
        >
            {/* 이미지 영역 */}
            <Skeleton variant="rect" height={140} className="w-full" />

            {/* 콘텐츠 영역 */}
            <div className="p-4">
                {/* 카테고리 배지 */}
                <Skeleton variant="rect" width={60} height={20} className="rounded-full mb-3" />

                {/* 제목 */}
                <Skeleton variant="text" width="85%" height={22} className="mb-2" />

                {/* 설명 */}
                <Skeleton variant="text" width="100%" height={16} className="mb-1" />
                <Skeleton variant="text" width="70%" height={16} className="mb-4" />

                {/* 하단 정보 */}
                <div className="flex items-center justify-between pt-3 border-t border-[var(--color-border)]">
                    {/* 멤버 정보 */}
                    <div className="flex items-center gap-2">
                        <div className="flex -space-x-2">
                            <Skeleton variant="circle" width={24} height={24} />
                            <Skeleton variant="circle" width={24} height={24} />
                            <Skeleton variant="circle" width={24} height={24} />
                        </div>
                        <Skeleton variant="text" width={40} height={14} />
                    </div>
                    {/* 날짜 정보 */}
                    <Skeleton variant="text" width={60} height={14} />
                </div>
            </div>
        </div>
    );
};

interface StudyCardSkeletonGridProps {
    className?: string;
    /** 표시할 카드 수 */
    count?: number;
}

/**
 * 스터디 카드 그리드 스켈레톤
 * - StudyPage에서 사용
 */
export const StudyCardSkeletonGrid: React.FC<StudyCardSkeletonGridProps> = ({
    className,
    count = 8,
}) => {
    return (
        <div className={cn('grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5', className)}>
            {Array.from({ length: count }).map((_, index) => (
                <StudyCardSkeleton key={index} />
            ))}
        </div>
    );
};
