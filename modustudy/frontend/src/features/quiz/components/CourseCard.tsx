/**
 * =============================================================================
 * CourseCard.tsx - 코스 카드 컴포넌트
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 컴포넌트는 개별 코스 정보를 카드 형태로 표시합니다.
 * 카드를 클릭하면 해당 코스의 상세 페이지로 이동합니다.
 * 각 카드는 코스 제목, 설명, 카테고리 배지를 보여줍니다.
 * =============================================================================
 */

// =============================================================================
// IMPORTS - 필요한 모듈 가져오기
// =============================================================================

import { BookOpen } from 'lucide-react';

import type { Course } from '../types/QuizCourse.types';
import { CATEGORY_CONFIG, DEFAULT_CATEGORY_CONFIG } from '../types/QuizCourse.types';

// =============================================================================
// PROPS INTERFACE - 컴포넌트가 받는 속성 정의
// =============================================================================

interface CourseCardProps {
    course: Course;
    onCardClick?: (courseId: string) => void;
}

// =============================================================================
// COMPONENT DEFINITION - 컴포넌트 정의
// =============================================================================

export const CourseCard = ({ course, onCardClick }: CourseCardProps) => {
    // =========================================================================
    // DERIVED DATA - 데이터 가공
    // =========================================================================

    // 1. DB code 우선 (예: "ALGORITHM")
    // 2. 기존 category Fallback (예: "DataStructure")
    // 3. 매핑되지 않은 경우 Default 설정 사용
    const categoryConfig =
        (course.code && CATEGORY_CONFIG[course.code]) ||
        CATEGORY_CONFIG[course.category] ||
        DEFAULT_CATEGORY_CONFIG;

    // =========================================================================
    // RENDER - JSX 반환 (화면에 표시될 내용)
    // =========================================================================

    return (
        <div
            style={{
                backgroundColor: 'var(--color-surface)',
                borderRadius: 'var(--radius-xl)',
                border: '2px solid var(--color-border)',
                transition: 'all var(--transition-base)',
                boxShadow: 'var(--shadow-sm)',
                overflow: 'hidden',
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
                e.currentTarget.style.borderColor = categoryConfig.color;
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'var(--shadow-sm)';
                e.currentTarget.style.borderColor = 'var(--color-border)';
            }}
        >
            {/* CARD HEADER */}
            <div
                onClick={() => onCardClick?.(course.id)}
                className="cursor-pointer"
                style={{ padding: 'var(--spacing-xl)', paddingBottom: 'var(--spacing-lg)' }}
            >
                {/* TOP ROW: Icon and Category Badge */}
                <div className="flex items-start justify-between mb-4">
                    {/* GRADIENT ICON BOX */}
                    <div
                        className="flex items-center justify-center"
                        style={{
                            width: '48px',
                            height: '48px',
                            borderRadius: 'var(--radius-lg)',
                            background: categoryConfig.gradient,
                        }}
                    >
                        <BookOpen size={24} color="white" />
                    </div>

                    {/* CATEGORY BADGE */}
                    <span
                        style={{
                            padding: '0.25rem 0.75rem',
                            borderRadius: 'var(--radius-pill)',
                            backgroundColor: `color-mix(in srgb, ${categoryConfig.color} 15%, transparent)`,
                            color: categoryConfig.color,
                            fontSize: 'var(--font-size-xs)',
                            fontWeight: 'var(--font-weight-semibold)',
                        }}
                    >
                        {categoryConfig.label}
                    </span>
                </div>

                {/* COURSE TITLE */}
                <h3
                    className="text-xl font-bold mb-2"
                    style={{ color: 'var(--color-text-primary)' }}
                >
                    {course.title}
                </h3>

                {/* COURSE DESCRIPTION */}
                <p
                    className="text-sm"
                    style={{
                        color: 'var(--color-text-secondary)',
                        lineHeight: 'var(--line-height-relaxed)',
                    }}
                >
                    {course.description}
                </p>
            </div>
        </div>
    );
};
