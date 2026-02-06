import { create } from 'zustand';
import {
    DashboardSummary,
    UserSignupStats,
    StudyStatusStats,
    LoginMethodStats,
    QuizStats,
    RecentUser,
    PopularStudy,
    DailyMeetingStats,
    DailyAttendanceStats,
    DailyActivityStats,
    UserLevelStats,
    StudyTopicStats,
    getDashboardSummary,
    getUserSignupStats,
    getStudyStatusStats,
    getLoginMethodStats,
    getQuizStats,
    getRecentUsers,
    getPopularStudies,
    getDailyMeetingStats,
    getDailyAttendanceStats,
    getDailyActivityStats,
    getUserLevelStats,
    getStudyTopicStats
} from '../../../api/endpoints/adminApi';

interface AdminState {
    // 데이터
    summary: DashboardSummary | null;
    userSignupStats: UserSignupStats[];
    studyStatusStats: StudyStatusStats[];
    loginMethodStats: LoginMethodStats[];
    quizStats: QuizStats | null;
    recentUsers: RecentUser[];
    popularStudies: PopularStudy[];
    // 새로운 시계열 통계
    dailyMeetingStats: DailyMeetingStats[];
    dailyAttendanceStats: DailyAttendanceStats[];
    dailyActivityStats: DailyActivityStats[];
    userLevelStats: UserLevelStats[];
    studyTopicStats: StudyTopicStats[];

    // 로딩 상태
    isLoading: boolean;
    error: string | null;

    // 액션
    fetchDashboardData: () => Promise<void>;
    fetchSummary: () => Promise<void>;
    fetchUserSignupStats: (days?: number) => Promise<void>;
    fetchStudyStatusStats: () => Promise<void>;
    fetchLoginMethodStats: () => Promise<void>;
    fetchQuizStats: (days?: number) => Promise<void>;
    fetchRecentUsers: (limit?: number) => Promise<void>;
    fetchPopularStudies: (limit?: number) => Promise<void>;
    // 새로운 액션
    fetchDailyMeetingStats: (days?: number) => Promise<void>;
    fetchDailyAttendanceStats: (days?: number) => Promise<void>;
    fetchDailyActivityStats: (days?: number) => Promise<void>;
    fetchUserLevelStats: () => Promise<void>;
    fetchStudyTopicStats: () => Promise<void>;
}

export const useAdminStore = create<AdminState>((set) => ({
    // 초기 상태
    summary: null,
    userSignupStats: [],
    studyStatusStats: [],
    loginMethodStats: [],
    quizStats: null,
    recentUsers: [],
    popularStudies: [],
    // 새로운 시계열 통계 초기값
    dailyMeetingStats: [],
    dailyAttendanceStats: [],
    dailyActivityStats: [],
    userLevelStats: [],
    studyTopicStats: [],
    isLoading: false,
    error: null,

    // 모든 대시보드 데이터 fetch
    fetchDashboardData: async () => {
        set({ isLoading: true, error: null });
        try {
            const [
                summary,
                userSignupStats,
                studyStatusStats,
                loginMethodStats,
                quizStats,
                recentUsers,
                popularStudies,
                dailyMeetingStats,
                dailyAttendanceStats,
                dailyActivityStats,
                userLevelStats,
                studyTopicStats
            ] = await Promise.all([
                getDashboardSummary(),
                getUserSignupStats(30),
                getStudyStatusStats(),
                getLoginMethodStats(),
                getQuizStats(30),
                getRecentUsers(10),
                getPopularStudies(5),
                getDailyMeetingStats(30),
                getDailyAttendanceStats(30),
                getDailyActivityStats(30),
                getUserLevelStats(),
                getStudyTopicStats()
            ]);

            set({
                summary,
                userSignupStats,
                studyStatusStats,
                loginMethodStats,
                quizStats,
                recentUsers,
                popularStudies,
                dailyMeetingStats,
                dailyAttendanceStats,
                dailyActivityStats,
                userLevelStats,
                studyTopicStats,
                isLoading: false
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '데이터를 불러오는데 실패했습니다.',
                isLoading: false
            });
        }
    },

    fetchSummary: async () => {
        try {
            const summary = await getDashboardSummary();
            set({ summary });
        } catch (error) {
        }
    },

    fetchUserSignupStats: async (days = 30) => {
        try {
            const userSignupStats = await getUserSignupStats(days);
            set({ userSignupStats });
        } catch (error) {
        }
    },

    fetchStudyStatusStats: async () => {
        try {
            const studyStatusStats = await getStudyStatusStats();
            set({ studyStatusStats });
        } catch (error) {
        }
    },

    fetchLoginMethodStats: async () => {
        try {
            const loginMethodStats = await getLoginMethodStats();
            set({ loginMethodStats });
        } catch (error) {
        }
    },

    fetchQuizStats: async (days = 30) => {
        try {
            const quizStats = await getQuizStats(days);
            set({ quizStats });
        } catch (error) {
        }
    },

    fetchRecentUsers: async (limit = 10) => {
        try {
            const recentUsers = await getRecentUsers(limit);
            set({ recentUsers });
        } catch (error) {
        }
    },

    fetchPopularStudies: async (limit = 5) => {
        try {
            const popularStudies = await getPopularStudies(limit);
            set({ popularStudies });
        } catch (error) {
        }
    },

    // ===== 새로운 시계열 통계 액션 =====

    fetchDailyMeetingStats: async (days = 30) => {
        try {
            const dailyMeetingStats = await getDailyMeetingStats(days);
            set({ dailyMeetingStats });
        } catch (error) {
        }
    },

    fetchDailyAttendanceStats: async (days = 30) => {
        try {
            const dailyAttendanceStats = await getDailyAttendanceStats(days);
            set({ dailyAttendanceStats });
        } catch (error) {
        }
    },

    fetchDailyActivityStats: async (days = 30) => {
        try {
            const dailyActivityStats = await getDailyActivityStats(days);
            set({ dailyActivityStats });
        } catch (error) {
        }
    },

    fetchUserLevelStats: async () => {
        try {
            const userLevelStats = await getUserLevelStats();
            set({ userLevelStats });
        } catch (error) {
        }
    },

    fetchStudyTopicStats: async () => {
        try {
            const studyTopicStats = await getStudyTopicStats();
            set({ studyTopicStats });
        } catch (error) {
        }
    }
}));
