import api from '@/api/axios';

export interface OptionItem {
    optionId: number;
    text: string;
}

export interface QuestionDetail {
    questionNumber: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER' | 'MULTIPLE_CHOICE_MULTIPLE'; // Matches backend enum
    options: OptionItem[];
    correctAnswer: string;
    explanation: string;
    category: string;
    lastReviewAt: string; // ISO date string
}

export interface ReviewItemDto {
    reviewItemId: number;
    contentType: 'COURSE_QUESTION' | 'STUDY_QUESTION';
    contentId: number;
    stability: number;
    difficulty: number;
    state: number;
    reps: number;
    lapses: number;
    nextReviewAt: string;
    question: QuestionDetail;
}

export interface TodayReviewResponse {
    items: ReviewItemDto[];
    totalCount: number;
}

export interface ApiResponse<T> {
    status: number;
    message: string;
    data: T;
}

export const getTodayReviews = async (): Promise<TodayReviewResponse> => {
    const response = await api.get<ApiResponse<TodayReviewResponse>>('/api/v1/reviews/today');
    return response.data.data;
};

export const getWrongAnswers = async (): Promise<TodayReviewResponse> => {
    const response = await api.get<ApiResponse<TodayReviewResponse>>('/api/v1/reviews/wrong-answers');
    return response.data.data;
};

export interface ReviewSubmitRequest {
    contentType: 'COURSE_QUESTION' | 'STUDY_QUESTION';
    contentId: number;
    userAnswer: string | null;
    responseTimeMs: number;
}

export interface ReviewSubmitResponse {
    reviewItemId: number;
    state: number;
    stability: number;
    difficulty: number;
    scheduledDays: number;
    nextReviewAt: string;
}

export const submitReview = async (data: ReviewSubmitRequest): Promise<ReviewSubmitResponse> => {
    const response = await api.post<ApiResponse<ReviewSubmitResponse>>('/api/v1/reviews', data);
    return response.data.data;
};
