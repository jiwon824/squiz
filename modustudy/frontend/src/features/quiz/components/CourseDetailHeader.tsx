/**
 * =============================================================================
 * CourseDetailHeader.tsx - 코스 상세 헤더 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 코스 상세 페이지 상단에 표시되는 헤더 컴포넌트입니다.
 * 뒤로 가기 버튼, 코스 제목, 설명, 카테고리 배지를 표시합니다.
 * 
 * =============================================================================
 */

import { BookOpen } from 'lucide-react';
import type { CourseCategory } from '../types/QuizCourse.types';
import { CATEGORY_CONFIG } from '../types/QuizCourse.types';

// =============================================================================
// PROPS INTERFACE
// =============================================================================

interface CourseDetailHeaderProps {
    name: string;
    description: string;
    category: CourseCategory;
}

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetailHeader = ({
    name,
    description,
    category,
}: CourseDetailHeaderProps) => {
    const categoryConfig = CATEGORY_CONFIG[category];

    return (
        <div style={{ marginBottom: 'var(--spacing-xl)' }}>
            {/* 헤더 카드 */}
            <div
                style={{
                    backgroundColor: 'var(--color-surface)',
                    borderRadius: 'var(--radius-xl)',
                    border: '2px solid var(--color-border)',
                    padding: 'var(--spacing-xl)',
                    boxShadow: 'var(--shadow-sm)',
                }}
            >
                {/* 상단: 아이콘과 배지 */}
                <div className="flex items-start justify-between mb-4">
                    {/* 그라데이션 아이콘 박스 */}
                    <div
                        className="flex items-center justify-center"
                        style={{
                            width: '56px',
                            height: '56px',
                            borderRadius: 'var(--radius-lg)',
                            background: categoryConfig.gradient,
                        }}
                    >
                        <BookOpen size={28} color="white" />
                    </div>

                    {/* 카테고리 배지 */}
                    <span
                        style={{
                            padding: '0.375rem 1rem',
                            borderRadius: 'var(--radius-pill)',
                            backgroundColor: `color-mix(in srgb, ${categoryConfig.color} 15%, transparent)`,
                            color: categoryConfig.color,
                            fontSize: 'var(--font-size-sm)',
                            fontWeight: 'var(--font-weight-semibold)',
                        }}
                    >
                        {categoryConfig.label}
                    </span>
                </div>

                {/* 코스 제목 */}
                <h1
                    className="text-3xl font-bold mb-3"
                    style={{ color: 'var(--color-text-primary)' }}
                >
                    {name}
                </h1>

                {/* 코스 설명 */}
                <p
                    className="text-base"
                    style={{
                        color: 'var(--color-text-secondary)',
                        lineHeight: 'var(--line-height-relaxed)',
                        marginBottom: 0,
                    }}
                >
                    {description}
                </p>
            </div>
        </div>
    );
};
