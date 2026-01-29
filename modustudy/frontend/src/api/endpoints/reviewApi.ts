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

// --- API 함수 ---

const BASE = '/api/v1/reviews';

/** 복습 결과 제출 */
export async function submitReview(data: ReviewSubmitRequest): Promise<ReviewSubmitResponse> {
  console.log('[reviewApi] submitReview 요청:', data);
  const res = await api.post<{ success: boolean; data: ReviewSubmitResponse }>(BASE, data);
  console.log('[reviewApi] submitReview 응답:', res.data);
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
