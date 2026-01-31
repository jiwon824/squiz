import api from '@/api/axios';

export interface OptionItem {
    optionId: number;
    text: string;
}

export interface QuestionDetail {
    questionNumber: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER'; // Adjust based on backend enum if needed
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

export const getTodayReviews = async (): Promise<TodayReviewResponse> => {
    const response = await api.get<TodayReviewResponse>('/api/v1/reviews/today');
    return response.data;
};

export const getWrongAnswers = async (): Promise<TodayReviewResponse> => {
    const response = await api.get<TodayReviewResponse>('/api/v1/reviews/wrong-answers');
    return response.data;
};
