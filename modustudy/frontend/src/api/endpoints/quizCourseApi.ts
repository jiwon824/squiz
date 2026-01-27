/**
 * =============================================================================
 * quizCourseApi.ts - Quiz Course API Client
 * =============================================================================
 *
 * 퀴즈 코스 관련 API 호출을 담당하는 모듈입니다.
 * Base URL: /api/v1/quiz-courses
 *
 * =============================================================================
 */

import api from '../axios';

// =============================================================================
// API Response Types
// =============================================================================

/** 기본 API 응답 래퍼 */
export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
    error?: {
        code: string;
        message: string;
    };
}

/** 코스 목록 아이템 */
export interface CourseListItem {
    courseId: number;
    code: string;
    name: string;
    description: string;
    totalSections: number;
    badgeCode: string | null;
    badgeName: string | null;
}

/** 코스 목록 응답 */
export interface CourseListData {
    courses: CourseListItem[];
}

/** 뱃지 정보 */
export interface BadgeInfo {
    code: string;
    name: string;
    description: string;
    icon?: string;
}

/** 섹션 기본 정보 */
export interface SectionInfo {
    sectionNumber: number;
    name: string;
    description?: string;
    totalQuestions: number;
    passScore: number;
}

/** 코스 상세 응답 */
export interface CourseDetailData {
    courseId: number;
    code: string;
    name: string;
    description: string;
    totalSections: number;
    badge: BadgeInfo | null;
    sections: SectionInfo[];
}

/** 섹션 진행 상황 포함 정보 */
export interface SectionWithProgress extends SectionInfo {
    isUnlocked: boolean;
    isPassed: boolean;
    bestScore: number | null;
    attemptCount: number;
}

/** 사용자 진행 상황 */
export interface MyProgress {
    currentSection: number;
    completedSections: number;
    isCompleted: boolean;
}

/** 섹션 목록 응답 (진행 상황 포함) */
export interface SectionsWithProgressData {
    courseId: number;
    courseName: string;
    myProgress: MyProgress;
    sections: SectionWithProgress[];
}

/** 문제 선택지 */
export interface QuestionOption {
    id: string;
    text: string;
}

/** 시도 내 문제 */
export interface AttemptQuestion {
    questionId: number;
    questionText: string;
    questionType: 'MULTIPLE_CHOICE' | 'MULTIPLE_CHOICE_MULTIPLE' | 'SHORT_ANSWER';
    options: QuestionOption[] | null;
    savedAnswer: string | string[] | null;
}

/** 섹션 시도 시작/재개 응답 */
export interface AttemptData {
    attemptId: number;
    sectionNumber: number;
    sectionName: string;
    status: 'IN_PROGRESS' | 'SUBMITTED' | 'ABANDONED';
    totalQuestions: number;
    answeredCount: number;
    passScore: number;
    startedAt: string;
    questions: AttemptQuestion[];
}

/** 채점 결과 개별 문제 */
export interface QuestionResult {
    orderIndex: number;
    questionId: number;
    userAnswer: string[];
    correctAnswer: string[];
    isCorrect: boolean;
    explanation: string;
}

/** 제출 결과 응답 */
export interface SubmitResultData {
    attemptId: number;
    status: 'SUBMITTED';
    score: number;
    correctCount: number;
    totalQuestions: number;
    passScore: number;
    isPassed: boolean;
    isNextSectionUnlocked: boolean;
    submittedAt: string;
    passedAt: string | null;
    earnedBadge?: BadgeInfo;
    results: QuestionResult[];
}

/** 진행 중인 코스 */
export interface InProgressCourse {
    courseId: number;
    courseCode: string;
    courseName: string;
    totalSections: number;
    completedSections: number;
    progressPercent: number;
    lastActivityAt: string;
}

/** 완료된 코스 */
export interface CompletedCourse {
    courseId: number;
    courseCode: string;
    courseName: string;
    totalSections: number;
    completedAt: string;
    earnedBadge: BadgeInfo | null;
}

/** 시작 전 코스 */
export interface NotStartedCourse {
    courseId: number;
    courseCode: string;
    courseName: string;
    totalSections: number;
}

/** 내 코스 진행 현황 응답 */
export interface MyProgressData {
    inProgress: InProgressCourse[];
    completed: CompletedCourse[];
    notStarted: NotStartedCourse[];
}

/** 섹션 상세 진행 */
export interface SectionDetail {
    sectionNumber: number;
    name: string;
    isPassed: boolean;
    bestScore: number | null;
    attemptCount: number;
    passedAt: string | null;
}

/** 특정 코스 진행 상세 응답 */
export interface CourseProgressDetailData {
    courseId: number;
    courseName: string;
    totalSections: number;
    completedSections: number;
    isCompleted: boolean;
    startedAt: string;
    sectionDetails: SectionDetail[];
}

/** 답안 저장 요청 */
export interface SaveAnswerRequest {
    answer: {
        questionId: number;
        answer: string | string[];
    };
}

// =============================================================================
// API Functions
// =============================================================================

/**
 * 코스 목록 조회
 * GET /api/v1/quiz-courses
 * 인증: 불필요
 */
export const fetchCourses = async (): Promise<CourseListItem[]> => {
    const response = await api.get<ApiResponse<CourseListData>>('/api/v1/quiz-courses');
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '코스 목록을 불러오는데 실패했습니다.');
    }
    return response.data.data.courses;
};

/**
 * 코스 상세 조회
 * GET /api/v1/quiz-courses/{courseId}
 * 인증: 불필요
 */
export const fetchCourseDetail = async (courseId: number): Promise<CourseDetailData> => {
    const response = await api.get<ApiResponse<CourseDetailData>>(`/api/v1/quiz-courses/${courseId}`);
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '코스 정보를 불러오는데 실패했습니다.');
    }
    return response.data.data;
};

/**
 * 섹션 목록 조회 (진행 상황 포함)
 * GET /api/v1/quiz-courses/{courseId}/sections
 * 인증: 필요
 */
export const fetchSectionsWithProgress = async (courseId: number): Promise<SectionsWithProgressData> => {
    const response = await api.get<ApiResponse<SectionsWithProgressData>>(
        `/api/v1/quiz-courses/${courseId}/sections`
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '섹션 목록을 불러오는데 실패했습니다.');
    }
    return response.data.data;
};

/**
 * 섹션 시도 시작/재개
 * POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts
 * 인증: 필요
 */
export const startOrResumeAttempt = async (
    courseId: number,
    sectionNumber: number
): Promise<AttemptData> => {
    const response = await api.post<ApiResponse<AttemptData>>(
        `/api/v1/quiz-courses/${courseId}/sections/${sectionNumber}/attempts`
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '퀴즈를 시작하는데 실패했습니다.');
    }
    return response.data.data;
};

/**
 * 단일 답안 실시간 저장
 * PATCH /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers
 * 인증: 필요
 */
export const saveAnswer = async (
    courseId: number,
    sectionNumber: number,
    attemptId: number,
    questionId: number,
    answer: string | string[]
): Promise<void> => {
    const request: SaveAnswerRequest = {
        answer: {
            questionId,
            answer,
        },
    };
    const response = await api.patch<ApiResponse<null>>(
        `/api/v1/quiz-courses/${courseId}/sections/${sectionNumber}/attempts/${attemptId}/answers`,
        request
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '답안 저장에 실패했습니다.');
    }
};

/**
 * 섹션 제출 (채점)
 * POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit
 * 인증: 필요
 */
export const submitAttempt = async (
    courseId: number,
    sectionNumber: number,
    attemptId: number
): Promise<SubmitResultData> => {
    const response = await api.post<ApiResponse<SubmitResultData> & { message?: string }>(
        `/api/v1/quiz-courses/${courseId}/sections/${sectionNumber}/attempts/${attemptId}/submit`
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '제출에 실패했습니다.');
    }
    return response.data.data;
};

/**
 * 시도 포기
 * DELETE /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}
 * 인증: 필요
 */
export const abandonAttempt = async (
    courseId: number,
    sectionNumber: number,
    attemptId: number
): Promise<void> => {
    const response = await api.delete<ApiResponse<null>>(
        `/api/v1/quiz-courses/${courseId}/sections/${sectionNumber}/attempts/${attemptId}`
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '시도 포기에 실패했습니다.');
    }
};

/**
 * 내 코스 진행 현황
 * GET /api/v1/quiz-courses/my/progress
 * 인증: 필요
 */
export const fetchMyProgress = async (): Promise<MyProgressData> => {
    const response = await api.get<ApiResponse<MyProgressData>>('/api/v1/quiz-courses/my/progress');
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '진행 현황을 불러오는데 실패했습니다.');
    }
    return response.data.data;
};

/**
 * 특정 코스 진행 상세
 * GET /api/v1/quiz-courses/my/progress/{courseId}
 * 인증: 필요
 */
export const fetchCourseProgressDetail = async (courseId: number): Promise<CourseProgressDetailData> => {
    const response = await api.get<ApiResponse<CourseProgressDetailData>>(
        `/api/v1/quiz-courses/my/progress/${courseId}`
    );
    if (!response.data.success) {
        throw new Error(response.data.error?.message || '코스 진행 상세를 불러오는데 실패했습니다.');
    }
    return response.data.data;
};
