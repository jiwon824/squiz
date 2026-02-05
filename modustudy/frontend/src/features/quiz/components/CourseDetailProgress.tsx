/**
 * =============================================================================
 * CourseDetailProgress.tsx - 코스 진행률 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 코스의 전체 진행률을 시각적으로 표시합니다.
 * 완료된 섹션 수와 그라데이션 프로그레스 바를 보여줍니다.
 * 
 * =============================================================================
 */

import type { CourseCategory } from '../types/QuizCourse.types';
import { CATEGORY_CONFIG } from '../types/QuizCourse.types';

// =============================================================================
// PROPS INTERFACE
// =============================================================================

interface CourseDetailProgressProps {
    completedCount: number;
    totalCount: number;
    category: CourseCategory;
}

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetailProgress = ({
    completedCount,
    totalCount,
    category,
}: CourseDetailProgressProps) => {
    const categoryConfig = CATEGORY_CONFIG[category];
    const progressPercent = totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

    return (
        <div
            style={{
                backgroundColor: 'var(--color-surface)',
                borderRadius: 'var(--radius-lg)',
                border: '1px solid var(--color-border)',
                padding: 'var(--spacing-lg)',
                marginBottom: 'var(--spacing-lg)',
            }}
        >
            {/* 진행률 정보 */}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                    <span
                        className="text-lg font-semibold"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        진행률
                    </span>
                </div>
                <div className="flex items-center gap-2">
                    <span
                        className="text-2xl font-bold"
                        style={{ color: categoryConfig.color }}
                    >
                        {completedCount}/{totalCount}
                    </span>
                    <span
                        className="text-sm"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        섹션 완료
                    </span>
                </div>
            </div>

            {/* 프로그레스 바 */}
            <div
                style={{
                    height: '10px',
                    backgroundColor: 'var(--color-border)',
                    borderRadius: 'var(--radius-full)',
                    overflow: 'hidden',
                }}
            >
                <div
                    style={{
                        height: '100%',
                        width: `${progressPercent}%`,
                        background: categoryConfig.color,
                        borderRadius: 'var(--radius-full)',
                        transition: 'width var(--transition-base)',
                    }}
                />
            </div>

            {/* 퍼센트 표시 */}
            <div className="flex justify-end mt-2">
                <span
                    className="text-sm font-medium"
                    style={{ color: 'var(--color-text-tertiary)' }}
                >
                    {Math.round(progressPercent)}% 완료
                </span>
            </div>
        </div>
    );
};
