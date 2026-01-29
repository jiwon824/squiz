/**
 * AI API
 * 요약/퀴즈/검증/템플릿추천 엔드포인트
 */

import api from '../axios';

// ========== 요청/응답 타입 ==========

// 요약 요청
export interface AiSummarizeRequest {
  transcript: string;
  maxTokens?: number;
}

// 요약 응답
export interface AiSummarizeResponse {
  summary: string;
  tokensUsed: number;
}

// 퀴즈 요청
export interface AiQuizRequest {
  summary: string;
  numQuestions?: number;
  userWeakPoints?: string[];
  userRecentErrors?: Array<{
    topic: string;
    errorReason: string;
  }>;
}

// 퀴즈 아이템
export interface QuizItem {
  type: 'multiple_choice' | 'ox' | 'short_answer';
  question: string;
  options?: string[];
  answer: string;
  explanation: string;
}

// 퀴즈 응답
export interface AiQuizResponse {
  questions: QuizItem[];
  source: string;
  rawResponse?: string;
}

// 검증 요청
export interface AiVerifyRequest {
  summary: string;
  originalTopic?: string;
}

// 검증 오류 아이템
export interface VerifyErrorItem {
  claim: string;
  correction: string;
  severity: 'high' | 'medium' | 'low';
}

// 검증 응답
export interface AiVerifyResponse {
  hasErrors: boolean;
  errors: VerifyErrorItem[];
  confidence: string;
  source: string;
}

// 템플릿 추천 요청
export interface AiTemplateRecommendRequest {
  userTech?: string[];
  userSchedule?: Record<string, { start: string; end: string }>;
  studyType?: string;
  difficultyPreference?: string;
}

// 템플릿 추천 응답
export interface AiTemplateRecommendResponse {
  templateType: string;
  topic: string;
  format: string;
  difficulty: string;
  goal: string;
  textbook: string;
  scheduleSuggestion?: {
    days: string[];
    time: string;
  };
  reason: string;
  tokensUsed: number;
}

// 헬스체크 응답
export interface AiHealthResponse {
  status: string;
  summarizeLoaded: boolean;
  recommendLoaded: boolean;
  claudeConfigured: boolean;
  claudeModel: string;
  whisperLoaded: boolean;
  whisperModel: string;
}

// ========== API 함수 ==========

export const aiApi = {
  /**
   * AI 서버 헬스체크
   * GET /api/v1/ai/health
   */
  healthCheck: async (): Promise<AiHealthResponse> => {
    const response = await api.get('/api/v1/ai/health');
    return response.data.data;
  },

  /**
   * 회의록 요약
   * POST /api/v1/ai/summarize
   */
  summarize: async (request: AiSummarizeRequest): Promise<AiSummarizeResponse> => {
    const response = await api.post('/api/v1/ai/summarize', request);
    return response.data.data;
  },

  /**
   * 복습 퀴즈 생성
   * POST /api/v1/ai/quiz
   */
  generateQuiz: async (request: AiQuizRequest): Promise<AiQuizResponse> => {
    const response = await api.post('/api/v1/ai/quiz', request);
    return response.data.data;
  },

  /**
   * 콘텐츠 사실 검증
   * POST /api/v1/ai/verify
   */
  verifyContent: async (request: AiVerifyRequest): Promise<AiVerifyResponse> => {
    const response = await api.post('/api/v1/ai/verify', request);
    return response.data.data;
  },

  /**
   * 스터디 템플릿 추천
   * POST /api/v1/study-templates/recommend
   */
  recommendTemplate: async (request: AiTemplateRecommendRequest): Promise<AiTemplateRecommendResponse> => {
    const response = await api.post('/api/v1/study-templates/recommend', {
      studyType: request.studyType,
      difficultyPreference: request.difficultyPreference,
    });
    return response.data;
  },
};

export default aiApi;
