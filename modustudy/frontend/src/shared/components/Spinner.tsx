import React from 'react';
import { cn } from '@/shared/utils/cn';

// 스피너 크기 프리셋
type SpinnerSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | number;

// 스피너 표시 방식
type SpinnerVariant = 'inline' | 'button' | 'overlay' | 'center' | 'fullscreen';

interface SpinnerProps {
    /** 스피너 크기 (프리셋 또는 픽셀 값) */
    size?: SpinnerSize;
    /** 스피너 표시 방식 */
    variant?: SpinnerVariant;
    /** 추가 CSS 클래스 */
    className?: string;
    /** 로딩 텍스트 (선택) */
    label?: string;
    /** 색상 (CSS 변수 또는 색상값) */
    color?: string;
    /** 트랙(배경 원) 투명도 (0~1, 기본 0.3) */
    trackOpacity?: number;
}

// 크기별 픽셀 값
const sizeMap: Record<string, number> = {
    xs: 14,
    sm: 18,
    md: 24,
    lg: 32,
    xl: 48,
};

// variant별 스타일
const variantStyles: Record<SpinnerVariant, string> = {
    inline: '',
    button: '',
    overlay: 'absolute inset-0 flex items-center justify-center bg-white/80 rounded-inherit z-10',
    center: 'flex items-center justify-center py-8',
    fullscreen: 'min-h-screen flex items-center justify-center',
};

/**
 * SVG 기반 통합 스피너 컴포넌트
 * - 트랙(배경 원)과 진행 호로 구성된 세련된 디자인
 * - Loader2 대신 커스텀 SVG 사용
 *
 * @example
 * // 기본 인라인 스피너
 * <Spinner />
 *
 * // 버튼 내부 스피너
 * <Button disabled={loading}>
 *   {loading && <Spinner size="sm" variant="button" />}
 *   저장
 * </Button>
 *
 * // 컴포넌트 오버레이 스피너
 * <div className="relative">
 *   <Content />
 *   {loading && <Spinner variant="overlay" label="로딩 중..." />}
 * </div>
 *
 * // 중앙 정렬 스피너
 * {loading && <Spinner variant="center" size="lg" label="데이터를 불러오는 중..." />}
 */
export const Spinner: React.FC<SpinnerProps> = ({
    size = 'md',
    variant = 'inline',
    className = '',
    label,
    color,
    trackOpacity = 0.3,
}) => {
    // 크기 계산
    const pixelSize = typeof size === 'number' ? size : sizeMap[size] || 24;

    // 색상 결정 (prop 또는 CSS 변수)
    const spinnerColor = color || 'var(--color-primary)';

    // SVG 스피너 아이콘
    const spinnerIcon = (
        <svg
            className={cn('animate-spin', className)}
            viewBox="0 0 64 64"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            width={pixelSize}
            height={pixelSize}
            style={{ color: spinnerColor }}
        >
            {/* 트랙 (배경 원) */}
            <path
                d="M32 3C35.8083 3 39.5794 3.75011 43.0978 5.20749C46.6163 6.66488 49.8132 8.80101 52.5061 11.4939C55.199 14.1868 57.3351 17.3837 58.7925 20.9022C60.2499 24.4206 61 28.1917 61 32C61 35.8083 60.2499 39.5794 58.7925 43.0978C57.3351 46.6163 55.199 49.8132 52.5061 52.5061C49.8132 55.199 46.6163 57.3351 43.0978 58.7925C39.5794 60.2499 35.8083 61 32 61C28.1917 61 24.4206 60.2499 20.9022 58.7925C17.3837 57.3351 14.1868 55.199 11.4939 52.5061C8.801 49.8132 6.66487 46.6163 5.20749 43.0978C3.7501 39.5794 3 35.8083 3 32C3 28.1917 3.75011 24.4206 5.2075 20.9022C6.66489 17.3837 8.80101 14.1868 11.4939 11.4939C14.1868 8.80099 17.3838 6.66487 20.9022 5.20749C24.4206 3.7501 28.1917 3 32 3L32 3Z"
                stroke="currentColor"
                strokeWidth="5"
                strokeLinecap="round"
                strokeLinejoin="round"
                opacity={trackOpacity}
            />
            {/* 진행 호 */}
            <path
                d="M32 3C36.5778 3 41.0906 4.08374 45.1692 6.16256C49.2477 8.24138 52.7762 11.2562 55.466 14.9605C58.1558 18.6647 59.9304 22.9531 60.6448 27.4748C61.3591 31.9965 60.9928 36.6232 59.5759 40.9762"
                stroke="currentColor"
                strokeWidth="5"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );

    // variant에 따른 래퍼 처리
    if (variant === 'inline' || variant === 'button') {
        return label ? (
            <span className="inline-flex items-center gap-2">
                {spinnerIcon}
                <span className="text-[var(--color-text-secondary)] text-sm">{label}</span>
            </span>
        ) : (
            spinnerIcon
        );
    }

    // overlay, center variant
    return (
        <div className={variantStyles[variant]}>
            <div className="flex flex-col items-center gap-3">
                {spinnerIcon}
                {label && (
                    <span className="text-[var(--color-text-secondary)] text-sm font-medium">
                        {label}
                    </span>
                )}
            </div>
        </div>
    );
};

// 편의를 위한 프리셋 컴포넌트들
export const ButtonSpinner: React.FC<{ className?: string }> = ({ className }) => (
    <Spinner size="sm" variant="button" className={className} />
);

export const PageSpinner: React.FC<{ label?: string }> = ({ label = '로딩 중...' }) => (
    <Spinner size="lg" variant="center" label={label} />
);

export const OverlaySpinner: React.FC<{ label?: string }> = ({ label }) => (
    <Spinner size="md" variant="overlay" label={label} />
);

export const FullscreenSpinner: React.FC<{ label?: string }> = ({ label = '로딩 중...' }) => (
    <Spinner size="xl" variant="fullscreen" label={label} />
);
