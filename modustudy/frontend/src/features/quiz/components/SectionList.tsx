/**
 * =============================================================================
 * SectionList.tsx - 섹션 목록 컴포넌트
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 컴포넌트는 하나의 코스 내에 포함된 퀴즈 섹션들의 목록을 표시합니다.
 * 각 섹션은 제목, 난이도, 완료 상태를 보여주며, 퀴즈를 시작할 수 있는 재생(Play) 버튼을 포함합니다.
 * 
 * * 리액트 컴포넌트란 무엇인가요? (WHAT IS A REACT COMPONENT?):
 * 리액트 컴포넌트는 재사용 가능한 UI 조각입니다. 레고 블록처럼 생각하면 쉽습니다. 
 * 동일한 컴포넌트를 서로 다른 데이터(Props)와 함께 여러 번 사용할 수 있습니다.
 * 이 SectionList 컴포넌트는 전달받은 어떤 섹션 데이터든 목록으로 그려낼 수 있습니다.
 * 
 * * 컴포넌트 계층 구조 (COMPONENT HIERARCHY):
 * QuizCourseList → CourseCard → SectionList
 * - QuizCourseList: 모든 코스를 보여주는 부모 페이지입니다.
 * - CourseCard: 하나의 코스와 그에 속한 섹션들을 보여줍니다.
 * - SectionList (현재 파일): CourseCard 내부에서 섹션 목록을 렌더링합니다.
 * =============================================================================
 */

// =============================================================================
// IMPORTS - 필요한 모듈 가져오기
// =============================================================================

/**
 * Lucide-react는 일관성 있고 아름다운 아이콘들을 제공합니다.
 * 앱의 용량(번들 크기)을 작게 유지하기 위해, 필요한 아이콘들만 골라서 가져옵니다(Import).
 * * - CheckCircle: 체크 표시가 된 채워진 원 (완료된 섹션용)
 * - Circle: 비어 있는 원 (미완료 섹션용)
 * - Play: 삼각형 모양의 재생 버튼 (퀴즈 시작용)
 */
import { CheckCircle, Circle, Play } from 'lucide-react';

/**
 * TypeScript 타입들을 타입 파일에서 가져옵니다.
 * "type" 키워드는 이 import가 타입 체크 용도로만 사용되며, 
 * 최종 자바스크립트 번들에는 포함되지 않는다는 것을 TypeScript에게 알려줍니다.
 */
import type { Section } from '../types/QuizCourse.types';

/**
 * 난이도별 한국어 라벨과 색상 정보를 담고 있는 DIFFICULTY_CONFIG 상수를 가져옵니다.
 * (초급, 중급, 고급)
 */
import { DIFFICULTY_CONFIG } from '../types/QuizCourse.types';

// =============================================================================
// PROPS INTERFACE - 컴포넌트가 받는 속성 정의
// =============================================================================

/**
 * SectionListProps - 이 컴포넌트가 받는 props(속성)를 정의합니다.
 * * Props란 무엇인가요? (WHAT ARE PROPS?):
 * Props는 컴포넌트에 전달하는 함수의 인자(Arguments)와 같습니다. <SectionList>를 사용할 때,
 * 필요한 데이터를 이 Props를 통해 전달합니다. Props는 '읽기 전용(READ-ONLY)'입니다.
 * 즉, 컴포넌트는 자신이 전달받은 Props를 직접 수정할 수 없습니다.
 * * @property {Section[]} sections - 화면에 표시할 섹션 객체들의 배열입니다.
 * @property {string} categoryColor - 재생 버튼에 사용될 색상입니다. (코스 카테고리 색상과 일치시킴)
 * @property {function} onSectionClick - 섹션을 클릭했을 때 실행될 선택적 콜백 함수입니다.
 * 변수명 뒤의 "?"는 이 값이 필수 사항이 아님을 의미
 */
interface SectionListProps {
    sections: Section[];
    categoryColor: string;
    onSectionClick?: (sectionId: string) => void;
}

// =============================================================================
// COMPONENT DEFINITION - 컴포넌트 정의
// =============================================================================

/**
 * SectionList 컴포넌트 - 섹션 목록 컴포넌트
 * * 컴포넌트 문법 설명:
 * - "export const"는 이 컴포넌트를 다른 파일에서 가져와서(import) 사용할 수 있게 만듭니다.
 * - "= ({ sections, categoryColor, onSectionClick })"는 props를 구조 분해 할당합니다.
 * (구조 분해 할당은 객체 안의 속성들을 꺼내어 각각의 변수로 담는 방식입니다.)
 * - ": SectionListProps"는 이 props들이 어떤 타입을 가져야 하는지 타입스크립트에게 알려줍니다.
 * - "=> { ... }"는 JSX(화면 구성을 위한 코드)를 반환하는 화살표 함수
 * 
 * * @param props - 이 컴포넌트에 전달되는 속성(데이터)들입니다.
 * @returns 섹션 목록 UI를 나타내는 JSX를 반환합니다.
 */
export const SectionList = ({ sections, categoryColor, onSectionClick }: SectionListProps) => {
    // =========================================================================
    // RENDER - JSX 반환 (화면에 표시될 내용)
    // =========================================================================

    /**
     * 컴포넌트는 JSX를 반환합니다 - HTML처럼 보이지만 실제로는 JavaScript인 문법입니다.
     * JSX를 사용하면 UI 코드를 익숙하고 읽기 쉬운 방식으로 작성할 수 있습니다.
     */
    return (
        /**
         * 모든 섹션들을 담는 컨테이너 div
         * React에서 "className"은 HTML의 "class"와 같습니다 (class는 자바스크립트에서 예약어이기 때문).
         * Tailwind CSS 클래스 설명:
         * - flex: flexbox로 표시
         * - flex-col: 아이템들을 수직(세로) 방향으로 쌓음
         * - gap-2: 아이템들 사이에 0.5rem(8px)의 간격 추가
         */
        <div className="flex flex-col gap-2">
            {/**
             * map() 함수 설명 (MAP FUNCTION EXPLANATION):
             * map() 함수는 배열 안의 각 항목을 다른 형태(요소)로 변환해 줍니다.
             * 여기서는 각 섹션(Section) 객체를 화면에 보여질 JSX 요소(클릭 가능한 행)로 변환합니다.
             * * 문법: array.map((항목) => <컴포넌트 />)
             * - "section"은 현재 처리 중인 배열 내의 항목을 의미합니다.
             * - 화살표 함수는 해당 항목을 바탕으로 생성된 JSX를 반환합니다.
             * - 리액트는 이렇게 반환된 모든 JSX 요소들을 모아 화면에 렌더링합니다.
             */}
            {sections.map((section) => {
                // 각 섹션에 해당하는 난이도 설정(Configuration)을 조회합니다.
                // 이 과정을 통해 해당 난이도 수준에 맞는 한국어 레이블과 색상 정보를 가져옵니다.
                const difficultyConfig = DIFFICULTY_CONFIG[section.difficulty];

                /**
                 * 하나의 섹션 행(Row)에 대한 JSX를 반환합니다.
                 * * KEY 속성 설명 (KEY PROP EXPLANATION):
                 * 리액트에서 목록(List)을 렌더링할 때 "key" 속성은 **필수**입니다.
                 * 리액트는 목록이 변경될 때 이 key를 사용하여 DOM을 효율적으로 업데이트합니다.
                 * key는 반드시 고유해야 하며 변하지 않아야 합니다. (항목의 순서가 바뀔 수 있다면 배열의 인덱스를 key로 사용하지 마세요.)
                 * 여기서는 각 섹션의 고유함이 보장되는 section.id를 key로 사용합니다.
                 */
                return (
                    <div
                        key={section.id}  // React 조정(reconciliation)을 위한 고유 식별자

                        /**
                         * ONCLICK 핸들러:
                         * 이 div를 클릭하면 이 섹션의 id로 onSectionClick 콜백을 호출합니다.
                         * "?."는 선택적 체이닝입니다 - onSectionClick이 undefined가 아닐 때만
                         * 함수를 호출합니다.
                         */
                        onClick={() => onSectionClick?.(section.id)}

                        /**
                         * 스타일링을 위한 Tailwind 클래스들:
                         * - cursor-pointer: 호버 시 포인터 커서 표시 (클릭 가능함을 나타냄)
                         * - group: 자식 요소의 group-hover 활성화
                         */
                        className="cursor-pointer group"

                        /**
                         * CSS 변수를 사용한 인라인 스타일:
                         * 동적 스타일링과 CSS 변수 접근을 위해 style 객체를 사용합니다.
                         * 이중 중괄호: 바깥 {}는 JSX 표현식, 안쪽 {}는 객체용입니다.
                         */
                        style={{
                            display: 'flex',              // 자식을 가로로 배치
                            alignItems: 'center',         // 아이템을 수직 중앙 정렬
                            gap: 'var(--spacing-md)',     // 아이템 사이 16px 간격
                            padding: 'var(--spacing-sm) var(--spacing-md)',  // 상하 8px, 좌우 16px
                            borderRadius: 'var(--radius-md)',    // 둥근 모서리
                            backgroundColor: 'var(--color-surface)',  // 카드 배경색
                            border: '1px solid var(--color-border)',  // 미묘한 테두리
                            transition: 'all var(--transition-fast)', // 부드러운 호버 애니메이션
                        }}

                        /**
                         * MOUSE EVENT HANDLERS:
                         * These functions run when the mouse enters/leaves the element.
                         * e.currentTarget refers to the element the handler is attached to.
                         * We directly modify the style to create hover effects.
                         */
                        onMouseEnter={(e) => {
                            e.currentTarget.style.borderColor = categoryColor;
                            e.currentTarget.style.backgroundColor = 'var(--color-surface-hover)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.borderColor = 'var(--color-border)';
                            e.currentTarget.style.backgroundColor = 'var(--color-surface)';
                        }}
                    >
                        {/* ============================================================
                            COMPLETION STATUS ICON
                            Shows a checkmark if completed, empty circle if not
                            ============================================================ */}
                        <div style={{ flexShrink: 0 }}>  {/* Prevent icon from shrinking */}
                            {/**
                             * CONDITIONAL RENDERING:
                             * The ternary operator (condition ? ifTrue : ifFalse) renders
                             * different content based on a condition.
                             * If section.isCompleted is true, show CheckCircle.
                             * Otherwise, show Circle.
                             */}
                            {section.isCompleted ? (
                                <CheckCircle
                                    size={20}  // Icon size in pixels
                                    style={{ color: 'var(--color-google-green)' }}  // Green for completed
                                />
                            ) : (
                                <Circle
                                    size={20}
                                    style={{ color: 'var(--color-text-muted)' }}  // Gray for incomplete
                                />
                            )}
                        </div>

                        {/* ============================================================
                            SECTION INFORMATION (Title and Question Count)
                            ============================================================ */}
                        <div style={{ flex: 1, minWidth: 0 }}>  {/* flex: 1 takes remaining space */}
                            {/* Section Title */}
                            <div
                                className="font-medium text-sm"  // Medium weight, small text
                                style={{
                                    // Completed sections have muted color and strikethrough
                                    color: section.isCompleted
                                        ? 'var(--color-text-secondary)'
                                        : 'var(--color-text-primary)',
                                    textDecoration: section.isCompleted ? 'line-through' : 'none',
                                }}
                            >
                                {section.title}  {/* Display the section title */}
                            </div>

                            {/* Question Count (only show if questionCount exists) */}
                            {/**
                             * LOGICAL AND (&&) FOR CONDITIONAL RENDERING:
                             * {condition && <Element />} only renders the element if condition is truthy.
                             * This is a shorthand for: condition ? <Element /> : null
                             */}
                            {section.questionCount && (
                                <div
                                    className="text-xs"  // Extra small text
                                    style={{ color: 'var(--color-text-tertiary)' }}
                                >
                                    {section.questionCount}문제  {/* "문제" means "questions" in Korean */}
                                </div>
                            )}
                        </div>

                        {/* ============================================================
                            DIFFICULTY BADGE
                            Shows the difficulty level with appropriate color
                            ============================================================ */}
                        <span
                            style={{
                                padding: '0.125rem 0.5rem',  // Compact padding
                                borderRadius: 'var(--radius-sm)',  // Small rounded corners
                                /**
                                 * COLOR-MIX EXPLANATION:
                                 * color-mix() is a modern CSS function that blends two colors.
                                 * Here we blend the difficulty color (15%) with transparent (85%)
                                 * to create a subtle, tinted background.
                                 */
                                backgroundColor: `color-mix(in srgb, ${difficultyConfig.color} 15%, transparent)`,
                                color: difficultyConfig.color,  // Text color matches difficulty
                                fontSize: 'var(--font-size-xs)',  // Extra small font
                                fontWeight: 'var(--font-weight-medium)',  // Medium weight
                                flexShrink: 0,  // Don't shrink the badge
                            }}
                        >
                            {difficultyConfig.label}  {/* Korean label: 초급/중급/고급 */}
                        </span>

                        {/* ============================================================
                            PLAY BUTTON (Appears on Hover)
                            ============================================================ */}
                        <div
                            /**
                             * GROUP-HOVER ANIMATION:
                             * - opacity-0: Initially invisible
                             * - group-hover:opacity-100: Becomes visible when parent (with "group" class) is hovered
                             * This creates a smooth reveal effect for the play button.
                             */
                            className="opacity-0 group-hover:opacity-100"
                            style={{
                                transition: 'opacity var(--transition-fast)',  // Smooth fade animation
                                flexShrink: 0,  // Don't shrink the button
                            }}
                        >
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    width: '28px',
                                    height: '28px',
                                    borderRadius: 'var(--radius-full)',  // Circular button
                                    backgroundColor: categoryColor,  // Color matches course category
                                }}
                            >
                                {/* Play icon - fill and stroke both white for solid look */}
                                <Play size={14} color="white" fill="white" />
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};
