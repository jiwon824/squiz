// 캘린더 스켈레톤 컴포넌트
import React from 'react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from '../base/Skeleton';

interface CalendarSkeletonProps {
    className?: string;
    /** 주간 뷰 여부 (기본값: false - 월간 뷰) */
    isWeeklyView?: boolean;
}

/**
 * 캘린더 로딩 시 표시되는 스켈레톤
 * - 월간/주간 뷰 지원
 * - 요일 헤더 + 날짜 그리드
 */
export const CalendarSkeleton: React.FC<CalendarSkeletonProps> = ({
    className,
    isWeeklyView = false,
}) => {
    // 요일 헤더
    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
    // 월간: 6주 x 7일 = 42일, 주간: 1주 x 7일 = 7일
    const dayCount = isWeeklyView ? 7 : 42;

    return (
        <div className={cn('flex flex-col', className)}>
            {/* 요일 헤더 */}
            <div className="grid grid-cols-7 gap-0 mb-2">
                {weekDays.map((day, index) => (
                    <div
                        key={day}
                        className={cn(
                            'py-2 text-center text-sm font-medium',
                            index === 0 && 'text-red-400',
                            index === 6 && 'text-blue-400',
                            index > 0 && index < 6 && 'text-gray-500'
                        )}
                    >
                        {day}
                    </div>
                ))}
            </div>

            {/* 날짜 그리드 */}
            <div className="grid grid-cols-7 gap-0">
                {Array.from({ length: dayCount }).map((_, index) => (
                    <div
                        key={index}
                        className="min-h-[100px] border border-[var(--color-border)] p-2"
                    >
                        {/* 날짜 숫자 */}
                        <Skeleton
                            variant="circle"
                            width={28}
                            height={28}
                            className="mb-2"
                        />
                        {/* 일정 항목들 (랜덤하게 1~3개) */}
                        {Array.from({ length: (index % 3) + 1 }).map((_, eventIndex) => (
                            <Skeleton
                                key={eventIndex}
                                variant="rect"
                                height={20}
                                className="rounded mb-1 w-full"
                            />
                        ))}
                    </div>
                ))}
            </div>
        </div>
    );
};
