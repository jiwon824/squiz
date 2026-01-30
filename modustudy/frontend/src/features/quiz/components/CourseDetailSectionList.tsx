/**
 * =============================================================================
 * CourseDetailSectionList.tsx - 코스 상세 섹션 목록 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 코스에 포함된 섹션들을 목록으로 표시합니다.
 * 
 * =============================================================================
 */

import { Play, CheckCircle, Trophy } from 'lucide-react';
import type { CourseDetailSection } from '../types/CourseDetailSection.types';
import { getSectionStatus } from '../types/CourseDetailSection.types';
import type { CourseCategory } from '../types/QuizCourse.types';
import { CATEGORY_CONFIG } from '../types/QuizCourse.types';

// =============================================================================
// PROPS INTERFACE
// =============================================================================

interface CourseDetailSectionListProps {
    sections: CourseDetailSection[];
    category: CourseCategory;
    onSectionClick?: (sectionNumber: number) => void;
}

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetailSectionList = ({
    sections,
    category,
    onSectionClick,
}: CourseDetailSectionListProps) => {
    const categoryConfig = CATEGORY_CONFIG[category];

    return (
        <div
            style={{
                backgroundColor: 'var(--color-surface)',
                borderRadius: 'var(--radius-xl)',
                border: '1px solid var(--color-border)',
                overflow: 'hidden',
            }}
        >
            {/* 섹션 헤더 */}
            <div
                style={{
                    padding: 'var(--spacing-md) var(--spacing-lg)',
                    borderBottom: '1px solid var(--color-border)',
                    backgroundColor: 'var(--color-background-secondary)',
                }}
            >
                <h2
                    className="text-lg font-semibold"
                    style={{ color: 'var(--color-text-primary)', marginBottom: 0 }}
                >
                    섹션 목록
                </h2>
            </div>

            {/* 섹션 목록 */}
            <div className="flex flex-col">
                {sections.map((section, index) => {
                    const status = getSectionStatus(section);
                    const isLast = index === sections.length - 1;

                    return (
                        <div
                            key={section.sectionNumber}
                            onClick={() => onSectionClick?.(section.sectionNumber)}
                            className="cursor-pointer group"
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 'var(--spacing-md)',
                                padding: 'var(--spacing-md) var(--spacing-lg)',
                                borderBottom: isLast ? 'none' : '1px solid var(--color-border)',
                                backgroundColor: 'var(--color-surface)',
                                transition: 'all var(--transition-fast)',
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.backgroundColor = 'var(--color-surface-hover)';
                                e.currentTarget.style.borderColor = categoryConfig.color;
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.backgroundColor = 'var(--color-surface)';
                                e.currentTarget.style.borderColor = 'var(--color-border)';
                            }}
                        >
                            {/* 섹션 번호 및 상태 아이콘 */}
                            <div
                                className="flex items-center justify-center"
                                style={{
                                    width: '40px',
                                    height: '40px',
                                    borderRadius: 'var(--radius-full)',
                                    backgroundColor: status === 'Completed'
                                        ? 'var(--color-google-green)'
                                        : categoryConfig.color,
                                    flexShrink: 0,
                                }}
                            >
                                {status === 'Completed' ? (
                                    <CheckCircle size={20} color="white" />
                                ) : (
                                    <Play size={18} color="white" fill="white" />
                                )}
                            </div>

                            {/* 섹션 정보 */}
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div className="flex items-center gap-2 mb-1">
                                    <span
                                        className="text-xs font-medium"
                                        style={{ color: 'var(--color-text-tertiary)' }}
                                    >
                                        Section {section.sectionNumber}
                                    </span>
                                </div>
                                <div
                                    className="font-medium"
                                    style={{
                                        color: status === 'Completed'
                                            ? 'var(--color-text-secondary)'
                                            : 'var(--color-text-primary)',
                                        textDecoration: status === 'Completed' ? 'line-through' : 'none',
                                    }}
                                >
                                    {section.name}
                                </div>

                                {/* 시도 횟수 */}
                                {section.attemptCount > 0 && (
                                    <div
                                        className="text-xs mt-1"
                                        style={{ color: 'var(--color-text-tertiary)' }}
                                    >
                                        {section.attemptCount}회 시도
                                    </div>
                                )}
                            </div>

                            {/* 최고 점수 배지 (완료된 경우만) */}
                            {status === 'Completed' && section.bestScore !== null && (
                                <div
                                    className="flex items-center gap-1"
                                    style={{
                                        padding: '0.25rem 0.75rem',
                                        borderRadius: 'var(--radius-pill)',
                                        backgroundColor: 'var(--color-google-green-light)',
                                        flexShrink: 0,
                                    }}
                                >
                                    <Trophy size={14} style={{ color: 'var(--color-google-green)' }} />
                                    <span
                                        className="text-sm font-semibold"
                                        style={{ color: 'var(--color-google-green)' }}
                                    >
                                        {section.bestScore}점
                                    </span>
                                </div>
                            )}

                            {/* 시작 버튼 (완료되지 않은 모든 섹션에 표시) */}
                            {status !== 'Completed' && (
                                <button
                                    className="opacity-0 group-hover:opacity-100"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onSectionClick?.(section.sectionNumber);
                                    }}
                                    style={{
                                        padding: '0.5rem 1rem',
                                        borderRadius: 'var(--radius-md)',
                                        backgroundColor: categoryConfig.color,
                                        color: 'white',
                                        fontWeight: 'var(--font-weight-semibold)',
                                        fontSize: 'var(--font-size-sm)',
                                        border: 'none',
                                        cursor: 'pointer',
                                        transition: 'opacity var(--transition-fast)',
                                        flexShrink: 0,
                                    }}
                                >
                                    시작
                                </button>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};
