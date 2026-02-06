/**
 * =============================================================================
 * ContinuousQuiz.types.ts - 연속 학습 전용 타입 및 헬퍼 함수
 * =============================================================================
 *
 * 연속 학습 모드에서 사용되는 타입 정의와 유틸리티 함수를 제공합니다.
 *
 * =============================================================================
 */

import type { ContinuousQuizQuestion } from '@/api/endpoints/continuousQuizApi';
import type { QuizQuestion as QuizQuestionType, QuestionType } from './QuizQuestion.types';

// =============================================================================
// 상태 타입
// =============================================================================

/** 문제 관련 상태 */
export interface QuestionState {
    /** UI용 문제 데이터 */
    current: QuizQuestionType | null;
    /** API 원본 데이터 */
    currentApi: ContinuousQuizQuestion | null;
    /** 현재 답안 */
    answer: string | string[] | undefined;
    /** 다음 문제 (prefetch) */
    next: ContinuousQuizQuestion | null;
}

/** 진행 통계 상태 */
export interface ProgressState {
    /** 푼 문제 수 */
    solvedCount: number;
    /** 정답 수 */
    correctCount: number;
    /** 총 응답 시간 (밀리초) - FSRS 측정값 누적 */
    totalResponseTimeMs: number;
}

/** 피드백 데이터 */
export interface FeedbackData {
    /** 정답 여부 */
    isCorrect: boolean;
    /** 정답 텍스트 */
    correctAnswer: string;
    /** 해설 (선택) */
    explanation?: string;
}

/** 세션 요약 데이터 */
export interface SessionSummary {
    /** 총 문제 수 */
    totalQuestions: number;
    /** 정답 수 */
    correctCount: number;
    /** 오답 수 */
    incorrectCount: number;
    /** 평균 응답 시간 (밀리초) - FSRS 측정값 */
    averageResponseTimeMs: number;
}

/** 모달 상태 */
export interface ModalState {
    /** 피드백 모달 표시 여부 */
    showFeedback: boolean;
    /** 건너뛰기 확인 모달 표시 여부 */
    showSkipConfirm: boolean;
    /** 피드백 데이터 */
    feedbackData: FeedbackData | null;
}

/** 세션 상태 */
export interface SessionState {
    /** 세션 완료 여부 */
    isComplete: boolean;
    /** 세션 요약 데이터 */
    summary: SessionSummary | null;
}

/** UI 상태 */
export interface UiState {
    /** 초기 로딩 중 */
    isLoading: boolean;
    /** 제출 중 */
    isSubmitting: boolean;
    /** 에러 메시지 */
    error: string | null;
}

// =============================================================================
// 헬퍼 함수
// =============================================================================

/**
 * 답변이 유효한지(null이나 비어있지 않은지) 확인하는 헬퍼 함수
 *
 * @param answer - 검증할 답변
 * @returns 유효한 답변인지 여부 (타입 가드)
 */
export const isValidAnswer = (answer: string | string[] | null | undefined): answer is string | string[] => {
    if (answer === null || answer === undefined) return false;
    if (typeof answer === 'string') return answer.trim() !== '';
    if (Array.isArray(answer)) return answer.length > 0 && answer.some(a => a.trim() !== '');
    return false;
};

/**
 * API 문제 데이터를 UI 컴포넌트가 기대하는 형식으로 변환
 *
 * @param apiQuestion - API에서 받은 문제 데이터
 * @returns UI 컴포넌트용 문제 데이터
 */
export const mapApiQuestionToUiQuestion = (apiQuestion: ContinuousQuizQuestion): QuizQuestionType => {
    const typeMap: Record<string, QuestionType> = {
        'MULTIPLE_CHOICE': 'single-choice',
        'MULTIPLE_CHOICE_MULTIPLE': 'multiple-choice',
        'SHORT_ANSWER': 'short-answer',
    };

    return {
        id: String(apiQuestion.questionId),
        type: typeMap[apiQuestion.questionType] || 'single-choice',
        question: apiQuestion.questionText,
        options: apiQuestion.options?.map(opt => ({
            id: opt.id,
            text: opt.text,
        })),
        correctAnswer: '',
        difficulty: 'Medium',
        category: 'CS',
    };
};
