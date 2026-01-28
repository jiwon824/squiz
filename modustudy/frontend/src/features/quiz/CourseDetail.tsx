/**
 * =============================================================================
 * CourseDetail.tsx - 코스 상세 페이지 (메인 컴포넌트)
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 개별 코스의 상세 정보를 보여주는 페이지 컴포넌트입니다.
 * 코스 헤더, 진행률, 섹션 목록을 조합하여 전체 페이지를 구성합니다.
 * 
 * 조건부 API 호출:
 * - 로그인 사용자: fetchSectionsWithProgress() → 진행률 포함
 * - 비로그인 사용자: fetchCourseDetail() → 기본 섹션 정보만
 * 
 * URL: /quiz-practice/:courseId
 * 
 * =============================================================================
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, AlertCircle, RefreshCw, LogIn } from 'lucide-react';

import { CourseDetailHeader } from './components/CourseDetailHeader';
import { CourseDetailProgress } from './components/CourseDetailProgress';
import { CourseDetailSectionList } from './components/CourseDetailSectionList';
import { useAuthStore } from '@/store/authStore';
import {
    fetchCourseDetail,
    fetchSectionsWithProgress,
    CourseDetailData,
    SectionsWithProgressData,
    SectionInfo,
    SectionWithProgress,
} from '@/api/endpoints/quizCourseApi';

import type { CourseCategory } from './types/QuizCourse.types';
import type { CourseDetailSection } from './types/CourseDetailSection.types';

// =============================================================================
// UNIFIED DATA TYPES - 통합 데이터 타입
// =============================================================================

/**
 * 통합 코스 데이터 타입
 * 로그인/비로그인 API 응답을 하나의 형식으로 통합
 */
interface UnifiedCourseData {
    courseId: number;
    courseName: string;
    description: string;
    totalSections: number;
    sections: CourseDetailSection[];
    isAuthenticated: boolean; // API 호출 시 로그인 상태였는지 여부
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * 인증된 API 응답의 섹션 데이터를 UI 컴포넌트가 기대하는 형식으로 변환
 */
const mapAuthenticatedSectionToUiSection = (apiSection: SectionWithProgress): CourseDetailSection => {
    return {
        sectionNumber: apiSection.sectionNumber,
        name: apiSection.name,
        isUnlocked: apiSection.isUnlocked,
        isPassed: apiSection.isPassed,
        bestScore: apiSection.bestScore,
        attemptCount: apiSection.attemptCount,
        inProgressAttemptId: apiSection.inProgressAttemptId,
    };
};

/**
 * 공개 API 응답의 섹션 데이터를 UI 컴포넌트가 기대하는 형식으로 변환
 * 비로그인 사용자용: 첫 번째 섹션만 해금, 나머지는 잠금 상태
 */
const mapPublicSectionToUiSection = (
    apiSection: SectionInfo,
    index: number
): CourseDetailSection => {
    return {
        sectionNumber: apiSection.sectionNumber,
        name: apiSection.name,
        // 비로그인 시: 첫 번째 섹션만 해금 (미리보기 용도)
        isUnlocked: index === 0,
        isPassed: false,
        bestScore: null,
        attemptCount: 0,
        inProgressAttemptId: null,
    };
};

/**
 * 코스 이름을 기반으로 카테고리로 변환
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

/**
 * SectionsWithProgressData를 UnifiedCourseData로 변환
 */
const mapAuthenticatedResponseToUnified = (
    data: SectionsWithProgressData
): UnifiedCourseData => {
    return {
        courseId: data.courseId,
        courseName: data.courseName,
        description: `${data.sections.length}개의 섹션으로 구성된 코스입니다`,
        totalSections: data.sections.length,
        sections: data.sections.map(mapAuthenticatedSectionToUiSection),
        isAuthenticated: true,
    };
};

/**
 * CourseDetailData를 UnifiedCourseData로 변환 (비로그인 사용자용)
 */
const mapPublicResponseToUnified = (
    data: CourseDetailData
): UnifiedCourseData => {
    return {
        courseId: data.courseId,
        courseName: data.name,
        description: data.description,
        totalSections: data.totalSections,
        sections: data.sections.map((section, index) =>
            mapPublicSectionToUiSection(section, index)
        ),
        isAuthenticated: false,
    };
};

// =============================================================================
// COMPONENT
// =============================================================================

export const CourseDetail = () => {
    const { courseId } = useParams<{ courseId: string }>();
    const navigate = useNavigate();

    // 인증 상태 조회
    const { isLoggedIn } = useAuthStore();

    // 통합 데이터 상태 관리
    const [courseData, setCourseData] = useState<UnifiedCourseData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // =========================================================================
    // DATA FETCHING - 조건부 API 호출
    // =========================================================================

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

                // 조건부 API 호출: 로그인 상태에 따라 다른 엔드포인트 사용
                if (isLoggedIn) {
                    // 로그인 사용자: 진행률 포함 API 호출
                    console.log('[CourseDetail] 로그인 상태 - fetchSectionsWithProgress 호출');
                    try {
                        const data = await fetchSectionsWithProgress(numericCourseId);
                        setCourseData(mapAuthenticatedResponseToUnified(data));
                    } catch (authError) {
                        // 인증 API 실패 시 공개 API로 폴백
                        console.warn('[CourseDetail] 인증 API 실패, 공개 API로 폴백:', authError);
                        const data = await fetchCourseDetail(numericCourseId);
                        setCourseData(mapPublicResponseToUnified(data));
                    }
                } else {
                    // 비로그인 사용자: 공개 API 호출
                    console.log('[CourseDetail] 비로그인 상태 - fetchCourseDetail 호출');
                    const data = await fetchCourseDetail(numericCourseId);
                    setCourseData(mapPublicResponseToUnified(data));
                }
            } catch (err) {
                console.error('[CourseDetail] 코스 로딩 실패:', err);
                setError(err instanceof Error ? err.message : '코스 정보를 불러오는데 실패했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        loadCourseDetail();
    }, [courseId, isLoggedIn]);

    // =========================================================================
    // EVENT HANDLERS
    // =========================================================================

    // 재시도 핸들러
    const handleRetry = () => {
        window.location.reload();
    };

    // 섹션 클릭 핸들러 - 로그인 상태에 따라 다른 동작
    const handleSectionClick = (sectionNumber: number) => {
        if (!courseData?.isAuthenticated) {
            // 비로그인 사용자: 로그인 페이지로 리다이렉트
            console.log('[CourseDetail] 비로그인 사용자 - 로그인 유도');
            // 현재 페이지를 returnUrl로 저장하여 로그인 후 돌아올 수 있도록
            navigate('/login', {
                state: {
                    returnUrl: `/quiz-practice/${courseId}/section/${sectionNumber}/session`,
                    message: '퀴즈를 시작하려면 로그인이 필요합니다.'
                }
            });
            return;
        }

        // 진행 중인 시도가 있는지 확인
        const section = courseData.sections.find(s => s.sectionNumber === sectionNumber);
        const attemptId = section?.inProgressAttemptId;

        // 로그인 사용자: 퀴즈 세션 페이지로 이동
        if (attemptId) {
            // 명시적 attemptId로 재개
            console.log(`[CourseDetail] 퀴즈 재개: 코스 ${courseId}, 섹션 ${sectionNumber}, attemptId ${attemptId}`);
            navigate(`/quiz-practice/${courseId}/section/${sectionNumber}/session/${attemptId}`);
        } else {
            // 새 시도 시작
            console.log(`[CourseDetail] 퀴즈 시작: 코스 ${courseId}, 섹션 ${sectionNumber}`);
            navigate(`/quiz-practice/${courseId}/section/${sectionNumber}/session`);
        }
    };

    // =========================================================================
    // RENDER - 로딩 상태
    // =========================================================================

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

    // =========================================================================
    // RENDER - 에러 상태
    // =========================================================================

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

    // =========================================================================
    // RENDER - 정상 상태
    // =========================================================================

    const category = mapCodeToCategory(courseData.courseName);
    const completedCount = courseData.sections.filter(s => s.isPassed).length;

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
                    description={courseData.description}
                    category={category}
                />

                {/* 비로그인 사용자용 로그인 유도 배너 */}
                {!courseData.isAuthenticated && (
                    <div
                        className="flex items-center justify-between mb-6"
                        style={{
                            backgroundColor: 'var(--color-warning-light)',
                            borderRadius: 'var(--radius-lg)',
                            padding: 'var(--spacing-md) var(--spacing-lg)',
                            border: '1px solid var(--color-warning)',
                        }}
                    >
                        <div className="flex items-center gap-3">
                            <LogIn size={20} style={{ color: 'var(--color-warning-dark)' }} />
                            <p style={{ color: 'var(--color-warning-dark)', marginBottom: 0 }}>
                                <strong>로그인하면</strong> 진행률을 저장하고 다음 섹션을 해금할 수 있습니다.
                            </p>
                        </div>
                        <button
                            onClick={() => navigate('/login', {
                                state: { returnUrl: `/quiz-practice/${courseId}` }
                            })}
                            style={{
                                padding: '0.5rem 1rem',
                                borderRadius: 'var(--radius-md)',
                                backgroundColor: 'var(--color-warning-dark)',
                                color: 'white',
                                fontWeight: 'var(--font-weight-semibold)',
                                fontSize: 'var(--font-size-sm)',
                                border: 'none',
                                cursor: 'pointer',
                                transition: 'all var(--transition-fast)',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            로그인
                        </button>
                    </div>
                )}

                {/* 진행률 */}
                <CourseDetailProgress
                    completedCount={completedCount}
                    totalCount={courseData.totalSections}
                    category={category}
                />

                {/* 섹션 목록 */}
                <CourseDetailSectionList
                    sections={courseData.sections}
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
                        <strong>Tip:</strong> {courseData.isAuthenticated
                            ? '섹션을 순서대로 완료하면 다음 섹션이 해금됩니다!'
                            : '로그인하여 학습 진행률을 저장하고 뱃지를 획득하세요!'}
                    </p>
                </div>
            </div>
        </div>
    );
};
