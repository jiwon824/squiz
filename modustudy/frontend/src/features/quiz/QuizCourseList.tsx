/**
 * =============================================================================
 * QuizCourseList.tsx - 퀴즈 코스 목록 페이지 (메인 컴포넌트)
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 파일은 퀴즈 연습 모드 페이지의 **메인 컨테이너 컴포넌트**입니다.
 * 사용자가 둘러보고 상호작용할 수 있는 코스 카드들을 그리드 형태로 보여줍니다.
 * 사용자는 카테고리별로 코스를 필터링하거나, 카드를 클릭하여 세부 섹션을 확인할 수 있습니다.
 * 
 * * "페이지 컴포넌트"란?:
 * 페이지 컴포넌트는 보통 특정 URL 경로(Route)에 연결되어 화면에 나타납니다.
 * 이 컴포넌트는 "/quiz-practice" 경로에서 표시됩니다.
 * 
 * * 컴포넌트 계층 구조 (COMPONENT HIERARCHY):
 * App (앱 전체)
 * └── Routes (경로 설정)
 * └── QuizCourseList (현재 파일) ← 현재 위치
 * └── CourseCard (여러 개의 카드 컴포넌트)
 * └── SectionList (섹션 목록 컴포넌트)
 * 
 * * 이 파일에 사용된 리액트 핵심 개념:
 * 1. useState - 필터링 상태(카테고리 선택 등)를 관리합니다.
 * 2. useMemo - 필터링된 결과 계산을 최적화하여 성능을 높입니다.
 * 3. useNavigate - 코드 내에서 다른 페이지로 이동하는 기능을 처리합니다.
 * 4. useEffect - API 호출 등 사이드 이펙트를 처리합니다.
 * 5. Array.map() - 데이터 배열을 순회하며 여러 개의 컴포넌트를 렌더링합니다.
 * =============================================================================
 */

// =============================================================================
// IMPORTS - 필요한 모듈 가져오기
// =============================================================================

import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Filter, Loader2, AlertCircle, RefreshCw } from 'lucide-react';

import { CourseCard } from './components/CourseCard';
import { fetchCourses, CourseListItem } from '@/api/endpoints/quizCourseApi';

import type { CourseCategory, Course } from './types/QuizCourse.types';
import { CATEGORY_CONFIG } from './types/QuizCourse.types';

// =============================================================================
// TYPE DEFINITIONS - 타입 정의
// =============================================================================

type FilterOption = 'All' | CourseCategory;

// =============================================================================
// HELPER FUNCTION - API 데이터를 UI 타입으로 변환
// =============================================================================

/**
 * API 응답 데이터를 기존 Course 타입으로 변환
 * 기존 CourseCard 컴포넌트와의 호환성을 유지하기 위함
 */
const mapApiCourseToUiCourse = (apiCourse: CourseListItem): Course => {
    // API의 code를 카테고리로 매핑 (예: "OS", "NETWORK" -> "OS", "Network")
    const categoryMap: Record<string, CourseCategory> = {
        'OS': 'OS',
        'NETWORK': 'Network',
        'DB': 'DB',
        'DS': 'DataStructure',
        'DATA_STRUCTURE': 'DataStructure',
    };

    const category = categoryMap[apiCourse.code?.toUpperCase()] || 'OS';

    // courseId 추출: 'courseId' 필드를 우선 사용하고, 없으면 'id' 필드를 확인
    // 타입 단언을 사용하여 API 응답이 다른 필드명을 사용할 경우도 처리
    const rawCourseId = apiCourse.courseId ?? (apiCourse as unknown as { id?: number | string }).id;

    // courseId가 없는 경우 경고 로그 출력 (디버깅용)
    if (rawCourseId === undefined || rawCourseId === null) {
        console.warn('[mapApiCourseToUiCourse] courseId가 없습니다. API 응답:', apiCourse);
    }

    return {
        id: String(rawCourseId ?? ''),
        title: apiCourse.name ?? '',
        description: apiCourse.description ?? '',
        category: category,
        // 섹션 정보는 상세 페이지에서 로드하므로 빈 배열로 초기화
        sections: [],
    };
};

// =============================================================================
// COMPONENT DEFINITION - 컴포넌트 정의
// =============================================================================

export const QuizCourseList = () => {
    // =========================================================================
    // HOOKS - React 훅 사용
    // =========================================================================

    const navigate = useNavigate();

    // 필터 상태
    const [activeFilter, setActiveFilter] = useState<FilterOption>('All');

    // API 데이터 상태
    const [courses, setCourses] = useState<Course[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // =========================================================================
    // CONSTANTS - 상수 정의
    // =========================================================================

    const filterOptions: FilterOption[] = ['All', 'OS', 'Network', 'DB', 'DataStructure'];

    // =========================================================================
    // DATA FETCHING - API 호출
    // =========================================================================

    useEffect(() => {
        const loadCourses = async () => {
            setIsLoading(true);
            setError(null);

            try {
                const apiCourses = await fetchCourses();
                const uiCourses = apiCourses.map(mapApiCourseToUiCourse);
                setCourses(uiCourses);
            } catch (err) {
                console.error('[QuizCourseList] 코스 로딩 실패:', err);
                setError(err instanceof Error ? err.message : '코스를 불러오는데 실패했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        loadCourses();
    }, []);

    // =========================================================================
    // MEMOIZED CALCULATIONS - 메모이제이션된 계산
    // =========================================================================

    const filteredCourses = useMemo(() => {
        if (activeFilter === 'All') {
            return courses;
        }
        return courses.filter(course => course.category === activeFilter);
    }, [activeFilter, courses]);

    // =========================================================================
    // EVENT HANDLERS - 이벤트 핸들러 함수
    // =========================================================================

    const handleSectionClick = (courseId: string, sectionId: string) => {
        console.log(`퀴즈 시작: 코스 ${courseId}, 섹션 ${sectionId}`);
    };

    const handleCardClick = (courseId: string) => {
        // courseId가 유효하지 않은 경우 네비게이션 방지
        if (!courseId || courseId === 'undefined' || courseId === 'null') {
            console.error('[handleCardClick] 유효하지 않은 courseId:', courseId);
            return;
        }
        navigate(`/quiz-practice/${courseId}`);
    };

    const handleRetry = () => {
        window.location.reload();
    };

    // =========================================================================
    // RENDER - JSX 반환 (화면에 표시될 내용)
    // =========================================================================

    return (
        <div
            className="min-h-screen"
            style={{
                backgroundColor: 'var(--color-background)',
                padding: 'var(--spacing-2xl) var(--spacing-lg)',
            }}
        >
            <div className="max-w-6xl mx-auto">

                {/* BACK BUTTON - 뒤로가기 버튼 */}
                <div className="mb-4">
                    <button
                        onClick={() => navigate('/quiz')}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.5rem 1rem',
                            borderRadius: 'var(--radius-md)',
                            color: 'var(--color-text-secondary)',
                            fontWeight: '600',
                            transition: 'all 0.2s',
                            background: 'transparent',
                            border: 'none',
                            cursor: 'pointer',
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--color-background-secondary)';
                            e.currentTarget.style.color = 'var(--color-text-primary)';
                            e.currentTarget.style.transform = 'translateX(-2px)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'transparent';
                            e.currentTarget.style.color = 'var(--color-text-secondary)';
                            e.currentTarget.style.transform = 'translateX(0)';
                        }}
                    >
                        <ArrowLeft size={20} />
                        <span>게임 선택으로 돌아가기</span>
                    </button>
                </div>

                {/* PAGE HEADER - 페이지 헤더 */}
                <div className="text-center mb-8">
                    <h1
                        className="text-4xl font-bold mb-4"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        연습 모드
                    </h1>
                    <p
                        className="text-lg"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        코스를 선택하고 자신만의 속도로 CS 기초를 학습하세요
                    </p>
                </div>

                {/* CATEGORY FILTER TABS - 카테고리 필터 탭 */}
                <div
                    className="flex flex-wrap items-center justify-center gap-2 mb-8"
                    style={{ padding: 'var(--spacing-md) 0' }}
                >
                    <Filter
                        size={18}
                        style={{ color: 'var(--color-text-tertiary)', marginRight: '0.5rem' }}
                    />

                    {filterOptions.map((option) => {
                        const isActive = activeFilter === option;
                        const config = option !== 'All' ? CATEGORY_CONFIG[option] : null;

                        return (
                            <button
                                key={option}
                                onClick={() => setActiveFilter(option)}
                                style={{
                                    padding: '0.5rem 1rem',
                                    borderRadius: 'var(--radius-pill)',
                                    border: '1px solid',
                                    borderColor: isActive
                                        ? (config?.color || 'var(--color-primary)')
                                        : 'var(--color-border)',
                                    backgroundColor: isActive
                                        ? `color-mix(in srgb, ${config?.color || 'var(--color-primary)'} 15%, transparent)`
                                        : 'transparent',
                                    color: isActive
                                        ? (config?.color || 'var(--color-primary)')
                                        : 'var(--color-text-secondary)',
                                    fontWeight: 'var(--font-weight-medium)',
                                    fontSize: 'var(--font-size-sm)',
                                    cursor: 'pointer',
                                    transition: 'all var(--transition-fast)',
                                }}
                                onMouseEnter={(e) => {
                                    if (!isActive) {
                                        e.currentTarget.style.borderColor = config?.color || 'var(--color-primary)';
                                        e.currentTarget.style.color = config?.color || 'var(--color-primary)';
                                    }
                                }}
                                onMouseLeave={(e) => {
                                    if (!isActive) {
                                        e.currentTarget.style.borderColor = 'var(--color-border)';
                                        e.currentTarget.style.color = 'var(--color-text-secondary)';
                                    }
                                }}
                            >
                                {option === 'All' ? '전체 코스' : config?.label}
                            </button>
                        );
                    })}
                </div>

                {/* LOADING STATE - 로딩 상태 */}
                {isLoading && (
                    <div
                        className="flex flex-col items-center justify-center py-16"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        <Loader2
                            size={48}
                            className="animate-spin mb-4"
                            style={{ color: 'var(--color-primary)' }}
                        />
                        <p className="text-lg">코스를 불러오는 중...</p>
                    </div>
                )}

                {/* ERROR STATE - 에러 상태 */}
                {!isLoading && error && (
                    <div
                        className="flex flex-col items-center justify-center py-16"
                        style={{
                            backgroundColor: 'var(--color-error-light)',
                            borderRadius: 'var(--radius-lg)',
                            padding: 'var(--spacing-2xl)',
                        }}
                    >
                        <AlertCircle
                            size={48}
                            className="mb-4"
                            style={{ color: 'var(--color-error)' }}
                        />
                        <p
                            className="text-lg font-semibold mb-2"
                            style={{ color: 'var(--color-error)' }}
                        >
                            코스를 불러오는데 실패했습니다
                        </p>
                        <p
                            className="text-sm mb-4"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            {error}
                        </p>
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
                                fontWeight: '600',
                                border: 'none',
                                cursor: 'pointer',
                                transition: 'all 0.2s',
                            }}
                        >
                            <RefreshCw size={18} />
                            다시 시도
                        </button>
                    </div>
                )}

                {/* COURSE GRID - 코스 카드 그리드 */}
                {!isLoading && !error && (
                    <div
                        style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                            gap: 'var(--spacing-lg)',
                        }}
                    >
                        {filteredCourses.map((course) => (
                            <CourseCard
                                key={course.id}
                                course={course}
                                onSectionClick={handleSectionClick}
                                onCardClick={handleCardClick}
                            />
                        ))}
                    </div>
                )}

                {/* EMPTY STATE - 빈 상태 메시지 */}
                {!isLoading && !error && filteredCourses.length === 0 && (
                    <div
                        className="text-center py-12"
                        style={{ color: 'var(--color-text-tertiary)' }}
                    >
                        <p className="text-lg">해당 카테고리의 코스가 없습니다.</p>
                    </div>
                )}

                {/* INFO FOOTER - 안내 푸터 */}
                {!isLoading && !error && courses.length > 0 && (
                    <div
                        className="mt-12 text-center"
                        style={{
                            padding: 'var(--spacing-lg)',
                            backgroundColor: 'var(--color-primary-alpha-10)',
                            borderRadius: 'var(--radius-lg)',
                        }}
                    >
                        <p style={{ color: 'var(--color-text-secondary)' }}>
                            <strong>Tip:</strong> 코스를 클릭하여 섹션을 확인하세요. 섹션을 완료하면 진행률이 업데이트됩니다!
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};
