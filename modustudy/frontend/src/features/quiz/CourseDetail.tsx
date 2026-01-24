/**
 * =============================================================================
 * CourseDetail.tsx - 코스 상세 페이지 (메인 컴포넌트)
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 개별 코스의 상세 정보를 보여주는 페이지 컴포넌트입니다.
 * 코스 헤더, 진행률, 섹션 목록을 조합하여 전체 페이지를 구성합니다.
 * 
 * URL: /quiz-practice/:courseId
 * 
 * =============================================================================
 */

import { useParams, useNavigate } from 'react-router-dom';
import { CourseDetailHeader } from './components/CourseDetailHeader';
import { CourseDetailProgress } from './components/CourseDetailProgress';
import { CourseDetailSectionList } from './components/CourseDetailSectionList';
import { getCourseDetailById } from './data/courseDetailData';

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetail = () => {
    const { courseId } = useParams<{ courseId: string }>();
    const navigate = useNavigate();

    // 코스 데이터 조회
    const courseDetail = courseId ? getCourseDetailById(courseId) : undefined;

    // 코스를 찾지 못한 경우
    if (!courseDetail) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div
                    className="text-center"
                    style={{
                        backgroundColor: 'var(--color-surface)',
                        borderRadius: 'var(--radius-xl)',
                        padding: 'var(--spacing-2xl)',
                        boxShadow: 'var(--shadow-md)',
                    }}
                >
                    <h2
                        className="text-2xl font-bold mb-4"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        코스를 찾을 수 없습니다
                    </h2>
                    <p
                        className="text-base mb-6"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        요청하신 코스가 존재하지 않거나 삭제되었습니다.
                    </p>
                    <button
                        onClick={() => navigate('/quiz-practice')}
                        style={{
                            padding: '0.75rem 1.5rem',
                            borderRadius: 'var(--radius-md)',
                            backgroundColor: 'var(--color-primary)',
                            color: 'white',
                            fontWeight: 'var(--font-weight-semibold)',
                            border: 'none',
                            cursor: 'pointer',
                            transition: 'all var(--transition-fast)',
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.backgroundColor = 'var(--color-primary-dark)';
                            e.currentTarget.style.transform = 'translateY(-2px)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.backgroundColor = 'var(--color-primary)';
                            e.currentTarget.style.transform = 'translateY(0)';
                        }}
                    >
                        코스 목록으로 돌아가기
                    </button>
                </div>
            </div>
        );
    }

    // 완료된 섹션 수 계산
    const completedCount = courseDetail.sections.filter(s => s.isPassed).length;

    // 섹션 클릭 핸들러 - 퀴즈 세션 페이지로 이동
    const handleSectionClick = (sectionNumber: number) => {
        console.log(`퀴즈 시작: 코스 ${courseId}, 섹션 ${sectionNumber}`);
        navigate(`/quiz-practice/${courseId}/section/${sectionNumber}/session`);
    };

    return (
        <div
            className="min-h-screen"
            style={{
                backgroundColor: 'var(--color-background)',
                padding: 'var(--spacing-2xl) var(--spacing-lg)',
            }}
        >
            <div className="max-w-4xl mx-auto">
                {/* 코스 헤더 */}
                <CourseDetailHeader
                    name={courseDetail.name}
                    description={courseDetail.description}
                    category={courseDetail.category}
                />

                {/* 진행률 */}
                <CourseDetailProgress
                    completedCount={completedCount}
                    totalCount={courseDetail.totalSections}
                    category={courseDetail.category}
                />

                {/* 섹션 목록 */}
                <CourseDetailSectionList
                    sections={courseDetail.sections}
                    category={courseDetail.category}
                    onSectionClick={handleSectionClick}
                />

                {/* 안내 푸터 */}
                <div
                    className="mt-8 text-center"
                    style={{
                        padding: 'var(--spacing-lg)',
                        backgroundColor: 'var(--color-primary-alpha-10)',
                        borderRadius: 'var(--radius-lg)',
                    }}
                >
                    <p style={{ color: 'var(--color-text-secondary)', marginBottom: 0 }}>
                        <strong>Tip:</strong> 섹션을 순서대로 완료하면 다음 섹션이 해금됩니다!
                    </p>
                </div>
            </div>
        </div>
    );
};
