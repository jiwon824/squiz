// 미팅룸 진입 스켈레톤 컴포넌트
import React from 'react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from '../base/Skeleton';

interface MeetingRoomSkeletonProps {
    className?: string;
    /** 표시할 참가자 슬롯 수 */
    participantCount?: number;
}

/**
 * 미팅룸 진입 시 표시되는 스켈레톤
 * - 비디오 그리드 레이아웃
 * - 하단 컨트롤 바
 */
export const MeetingRoomSkeleton: React.FC<MeetingRoomSkeletonProps> = ({
    className,
    participantCount = 4,
}) => {
    // 참가자 수에 따른 그리드 컬럼 계산
    const getGridCols = () => {
        if (participantCount <= 1) return 'grid-cols-1';
        if (participantCount <= 4) return 'grid-cols-2';
        if (participantCount <= 9) return 'grid-cols-3';
        return 'grid-cols-4';
    };

    return (
        <div className={cn('flex flex-col h-full bg-[var(--color-background)]', className)}>
            {/* 상단 헤더 영역 */}
            <div className="flex items-center justify-between px-6 py-4 bg-white rounded-2xl mx-4 mt-4 shadow-sm">
                <div className="flex items-center gap-4">
                    {/* 타이머 */}
                    <Skeleton variant="rect" width={80} height={24} className="rounded-lg" />
                    {/* 구분선 */}
                    <div className="w-px h-5 bg-gray-200" />
                    {/* 모드 배지 */}
                    <Skeleton variant="rect" width={60} height={24} className="rounded-full" />
                </div>
                <div className="flex items-center gap-2">
                    {/* 컨트롤 버튼들 */}
                    <Skeleton variant="rect" width={32} height={32} className="rounded-lg" />
                    <Skeleton variant="rect" width={32} height={32} className="rounded-lg" />
                </div>
            </div>

            {/* 비디오 그리드 영역 */}
            <div className="flex-1 p-4">
                <div className={cn('grid gap-4 h-full', getGridCols())}>
                    {Array.from({ length: participantCount }).map((_, index) => (
                        <div
                            key={index}
                            className="relative bg-gray-800 rounded-2xl overflow-hidden flex items-center justify-center"
                        >
                            {/* 비디오 플레이스홀더 */}
                            <Skeleton
                                variant="circle"
                                width={80}
                                height={80}
                                className="absolute"
                            />
                            {/* 하단 이름 영역 */}
                            <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between">
                                <Skeleton variant="rect" width={100} height={20} className="rounded-lg" />
                                <div className="flex gap-1">
                                    <Skeleton variant="rect" width={24} height={24} className="rounded-md" />
                                    <Skeleton variant="rect" width={24} height={24} className="rounded-md" />
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* 하단 컨트롤 바 */}
            <div className="flex items-center justify-center gap-4 px-6 py-4 bg-white mx-4 mb-4 rounded-2xl shadow-sm">
                {/* 마이크 버튼 */}
                <Skeleton variant="rect" width={48} height={48} className="rounded-xl" />
                {/* 카메라 버튼 */}
                <Skeleton variant="rect" width={48} height={48} className="rounded-xl" />
                {/* 화면 공유 버튼 */}
                <Skeleton variant="rect" width={48} height={48} className="rounded-xl" />
                {/* 종료 버튼 */}
                <Skeleton variant="rect" width={48} height={48} className="rounded-xl" />
            </div>
        </div>
    );
};
