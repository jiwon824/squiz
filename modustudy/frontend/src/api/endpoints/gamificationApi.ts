/**
 * 게이미피케이션 API
 * 사용자 통계, 활동 기록, 뱃지, 패널티 관련 API 모음
 */

import api from '../axios';

// ========== 타입 정의 ==========

// 레벨 진행도
export interface LevelProgress {
    current: number;  // 현재 경험치
    required: number; // 필요 경험치
    percentage: number; // 진행률 %
}

// 다음 레벨 정보
export interface NextLevel {
    level: number;
    name: string;
}

// 사용자 통계 응답
export interface UserStatsResponse {
    level: number;
    levelName: string;
    levelProgress: LevelProgress;
    nextLevel: NextLevel;
    totalActivityDays: number;    // 활동 일수
    currentStreak: number;        // 현재 연속 일수
    maxStreak: number;            // 최대 연속 일수
    lastActivityDate: string;     // 마지막 활동 날짜
    totalStudiesJoined: number;   // 참여한 스터디 수
    totalStudiesLed: number;      // 주최한 스터디 수
    totalAttendance: number;      // 출석 횟수
    totalChatCount: number;       // 채팅 개수
    totalQuizCount: number;       // 퀴즈 개수
    totalMaterialsUploaded: number; // 업로드한 자료 수
    totalRetrospectives: number;  // 회고 수
    joinedAt: string;             // 가입일시
}

// 활동 기록 (하루)
export interface ContributionDay {
    date: string;       // "2025-02-01"
    hasActivity: boolean;
}

// 활동 요약
export interface ContributionSummary {
    totalDays: number;     // 총 일수
    activeDays: number;    // 활동한 일수
    currentStreak: number; // 현재 연속 일수
    maxStreak: number;     // 최대 연속 일수
}

// 월별 통계
export interface MonthlyStats {
    month: number;
    activeDays: number;
}

// 활동 기록(잔디) 응답
export interface ContributionResponse {
    year: number;
    month?: number;  // 월간 조회 시에만
    contributions: ContributionDay[];
    summary: ContributionSummary;
    monthlyStats?: MonthlyStats[]; // 연간 조회 시에만
}

// 뱃지 진행도
export interface BadgeProgress {
    current: number;
    required: number;
    percentage: number;
}

// 뱃지 정보
export interface BadgeInfo {
    id: number;
    code: string;       // FIRST_STUDY 등
    name: string;
    description: string;
    icon: string;
    isEarned: boolean;
    earnedAt?: string;
    progress: BadgeProgress;
}

// 뱃지 카테고리
export interface BadgeCategory {
    category: string;
    categoryName: string;
    badges: BadgeInfo[];
}

// 뱃지 목록 응답
export interface BadgeListResponse {
    categories: BadgeCategory[];
    totalBadges: number;
    earnedCount: number;
}

// 패널티 정보
export interface PenaltyInfo {
    id: number;
    code: string;            // THREE_DAY_QUIT 등
    name: string;            // 작심삼일 등
    description: string;
    icon: string;
    grantCondition: string;  // 부여 조건
    removalCondition: string; // 해제 조건
    removalProgress: number; // 해제 진행도
    removalRequired: number; // 해제 필요도
    studyId: number;
    studyName: string;
    isActive: boolean;
    grantedAt: string;
}

// 해제된 패널티 정보
export interface RemovedPenalty {
    id: number;
    code: string;
    name: string;
    description: string;
    icon: string;
    grantedAt: string;
    removedAt: string;
    studyId: number;
    studyName: string;
}

// 패널티 목록 응답
export interface PenaltyListResponse {
    activePenalties: PenaltyInfo[];
    removedPenalties: RemovedPenalty[];
    totalActive: number;
    totalRemoved: number;
}

// ========== API 함수 ==========

export const gamificationApi = {
    /**
     * 사용자 활동 통계 조회
     * GET /api/v1/gamification/stats
     */
    getStats: async (): Promise<UserStatsResponse> => {
        const response = await api.get<any>('/api/v1/gamification/stats');
        const data = response.data;
        // 백엔드 응답 구조: { success: true, data: {...} } 또는 직접 객체
        return data.data || data;
    },

    /**
     * 활동 잔디 그래프 조회
     * GET /api/v1/gamification/contributions?year=2025&month=2
     * @param year 연도 (필수)
     * @param month 월 (선택, 미지정 시 연간 조회)
     */
    getContributions: async (year: number, month?: number): Promise<ContributionResponse> => {
        const params = new URLSearchParams();
        params.set('year', year.toString());
        if (month) {
            params.set('month', month.toString());
        }

        const response = await api.get<any>(`/api/v1/gamification/contributions?${params.toString()}`);
        const data = response.data;
        return data.data || data;
    },

    /**
     * 뱃지 목록 조회
     * GET /api/v1/gamification/badges
     */
    getBadges: async (): Promise<BadgeListResponse> => {
        const response = await api.get<any>('/api/v1/gamification/badges');
        const data = response.data;
        return data.data || data;
    },

    /**
     * 패널티 목록 조회
     * GET /api/v1/gamification/penalties
     */
    getPenalties: async (): Promise<PenaltyListResponse> => {
        const response = await api.get<any>('/api/v1/gamification/penalties');
        const data = response.data;
        return data.data || data;
    },
};

export default gamificationApi;
