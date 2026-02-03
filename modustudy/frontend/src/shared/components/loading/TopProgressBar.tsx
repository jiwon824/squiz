// 피그마 스타일 상단 로딩 게이지바 컴포넌트
import React, { useEffect, useState } from 'react';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';

interface TopProgressBarProps {
    className?: string;
}

/**
 * 상단 로딩 프로그레스 바
 * - 피그마 스타일의 얇은 게이지바
 * - 전역 로딩 상태(globalLoading)와 연동
 * - GPU 가속 애니메이션 사용
 */
export const TopProgressBar: React.FC<TopProgressBarProps> = ({ className }) => {
    const { globalLoading } = useUIStore();
    const { isLoading, progress } = globalLoading;

    // 완료 후 fade-out을 위한 상태
    const [isFinishing, setIsFinishing] = useState(false);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (isLoading) {
            // 로딩 시작 시 즉시 표시
            setIsVisible(true);
            setIsFinishing(false);
        } else if (isVisible && progress >= 100) {
            // 로딩 완료 시 fade-out 후 숨김
            setIsFinishing(true);
            const timer = setTimeout(() => {
                setIsVisible(false);
                setIsFinishing(false);
            }, 300); // fade-out 애니메이션 시간
            return () => clearTimeout(timer);
        }
    }, [isLoading, progress, isVisible]);

    // 로딩 중이 아니고 표시 상태도 아니면 렌더링하지 않음
    if (!isVisible && !isLoading) {
        return null;
    }

    return (
        <div
            className={cn(
                'top-progress-bar',
                isFinishing && 'finishing',
                className
            )}
            style={{
                transform: `scaleX(${progress / 100})`,
            }}
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-busy={isLoading}
        />
    );
};
