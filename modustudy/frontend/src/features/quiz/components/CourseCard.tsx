/**
 * =============================================================================
 * CourseCard.tsx - 코스 카드 컴포넌트
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 컴포넌트는 개별 코스 정보를 상호작용 가능한 카드 형태로 표시합니다.
 * 사용자는 카드를 확장하여 해당 코스에 포함된 세부 섹션들을 확인할 수 있습니다.
 * 각 카드는 코스 제목, 설명, 카테고리 배지, 그리고 진행률을 보여줍니다.
 * * 주요 기능:
 * 1. 확장/축소가 가능한 섹션 목록
 * 2. 완료율을 보여주는 프로그레스 바(진행 상태 바)
 * 3. 카테고리에 맞춘 색상의 배지 표시
 * 4. 부드러운 호버 애니메이션 (글래스모피즘 스타일 적용)
 * * 사용된 리액트 개념:
 * - useState: 카드의 확장/축소 상태를 관리합니다.
 * - Props: 부모 컴포넌트로부터 코스 데이터를 전달받습니다.
 * - 조건부 렌더링: 상태(State)에 따라 서로 다른 UI를 화면에 보여줍니다.
 * - 이벤트 핸들러: 클릭 및 호버 상호작용을 처리합니다.
 * =============================================================================
 */

// =============================================================================
// IMPORTS - 필요한 모듈 가져오기
// =============================================================================

/**
 * useState 훅 설명 (useState HOOK EXPLANATION):
 * useState는 리액트의 "훅(Hook)"으로, 컴포넌트에 상태(State)를 추가할 수 있게 해주는 특별한 함수입니다.
 * 상태는 시간이 지남에 따라 변할 수 있는 데이터를 의미합니다. 상태가 변하면 리액트는 컴포넌트를 다시 그립니다(재렌더링).
 * * 문법: const [value, setValue] = useState(initialValue);
 * - value: 현재의 상태 값
 * - setValue: 상태를 업데이트(변경)하는 함수
 * - initialValue: 상태의 초기 설정값
 */
import { useState } from 'react';

/**
 * lucide-react 라이브러리에서 아이콘을 가져옵니다.
 * - ChevronDown: 아래쪽 화살표 (섹션 목록이 접혀 있을 때 표시됨)
 * - ChevronUp: 위쪽 화살표 (섹션 목록이 펼쳐져 있을 때 표시됨)
 * - BookOpen: 책 아이콘 (코스 아이콘으로 사용됨)
 */
import { ChevronDown, ChevronUp, BookOpen } from 'lucide-react';

/**
 * TypeScript 타입과 설정을 가져옵니다.
 * "type" 키워드는 이들이 런타임이 아닌 타입 검사용으로만 사용됨을 의미합니다.
 */
import type { Course } from '../types/QuizCourse.types';
import { CATEGORY_CONFIG } from '../types/QuizCourse.types';

/**
 * 이 카드 안에서 섹션 목록을 보여주기 위해 SectionList 컴포넌트를 가져옵니다(Import).
 * 이것은 '컴포넌트 합성(Component Composition)'이라 불리는 기법으로, 
 * 하나의 컴포넌트 내부에서 다른 컴포넌트를 사용하는 방식입니다.
 */
import { SectionList } from './SectionList';

// =============================================================================
// PROPS INTERFACE - 컴포넌트가 받는 속성 정의
// =============================================================================

/**
 * CourseCardProps - CourseCard 컴포넌트를 위한 Props 인터페이스입니다.
 * * @property {Course} course - 화면에 표시할 코스 데이터 객체입니다.
 * @property {function} onSectionClick - 섹션을 클릭했을 때 실행되는 선택적 콜백 함수입니다.
 * 코스 ID(courseId)와 섹션 ID(sectionId)를 모두 전달받습니다.
 */
interface CourseCardProps {
    course: Course;
    onSectionClick?: (courseId: string, sectionId: string) => void;
}

// =============================================================================
// COMPONENT DEFINITION - 컴포넌트 정의
// =============================================================================

/**
 * CourseCard Component - 코스 카드 컴포넌트
 * 
 * This is the main export of this file. It renders a card for one course.
 * 
 * @param props.course - The course object containing all course data
 * @param props.onSectionClick - Callback function for section click events
 * @returns JSX representing the course card UI
 */
export const CourseCard = ({ course, onSectionClick }: CourseCardProps) => {
    // =========================================================================
    // STATE - 컴포넌트 상태 관리
    // =========================================================================

    /**
     * isExpanded 상태:
     * 섹션 목록이 보이는지 숨겨지는지를 제어합니다.
     * - true: 섹션 목록이 보임 (카드가 확장됨)
     * - false: 섹션 목록이 숨겨짐 (카드가 축소됨)
     * 
     * useState(false)는 카드가 축소된 상태로 시작함을 의미합니다.
     * setIsExpanded is the function we call to change this value.
     */
    const [isExpanded, setIsExpanded] = useState(false);

    // =========================================================================
    // DERIVED DATA - 데이터 가공
    // =========================================================================

    /**
     * 카테고리 설정 (CATEGORY CONFIGURATION):
     * 이 코스의 카테고리에 대한 표시 설정을 조회합니다.
     * 이를 통해 카테고리의 한국어 레이블, 색상, 그라데이션을 얻을 수 있습니다.
     * 
     * 예시: course.category가 'OS'라면, categoryConfig는 다음과 같을 것입니다:
     * { label: '운영체제', color: 'var(--color-primary)', gradient: '...' }
     */
    const categoryConfig = CATEGORY_CONFIG[course.category];

    /**
     * 진행률 계산 (PROGRESS CALCULATION):
     * 진행률 표시줄에 표시할 완료된 섹션 수를 계산합니다.
     * 
     * filter() 메서드는 테스트를 통과하는 항목만 포함하는 새로운 배열을 생성합니다.
     * 여기서는 isCompleted가 true인 섹션만 필터링한 후 그 수를 셉니다.
     */
    const completedCount = course.sections.filter(s => s.isCompleted).length;
    const totalCount = course.sections.length;

    /**
     * 진행률 백분율 계산 (PERCENTAGE CALCULATION):
     * 완료된 섹션 수를 백분율(0-100)로 변환합니다.
     * 삼항 연산자를 사용하여 섹션이 없을 때 0으로 나누는 오류를 방지합니다.
     */
    const progressPercent = totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

    // =========================================================================
    // RENDER - JSX 반환 (화면에 표시될 내용)
    // =========================================================================

    return (
        /**
         * 메인 카드 컨테이너 (MAIN CARD CONTAINER):
         * 카드 전체를 감싸는 외부 div 요소입니다.
         * 디자인 시스템과의 일관성을 위해 CSS 변수를 사용하여 스타일을 적용합니다.
         */
        <div
            style={{
                // Glassmorphism-style card appearance
                backgroundColor: 'var(--color-surface)',      // White/light background
                borderRadius: 'var(--radius-xl)',             // 16px rounded corners
                border: '2px solid var(--color-border)',      // Subtle border
                transition: 'all var(--transition-base)',     // Smooth animation (250ms)
                boxShadow: 'var(--shadow-sm)',                // Subtle shadow
                overflow: 'hidden',                           // Clip children to rounded corners
            }}
            /**
             * 호버 효과 핸들러 (HOVER EFFECT HANDLERS):
             * 카드 위에 마우스를 올렸을 때 카드가 위로 떠오르는듯한 효과를 만듭니다.
             * * onMouseEnter: 마우스 커서가 요소 위로 올라갔을 때 실행됩니다.
             * onMouseLeave: 마우스 커서가 요소를 벗어날 때 실행됩니다.
             * * 시각적 혼란을 방지하기 위해, 카드가 확장되지 않은 상태(NOT expanded)에서만
             * 호버 효과를 적용합니다.
             */
            onMouseEnter={(e) => {
                if (!isExpanded) {
                    // Transform to lift the card up by 4 pixels
                    e.currentTarget.style.transform = 'translateY(-4px)';
                    // Apply larger shadow for depth effect
                    e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
                    // Change border color to match category
                    e.currentTarget.style.borderColor = categoryConfig.color;
                }
            }}
            onMouseLeave={(e) => {
                if (!isExpanded) {
                    // Reset all styles back to normal
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = 'var(--shadow-sm)';
                    e.currentTarget.style.borderColor = 'var(--color-border)';
                }
            }}
        >
            {/* ================================================================
                CARD HEADER - 클릭 가능한 카드 헤더
                This section is always visible and clickable to expand/collapse
                ================================================================ */}
            <div
                /**
                 * 클릭하여 토글 (CLICK TO TOGGLE):
                 * 클릭하면 isExpanded 상태를 토글(반전)합니다.
                 * !isExpanded는 "isExpanded의 반대 상태"를 의미합니다.
                 * - isExpanded가 false이면 true가 되어 확장됩니다.
                 * - isExpanded가 true이면 false가 되어 축소됩니다.
                 */
                onClick={() => setIsExpanded(!isExpanded)}
                className="cursor-pointer"  // 클릭 가능한 요소임을 나타내는 포인터 커서 표시
                style={{ padding: 'var(--spacing-xl)' }}  // 32px padding
            >
                {/* ============================================================
                    TOP ROW: Icon and Category Badge
                    ============================================================ */}
                <div className="flex items-start justify-between mb-4">
                    {/* GRADIENT ICON BOX */}
                    <div
                        className="flex items-center justify-center"
                        style={{
                            width: '48px',
                            height: '48px',
                            borderRadius: 'var(--radius-lg)',  // 12px radius
                            /**
                             * 그라데이션 배경 (GRADIENT BACKGROUND):
                             * CATEGORY_CONFIG에 정의된 그라데이션을 사용합니다.
                             * 각 카테고리는 시각적 구분을 위해 고유한 그라데이션을 가집니다.
                             */
                            background: categoryConfig.gradient,
                        }}
                    >
                        {/* Book icon in white, centered in the gradient box */}
                        <BookOpen size={24} color="white" />
                    </div>

                    {/* CATEGORY BADGE */}
                    <span
                        style={{
                            padding: '0.25rem 0.75rem',
                            borderRadius: 'var(--radius-pill)',  // Pill-shaped badge
                            /**
                             * SEMI-TRANSPARENT BACKGROUND:
                             * color-mix blends the category color with transparent
                             * to create a subtle tinted background.
                             */
                            backgroundColor: `color-mix(in srgb, ${categoryConfig.color} 15%, transparent)`,
                            color: categoryConfig.color,  // Text color matches category
                            fontSize: 'var(--font-size-xs)',
                            fontWeight: 'var(--font-weight-semibold)',
                        }}
                    >
                        {categoryConfig.label}  {/* Korean category name */}
                    </span>
                </div>

                {/* ============================================================
                    COURSE TITLE
                    ============================================================ */}
                <h3
                    className="text-xl font-bold mb-2"
                    style={{ color: 'var(--color-text-primary)' }}
                >
                    {course.title}  {/* Course title from data */}
                </h3>

                {/* ============================================================
                    COURSE DESCRIPTION
                    ============================================================ */}
                <p
                    className="text-sm mb-4"
                    style={{
                        color: 'var(--color-text-secondary)',
                        lineHeight: 'var(--line-height-relaxed)',  // 1.75 for readability
                    }}
                >
                    {course.description}  {/* Course description from data */}
                </p>

                {/* ============================================================
                    PROGRESS BAR SECTION
                    Shows how many sections are completed
                    ============================================================ */}
                <div className="mb-3">
                    {/* Progress text row */}
                    <div className="flex items-center justify-between mb-1">
                        <span
                            className="text-xs"
                            style={{ color: 'var(--color-text-tertiary)' }}
                        >
                            진행률  {/* Korean: "Progress" */}
                        </span>
                        <span
                            className="text-xs font-medium"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            {completedCount}/{totalCount} 섹션  {/* e.g., "2/4 섹션" */}
                        </span>
                    </div>

                    {/* Progress bar track (background) */}
                    <div
                        style={{
                            height: '6px',
                            backgroundColor: 'var(--color-border)',  // Gray background
                            borderRadius: 'var(--radius-full)',      // Fully rounded
                            overflow: 'hidden',                       // Clip the fill
                        }}
                    >
                        {/* Progress bar fill (colored portion) */}
                        <div
                            style={{
                                height: '100%',
                                /**
                                 * 동적 너비 설정 (DYNAMIC WIDTH):
                                 * 백틱(`)을 사용하는 템플릿 리터럴 문법을 통해 ${...} 안에 
                                 * 자바스크립트 표현식을 삽입할 수 있습니다.
                                 * 예를 들어, progressPercent가 50이라면 이 코드는 "width: 50%"로 변환됩니다.
                                 */
                                width: `${progressPercent}%`,
                                background: categoryConfig.gradient,  // Gradient fill
                                borderRadius: 'var(--radius-full)',
                                transition: 'width var(--transition-base)',  // Animate changes
                            }}
                        />
                    </div>
                </div>

                {/* ============================================================
                    EXPAND/COLLAPSE INDICATOR
                    Shows current state and hints that clicking will toggle
                    ============================================================ */}
                <div
                    className="flex items-center justify-center gap-1"
                    style={{
                        color: categoryConfig.color,
                        fontWeight: 'var(--font-weight-medium)',
                        fontSize: 'var(--font-size-sm)',
                    }}
                >
                    {/* Text changes based on expanded state */}
                    <span>{isExpanded ? '섹션 닫기' : '섹션 보기'}</span>
                    {/**
                     * CONDITIONAL ICON:
                     * Show ChevronUp when expanded (to hint "click to collapse")
                     * Show ChevronDown when collapsed (to hint "click to expand")
                     */}
                    {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                </div>
            </div>

            {/* ================================================================
                EXPANDABLE SECTION LIST
                This section animates in/out based on isExpanded state
                ================================================================ */}
            <div
                style={{
                    /**
                     * 축소/확장 애니메이션 (COLLAPSIBLE ANIMATION):
                     * 애니메이션 적용이 불가능한 'display: none' 방식 대신, 
                     * 'maxHeight' 값을 0에서 큰 값 사이로 조절하여 애니메이션을 구현합니다.
                     * * maxHeight가 0일 때는 콘텐츠가 보이지 않습니다.
                     * maxHeight가 500px(충분히 큰 값)일 때는 콘텐츠가 화면에 나타납니다.
                     * transition 속성을 통해 이 두 상태 사이를 부드럽게 연결해 줍니다.
                     */
                    maxHeight: isExpanded ? '500px' : '0',
                    overflow: 'hidden',  // Hide content that exceeds maxHeight
                    transition: 'max-height var(--transition-smooth)',  // 300ms animation
                }}
            >
                {/* Container for the section list with styled background */}
                <div
                    style={{
                        borderTop: '1px solid var(--color-border)',  // Separator line
                        backgroundColor: 'var(--color-background-secondary)',  // Slightly tinted
                        padding: 'var(--spacing-md)',  // 16px padding
                    }}
                >
                    {/**
                     * 섹션 리스트 컴포넌트 (SECTIONLIST COMPONENT):
                     * 여기서 SectionList 컴포넌트를 렌더링하며 다음 정보를 전달합니다:
                     * - sections: 이 코스에 속한 섹션들의 배열
                     * - categoryColor: 카테고리 색상에 맞춘 재생 버튼 색상
                     * - onSectionClick: 코스 ID를 포함하는 콜백 함수
                     * * PROPS 내 화살표 함수 설명:
                     * (sectionId: string) => onSectionClick?.(course.id, sectionId)
                     * SectionList가 이 함수를 sectionId와 함께 호출하면,
                     * 우리는 여기에 course.id를 추가하여 부모 컴포넌트의 콜백으로 전달합니다.
                     */}
                    <SectionList
                        sections={course.sections}
                        categoryColor={categoryConfig.color}
                        onSectionClick={(sectionId: string) => onSectionClick?.(course.id, sectionId)}
                    />
                </div>
            </div>
        </div>
    );
};
