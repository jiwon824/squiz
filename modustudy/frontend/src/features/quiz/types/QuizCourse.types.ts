/**
 * =============================================================================
 * QuizCourse.types.ts - 퀴즈 코스 타입 정의 파일
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 파일은 퀴즈 코스 기능에 필요한 타입(Types)과 인터페이스(Interfaces)를 정의합니다.
 * 타입스크립트의 타입은 데이터가 어떤 형태를 가져야 하는지 정의하며, 코드가 실행되기 전에
 * 에러를 잡아내는 역할을 합니다. 타입은 객체가 어떤 속성을 가져야 하는지 설명하는
 * '설계도'라고 생각하면 쉽습니다.
 * * 인터페이스란? (WHAT IS AN INTERFACE?):
 * 인터페이스는 일종의 '계약'입니다. "이 타입을 사용하는 모든 객체는 반드시 정의된 
 * 특정 속성과 타입을 가져야 한다"고 명시하는 것입니다. 예를 들어, Section 인터페이스는
 * 모든 섹션 객체가 id(문자열), title(문자열) 등을 가져야 함을 보장합니다.
 * * 타입 별칭이란? (WHAT IS A TYPE ALIAS?):
 * 타입 별칭은 특정 타입에 이름을 붙이는 것입니다. 예를 들어 `CourseCategory`는 
 * 허용된 특정 문자열들의 집합('OS' | 'Network' | 'DB' | 'DataStructure')에 붙인 이름입니다.
 * "|" 기호는 "또는(OR)"을 의미하며, CourseCategory는 이 네 가지 문자열 중 하나만 가질 수 있습니다.
 * =============================================================================
 */

// -----------------------------------------------------------------------------
// 카테고리 타입 정의 (CATEGORY TYPE DEFINITION)
// -----------------------------------------------------------------------------
/**
 * CourseCategory - 코스 카테고리 타입
 * * 이것은 "유니온 타입(Union Type)"입니다. 즉, CourseCategory는 오직 명시된 
 * 네 가지 문자열 값 중 하나만 가질 수 있습니다. 만약 "Python"과 같은 다른 문자열을
 * 사용하려고 하면 타입스크립트가 에러를 표시합니다.
 * * - 'OS': 운영체제
 * - 'Network': 네트워크 기초
 * - 'DB': 데이터베이스
 * - 'DataStructure': 자료구조 및 알고리즘
 */
export type CourseCategory = 'OS' | 'Network' | 'DB' | 'DataStructure';

// -----------------------------------------------------------------------------
// 난이도 레벨 타입 정의 (DIFFICULTY LEVEL TYPE DEFINITION)
// -----------------------------------------------------------------------------
/**
 * DifficultyLevel - 난이도 레벨 타입
 * * CourseCategory와 마찬가지로, 난이도를 정확히 세 가지 값으로 제한하는 유니온 타입입니다.
 * 이를 통해 모든 섹션에서 일관된 난이도 명칭을 사용하도록 보장합니다.
 * * - 'Easy': 초급
 * - 'Medium': 중급  
 * - 'Hard': 고급
 */
export type DifficultyLevel = 'Easy' | 'Medium' | 'Hard';

// -----------------------------------------------------------------------------
// 섹션 인터페이스 (SECTION INTERFACE)
// -----------------------------------------------------------------------------
/**
 * Section - 섹션 인터페이스
 * * Section은 하나의 코스 내에 포함된 개별 퀴즈 단위를 나타냅니다. 
 * 예를 들어 "운영체제" 코스 안에 "프로세스 관리", "CPU 스케줄링" 같은 섹션이 있을 수 있습니다.
 * * 속성 설명 (PROPERTY EXPLANATIONS):
 * @property {string} id - 섹션의 고유 식별자 (예: "os-process-1")
 * @property {string} title - 화면에 표시될 한국어 이름 (예: "프로세스 생명주기")
 * @property {DifficultyLevel} difficulty - 해당 섹션의 난이도 (초급/중급/고급)
 * @property {boolean} isCompleted - 사용자가 이 섹션을 완료했는지 여부
 * @property {number} [questionCount] - 선택사항: 이 섹션에 포함된 문항 수
 * 속성명 뒤의 "?"는 이 값이 없을 수도 있음을 의미합니다.
 */
export interface Section {
    id: string;
    title: string;
    difficulty: DifficultyLevel;
    isCompleted: boolean;
    questionCount?: number; // "?"는 선택적 속성(Optional)임을 나타냅니다.
}

// -----------------------------------------------------------------------------
// 코스 인터페이스 (COURSE INTERFACE)
// -----------------------------------------------------------------------------
/**
 * Course - 코스 인터페이스
 * * Course는 관련된 섹션(Section)들의 집합입니다. "프로세스 관리"나 "TCP/IP 프로토콜"과 같은
 * 주요 주제를 나타냅니다. 각 코스는 반드시 하나의 카테고리(OS, Network, DB, DataStructure)에 속합니다.
 * * 속성 설명 (PROPERTY EXPLANATIONS):
 * @property {string} id - 코스의 고유 식별자 (예: "os-process")
 * @property {string} title - 화면에 표시될 한국어 제목 (예: "프로세스 관리")
 * @property {string} description - 이 코스에서 다루는 내용에 대한 짧은 설명
 * @property {CourseCategory} category - 코스가 속한 카테고리
 * @property {Section[]} sections - 섹션 객체들의 배열 ([]는 "배열"을 의미합니다)
 */
export interface Course {
    id: string;
    title: string;
    description: string;
    category: CourseCategory;
    sections: Section[]; // Section[]은 "Section 객체들을 담은 배열"을 의미합니다.
}

// -----------------------------------------------------------------------------
// 카테고리 표시 설정 (CATEGORY CONFIGURATION)
// -----------------------------------------------------------------------------
/**
 * CATEGORY_CONFIG - 카테고리 표시 설정
 * * 이 객체는 각 카테고리별 표시 설정(한국어 레이블, 색상, 그라데이션)을 저장합니다.
 * "Record" 타입을 사용하여 다음과 같이 맵(Map) 구조를 형성합니다:
 * - 키(Key): CourseCategory (OS, Network, DB, DataStructure)
 * - 값(Value): label, color, gradient 속성을 가진 객체
 * * 왜 설정 객체를 사용하나요?
 * 여러 곳에 색상 코드를 직접 입력(Hardcoding)하는 대신, 여기서 한 번만 정의하여 관리합니다.
 * "OS" 카테고리의 색상을 바꾸고 싶다면 여기만 수정하면 모든 곳에 자동으로 반영됩니다.
 * * CSS 변수 사용:
 * "var(--color-xxx)" 문법은 index.css에 정의된 CSS 변수를 참조합니다.
 * 이를 통해 라이트/다크 모드에 따라 색상이 자동으로 변경될 수 있습니다.
 */
export const CATEGORY_CONFIG: Record<CourseCategory, { label: string; color: string; gradient: string }> = {
    OS: {
        label: '운영체제',
        color: 'var(--color-primary)',
        gradient: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%)',
    },
    Network: {
        label: '네트워크',
        color: 'var(--color-secondary)',
        gradient: 'linear-gradient(135deg, var(--color-secondary) 0%, var(--color-accent) 100%)',
    },
    DB: {
        label: '데이터베이스',
        color: 'var(--color-accent)',
        gradient: 'linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%)',
    },
    DataStructure: {
        label: '자료구조',
        color: 'var(--color-google-blue)',
        gradient: 'linear-gradient(135deg, var(--color-google-blue) 0%, var(--color-google-green) 100%)',
    },
};

// -----------------------------------------------------------------------------
// 난이도 표시 설정 (DIFFICULTY CONFIGURATION)
// -----------------------------------------------------------------------------
/**
 * DIFFICULTY_CONFIG - 난이도 표시 설정
 * * CATEGORY_CONFIG와 유사하게 각 난이도 레벨에 대한 표시 설정을 저장합니다.
 * 각 난이도는 한국어 레이블과 고유 색상을 가집니다.
 * * 색상 선택 기준:
 * - 초급: 초록색 (안전, 쉬움)
 * - 중급: 노란색 (주의, 중간)
 * - 고급: 빨간색 (도전, 어려움)
 */
export const DIFFICULTY_CONFIG: Record<DifficultyLevel, { label: string; color: string }> = {
    Easy: {
        label: '초급',
        color: 'var(--color-google-green)',
    },
    Medium: {
        label: '중급',
        color: 'var(--color-google-yellow)',
    },
    Hard: {
        label: '고급',
        color: 'var(--color-google-red)',
    },
};