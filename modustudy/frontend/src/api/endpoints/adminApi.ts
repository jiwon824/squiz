// Admin API - 관리자 대시보드 API 함수들
import api from '../axios';

// ===== 타입 정의 =====
export interface DashboardSummary {
    totalUsers: number;
    activeStudies: number;
    todaySignups: number;
    pendingReports: number;
}

export interface UserSignupStats {
    date: string;
    count: number;
}

export interface StudyStatusStats {
    status: string;
    count: number;
}

export interface LoginMethodStats {
    method: string;
    count: number;
}

export interface DailyQuizAttempt {
    date: string;
    count: number;
}

export interface CourseParticipation {
    courseName: string;
    participantCount: number;
}

export interface QuizStats {
    dailyAttempts: DailyQuizAttempt[];
    courseParticipation: CourseParticipation[];
}

export interface RecentUser {
    id: number;
    nickname: string;
    email: string;
    loginMethod: string;
    createdAt: string;
}

export interface PopularStudy {
    id: number;
    name: string;
    topicName: string;
    memberCount: number;
    status: string;
}

// ===== 새로운 시계열 통계 타입 =====
export interface DailyMeetingStats {
    date: string;
    count: number;
}

export interface DailyAttendanceStats {
    date: string;
    count: number;
}

export interface DailyActivityStats {
    date: string;
    count: number;
}

export interface UserLevelStats {
    level: number;
    levelName: string;
    count: number;
}

export interface StudyTopicStats {
    topicName: string;
    count: number;
}

// ===== API 함수들 =====

/**
 * 대시보드 요약 정보
 */
export const getDashboardSummary = async (): Promise<DashboardSummary> => {
    const response = await api.get('/api/v1/admin/dashboard/summary');
    return response.data.data;
};

/**
 * 회원 가입 추이
 */
export const getUserSignupStats = async (days: number = 30): Promise<UserSignupStats[]> => {
    const response = await api.get('/api/v1/admin/stats/users', {
        params: { days }
    });
    return response.data.data || [];
};

/**
 * 스터디 상태별 통계
 */
export const getStudyStatusStats = async (): Promise<StudyStatusStats[]> => {
    const response = await api.get('/api/v1/admin/stats/studies');
    return response.data.data || [];
};

/**
 * 로그인 방식별 통계
 */
export const getLoginMethodStats = async (): Promise<LoginMethodStats[]> => {
    const response = await api.get('/api/v1/admin/stats/login-methods');
    return response.data.data || [];
};

/**
 * 퀴즈 통계
 */
export const getQuizStats = async (days: number = 30): Promise<QuizStats> => {
    const response = await api.get('/api/v1/admin/stats/quiz', {
        params: { days }
    });
    return response.data.data;
};

/**
 * 최근 가입 회원
 */
export const getRecentUsers = async (limit: number = 10): Promise<RecentUser[]> => {
    const response = await api.get('/api/v1/admin/recent-users', {
        params: { limit }
    });
    return response.data.data || [];
};

/**
 * 인기 스터디
 */
export const getPopularStudies = async (limit: number = 5): Promise<PopularStudy[]> => {
    const response = await api.get('/api/v1/admin/popular-studies', {
        params: { limit }
    });
    return response.data.data || [];
};

// ===== 새로운 시계열 통계 API =====

/**
 * 일별 미팅 통계
 */
export const getDailyMeetingStats = async (days: number = 30): Promise<DailyMeetingStats[]> => {
    const response = await api.get('/api/v1/admin/stats/meetings', {
        params: { days }
    });
    return response.data.data || [];
};

/**
 * 일별 출석 통계
 */
export const getDailyAttendanceStats = async (days: number = 30): Promise<DailyAttendanceStats[]> => {
    const response = await api.get('/api/v1/admin/stats/attendance', {
        params: { days }
    });
    return response.data.data || [];
};

/**
 * 일별 활동 통계 (잔디)
 */
export const getDailyActivityStats = async (days: number = 30): Promise<DailyActivityStats[]> => {
    const response = await api.get('/api/v1/admin/stats/activity', {
        params: { days }
    });
    return response.data.data || [];
};

/**
 * 레벨별 사용자 분포
 */
export const getUserLevelStats = async (): Promise<UserLevelStats[]> => {
    const response = await api.get('/api/v1/admin/stats/levels');
    return response.data.data || [];
};

/**
 * 토픽별 스터디 분포
 */
export const getStudyTopicStats = async (): Promise<StudyTopicStats[]> => {
    const response = await api.get('/api/v1/admin/stats/topics');
    return response.data.data || [];
};
