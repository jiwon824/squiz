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
// 필터 카테고리 타입 정의 (FILTER CATEGORY TYPE DEFINITION)
// -----------------------------------------------------------------------------
/**
 * FilterCategory - 필터링에 사용되는 카테고리 타입
 *
 * 시연용 더미 데이터에 맞춰 6개 그룹으로 분류:
 * - 'CSFundamentals': CS 기초 (OS, 네트워크, DB, 알고리즘, 컴퓨터구조 등)
 * - 'Backend': 백엔드 (Java/Spring, Node.js, Python)
 * - 'Frontend': 프론트엔드/모바일 (React, 웹 기초, 모바일, Kotlin)
 * - 'Infrastructure': 인프라/DevOps (DevOps, Linux, 시스템설계, Git)
 * - 'Design': 설계/기타 (디자인패턴, SW공학, 보안, AI/ML, NoSQL)
 * - 'Certification': 자격증 (정보처리기사, SQLD)
 */
export type FilterCategory =
    | 'CSFundamentals'
    | 'Backend'
    | 'Frontend'
    | 'Infrastructure'
    | 'Design'
    | 'Certification';

/**
 * CourseCategory - 코스 카테고리 타입 (하위 호환성 유지)
 * @deprecated FilterCategory 사용 권장
 */
export type CourseCategory = FilterCategory | 'OS' | 'Network' | 'DB' | 'DataStructure';

// -----------------------------------------------------------------------------
// API 코드 → 필터 카테고리 매핑 (CODE TO FILTER CATEGORY MAPPING)
// -----------------------------------------------------------------------------
/**
 * API에서 받은 코스 코드를 필터 카테고리로 변환하는 매핑 테이블
 */
export const CODE_TO_FILTER_CATEGORY: Record<string, FilterCategory> = {
    // CS 기초
    OS: 'CSFundamentals',
    NETWORK: 'CSFundamentals',
    DB: 'CSFundamentals',
    ALGORITHM: 'CSFundamentals',
    COMPUTER_ARCH: 'CSFundamentals',
    OS_EXT: 'CSFundamentals',
    NETWORK_EXT: 'CSFundamentals',
    // 백엔드
    JAVA_SPRING: 'Backend',
    NODEJS: 'Backend',
    PYTHON: 'Backend',
    // 프론트엔드/모바일
    REACT: 'Frontend',
    WEB_BASIC: 'Frontend',
    MOBILE: 'Frontend',
    KOTLIN: 'Frontend',
    // 인프라/DevOps
    DEVOPS: 'Infrastructure',
    LINUX: 'Infrastructure',
    SYSTEM_DESIGN: 'Infrastructure',
    GIT: 'Infrastructure',
    // 설계/기타
    DESIGN_PATTERN: 'Design',
    SW_ENG: 'Design',
    SECURITY: 'Design',
    AI_ML: 'Design',
    NOSQL_MQ: 'Design',
    // 자격증
    CERT_EIP: 'Certification',
    CERT_SQLD: 'Certification',
};

// -----------------------------------------------------------------------------
// 필터 카테고리 표시 설정 (FILTER CATEGORY DISPLAY CONFIG)
// -----------------------------------------------------------------------------
/**
 * 필터 버튼에 표시될 카테고리별 설정
 */
export const FILTER_CATEGORY_CONFIG: Record<
    FilterCategory,
    { label: string; color: string; icon: LucideIcon }
> = {
    CSFundamentals: {
        label: 'CS 기초',
        color: '#6366F1', // Indigo
        icon: Cpu,
    },
    Backend: {
        label: '백엔드',
        color: '#6DB33F', // Spring Green
        icon: Server,
    },
    Frontend: {
        label: '프론트엔드',
        color: '#61DAFB', // React Blue
        icon: Layout,
    },
    Infrastructure: {
        label: '인프라',
        color: '#FF6B35', // Orange
        icon: Cloud,
    },
    Design: {
        label: '설계/기타',
        color: '#8B5CF6', // Purple
        icon: Box,
    },
    Certification: {
        label: '자격증',
        color: '#F59E0B', // Amber
        icon: Award,
    },
};

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
    { label: string; color: string; icon: LucideIcon }
> = {
    // 기존 매핑 -> Undefined in techItems (Other color)
    OS: {
        label: '운영체제',
        color: '#9CA3AF',
        icon: Cpu,
    },
    Network: {
        label: '네트워크',
        color: '#9CA3AF',
        icon: Network,
    },
    DB: {
        label: '데이터베이스',
        color: '#9CA3AF',
        icon: Database,
    },
    DataStructure: {
        label: '자료구조',
        color: '#9CA3AF',
        icon: Binary,
    },
    // 추가된 DB 코드 매핑
    ALGORITHM: {
        label: '알고리즘',
        color: '#9CA3AF',
        icon: Workflow,
    },
    // Defined in techItems (Spring Boot: #6DB33F)
    JAVA_SPRING: {
        label: 'Spring',
        color: '#6DB33F',
        icon: Leaf,
    },
    // Undefined
    WEB: {
        label: 'Web',
        color: '#9CA3AF',
        icon: Globe,
    },
    // FrontEnd & Mobile
    // Defined in techItems (React: #61DAFB)
    REACT: {
        label: 'React',
        color: '#61DAFB',
        icon: Atom,
    },
    // Undefined
    MOBILE: {
        label: '모바일',
        color: '#9CA3AF',
        icon: Smartphone,
    },
    // Undefined
    WEB_BASIC: {
        label: '웹 기초',
        color: '#9CA3AF',
        icon: Layout,
    },
    // BackEnd & Languages
    // Defined in techItems (Python: #3776AB)
    PYTHON: {
        label: 'Python',
        color: '#3776AB',
        icon: Code2,
    },
    // Defined in techItems (Node.js: #339933)
    NODEJS: {
        label: 'Node.js',
        color: '#339933',
        icon: Server,
    },
    // Defined in techItems (Kotlin: #7F52FF)
    KOTLIN: {
        label: 'Kotlin',
        color: '#7F52FF',
        icon: FileCode,
    },
    // Defined in techItems (TypeScript: #3178C6)
    TYPESCRIPT: {
        label: 'TypeScript',
        color: '#3178C6',
        icon: Braces,
    },
    // Defined in techItems (JavaScript: #F7DF1E)
    JAVASCRIPT: {
        label: 'JavaScript',
        color: '#F7DF1E',
        icon: Code2,
    },
    // Infrastructure & DevOps
    // Undefined
    DEVOPS: {
        label: 'DevOps',
        color: '#9CA3AF',
        icon: Layers,
    },
    // Defined in techItems (Linux: #FCC624)
    LINUX: {
        label: 'Linux',
        color: '#FCC624',
        icon: Terminal,
    },
    // Defined in techItems (Git: #F05032)
    GIT: {
        label: 'Git',
        color: '#F05032',
        icon: GitBranch,
    },
    // CS Fundamentals & Design
    // All Undefined
    DESIGN_PATTERN: {
        label: '디자인패턴',
        color: '#9CA3AF',
        icon: Box,
    },
    SYSTEM_DESIGN: {
        label: '시스템설계',
        color: '#9CA3AF',
        icon: Cloud,
    },
    COMPUTER_ARCH: {
        label: '컴퓨터구조',
        color: '#9CA3AF',
        icon: MonitorPlay,
    },
    SW_ENG: {
        label: 'SW공학',
        color: '#9CA3AF',
        icon: Code2,
    },
    OS_EXT: {
        label: 'OS심화',
        color: '#9CA3AF',
        icon: Cpu,
    },
    NETWORK_EXT: {
        label: '네트워크심화',
        color: '#9CA3AF',
        icon: Network,
    },
    // Advanced Tech
    // All Undefined
    AI_ML: {
        label: 'AI/ML',
        color: '#9CA3AF',
        icon: Cpu,
    },
    NOSQL_MQ: {
        label: 'NoSQL/MQ',
        color: '#9CA3AF',
        icon: Database,
    },
    SECURITY: {
        label: '정보보안',
        color: '#9CA3AF',
        icon: Shield,
    },
    // Certification
    // Undefined
    CERT_EIP: {
        label: 'CERT',
        color: '#9CA3AF',
        icon: Award,
    },
    CERT_SQLD: {
        label: 'CERT',
        color: '#9CA3AF',
        icon: Award,
    },
};

/**
 * 기본 카테고리 설정 (매핑되지 않은 코드용 Fallback)
 */
export const DEFAULT_CATEGORY_CONFIG: {
    label: string;
    color: string;
    icon: LucideIcon;
} = {
    label: '기타',
    color: 'var(--color-gray-500)',
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