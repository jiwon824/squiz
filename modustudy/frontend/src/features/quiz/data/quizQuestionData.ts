/**
 * =============================================================================
 * quizQuestionData.ts - 퀴즈 문제 목(Mock) 데이터
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 30개의 퀴즈 문제 데이터를 제공합니다.
 * - 단일선택 문제 10개
 * - 다중선택 문제 10개
 * - 단답형 문제 10개
 * 
 * 카테고리: OS, Network, DB, DataStructure
 * 
 * =============================================================================
 */

import { QuizQuestion } from '../types/QuizQuestion.types';

// -----------------------------------------------------------------------------
// 단일선택 문제 (SINGLE-CHOICE QUESTIONS)
// -----------------------------------------------------------------------------
const singleChoiceQuestions: QuizQuestion[] = [
    {
        id: 'sc-001',
        type: 'single-choice',
        question: '운영체제에서 프로세스와 스레드의 가장 큰 차이점은 무엇인가요?',
        options: [
            { id: 'a', text: '프로세스는 메모리를 공유하고, 스레드는 독립적인 메모리를 갖는다' },
            { id: 'b', text: '스레드는 프로세스 내에서 메모리를 공유하고, 프로세스는 독립적인 메모리 공간을 갖는다' },
            { id: 'c', text: '프로세스와 스레드는 동일한 개념이다' },
            { id: 'd', text: '스레드는 하드웨어 레벨에서만 동작한다' },
        ],
        correctAnswer: 'b',
        difficulty: 'Easy',
        category: 'OS',
    },
    {
        id: 'sc-002',
        type: 'single-choice',
        question: 'TCP와 UDP의 차이점으로 올바른 것은?',
        options: [
            { id: 'a', text: 'TCP는 비연결형이고, UDP는 연결형이다' },
            { id: 'b', text: 'TCP는 신뢰성 있는 전송을 보장하고, UDP는 보장하지 않는다' },
            { id: 'c', text: 'UDP가 TCP보다 느리다' },
            { id: 'd', text: 'TCP는 브로드캐스트를 지원한다' },
        ],
        correctAnswer: 'b',
        difficulty: 'Easy',
        category: 'Network',
    },
    {
        id: 'sc-003',
        type: 'single-choice',
        question: '관계형 데이터베이스에서 기본키(Primary Key)의 특징이 아닌 것은?',
        options: [
            { id: 'a', text: 'NULL 값을 허용하지 않는다' },
            { id: 'b', text: '중복된 값을 허용하지 않는다' },
            { id: 'c', text: '테이블당 여러 개를 가질 수 있다' },
            { id: 'd', text: '행을 고유하게 식별한다' },
        ],
        correctAnswer: 'c',
        difficulty: 'Easy',
        category: 'DB',
    },
    {
        id: 'sc-004',
        type: 'single-choice',
        question: '스택(Stack) 자료구조의 특징은?',
        options: [
            { id: 'a', text: 'FIFO (First In First Out)' },
            { id: 'b', text: 'LIFO (Last In First Out)' },
            { id: 'c', text: '랜덤 접근이 가능하다' },
            { id: 'd', text: '양쪽 끝에서 삽입/삭제가 가능하다' },
        ],
        correctAnswer: 'b',
        difficulty: 'Easy',
        category: 'DataStructure',
    },
    {
        id: 'sc-005',
        type: 'single-choice',
        question: '교착상태(Deadlock)의 필요조건이 아닌 것은?',
        options: [
            { id: 'a', text: '상호 배제 (Mutual Exclusion)' },
            { id: 'b', text: '점유와 대기 (Hold and Wait)' },
            { id: 'c', text: '선점 (Preemption)' },
            { id: 'd', text: '순환 대기 (Circular Wait)' },
        ],
        correctAnswer: 'c',
        difficulty: 'Medium',
        category: 'OS',
    },
    {
        id: 'sc-006',
        type: 'single-choice',
        question: 'HTTP 상태 코드 404는 무엇을 의미하나요?',
        options: [
            { id: 'a', text: '서버 내부 오류' },
            { id: 'b', text: '리소스를 찾을 수 없음' },
            { id: 'c', text: '인증 필요' },
            { id: 'd', text: '요청 성공' },
        ],
        correctAnswer: 'b',
        difficulty: 'Easy',
        category: 'Network',
    },
    {
        id: 'sc-007',
        type: 'single-choice',
        question: 'SQL에서 중복을 제거하고 데이터를 조회하는 키워드는?',
        options: [
            { id: 'a', text: 'UNIQUE' },
            { id: 'b', text: 'DISTINCT' },
            { id: 'c', text: 'DIFFERENT' },
            { id: 'd', text: 'SINGLE' },
        ],
        correctAnswer: 'b',
        difficulty: 'Easy',
        category: 'DB',
    },
    {
        id: 'sc-008',
        type: 'single-choice',
        question: '이진 탐색(Binary Search)의 시간 복잡도는?',
        options: [
            { id: 'a', text: 'O(n)' },
            { id: 'b', text: 'O(n²)' },
            { id: 'c', text: 'O(log n)' },
            { id: 'd', text: 'O(1)' },
        ],
        correctAnswer: 'c',
        difficulty: 'Easy',
        category: 'DataStructure',
    },
    {
        id: 'sc-009',
        type: 'single-choice',
        question: '가상 메모리에서 페이지 폴트(Page Fault)가 발생했을 때 일어나는 일은?',
        options: [
            { id: 'a', text: '프로세스가 즉시 종료된다' },
            { id: 'b', text: '운영체제가 필요한 페이지를 디스크에서 메모리로 로드한다' },
            { id: 'c', text: 'CPU가 재부팅된다' },
            { id: 'd', text: '모든 프로세스가 중단된다' },
        ],
        correctAnswer: 'b',
        difficulty: 'Medium',
        category: 'OS',
    },
    {
        id: 'sc-010',
        type: 'single-choice',
        question: 'OSI 7계층에서 라우터가 동작하는 계층은?',
        options: [
            { id: 'a', text: '물리 계층' },
            { id: 'b', text: '데이터 링크 계층' },
            { id: 'c', text: '네트워크 계층' },
            { id: 'd', text: '전송 계층' },
        ],
        correctAnswer: 'c',
        difficulty: 'Medium',
        category: 'Network',
    },
];

// -----------------------------------------------------------------------------
// 다중선택 문제 (MULTIPLE-CHOICE QUESTIONS)
// -----------------------------------------------------------------------------
const multipleChoiceQuestions: QuizQuestion[] = [
    {
        id: 'mc-001',
        type: 'multiple-choice',
        question: '프로세스 스케줄링 알고리즘으로 올바른 것을 모두 고르세요.',
        options: [
            { id: 'a', text: 'Round Robin (RR)' },
            { id: 'b', text: 'First Come First Served (FCFS)' },
            { id: 'c', text: 'Quick Sort (QS)' },
            { id: 'd', text: 'Shortest Job First (SJF)' },
        ],
        correctAnswer: ['a', 'b', 'd'],
        difficulty: 'Medium',
        category: 'OS',
    },
    {
        id: 'mc-002',
        type: 'multiple-choice',
        question: 'HTTP 메서드 중 CRUD 작업과 관련된 것을 모두 고르세요.',
        options: [
            { id: 'a', text: 'GET' },
            { id: 'b', text: 'POST' },
            { id: 'c', text: 'CONNECT' },
            { id: 'd', text: 'DELETE' },
        ],
        correctAnswer: ['a', 'b', 'd'],
        difficulty: 'Easy',
        category: 'Network',
    },
    {
        id: 'mc-003',
        type: 'multiple-choice',
        question: 'SQL의 DML(Data Manipulation Language)에 해당하는 것을 모두 고르세요.',
        options: [
            { id: 'a', text: 'SELECT' },
            { id: 'b', text: 'CREATE' },
            { id: 'c', text: 'INSERT' },
            { id: 'd', text: 'UPDATE' },
        ],
        correctAnswer: ['a', 'c', 'd'],
        difficulty: 'Medium',
        category: 'DB',
    },
    {
        id: 'mc-004',
        type: 'multiple-choice',
        question: '선형 자료구조에 해당하는 것을 모두 고르세요.',
        options: [
            { id: 'a', text: '배열 (Array)' },
            { id: 'b', text: '이진 트리 (Binary Tree)' },
            { id: 'c', text: '연결 리스트 (Linked List)' },
            { id: 'd', text: '큐 (Queue)' },
        ],
        correctAnswer: ['a', 'c', 'd'],
        difficulty: 'Easy',
        category: 'DataStructure',
    },
    {
        id: 'mc-005',
        type: 'multiple-choice',
        question: '운영체제의 주요 기능을 모두 고르세요.',
        options: [
            { id: 'a', text: '프로세스 관리' },
            { id: 'b', text: '메모리 관리' },
            { id: 'c', text: '웹 브라우저 렌더링' },
            { id: 'd', text: '파일 시스템 관리' },
        ],
        correctAnswer: ['a', 'b', 'd'],
        difficulty: 'Easy',
        category: 'OS',
    },
    {
        id: 'mc-006',
        type: 'multiple-choice',
        question: 'IP 주소의 특징으로 올바른 것을 모두 고르세요.',
        options: [
            { id: 'a', text: 'IPv4는 32비트로 구성된다' },
            { id: 'b', text: 'IPv6는 64비트로 구성된다' },
            { id: 'c', text: 'IP 주소는 네트워크에서 장치를 식별한다' },
            { id: 'd', text: '127.0.0.1은 루프백 주소이다' },
        ],
        correctAnswer: ['a', 'c', 'd'],
        difficulty: 'Medium',
        category: 'Network',
    },
    {
        id: 'mc-007',
        type: 'multiple-choice',
        question: '데이터베이스 정규화의 목적으로 올바른 것을 모두 고르세요.',
        options: [
            { id: 'a', text: '데이터 중복 제거' },
            { id: 'b', text: '쿼리 속도 향상' },
            { id: 'c', text: '데이터 무결성 유지' },
            { id: 'd', text: '이상 현상 방지' },
        ],
        correctAnswer: ['a', 'c', 'd'],
        difficulty: 'Medium',
        category: 'DB',
    },
    {
        id: 'mc-008',
        type: 'multiple-choice',
        question: '정렬 알고리즘 중 평균 시간 복잡도가 O(n log n)인 것을 모두 고르세요.',
        options: [
            { id: 'a', text: '버블 정렬 (Bubble Sort)' },
            { id: 'b', text: '병합 정렬 (Merge Sort)' },
            { id: 'c', text: '퀵 정렬 (Quick Sort)' },
            { id: 'd', text: '힙 정렬 (Heap Sort)' },
        ],
        correctAnswer: ['b', 'c', 'd'],
        difficulty: 'Medium',
        category: 'DataStructure',
    },
    {
        id: 'mc-009',
        type: 'multiple-choice',
        question: '프로세스 동기화 기법으로 올바른 것을 모두 고르세요.',
        options: [
            { id: 'a', text: '뮤텍스 (Mutex)' },
            { id: 'b', text: '세마포어 (Semaphore)' },
            { id: 'c', text: '페이징 (Paging)' },
            { id: 'd', text: '모니터 (Monitor)' },
        ],
        correctAnswer: ['a', 'b', 'd'],
        difficulty: 'Hard',
        category: 'OS',
    },
    {
        id: 'mc-010',
        type: 'multiple-choice',
        question: '그래프 탐색 알고리즘으로 올바른 것을 모두 고르세요.',
        options: [
            { id: 'a', text: 'DFS (깊이 우선 탐색)' },
            { id: 'b', text: 'BFS (너비 우선 탐색)' },
            { id: 'c', text: '이진 탐색 (Binary Search)' },
            { id: 'd', text: '다익스트라 (Dijkstra)' },
        ],
        correctAnswer: ['a', 'b', 'd'],
        difficulty: 'Medium',
        category: 'DataStructure',
    },
];

// -----------------------------------------------------------------------------
// 단답형 문제 (SHORT-ANSWER QUESTIONS)
// -----------------------------------------------------------------------------
const shortAnswerQuestions: QuizQuestion[] = [
    {
        id: 'sa-001',
        type: 'short-answer',
        question: 'LIFO 구조를 가진 자료구조의 이름을 영어로 작성하세요.',
        correctAnswer: 'stack',
        difficulty: 'Easy',
        category: 'DataStructure',
    },
    {
        id: 'sa-002',
        type: 'short-answer',
        question: 'TCP의 3-way handshake에서 클라이언트가 처음 보내는 패킷의 이름을 영어로 작성하세요.',
        correctAnswer: 'syn',
        difficulty: 'Medium',
        category: 'Network',
    },
    {
        id: 'sa-003',
        type: 'short-answer',
        question: 'SQL에서 조건을 지정할 때 사용하는 키워드를 작성하세요.',
        correctAnswer: 'where',
        difficulty: 'Easy',
        category: 'DB',
    },
    {
        id: 'sa-004',
        type: 'short-answer',
        question: '운영체제에서 실행 중인 프로그램을 무엇이라고 하나요? (영어로 작성)',
        correctAnswer: 'process',
        difficulty: 'Easy',
        category: 'OS',
    },
    {
        id: 'sa-005',
        type: 'short-answer',
        question: 'HTTP의 기본 포트 번호를 숫자로 작성하세요.',
        correctAnswer: '80',
        difficulty: 'Easy',
        category: 'Network',
    },
    {
        id: 'sa-006',
        type: 'short-answer',
        question: '데이터베이스에서 테이블 간의 관계를 정의할 때 사용하는 키의 이름은? (영어로 작성)',
        correctAnswer: 'foreign key',
        difficulty: 'Medium',
        category: 'DB',
    },
    {
        id: 'sa-007',
        type: 'short-answer',
        question: 'FIFO 구조를 가진 자료구조의 이름을 영어로 작성하세요.',
        correctAnswer: 'queue',
        difficulty: 'Easy',
        category: 'DataStructure',
    },
    {
        id: 'sa-008',
        type: 'short-answer',
        question: '프로세스가 CPU를 기다리는 상태를 무엇이라고 하나요? (영어로 작성)',
        correctAnswer: 'ready',
        difficulty: 'Medium',
        category: 'OS',
    },
    {
        id: 'sa-009',
        type: 'short-answer',
        question: 'DNS가 하는 역할을 한 단어로 표현하면? (도메인을 IP로 ___)',
        correctAnswer: '변환',
        difficulty: 'Easy',
        category: 'Network',
    },
    {
        id: 'sa-010',
        type: 'short-answer',
        question: '이진 트리에서 노드의 최대 자식 수는 몇 개인가요? (숫자로 작성)',
        correctAnswer: '2',
        difficulty: 'Easy',
        category: 'DataStructure',
    },
];

// -----------------------------------------------------------------------------
// 전체 문제 목록 (ALL QUESTIONS)
// -----------------------------------------------------------------------------
/**
 * quizQuestions - 전체 퀴즈 문제 배열
 * 
 * 30개의 문제 (단일선택 10개 + 다중선택 10개 + 단답형 10개)
 */
export const quizQuestions: QuizQuestion[] = [
    ...singleChoiceQuestions,
    ...multipleChoiceQuestions,
    ...shortAnswerQuestions,
];

// -----------------------------------------------------------------------------
// 유틸리티 함수 (UTILITY FUNCTIONS)
// -----------------------------------------------------------------------------
/**
 * getQuestionById - ID로 문제 조회
 * 
 * @param id - 문제 ID
 * @returns 해당 문제 객체 또는 undefined
 */
export const getQuestionById = (id: string): QuizQuestion | undefined => {
    return quizQuestions.find(q => q.id === id);
};

/**
 * getQuestionsByCategory - 카테고리별 문제 조회
 * 
 * @param category - 카테고리명
 * @returns 해당 카테고리의 문제 배열
 */
export const getQuestionsByCategory = (category: string): QuizQuestion[] => {
    return quizQuestions.filter(q => q.category === category);
};

/**
 * getQuestionsByType - 유형별 문제 조회
 * 
 * @param type - 문제 유형
 * @returns 해당 유형의 문제 배열
 */
export const getQuestionsByType = (type: QuizQuestion['type']): QuizQuestion[] => {
    return quizQuestions.filter(q => q.type === type);
};
