import api from '@/api/axios';

export interface OptionItem {
    id: string;
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
    totalPages: number;
    number: number;
    size: number;
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

export type WrongAnswerSortType = 'MOST_WRONG' | 'FSRS_RECOMMENDED' | 'LATEST';

export const getWrongAnswers = async (
    sortType?: WrongAnswerSortType,
    page: number = 0,
    size: number = 5
): Promise<TodayReviewResponse> => {
    const params = {
        sortType,
        page,
        size
    };
    const response = await api.get<ApiResponse<TodayReviewResponse>>('/api/v1/reviews/wrong-answers', { params });
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
    isCorrect: boolean;
    correctAnswer: string;
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

export interface ReviewCourseWeaknessResponse {
    courseWeaknessStats: {
        courseId: number;
        courseName: string;
        totalReps: number;
        totalLapses: number;
    }[];
}

export const getCourseWeaknessStats = async (): Promise<ReviewCourseWeaknessResponse> => {
    const response = await api.get<ApiResponse<ReviewCourseWeaknessResponse>>('/api/v1/reviews/courses/weakness');
    return response.data.data;
};

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

export const getReviewStats = async (): Promise<ReviewStatsResponse> => {
    const response = await api.get<ApiResponse<ReviewStatsResponse>>('/api/v1/reviews/stats');
    return response.data.data;
};
