import api from '../axios';

// --- 요청/응답 타입 ---

export type ReviewContentType = 'COURSE_QUESTION' | 'STUDY_QUESTION';

export interface ReviewSubmitRequest {
  contentType: ReviewContentType;
  contentId: number;
  isCorrect: boolean;
  responseTimeMs: number;
}

export interface ReviewSubmitResponse {
  reviewItemId: number;
  state: number;          // 0:New, 1:Learning, 2:Review, 3:Relearning
  stability: number;
  difficulty: number;
  scheduledDays: number;
  nextReviewAt: string;
}

export interface ReviewItemDto {
  reviewItemId: number;
  contentType: ReviewContentType;
  contentId: number;
  stability: number;
  difficulty: number;
  state: number;
  reps: number;
  lapses: number;
  nextReviewAt: string;
}

export interface TodayReviewResponse {
  items: ReviewItemDto[];
  totalCount: number;
}

export interface ReviewStatsResponse {
  totalItems: number;
  dueItems: number;
  newItems: number;
  learningItems: number;
  reviewItems: number;
  relearningItems: number;
  averageStability: number;
  totalReps: number;
  totalLapses: number;
  proficiency: number;
}

// 코스별 통계 타입
export interface CourseStatItem {
  courseId: number;
  courseName: string;
  totalQuestions: number;
  solvedCount: number;
}

export interface CourseStatsResponse {
  totalSolvedCount: number;
  courseStats: CourseStatItem[];
}

// --- API 함수 ---

const BASE = '/api/v1/reviews';

/** 복습 결과 제출 */
export async function submitReview(data: ReviewSubmitRequest): Promise<ReviewSubmitResponse> {
  // 도메인 모델(isCorrect)을 백엔드 JSON 형식(correct)으로 변환
  const backendPayload = {
    contentType: data.contentType,
    contentId: data.contentId,
    correct: data.isCorrect,
    responseTimeMs: data.responseTimeMs,
  };
  const res = await api.post<{ success: boolean; data: ReviewSubmitResponse }>(BASE, backendPayload);
  return res.data.data;
}

/** 오늘 복습 예정 항목 조회 */
export async function getTodayReviews(): Promise<TodayReviewResponse> {
  const res = await api.get<{ success: boolean; data: TodayReviewResponse }>(`${BASE}/today`);
  return res.data.data;
}

/** 복습 통계 조회 */
export async function getReviewStats(): Promise<ReviewStatsResponse> {
  const res = await api.get<{ success: boolean; data: ReviewStatsResponse }>(`${BASE}/stats`);
  return res.data.data;
}

/** 코스별 학습 통계 조회 (전체 맞춘 문제 수 포함) */
export async function getCourseStats(): Promise<CourseStatsResponse> {
  const res = await api.get<{ success: boolean; data: CourseStatsResponse }>(`${BASE}/courses/stats`);
  return res.data.data;
}
