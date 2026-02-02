/**
 * =============================================================================
 * courseDetailData.ts - 코스 상세 목업 데이터
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 코스 상세 페이지에서 사용할 목업(가짜/샘플) 데이터를 담고 있습니다.
 * 실제 서비스에서는 이 데이터를 백엔드 API에서 가져오게 됩니다.
 * 
 * =============================================================================
 */

import type { CourseDetail } from '../types/CourseDetailSection.types';

/**
 * courseDetailMockData - 코스별 상세 데이터 맵
 * 
 * 키(Key)는 코스 ID이며, 값(Value)은 CourseDetail 객체입니다.
 * Record 타입을 사용하여 courseId로 빠르게 조회할 수 있게 합니다.
 */
export const courseDetailMockData: Record<string, CourseDetail> = {
    // =========================================================================
    // 프로세스 관리 코스 (OS)
    // =========================================================================
    'os-process': {
        courseId: 'os-process',
        name: '프로세스 관리',
        description: '프로세스 생명주기, 스케줄링, 프로세스 간 통신에 대해 학습합니다',
        category: 'OS',
        totalSections: 5,
        sections: [
            {
                sectionNumber: 1,
                name: '프로세스 생명주기',
                isUnlocked: true,
                isPassed: true,
                bestScore: 90,

            },
            {
                sectionNumber: 2,
                name: 'CPU 스케줄링',
                isUnlocked: true,
                isPassed: true,
                bestScore: 85,

            },
            {
                sectionNumber: 3,
                name: '프로세스 동기화',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: '교착상태 (Deadlock)',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 5,
                name: '프로세스 간 통신 (IPC)',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // 메모리 관리 코스 (OS)
    // =========================================================================
    'os-memory': {
        courseId: 'os-memory',
        name: '메모리 관리',
        description: '가상 메모리, 페이징, 메모리 할당 전략을 이해합니다',
        category: 'OS',
        totalSections: 3,
        sections: [
            {
                sectionNumber: 1,
                name: '메모리 계층 구조',
                isUnlocked: true,
                isPassed: true,
                bestScore: 95,

            },
            {
                sectionNumber: 2,
                name: '페이징과 세그멘테이션',
                isUnlocked: true,
                isPassed: false,
                bestScore: 60,

            },
            {
                sectionNumber: 3,
                name: '가상 메모리',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // TCP/IP 프로토콜 코스 (Network)
    // =========================================================================
    'network-tcp': {
        courseId: 'network-tcp',
        name: 'TCP/IP 프로토콜',
        description: 'TCP/IP 네트워킹 기초와 프로토콜을 깊이 있게 학습합니다',
        category: 'Network',
        totalSections: 4,
        sections: [
            {
                sectionNumber: 1,
                name: 'OSI 7계층과 TCP/IP',
                isUnlocked: true,
                isPassed: true,
                bestScore: 100,

            },
            {
                sectionNumber: 2,
                name: 'TCP vs UDP 비교',
                isUnlocked: true,
                isPassed: true,
                bestScore: 80,

            },
            {
                sectionNumber: 3,
                name: 'TCP 3-way Handshake',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: '흐름 제어와 혼잡 제어',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // HTTP와 웹 코스 (Network)
    // =========================================================================
    'network-http': {
        courseId: 'network-http',
        name: 'HTTP와 웹',
        description: 'HTTP 프로토콜, REST API, 웹 보안을 탐구합니다',
        category: 'Network',
        totalSections: 4,
        sections: [
            {
                sectionNumber: 1,
                name: 'HTTP 메서드',
                isUnlocked: true,
                isPassed: true,
                bestScore: 88,

            },
            {
                sectionNumber: 2,
                name: 'HTTP 상태 코드',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 3,
                name: 'HTTPS와 TLS',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: 'REST API 설계',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // SQL 기초 코스 (DB)
    // =========================================================================
    'db-sql': {
        courseId: 'db-sql',
        name: 'SQL 기초',
        description: 'SQL 쿼리, 조인, 데이터베이스 연산을 마스터합니다',
        category: 'DB',
        totalSections: 4,
        sections: [
            {
                sectionNumber: 1,
                name: '기본 SELECT 문',
                isUnlocked: true,
                isPassed: true,
                bestScore: 92,

            },
            {
                sectionNumber: 2,
                name: 'JOIN 연산',
                isUnlocked: true,
                isPassed: true,
                bestScore: 78,

            },
            {
                sectionNumber: 3,
                name: '서브쿼리',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: '윈도우 함수',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // 데이터베이스 설계 코스 (DB)
    // =========================================================================
    'db-design': {
        courseId: 'db-design',
        name: '데이터베이스 설계',
        description: '정규화, 인덱싱, 트랜잭션 관리를 학습합니다',
        category: 'DB',
        totalSections: 4,
        sections: [
            {
                sectionNumber: 1,
                name: 'ER 다이어그램',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 2,
                name: '정규화',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 3,
                name: '인덱싱',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: 'ACID와 트랜잭션',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // 선형 자료구조 코스 (DataStructure)
    // =========================================================================
    'ds-linear': {
        courseId: 'ds-linear',
        name: '선형 자료구조',
        description: '배열, 연결 리스트, 스택, 큐의 기초를 학습합니다',
        category: 'DataStructure',
        totalSections: 4,
        sections: [
            {
                sectionNumber: 1,
                name: '배열 (Array)',
                isUnlocked: true,
                isPassed: true,
                bestScore: 100,

            },
            {
                sectionNumber: 2,
                name: '연결 리스트 (Linked List)',
                isUnlocked: true,
                isPassed: true,
                bestScore: 95,

            },
            {
                sectionNumber: 3,
                name: '스택과 큐',
                isUnlocked: true,
                isPassed: true,
                bestScore: 88,

            },
            {
                sectionNumber: 4,
                name: '덱과 우선순위 큐',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
        ],
    },

    // =========================================================================
    // 트리와 그래프 코스 (DataStructure)
    // =========================================================================
    'ds-tree': {
        courseId: 'ds-tree',
        name: '트리와 그래프',
        description: '이진 트리, BST, 힙, 그래프 알고리즘을 학습합니다',
        category: 'DataStructure',
        totalSections: 5,
        sections: [
            {
                sectionNumber: 1,
                name: '이진 트리',
                isUnlocked: true,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 2,
                name: '이진 탐색 트리 (BST)',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 3,
                name: '힙 (Heap)',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 4,
                name: '그래프 탐색',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
            {
                sectionNumber: 5,
                name: '최단 경로 알고리즘',
                isUnlocked: false,
                isPassed: false,
                bestScore: null,

            },
        ],
    },
};

/**
 * getCourseDetailById - 코스 ID로 상세 데이터를 조회하는 함수
 * 
 * @param courseId - 조회할 코스의 ID
 * @returns CourseDetail | undefined - 코스 데이터 또는 undefined
 */
export const getCourseDetailById = (courseId: string): CourseDetail | undefined => {
    return courseDetailMockData[courseId];
};
