/**
 * =============================================================================
 * quizCourseData.ts - 퀴즈 코스 목업 데이터
 * =============================================================================
 * * 목적 (PURPOSE):
 * 이 파일은 퀴즈 연습 모드에서 사용할 목업(가짜/샘플) 데이터를 담고 있습니다.
 * 실제 서비스에서는 이 데이터를 백엔드 API나 데이터베이스에서 가져오게 됩니다.
 * 목업 데이터는 백엔드가 준비되기 전, UI 개발과 테스트를 진행할 때 매우 유용합니다.
 * * 왜 파일을 분리하나요? (WHY IS THIS A SEPARATE FILE?):
 * 컴포넌트 코드와 데이터를 분리하는 것은 "관심사 분리(Separation of Concerns)" 원칙을 따르는 것입니다.
 * 이렇게 분리하면 다음과 같은 장점이 있습니다:
 * 1. 나중에 실제 API 호출 코드로 교체하기 쉽습니다.
 * 2. 컴포넌트 코드를 수정하지 않고도 코스 내용(데이터)만 업데이트할 수 있습니다.
 * 3. 여러 컴포넌트에서 동일한 데이터를 공유하여 사용할 수 있습니다.
 * * 데이터 구조 (DATA STRUCTURE):
 * - 각 코스(Course) 정보: ID, 제목, 설명, 카테고리, 섹션(Section) 배열
 * - 각 섹션(Section) 정보: ID, 제목, 난이도, 완료 여부, 문항 수
 * - 한국어 사용자 인터페이스를 위해 모든 텍스트는 한국어로 작성되었습니다.
 * =============================================================================
 */

// 우리가 정의한 타입 정의 파일에서 Course 타입을 가져옵니다. (Import)
// 이 과정을 통해 우리가 작성한 데이터가 예상된 구조와 일치하는지 확인합니다.
// 만약 오타를 내거나 필요한 속성을 누락하면, 타입스크립트(TypeScript)가 즉시 경고를 표시!
import type { Course } from '../types/QuizCourse.types';

/**
 * quizCourses - 퀴즈 코스 데이터 배열
 * * 사용 가능한 모든 코스 정보를 담고 있는 메인 배열입니다.
 * `: Course[]`라는 표기는 이 변수가 'Course 객체들로 이루어진 배열'임을 타입스크립트에게 알려주는 역할을 합니다.
 * * 배열 문법 설명:
 * - [...] 는 배열(목록)을 생성합니다.
 * - 그 안의 각 {...} 는 하나의 코스 객체를 나타냅니다.
 * - 각 객체는 쉼표(,)로 구분하여 나열합니다.
 */
export const quizCourses: Course[] = [
    // =========================================================================
    // OPERATING SYSTEM COURSES (운영체제 코스)
    // =========================================================================

    /**
     * Process Management Course (프로세스 관리 코스)
     * Covers process lifecycle, scheduling, synchronization, and deadlock.
     */
    {
        id: 'os-process',  // Unique identifier - used internally, not shown to users
        title: '프로세스 관리',  // Course title shown in the UI
        description: '프로세스 생명주기, 스케줄링, 프로세스 간 통신에 대해 학습합니다',
        category: 'OS',  // Must match one of the CourseCategory values
        sections: [
            // Each section represents one quiz unit within this course
            {
                id: 'os-process-1',
                title: '프로세스 생명주기',
                difficulty: 'Easy',
                isCompleted: true,  // User has completed this section
                questionCount: 10
            },
            {
                id: 'os-process-2',
                title: 'CPU 스케줄링',
                difficulty: 'Medium',
                isCompleted: true,
                questionCount: 15
            },
            {
                id: 'os-process-3',
                title: '프로세스 동기화',
                difficulty: 'Hard',
                isCompleted: false,  // User has NOT completed this section
                questionCount: 12
            },
            {
                id: 'os-process-4',
                title: '교착상태 (Deadlock)',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 10
            },
        ],
    },

    /**
     * Memory Management Course (메모리 관리 코스)
     * Covers memory hierarchy, paging, segmentation, and virtual memory.
     */
    {
        id: 'os-memory',
        title: '메모리 관리',
        description: '가상 메모리, 페이징, 메모리 할당 전략을 이해합니다',
        category: 'OS',
        sections: [
            {
                id: 'os-memory-1',
                title: '메모리 계층 구조',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 8
            },
            {
                id: 'os-memory-2',
                title: '페이징과 세그멘테이션',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 12
            },
            {
                id: 'os-memory-3',
                title: '가상 메모리',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 15
            },
        ],
    },

    // =========================================================================
    // NETWORK COURSES (네트워크 코스)
    // =========================================================================

    /**
     * TCP/IP Protocol Course (TCP/IP 프로토콜 코스)
     * Covers OSI model, TCP/UDP comparison, handshakes, and flow control.
     */
    {
        id: 'network-tcp',
        title: 'TCP/IP 프로토콜',
        description: 'TCP/IP 네트워킹 기초와 프로토콜을 깊이 있게 학습합니다',
        category: 'Network',
        sections: [
            {
                id: 'network-tcp-1',
                title: 'OSI 7계층과 TCP/IP',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 10
            },
            {
                id: 'network-tcp-2',
                title: 'TCP vs UDP 비교',
                difficulty: 'Medium',
                isCompleted: true,
                questionCount: 12
            },
            {
                id: 'network-tcp-3',
                title: 'TCP 3-way Handshake',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 8
            },
            {
                id: 'network-tcp-4',
                title: '흐름 제어와 혼잡 제어',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 10
            },
        ],
    },

    /**
     * HTTP & Web Course (HTTP와 웹 코스)
     * Covers HTTP methods, status codes, HTTPS/TLS, and REST API design.
     */
    {
        id: 'network-http',
        title: 'HTTP와 웹',
        description: 'HTTP 프로토콜, REST API, 웹 보안을 탐구합니다',
        category: 'Network',
        sections: [
            {
                id: 'network-http-1',
                title: 'HTTP 메서드',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 8
            },
            {
                id: 'network-http-2',
                title: 'HTTP 상태 코드',
                difficulty: 'Easy',
                isCompleted: false,
                questionCount: 10
            },
            {
                id: 'network-http-3',
                title: 'HTTPS와 TLS',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 12
            },
            {
                id: 'network-http-4',
                title: 'REST API 설계',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 15
            },
        ],
    },

    // =========================================================================
    // DATABASE COURSES (데이터베이스 코스)
    // =========================================================================

    /**
     * SQL Fundamentals Course (SQL 기초 코스)
     * Covers SELECT queries, JOINs, subqueries, and window functions.
     */
    {
        id: 'db-sql',
        title: 'SQL 기초',
        description: 'SQL 쿼리, 조인, 데이터베이스 연산을 마스터합니다',
        category: 'DB',
        sections: [
            {
                id: 'db-sql-1',
                title: '기본 SELECT 문',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 10
            },
            {
                id: 'db-sql-2',
                title: 'JOIN 연산',
                difficulty: 'Medium',
                isCompleted: true,
                questionCount: 15
            },
            {
                id: 'db-sql-3',
                title: '서브쿼리',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 12
            },
            {
                id: 'db-sql-4',
                title: '윈도우 함수',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 10
            },
        ],
    },

    /**
     * Database Design Course (데이터베이스 설계 코스)
     * Covers ER diagrams, normalization, indexing, and transactions.
     */
    {
        id: 'db-design',
        title: '데이터베이스 설계',
        description: '정규화, 인덱싱, 트랜잭션 관리를 학습합니다',
        category: 'DB',
        sections: [
            {
                id: 'db-design-1',
                title: 'ER 다이어그램',
                difficulty: 'Easy',
                isCompleted: false,
                questionCount: 8
            },
            {
                id: 'db-design-2',
                title: '정규화',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 12
            },
            {
                id: 'db-design-3',
                title: '인덱싱',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 10
            },
            {
                id: 'db-design-4',
                title: 'ACID와 트랜잭션',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 12
            },
        ],
    },

    // =========================================================================
    // DATA STRUCTURE COURSES (자료구조 코스)
    // =========================================================================

    /**
     * Linear Data Structures Course (선형 자료구조 코스)
     * Covers arrays, linked lists, stacks, queues, and deques.
     */
    {
        id: 'ds-linear',
        title: '선형 자료구조',
        description: '배열, 연결 리스트, 스택, 큐의 기초를 학습합니다',
        category: 'DataStructure',
        sections: [
            {
                id: 'ds-linear-1',
                title: '배열 (Array)',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 10
            },
            {
                id: 'ds-linear-2',
                title: '연결 리스트 (Linked List)',
                difficulty: 'Medium',
                isCompleted: true,
                questionCount: 12
            },
            {
                id: 'ds-linear-3',
                title: '스택과 큐',
                difficulty: 'Easy',
                isCompleted: true,
                questionCount: 10
            },
            {
                id: 'ds-linear-4',
                title: '덱과 우선순위 큐',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 8
            },
        ],
    },

    /**
     * Trees & Graphs Course (트리와 그래프 코스)
     * Covers binary trees, BST, heaps, and graph algorithms.
     */
    {
        id: 'ds-tree',
        title: '트리와 그래프',
        description: '이진 트리, BST, 힙, 그래프 알고리즘을 학습합니다',
        category: 'DataStructure',
        sections: [
            {
                id: 'ds-tree-1',
                title: '이진 트리',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 12
            },
            {
                id: 'ds-tree-2',
                title: '이진 탐색 트리 (BST)',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 15
            },
            {
                id: 'ds-tree-3',
                title: '힙 (Heap)',
                difficulty: 'Medium',
                isCompleted: false,
                questionCount: 10
            },
            {
                id: 'ds-tree-4',
                title: '그래프 탐색',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 15
            },
            {
                id: 'ds-tree-5',
                title: '최단 경로 알고리즘',
                difficulty: 'Hard',
                isCompleted: false,
                questionCount: 12
            },
        ],
    },
];
