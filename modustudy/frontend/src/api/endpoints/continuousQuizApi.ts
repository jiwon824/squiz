/**
 * =============================================================================
 * continuousQuizApi.ts - Continuous Quiz API Client
 * =============================================================================
 *
 * 연속 학습 모드 (Sayvoca 스타일) 퀴즈 API 호출을 담당하는 모듈입니다.
 * Base URL: /api/v1/continuous-quiz
 *
 * 핵심 특징:
 * - Atomic Interaction: 매 제출마다 다음 문제를 직접 반환
 * - Forward-only: 뒤로가기 없는 전진 전용 플로우
 * - FSRS Timing: 문제 렌더링부터 제출까지의 응답 시간 측정
 * - 무한 루프: 섹션 완료 개념 없음, 문제가 있으면 항상 반환
 *
 * =============================================================================
 */

import api from '../axios';
import { parseOptions, type QuestionOption } from '@/shared/utils/quizUtils';

// =============================================================================
// API Response Types (실제 백엔드 응답 구조와 매칭)
// =============================================================================

/** 기본 API 응답 래퍼 */
interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
    error?: {
        code: string;
        message: string;
    };
}

// -----------------------------------------------------------------------------
// 백엔드 DTO 타입 (실제 API 응답 구조)
// -----------------------------------------------------------------------------

/** 백엔드 GET /next 응답 (ContinuousQuestionResponse.java) */
interface BackendQuestionResponse {
    questionId: number;
    questionNumber: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'MULTIPLE_CHOICE_MULTIPLE' | 'SHORT_ANSWER';
    options: string | null; // JSON 문자열로 전달됨
    courseId: number;
    sectionNumber: number;
}

/** 백엔드 POST /submit 응답 (ContinuousSubmitResponse.java) */
interface BackendSubmitResponse {
    submittedQuestionId: number;
    correct: boolean; // Java의 boolean isCorrect → @JsonProperty("correct")로 직렬화
    userAnswer: string;
    correctAnswer: string;
    explanation: string | null;
    // FSRS 갱신 정보
    stability: number;
    difficulty: number;
    scheduledDays: number;
    nextReviewAt: string;
    state: number;
    reps: number;
    lapses: number;
    // 다음 문제
    nextQuestion: {
        questionId: number;
        questionNumber: number;
        questionText: string;
        questionType: string;
        options: string | null; // JSON 문자열
        courseId: number;
        sectionNumber: number;
    } | null;
}

// -----------------------------------------------------------------------------
// 프론트엔드 사용 타입 (변환된 구조)
// -----------------------------------------------------------------------------

/** 연속 퀴즈 문제 (프론트엔드용 변환된 구조) */
export interface ContinuousQuizQuestion {
    questionId: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'MULTIPLE_CHOICE_MULTIPLE' | 'SHORT_ANSWER';
    options: QuestionOption[];
}

/** 다음 문제 응답 (GET /next) - 프론트엔드 인터페이스 */
export interface NextQuestionResponse {
    question: ContinuousQuizQuestion | null;
    /** 세션 완료 여부 - 무한 루프 모드이므로 항상 false */
    isSessionComplete: boolean;
}

/** 답안 제출 요청 */
export interface SubmitAnswerRequest {
    userAnswer: string;
    responseTimeMs: number;
}

/** 답안 제출 응답 - 프론트엔드 인터페이스 */
export interface SubmitAnswerResponse {
    isCorrect: boolean;
    correctAnswer: string;
    nextQuestion: ContinuousQuizQuestion | null;
    /** 세션 완료 여부 - 무한 루프 모드이므로 항상 false */
    isSessionComplete: boolean;
    explanation?: string;
}

/** 코스별 퀴즈 통계 (Attempted vs Correct) */
export interface CourseQuizStat {
    courseName: string;
    courseCode: string;
    attemptedCount: number;
    correctCount: number;
}

/**
 * 백엔드 문제 응답을 프론트엔드 형식으로 변환
 */
const transformQuestion = (
    backendQuestion: BackendQuestionResponse | BackendSubmitResponse['nextQuestion']
): ContinuousQuizQuestion | null => {
    if (!backendQuestion) return null;

    // 주관식(SHORT_ANSWER) 임시 제외 - null 반환으로 스킵 처리
    if (backendQuestion.questionType === 'SHORT_ANSWER') return null;

    return {
        questionId: backendQuestion.questionId,
        questionText: backendQuestion.questionText,
        questionType: backendQuestion.questionType as ContinuousQuizQuestion['questionType'],
        options: parseOptions(backendQuestion.options),
    };
};

// =============================================================================
// API Functions
// =============================================================================

/**
 * 다음 문제 가져오기
 * GET /api/v1/continuous-quiz/courses/{courseId}/sections/{sectionNumber}/next
 * 인증: 필요
 *
 * 연속 학습 모드의 진입점입니다.
 * 무한 루프 모드이므로 문제가 있으면 항상 반환됩니다.
 */
export const fetchNextQuestion = async (
    courseId: number,
    sectionNumber: number
): Promise<NextQuestionResponse> => {

    const response = await api.get<ApiResponse<BackendQuestionResponse>>(
        `/api/v1/continuous-quiz/courses/${courseId}/sections/${sectionNumber}/next`
    );

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '문제를 불러오는데 실패했습니다.');
    }

    const backendData = response.data.data;

    // 백엔드 응답을 프론트엔드 형식으로 변환
    const question = transformQuestion(backendData);

    return {
        question,
        isSessionComplete: false, // 무한 루프 모드 - 항상 false
    };
};

/**
 * 답안 제출 및 다음 문제 받기
 * POST /api/v1/continuous-quiz/questions/{questionId}/submit
 * 인증: 필요
 *
 * Atomic API: 답안 제출과 동시에 채점 결과 + 다음 문제를 한 번에 반환합니다.
 * 무한 루프 모드이므로 문제가 있으면 항상 다음 문제가 반환됩니다.
 *
 * @param questionId - 현재 문제 ID
 * @param userAnswer - 사용자 답안 (복수 선택 시 쉼표로 구분)
 * @param responseTimeMs - 문제 렌더링부터 제출까지의 응답 시간 (ms)
 */
export const submitAnswer = async (
    questionId: number,
    userAnswer: string | string[],
    responseTimeMs: number
): Promise<SubmitAnswerResponse> => {
    // 복수 선택 답안은 쉼표로 구분된 문자열로 직렬화
    const serializedAnswer = Array.isArray(userAnswer) ? userAnswer.join(',') : userAnswer;

    const request: SubmitAnswerRequest = {
        userAnswer: serializedAnswer,
        responseTimeMs,
    };

    const response = await api.post<ApiResponse<BackendSubmitResponse>>(
        `/api/v1/continuous-quiz/questions/${questionId}/submit`,
        request
    );

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '답안 제출에 실패했습니다.');
    }

    const backendData = response.data.data;

    // 백엔드 응답을 프론트엔드 형식으로 변환
    const result: SubmitAnswerResponse = {
        isCorrect: backendData.correct,
        correctAnswer: backendData.correctAnswer,
        nextQuestion: transformQuestion(backendData.nextQuestion),
        isSessionComplete: false, // 무한 루프 모드 - 항상 false
        explanation: backendData.explanation || undefined,
    };

    return result;
};

/**
 * 코스별 퀴즈 통계 조회
 * GET /api/v1/continuous-quiz/course-stats
 * 인증: 필요
 */
export const getCourseQuizStats = async (): Promise<CourseQuizStat[]> => {
    const response = await api.get<ApiResponse<CourseQuizStat[]>>(
        '/api/v1/continuous-quiz/course-stats'
    );
    // TODO: Error handling if needed
    return response.data.data;
};
