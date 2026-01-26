/**
 * =============================================================================
 * QuizQuestion.types.ts - 퀴즈 문제 및 세션 타입 정의
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 퀴즈 세션에서 사용되는 문제, 답안, 세션 상태 등의 타입을 정의합니다.
 * 세 가지 문제 유형(단일선택, 다중선택, 단답형)을 지원합니다.
 * 
 * =============================================================================
 */

// -----------------------------------------------------------------------------
// 문제 유형 정의 (QUESTION TYPE DEFINITION)
// -----------------------------------------------------------------------------
/**
 * QuestionType - 문제 유형
 * 
 * 퀴즈에서 지원하는 세 가지 문제 유형입니다:
 * - 'single-choice': 단일 선택 (라디오 버튼)
 * - 'multiple-choice': 다중 선택 (체크박스)
 * - 'short-answer': 단답형 (텍스트 입력)
 */
export type QuestionType = 'single-choice' | 'multiple-choice' | 'short-answer';

// -----------------------------------------------------------------------------
// 선택지 인터페이스 (OPTION INTERFACE)
// -----------------------------------------------------------------------------
/**
 * QuestionOption - 선택지 인터페이스
 * 
 * 단일선택 및 다중선택 문제에서 사용되는 선택지 정보입니다.
 * 
 * @property {string} id - 선택지 고유 ID (예: "a", "b", "c", "d")
 * @property {string} text - 선택지 텍스트 내용
 */
export interface QuestionOption {
    id: string;
    text: string;
}

// -----------------------------------------------------------------------------
// 문제 인터페이스 (QUESTION INTERFACE)
// -----------------------------------------------------------------------------
/**
 * QuizQuestion - 퀴즈 문제 인터페이스
 * 
 * 개별 퀴즈 문제를 나타내는 인터페이스입니다.
 * 
 * @property {string} id - 문제 고유 ID
 * @property {QuestionType} type - 문제 유형 (단일선택/다중선택/단답형)
 * @property {string} question - 문제 내용 텍스트
 * @property {QuestionOption[]} options - 선택지 배열 (선택형 문제에만 사용)
 * @property {string | string[]} correctAnswer - 정답 (단답형/단일선택: string, 다중선택: string[])
 * @property {string} difficulty - 난이도 ('Easy' | 'Medium' | 'Hard')
 * @property {string} category - 문제 카테고리 (예: 'OS', 'Network', 'DB')
 */
export interface QuizQuestion {
    id: string;
    type: QuestionType;
    question: string;
    options?: QuestionOption[];
    correctAnswer: string | string[];
    difficulty: 'Easy' | 'Medium' | 'Hard';
    category: string;
}

// -----------------------------------------------------------------------------
// 답안 인터페이스 (ANSWER INTERFACE)
// -----------------------------------------------------------------------------
/**
 * QuizAnswer - 사용자 답안 인터페이스
 * 
 * 사용자가 제출한 개별 답안 정보입니다.
 * 
 * @property {string} questionId - 해당 문제 ID
 * @property {string | string[]} answer - 사용자 답안
 * @property {number} timestamp - 답안 제출 시간 (Unix timestamp)
 */
export interface QuizAnswer {
    questionId: string;
    answer: string | string[];
    timestamp: number;
}

// -----------------------------------------------------------------------------
// 세션 인터페이스 (SESSION INTERFACE)
// -----------------------------------------------------------------------------
/**
 * QuizSession - 퀴즈 세션 인터페이스
 * 
 * 퀴즈 진행 상태를 저장하는 세션 정보입니다.
 * localStorage에 저장되어 세션 복원에 사용됩니다.
 * 
 * @property {string} sessionId - 세션 고유 ID
 * @property {string} sectionId - 섹션 ID (어떤 섹션의 퀴즈인지)
 * @property {string[]} questionOrder - 셔플된 문제 ID 순서
 * @property {Record<string, QuizAnswer>} answers - 문제 ID별 답안 맵
 * @property {number} currentIndex - 현재 문제 인덱스 (0-based)
 * @property {number} startedAt - 세션 시작 시간 (Unix timestamp)
 * @property {number} lastSavedAt - 마지막 저장 시간 (Unix timestamp)
 */
export interface QuizSession {
    sessionId: string;
    sectionId: string;
    questionOrder: string[];
    answers: Record<string, QuizAnswer>;
    currentIndex: number;
    startedAt: number;
    lastSavedAt: number;
}

// -----------------------------------------------------------------------------
// 세션 저장 키 생성 함수 (SESSION KEY GENERATOR)
// -----------------------------------------------------------------------------
/**
 * getSessionStorageKey - 세션 저장 키 생성
 * 
 * localStorage에서 세션을 저장/조회할 때 사용하는 키를 생성합니다.
 * 
 * @param sectionId - 섹션 ID
 * @returns localStorage 키 문자열
 */
export const getSessionStorageKey = (sectionId: string): string => {
    return `quiz_temp_save_${sectionId}`;
};
