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

// =============================================================================
// IMPORTS
// =============================================================================
import {
    Cpu,
    Network,
    Database,
    Binary,
    Code2,
    Leaf,
    Globe,
    Atom,
    Smartphone,
    Layers,
    Terminal,
    Server,
    FileCode,
    GitBranch,
    Box,
    Layout,
    Cloud,
    Shield,
    Award,
    LucideIcon,
    MonitorPlay,
    Braces,
    Workflow,
} from 'lucide-react';

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
    // 기존 카테고리 (하위 호환성 유지)
    category: CourseCategory;
    // DB에서 넘어오는 원본 코드 (예: "ALGORITHM", "JAVA_SPRING") - Optional for backward compatibility during migration
    code?: string;
    sections: Section[]; // Section[]은 "Section 객체들을 담은 배열"을 의미합니다.
}

// -----------------------------------------------------------------------------
// 카테고리 표시 설정 (CATEGORY CONFIGURATION)
// -----------------------------------------------------------------------------
/**
 * CATEGORY_CONFIG - 카테고리 표시 설정
 * * 이 객체는 각 카테고리별 표시 설정(한국어 레이블, 색상, 그라데이션)을 저장합니다.
 * 백엔드에서 새로운 코드가 추가될 수 있으므로 키 타입을 string으로 확장했습니다.
 */
export const CATEGORY_CONFIG: Record<
    string,
    { label: string; color: string; gradient: string; icon: LucideIcon }
> = {
    // 기존 매핑
    OS: {
        label: '운영체제',
        color: 'var(--color-primary)',
        gradient: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%)',
        icon: Cpu,
    },
    Network: {
        label: '네트워크',
        color: 'var(--color-secondary)',
        gradient: 'linear-gradient(135deg, var(--color-secondary) 0%, var(--color-accent) 100%)',
        icon: Network,
    },
    DB: {
        label: '데이터베이스',
        color: 'var(--color-accent)',
        gradient: 'linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%)',
        icon: Database,
    },
    DataStructure: {
        label: '자료구조',
        color: 'var(--color-google-blue)',
        gradient: 'linear-gradient(135deg, var(--color-google-blue) 0%, var(--color-google-green) 100%)',
        icon: Binary,
    },
    // 추가된 DB 코드 매핑
    ALGORITHM: {
        label: '알고리즘',
        color: 'var(--color-warning)', // 노란색 계열
        gradient: 'linear-gradient(135deg, var(--color-warning) 0%, var(--color-error) 100%)',
        icon: Workflow,
    },
    JAVA_SPRING: {
        label: 'Spring',
        color: 'var(--color-success)', // 초록색 계열
        gradient: 'linear-gradient(135deg, var(--color-success) 0%, var(--color-info) 100%)',
        icon: Leaf,
    },
    WEB: {
        label: 'Web',
        color: 'var(--color-info)', // 파란색 계열
        gradient: 'linear-gradient(135deg, var(--color-info) 0%, var(--color-primary) 100%)',
        icon: Globe,
    },
    // FrontEnd & Mobile
    REACT: {
        label: 'React',
        color: '#61DAFB',
        gradient: 'linear-gradient(135deg, #61DAFB 0%, #2188FF 100%)',
        icon: Atom,
    },
    MOBILE: {
        label: '모바일',
        color: '#42A5F5',
        gradient: 'linear-gradient(135deg, #42A5F5 0%, #1565C0 100%)',
        icon: Smartphone,
    },
    WEB_BASIC: {
        label: '웹 기초',
        color: '#E65100',
        gradient: 'linear-gradient(135deg, #FFA726 0%, #E65100 100%)',
        icon: Layout,
    },
    // BackEnd & Languages
    PYTHON: {
        label: 'Python',
        color: '#FFD54F',
        gradient: 'linear-gradient(135deg, #FFD54F 0%, #FF6F00 100%)',
        icon: Code2,
    },
    NODEJS: {
        label: 'Node.js',
        color: '#66BB6A',
        gradient: 'linear-gradient(135deg, #81C784 0%, #2E7D32 100%)',
        icon: Server,
    },
    KOTLIN: {
        label: 'Kotlin',
        color: '#7E57C2',
        gradient: 'linear-gradient(135deg, #7E57C2 0%, #512DA8 100%)',
        icon: FileCode,
    },
    TYPESCRIPT: {
        label: 'TypeScript',
        color: '#3178C6',
        gradient: 'linear-gradient(135deg, #3178C6 0%, #235A97 100%)',
        icon: Braces,
    },
    JAVASCRIPT: {
        label: 'JavaScript',
        color: '#F7DF1E',
        gradient: 'linear-gradient(135deg, #F7DF1E 0%, #E6C800 100%)',
        icon: Code2,
    },
    // Infrastructure & DevOps
    DEVOPS: {
        label: 'DevOps',
        color: '#26A69A',
        gradient: 'linear-gradient(135deg, #26A69A 0%, #00695C 100%)',
        icon: Layers,
    },
    LINUX: {
        label: 'Linux',
        color: '#78909C',
        gradient: 'linear-gradient(135deg, #B0BEC5 0%, #455A64 100%)',
        icon: Terminal,
    },
    GIT: {
        label: 'Git',
        color: '#F4511E',
        gradient: 'linear-gradient(135deg, #FF7043 0%, #bf360c 100%)',
        icon: GitBranch,
    },
    // CS Fundamentals & Design
    DESIGN_PATTERN: {
        label: '디자인패턴',
        color: '#AB47BC',
        gradient: 'linear-gradient(135deg, #AB47BC 0%, #7B1FA2 100%)',
        icon: Box,
    },
    SYSTEM_DESIGN: {
        label: '시스템설계',
        color: '#5C6BC0',
        gradient: 'linear-gradient(135deg, #7986CB 0%, #3949AB 100%)',
        icon: Cloud,
    },
    COMPUTER_ARCH: {
        label: '컴퓨터구조',
        color: '#8D6E63',
        gradient: 'linear-gradient(135deg, #A1887F 0%, #5D4037 100%)',
        icon: MonitorPlay,
    },
    SW_ENG: {
        label: 'SW공학',
        color: '#7CB342', // light green
        gradient: 'linear-gradient(135deg, #AED581 0%, #689F38 100%)',
        icon: Code2,
    },
    OS_EXT: {
        label: 'OS심화',
        color: '#0288D1', // dark blue
        gradient: 'linear-gradient(135deg, #29B6F6 0%, #01579B 100%)',
        icon: Cpu,
    },
    NETWORK_EXT: {
        label: '네트워크심화',
        color: '#0097A7', // cyan
        gradient: 'linear-gradient(135deg, #4DD0E1 0%, #006064 100%)',
        icon: Network,
    },
    // Advanced Tech
    AI_ML: {
        label: 'AI/ML',
        color: '#FF4081',
        gradient: 'linear-gradient(135deg, #FF80AB 0%, #C51162 100%)',
        icon: Cpu,
    },
    NOSQL_MQ: {
        label: 'NoSQL/MQ',
        color: '#D81B60',
        gradient: 'linear-gradient(135deg, #EC407A 0%, #880E4F 100%)',
        icon: Database,
    },
    SECURITY: {
        label: '정보보안',
        color: '#424242',
        gradient: 'linear-gradient(135deg, #757575 0%, #212121 100%)',
        icon: Shield,
    },
    // Certification (Special Request)
    CERT_EIP: {
        label: 'CERT',
        color: '#607D8B', // blue grey
        gradient: 'linear-gradient(135deg, #90A4AE 0%, #455A64 100%)',
        icon: Award,
    },
    CERT_SQLD: {
        label: 'CERT',
        color: '#FFB74D', // orange
        gradient: 'linear-gradient(135deg, #FFB74D 0%, #F57C00 100%)',
        icon: Award,
    },
};

/**
 * 기본 카테고리 설정 (매핑되지 않은 코드용 Fallback)
 */
export const DEFAULT_CATEGORY_CONFIG: {
    label: string;
    color: string;
    gradient: string;
    icon: LucideIcon;
} = {
    label: '기타',
    color: 'var(--color-gray-500)',
    gradient: 'linear-gradient(135deg, var(--color-gray-500) 0%, var(--color-gray-700) 100%)',
    icon: Code2,
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