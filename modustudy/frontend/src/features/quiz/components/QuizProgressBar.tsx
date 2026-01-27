/**
 * =============================================================================
 * QuizProgressBar.tsx - 퀴즈 진행률 표시 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 현재 퀴즈 진행 상태를 시각적으로 표시합니다.
 * 예: "5 / 30" 형태의 텍스트와 진행률 바를 보여줍니다.
 * 
 * =============================================================================
 */

import { cn } from '@/shared/utils/cn';

// -----------------------------------------------------------------------------
// Props 인터페이스
// -----------------------------------------------------------------------------
interface QuizProgressBarProps {
    /** 현재 문제 번호 (1-based) */
    current: number;
    /** 전체 문제 수 */
    total: number;
    /** 추가 CSS 클래스 */
    className?: string;
}

// -----------------------------------------------------------------------------
// 컴포넌트
// -----------------------------------------------------------------------------
export const QuizProgressBar: React.FC<QuizProgressBarProps> = ({
    current,
    total,
    className,
}) => {
    // 진행률 계산 (퍼센트)
    const progress = Math.round((current / total) * 100);

    return (
        <div className={cn('w-full', className)}>
            {/* 텍스트 표시: "5 / 30" */}
            <div className="flex items-center justify-between mb-2">
                <span
                    className="text-sm font-medium"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    문제 진행
                </span>
                <span
                    className="text-sm font-bold"
                    style={{ color: 'var(--color-primary)' }}
                >
                    {current} / {total}
                </span>
            </div>

            {/* 진행률 바 */}
            <div
                className="w-full h-2 rounded-full overflow-hidden"
                style={{ backgroundColor: 'var(--color-gray-100)' }}
            >
                <div
                    className="h-full rounded-full transition-all duration-300 ease-out"
                    style={{
                        width: `${progress}%`,
                        background: 'linear-gradient(90deg, var(--color-primary) 0%, var(--color-secondary) 100%)',
                    }}
                />
            </div>

            {/* 퍼센트 표시 */}
            <div className="mt-1 text-right">
                <span
                    className="text-xs"
                    style={{ color: 'var(--color-text-tertiary)' }}
                >
                    {progress}% 완료
                </span>
            </div>
        </div>
    );
};
