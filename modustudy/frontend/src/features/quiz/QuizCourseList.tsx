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
 * 4. Array.map() - 데이터 배열을 순회하며 여러 개의 컴포넌트를 렌더링합니다.
 * 5. Props drilling - 데이터를 하위 컴포넌트로 전달하여 정보를 공유합니다.
 * =============================================================================
 */

// =============================================================================
// IMPORTS - 필요한 모듈 가져오기
// =============================================================================

/**
 * 리액트 훅 (REACT HOOKS):
 * 훅은 리액트의 다양한 기능을 컴포넌트 안에서 "낚아채듯(hook into)" 사용할 수 있게 해주는 특별한 함수들입니다.
 * 훅은 반드시 컴포넌트의 최상단에서 호출해야 하며, 반복문이나 조건문 안에서 호출해서는 안 됩니다.
 * * - useState: 컴포넌트에 상태(변화하는 데이터)를 추가합니다.
 * - useMemo: 복잡한 계산 값을 메모리에 저장(캐싱)하여, 불필요한 재계산을 방지하고 성능을 높입니다.
 */
import { useState, useMemo } from 'react';

/**
 * 리액트 라우터 (REACT ROUTER):
* useNavigate는 프로그램적으로(코드 상에서) 페이지를 이동시킬 수 있는 기능을 반환하는 훅입니다.
* 사용자가 버튼을 클릭하는 등의 특정 이벤트가 발생했을 때, 다른 페이지로 이동시키는 데 사용됩니다.
* * 사용 예시:
 * const navigate = useNavigate();
 * navigate('/quiz'); // Goes to the /quiz page
 */
import { useNavigate } from 'react-router-dom';

/**
 * 아이콘 (ICONS):
 * - ArrowLeft: "게임 선택으로 돌아가기" 버튼에 사용되는 뒤로 가기 화살표 아이콘
 * - Filter: 카테고리 필터 버튼 옆에 표시되는 필터 아이콘
 */
import { ArrowLeft, Filter } from 'lucide-react';

/**
 * 자식 컴포넌트 (CHILD COMPONENT):
 * CourseCard는 각 개별 코스를 화면에 표시해주는 컴포넌트입니다.
 * 우리는 이 CourseCard를 데이터에 있는 코스 수만큼 여러 개 렌더링(그려줄) 것입니다.
 */
import { CourseCard } from './components/CourseCard';

/**
 * 데이터 (DATA):
 * quizCourses는 우리가 가진 코스 객체들의 배열(리스트)입니다. (이것은 가짜 데이터입니다.)
 * 실제 서비스에서는 이 데이터가 API 호출을 통해 서버로부터 오게 됩니다.
 */
import { quizCourses } from './data/quizCourseData';

/**
 * 타입 및 설정 (TYPES AND CONFIGURATION):
 * - CourseCategory: Type for category values ('OS', 'Network', etc.)
 * - CATEGORY_CONFIG: Display settings for each category (Korean labels, colors)
 */
import type { CourseCategory } from './types/QuizCourse.types';
import { CATEGORY_CONFIG } from './types/QuizCourse.types';

// =============================================================================
// TYPE DEFINITIONS - 타입 정의
// =============================================================================

/**
 * FilterOption 타입:
 * 이 타입은 필터가 가질 수 있는 값들을 정의합니다.
 * 'All'(모든 코스 표시) 또는 'CourseCategory'에 정의된 값들 중 하나가 될 수 있습니다.
 * * | 기호는 "또는(OR)"을 의미
 * 즉, FilterOption은 'All'이거나 'CourseCategory' 중 하나여야 한다는 뜻
 */
type FilterOption = 'All' | CourseCategory;

// =============================================================================
// COMPONENT DEFINITION - 컴포넌트 정의
// =============================================================================

/**
 * QuizCourseList 컴포넌트 - 퀴즈 코스 목록 페이지 컴포넌트
 * * 이 파일의 메인 익스포트(Export) 항목입니다.
 * 리액트 라우터(React Router)가 /quiz-practice 경로로 안내할 때 이 컴포넌트가 렌더링됩니다.
 * * @returns 연습 모드 페이지 전체를 나타내는 JSX를 반환합니다.
 */
export const QuizCourseList = () => {
    // =========================================================================
    // HOOKS - React 훅 사용
    // =========================================================================

    /**
     * useNavigate 훅 (useNavigate HOOK):
     * 이 훅은 페이지 사이를 이동할 수 있게 해주는 함수를 반환합니다.
     * 우리는 이 함수를 "navigate"라는 변수에 담아 사용합니다.
     * * 사용법: navigate('/특정-경로')를 호출하면 URL이 변경되고 해당 페이지가 렌더링됩니다.
     */
    const navigate = useNavigate();

    /**
     * 필터 상태 (FILTER STATE):
     * activeFilter는 현재 어떤 카테고리 필터가 선택되었는지를 추적합니다.
     * 초기값은 'All'이며, 이는 기본적으로 모든 코스가 표시됨을 의미합니다.
     * * 사용자가 다른 필터 버튼을 클릭하면, setActiveFilter를 새 값으로 호출합니다.
     * 이 호출은 새로운 필터가 적용된 상태로 컴포넌트를 다시 렌더링(재실행)시킵니다.
     */
    const [activeFilter, setActiveFilter] = useState<FilterOption>('All');

    // =========================================================================
    // CONSTANTS - 상수 정의
    // =========================================================================

    /**
     * 필터 옵션 (FILTER OPTIONS):
     * 가능한 모든 필터 값들의 배열(리스트)입니다.
     * 이 배열은 UI에서 필터 버튼들을 렌더링하는 데 사용됩니다.
     */
    const filterOptions: FilterOption[] = ['All', 'OS', 'Network', 'DB', 'DataStructure'];

    // =========================================================================
    // MEMOIZED CALCULATIONS - 메모이제이션된 계산
    // =========================================================================

    /**
     * useMemo 훅 설명 (useMemo HOOK EXPLANATION):
     * useMemo는 비용이 많이 드는 계산 결과를 메모리에 저장(캐싱)하고, 
     * 의존성 배열(두 번째 인자)의 값이 변경될 때만 다시 계산합니다.
     * * useMemo를 사용하지 않을 경우:
     * - 컴포넌트가 렌더링될 때마다 필터링 계산이 매번 실행됩니다.
     * - 결과가 바뀌지 않았음에도 CPU 자원을 낭비하게 됩니다.
     * * useMemo를 사용할 경우:
     * - activeFilter 값이 바뀔 때만 계산이 실행됩니다.
     * - 그 외의 렌더링 시에는 이전에 저장해둔 결과를 즉시 반환합니다.
     * * 문법: useMemo(() => 계산식, [의존성배열])
     * - 첫 번째 인자: 계산된 값을 반환하는 함수
     * - 두 번째 인자: 변경 시 재계산을 트리거하는 값들의 배열
     */
    const filteredCourses = useMemo(() => {
        // 'All'이 선택된 경우, 필터링 없이 전체 배열을 반환합니다.
        if (activeFilter === 'All') {
            return quizCourses;
        }
        // 그렇지 않은 경우(All이 아닌 경우), 선택된 카테고리와 일치하는 코스만 필터링합니다.
        // filter()는 콜백 함수가 true를 반환하는 항목들만 모아서 새로운 배열을 만듭니다.
        return quizCourses.filter(course => course.category === activeFilter);
    }, [activeFilter]);  // Dependency: recalculate when activeFilter changes

    // =========================================================================
    // EVENT HANDLERS - 이벤트 핸들러 함수
    // =========================================================================

    /**
     * handleSectionClick:
     * 사용자가 코스 내의 특정 섹션을 클릭했을 때 호출되는 함수입니다.
     * 현재는 콘솔에 로그만 출력하지만, 나중에는 이 부분에서 
     * 실제 퀴즈 페이지로 이동(Navigate)하도록 구현할 수 있습니다.
     * * @param courseId - 클릭된 섹션이 포함된 코스의 ID
     * @param sectionId - 클릭된 섹션의 고유 ID
     */
    const handleSectionClick = (courseId: string, sectionId: string) => {
        // Log to browser console for debugging
        console.log(`퀴즈 시작: 코스 ${courseId}, 섹션 ${sectionId}`);

        // TODO: 실제 퀴즈 페이지로 이동하는 로직을 구현하세요.
        // 페이지 이동 기능을 활성화하려면 아래 코드의 주석을 해제하세요:
        // navigate(`/quiz-practice/${courseId}/${sectionId}`);
    };

    // =========================================================================
    // RENDER - JSX 반환 (화면에 표시될 내용)
    // =========================================================================

    return (
        /**
         * 페이지 컨테이너 (PAGE CONTAINER):
         * 페이지 전체를 감싸는 최상위 div 요소입니다.
         * min-h-screen 속성은 페이지의 최소 높이를 브라우저 화면 높이(viewport height)에 맞게 꽉 채워줍니다.
         */
        <div
            className="min-h-screen"  // Minimum height = full viewport height
            style={{
                backgroundColor: 'var(--color-background)',  // Page background color
                padding: 'var(--spacing-2xl) var(--spacing-lg)',  // 48px top/bottom, 24px left/right
            }}
        >
            {/* MAX-WIDTH CONTAINER: Centers content and limits width on large screens */}
            <div className="max-w-6xl mx-auto">

                {/* ============================================================
                    BACK BUTTON - 뒤로가기 버튼
                    Navigates back to the game selection page
                    ============================================================ */}
                <div className="mb-4">
                    <button
                        /**
                         * 클릭 시 페이지 이동 (ONCLICK NAVIGATION):
                         * 요소를 클릭하면 /quiz 페이지(게임 선택 화면)로 이동합니다.
                         */
                        onClick={() => navigate('/quiz')}
                        style={{
                            display: 'inline-flex',  // Flex for icon + text alignment
                            alignItems: 'center',    // Vertically center
                            gap: '0.5rem',           // 8px gap between icon and text
                            padding: '0.5rem 1rem',  // Button padding
                            borderRadius: 'var(--radius-md)',
                            color: 'var(--color-text-secondary)',
                            fontWeight: '600',
                            transition: 'all 0.2s',
                            background: 'transparent',  // No background initially
                            border: 'none',
                            cursor: 'pointer',
                        }}
                        /**
                         * 마우스 호버 효과 (HOVER EFFECTS):
                         * 마우스가 버튼 위에 올라갔을 때의 스타일 변화
                         */
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--color-background-secondary)';
                            e.currentTarget.style.color = 'var(--color-text-primary)';
                            e.currentTarget.style.transform = 'translateX(-2px)';  // Shift left
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'transparent';
                            e.currentTarget.style.color = 'var(--color-text-secondary)';
                            e.currentTarget.style.transform = 'translateX(0)';
                        }}
                    >
                        <ArrowLeft size={20} />
                        <span>게임 선택으로 돌아가기</span>  {/* Korean: "Back to Game Selection" */}
                    </button>
                </div>

                {/* ============================================================
                    PAGE HEADER - 페이지 헤더
                    ============================================================ */}
                <div className="text-center mb-8">
                    {/* Page Title */}
                    <h1
                        className="text-4xl font-bold mb-4"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        연습 모드  {/* Korean: "Practice Mode" */}
                    </h1>
                    {/* Subtitle/Description */}
                    <p
                        className="text-lg"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        코스를 선택하고 자신만의 속도로 CS 기초를 학습하세요
                        {/* Korean: "Select a course and learn CS fundamentals at your own pace" */}
                    </p>
                </div>

                {/* ============================================================
                    CATEGORY FILTER TABS - 카테고리 필터 탭
                    ============================================================ */}
                <div
                    className="flex flex-wrap items-center justify-center gap-2 mb-8"
                    style={{ padding: 'var(--spacing-md) 0' }}
                >
                    {/* Filter icon */}
                    <Filter
                        size={18}
                        style={{ color: 'var(--color-text-tertiary)', marginRight: '0.5rem' }}
                    />

                    {/**
                     * 필터 버튼 렌더링 (FILTER BUTTON RENDERING):
                     * filterOptions 배열을 순회하며 각 옵션에 대한 버튼을 생성합니다.
                     * 
                     * 각 옵션에 대해 다음을 수행합니다:
                     * 1. 현재 활성화된(선택된) 필터인지 확인
                     * 2. 'All'이 아닌 옵션의 경우 설정값(config)을 가져옴
                     * 3. 활성화 상태에 따라 적절한 스타일 적용
                     */}
                    {filterOptions.map((option) => {
                        // Check if this option is currently selected
                        const isActive = activeFilter === option;

                        // Get color config for this option (null for 'All')
                        const config = option !== 'All' ? CATEGORY_CONFIG[option] : null;

                        return (
                            <button
                                key={option}  // Unique key for React list rendering

                                /**
                                 * 활성 필터 설정 (SET ACTIVE FILTER):
                                 * 클릭하면 activeFilter 상태를 업데이트합니다.
                                 * 이로 인해 useMemo가 호출되어 filteredCourses가 재계산됩니다.
                                 */
                                onClick={() => setActiveFilter(option)}

                                style={{
                                    padding: '0.5rem 1rem',
                                    borderRadius: 'var(--radius-pill)',  // Pill-shaped button
                                    border: '1px solid',
                                    /**
                                     * 조건부 스타일링 (CONDITIONAL STYLING):
                                     * 활성 버튼은 비활성 버튼과 다른 색상을 가집니다.
                                     * 삼항 연산자를 사용하여 isActive 값에 따라 다른 스타일을 적용합니다.
                                     */
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
                                /**
                                 * 마우스 호버 효과 (HOVER EFFECTS):
                                 * 비활성 버튼에만 적용됩니다.
                                 * 호버 시 활성 상태일 때의 스타일 미리보기 표시
                                 */
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
                                {/* Display Korean label for categories, or "전체 코스" for 'All' */}
                                {option === 'All' ? '전체 코스' : config?.label}
                            </button>
                        );
                    })}
                </div>

                {/* ============================================================
                    COURSE GRID - 코스 카드 그리드
                    ============================================================ */}
                <div
                    style={{
                        /**
                         * CSS 그리드 레이아웃 (CSS GRID LAYOUT):
                         * - display: grid - CSS 그리드 기능을 활성화합니다.
                         * - gridTemplateColumns: repeat(auto-fill, ...) - 화면 너비에 맞춰 가능한 많은 
                         *   열(Column)을 생성합니다. 각 열의 너비는 최소 320px입니다.
                         * - minmax(320px, 1fr) - 각 열의 최소 너비는 320px이며, 최대 너비는 남은 공간을 
                         *   동일한 비율로 나누어 가집니다.
                         * - gap: 24px - 그리드 항목(카드) 사이의 간격을 24px로 설정합니다.
                         * * 이 설정을 통해 화면 너비에 따라 열의 개수가 자동으로 조절되는 
                         * 반응형 그리드 레이아웃이 만들어집니다.
                         */
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                        gap: 'var(--spacing-lg)',
                    }}
                >
                    {/**
                     * 코스 리스트 렌더링 (RENDERING THE COURSE LIST):
                     * 필터링된 코스 배열을 순회(Map)하며 각 코스에 대한 CourseCard 컴포넌트를 화면에 그립니다.
                     * * 각 CourseCard에 전달되는 정보(Props):
                     * - key: 리액트가 목록의 각 요소를 식별하기 위한 고유 값 (리스트 렌더링 시 필수)
                     * - course: 모든 데이터를 포함하고 있는 전체 코스 객체
                     * - onSectionClick: 섹션 클릭 시 실행될 콜백 함수
                     */}
                    {filteredCourses.map((course) => (
                        <CourseCard
                            key={course.id}
                            course={course}
                            onSectionClick={handleSectionClick}
                        />
                    ))}
                </div>

                {/* ============================================================
                    EMPTY STATE - 빈 상태 메시지
                    Shown when no courses match the current filter
                    ============================================================ */}
                {/**
                 * && 연산자를 사용한 조건부 렌더링 (CONDITIONAL RENDERING WITH &&):
                 * 이 부분은 filteredCourses.length가 0일 때만 화면에 그려집니다.
                 * && 연산자는 단락 평가(short-circuit) 방식으로 동작합니다. 
                 * 즉, 왼쪽 조건이 거짓(false)이면 오른쪽 코드는 실행(평가)되지 않습니다.
                 */}
                {filteredCourses.length === 0 && (
                    <div
                        className="text-center py-12"
                        style={{ color: 'var(--color-text-tertiary)' }}
                    >
                        <p className="text-lg">해당 카테고리의 코스가 없습니다.</p>
                        {/* Korean: "No courses found for this category." */}
                    </div>
                )}

                {/* ============================================================
                    INFO FOOTER - 안내 푸터
                    ============================================================ */}
                <div
                    className="mt-12 text-center"
                    style={{
                        padding: 'var(--spacing-lg)',
                        backgroundColor: 'var(--color-primary-alpha-10)',  // 10% primary color
                        borderRadius: 'var(--radius-lg)',
                    }}
                >
                    <p style={{ color: 'var(--color-text-secondary)' }}>
                        <strong>Tip:</strong> 코스를 클릭하여 섹션을 확인하세요. 섹션을 완료하면 진행률이 업데이트됩니다!
                        {/* Korean: "Click on a course to view sections. Complete sections to update your progress!" */}
                    </p>
                </div>
            </div>
        </div>
    );
};
