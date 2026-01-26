import { create } from 'zustand';
import {
    DashboardSummary,
    UserSignupStats,
    StudyStatusStats,
    LoginMethodStats,
    QuizStats,
    RecentUser,
    PopularStudy,
    getDashboardSummary,
    getUserSignupStats,
    getStudyStatusStats,
    getLoginMethodStats,
    getQuizStats,
    getRecentUsers,
    getPopularStudies
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
                popularStudies
            ] = await Promise.all([
                getDashboardSummary(),
                getUserSignupStats(30),
                getStudyStatusStats(),
                getLoginMethodStats(),
                getQuizStats(30),
                getRecentUsers(10),
                getPopularStudies(5)
            ]);

            set({
                summary,
                userSignupStats,
                studyStatusStats,
                loginMethodStats,
                quizStats,
                recentUsers,
                popularStudies,
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
            console.error('Failed to fetch summary:', error);
        }
    },

    fetchUserSignupStats: async (days = 30) => {
        try {
            const userSignupStats = await getUserSignupStats(days);
            set({ userSignupStats });
        } catch (error) {
            console.error('Failed to fetch user signup stats:', error);
        }
    },

    fetchStudyStatusStats: async () => {
        try {
            const studyStatusStats = await getStudyStatusStats();
            set({ studyStatusStats });
        } catch (error) {
            console.error('Failed to fetch study status stats:', error);
        }
    },

    fetchLoginMethodStats: async () => {
        try {
            const loginMethodStats = await getLoginMethodStats();
            set({ loginMethodStats });
        } catch (error) {
            console.error('Failed to fetch login method stats:', error);
        }
    },

    fetchQuizStats: async (days = 30) => {
        try {
            const quizStats = await getQuizStats(days);
            set({ quizStats });
        } catch (error) {
            console.error('Failed to fetch quiz stats:', error);
        }
    },

    fetchRecentUsers: async (limit = 10) => {
        try {
            const recentUsers = await getRecentUsers(limit);
            set({ recentUsers });
        } catch (error) {
            console.error('Failed to fetch recent users:', error);
        }
    },

    fetchPopularStudies: async (limit = 5) => {
        try {
            const popularStudies = await getPopularStudies(limit);
            set({ popularStudies });
        } catch (error) {
            console.error('Failed to fetch popular studies:', error);
        }
    }
}));
