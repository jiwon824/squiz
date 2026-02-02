/**
 * 스터디 퀴즈 API
 * AI 미팅 기반 자동 생성 퀴즈 조회
 */

import api from '../axios';

// 퀴즈 목록 아이템
export interface StudyQuizListItem {
    id: number;
    title: string;
    sourceType: 'MEETING' | 'MATERIAL' | 'MANUAL';
    sourceId: number | null;
    questionCount: number;
    status: 'ACTIVE' | 'DISABLED';
    createdAt: string;
}

// 퀴즈 문제
export interface StudyQuizQuestion {
    id: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER';
    options: string | null; // JSON string
    correctAnswer: string;
    explanation: string | null;
}

// 퀴즈 상세
export interface StudyQuizDetail {
    id: number;
    studyId: number;
    title: string;
    sourceType: 'MEETING' | 'MATERIAL' | 'MANUAL';
    sourceId: number | null;
    status: 'ACTIVE' | 'DISABLED';
    createdAt: string;
    questions: StudyQuizQuestion[];
}


export interface StudyQuizSubmitRequest {
    userAnswer: string;
    responseTimeMs: number;
}

export interface StudyQuizSubmitResponse {
    isCorrect: boolean;
    correctAnswer: string;
    explanation: string | null;
    scheduledMinutes: number;
    nextReviewAt: string;
    state: number;
    reps: number;
}

/**
 * 스터디별 퀴즈 목록 조회
 * GET /api/v1/studies/{studyId}/quizzes
 */
export const getStudyQuizzes = async (studyId: number): Promise<StudyQuizListItem[]> => {
    const response = await api.get(`/api/v1/studies/${studyId}/quizzes`);
    return response.data;
};

/**
 * 퀴즈 상세 조회
 * GET /api/v1/studies/{studyId}/quizzes/{quizId}
 */
export const getQuizDetail = async (studyId: number, quizId: number): Promise<StudyQuizDetail> => {
    const response = await api.get(`/api/v1/studies/${studyId}/quizzes/${quizId}`);
    return response.data;
};

/**
 * 퀴즈 답안 제출
 * POST /api/v1/studies/{studyId}/quizzes/{quizId}/questions/{questionId}/submit
 */
export const submitAnswer = async (
    studyId: number,
    quizId: number,
    questionId: number,
    data: StudyQuizSubmitRequest
): Promise<StudyQuizSubmitResponse> => {
    const response = await api.post(
        `/api/v1/studies/${studyId}/quizzes/${quizId}/questions/${questionId}/submit`,
        data
    );
    return response.data;
};

export const studyQuizApi = {
    getStudyQuizzes,
    getQuizDetail,
    submitAnswer,
};
