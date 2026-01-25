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

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, AlertCircle, RefreshCw } from 'lucide-react';

import { CourseDetailHeader } from './components/CourseDetailHeader';
import { CourseDetailProgress } from './components/CourseDetailProgress';
import { CourseDetailSectionList } from './components/CourseDetailSectionList';
import {
    fetchSectionsWithProgress,
    SectionsWithProgressData,
    SectionWithProgress,
} from '@/api/endpoints/quizCourseApi';

import type { CourseCategory } from './types/QuizCourse.types';
import type { CourseDetailSection } from './types/CourseDetailSection.types';

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * API 응답의 섹션 데이터를 UI 컴포넌트가 기대하는 형식으로 변환
 */
const mapApiSectionToUiSection = (apiSection: SectionWithProgress): CourseDetailSection => {
    return {
        sectionNumber: apiSection.sectionNumber,
        name: apiSection.name,
        isUnlocked: apiSection.isUnlocked,
        isPassed: apiSection.isPassed,
        bestScore: apiSection.bestScore,
        attemptCount: apiSection.attemptCount,
    };
};

/**
 * 코스 코드를 카테고리로 변환
 */
const mapCodeToCategory = (courseName: string): CourseCategory => {
    const lowerName = courseName.toLowerCase();
    if (lowerName.includes('운영체제') || lowerName.includes('os') || lowerName.includes('프로세스') || lowerName.includes('메모리')) {
        return 'OS';
    }
    if (lowerName.includes('네트워크') || lowerName.includes('network') || lowerName.includes('tcp') || lowerName.includes('http')) {
        return 'Network';
    }
    if (lowerName.includes('데이터베이스') || lowerName.includes('db') || lowerName.includes('sql')) {
        return 'DB';
    }
    if (lowerName.includes('자료구조') || lowerName.includes('data structure') || lowerName.includes('트리') || lowerName.includes('그래프')) {
        return 'DataStructure';
    }
    return 'OS'; // 기본값
};

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetail = () => {
    const { courseId } = useParams<{ courseId: string }>();
    const navigate = useNavigate();

    // API 상태 관리
    const [courseData, setCourseData] = useState<SectionsWithProgressData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // 데이터 로드
    useEffect(() => {
        const loadCourseDetail = async () => {
            if (!courseId) {
                setError('코스 ID가 없습니다.');
                setIsLoading(false);
                return;
            }

            setIsLoading(true);
            setError(null);

            try {
                const numericCourseId = parseInt(courseId, 10);
                if (isNaN(numericCourseId)) {
                    throw new Error('잘못된 코스 ID입니다.');
                }

                const data = await fetchSectionsWithProgress(numericCourseId);
                setCourseData(data);
            } catch (err) {
                console.error('[CourseDetail] 코스 로딩 실패:', err);
                setError(err instanceof Error ? err.message : '코스 정보를 불러오는데 실패했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        loadCourseDetail();
    }, [courseId]);

    // 재시도 핸들러
    const handleRetry = () => {
        window.location.reload();
    };

    // 로딩 상태
    if (isLoading) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div className="text-center">
                    <Loader2
                        size={48}
                        className="animate-spin mx-auto mb-4"
                        style={{ color: 'var(--color-primary)' }}
                    />
                    <p style={{ color: 'var(--color-text-secondary)' }}>
                        코스 정보를 불러오는 중...
                    </p>
                </div>
            </div>
        );
    }

    // 에러 상태 또는 코스를 찾지 못한 경우
    if (error || !courseData) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div
                    className="text-center max-w-md mx-auto"
                    style={{
                        backgroundColor: 'var(--color-surface)',
                        borderRadius: 'var(--radius-xl)',
                        padding: 'var(--spacing-2xl)',
                        boxShadow: 'var(--shadow-md)',
                    }}
                >
                    <AlertCircle
                        size={48}
                        className="mx-auto mb-4"
                        style={{ color: 'var(--color-error)' }}
                    />
                    <h2
                        className="text-2xl font-bold mb-4"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        {error ? '오류가 발생했습니다' : '코스를 찾을 수 없습니다'}
                    </h2>
                    <p
                        className="text-base mb-6"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        {error || '요청하신 코스가 존재하지 않거나 삭제되었습니다.'}
                    </p>
                    <div className="flex gap-3 justify-center">
                        <button
                            onClick={handleRetry}
                            style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.5rem',
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
                            <RefreshCw size={18} />
                            다시 시도
                        </button>
                        <button
                            onClick={() => navigate('/quiz-practice')}
                            style={{
                                padding: '0.75rem 1.5rem',
                                borderRadius: 'var(--radius-md)',
                                backgroundColor: 'transparent',
                                color: 'var(--color-text-secondary)',
                                fontWeight: 'var(--font-weight-semibold)',
                                border: '1px solid var(--color-border)',
                                cursor: 'pointer',
                                transition: 'all var(--transition-fast)',
                            }}
                        >
                            코스 목록으로 돌아가기
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // 데이터 변환
    const category = mapCodeToCategory(courseData.courseName);
    const sections = courseData.sections.map(mapApiSectionToUiSection);
    const completedCount = sections.filter(s => s.isPassed).length;

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
                    name={courseData.courseName}
                    description={`${courseData.sections.length}개의 섹션으로 구성된 코스입니다`}
                    category={category}
                />

                {/* 진행률 */}
                <CourseDetailProgress
                    completedCount={completedCount}
                    totalCount={courseData.sections.length}
                    category={category}
                />

                {/* 섹션 목록 */}
                <CourseDetailSectionList
                    sections={sections}
                    category={category}
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
