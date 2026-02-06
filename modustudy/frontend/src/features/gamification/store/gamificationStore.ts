/**
 * 게이미피케이션 상태 관리 스토어
 * 레벨, 경험치, 레벨업 감지 등 관리
 */

import { create } from 'zustand';
import { gamificationApi, UserStatsResponse } from '@/api/endpoints/gamificationApi';
import { getErrorMessage } from '@/shared/utils/errorUtils';

interface LevelUpInfo {
    previousLevel: number;
    newLevel: number;
    newLevelName: string;
}

interface GamificationState {
    // 사용자 통계
    stats: UserStatsResponse | null;
    isLoading: boolean;
    error: string | null;

    // 레벨업 모달 상태
    levelUpInfo: LevelUpInfo | null;
    isLevelUpModalOpen: boolean;

    // 이전 레벨 추적 (레벨업 감지용)
    previousLevel: number | null;

    // 액션
    fetchStats: () => Promise<void>;
    closeLevelUpModal: () => void;
    checkLevelUp: (newStats: UserStatsResponse) => void;
    // 테스트용 레벨업 트리거
    triggerTestLevelUp: (newLevel?: number) => void;
}

export const useGamificationStore = create<GamificationState>((set, get) => {
    // 개발 모드에서 테스트용으로 window에 노출
    if (typeof window !== 'undefined') {
        (window as any).testLevelUp = (level = 2) => {
            const levelNames: Record<number, string> = {
                1: '새싹',
                2: '학습자',
                3: '열정가',
                4: '성실러',
                5: '마스터',
                6: '그랜드마스터',
            };

            set({
                levelUpInfo: {
                    previousLevel: level - 1,
                    newLevel: level,
                    newLevelName: levelNames[level] || '학습자',
                },
                isLevelUpModalOpen: true,
            });
        };
    }

    return {
    // 초기 상태
    stats: null,
    isLoading: false,
    error: null,
    levelUpInfo: null,
    isLevelUpModalOpen: false,
    previousLevel: null,

    // 통계 조회
    fetchStats: async () => {
        const { previousLevel } = get();

        set({ isLoading: true, error: null });
        try {
            const stats = await gamificationApi.getStats();

            // 레벨업 감지
            if (previousLevel !== null && stats.level > previousLevel) {
                set({
                    levelUpInfo: {
                        previousLevel: previousLevel,
                        newLevel: stats.level,
                        newLevelName: stats.levelName,
                    },
                    isLevelUpModalOpen: true,
                });
            }

            set({
                stats,
                isLoading: false,
                previousLevel: stats.level,
            });
        } catch (error: unknown) {
            set({
                error: getErrorMessage(error, '통계 조회 실패'),
                isLoading: false,
            });
        }
    },

    // 레벨업 모달 닫기
    closeLevelUpModal: () => {
        set({
            isLevelUpModalOpen: false,
            levelUpInfo: null,
        });
    },

    // 레벨업 체크 (외부에서 직접 호출 가능)
    checkLevelUp: (newStats: UserStatsResponse) => {
        const { previousLevel } = get();

        if (previousLevel !== null && newStats.level > previousLevel) {
            set({
                levelUpInfo: {
                    previousLevel: previousLevel,
                    newLevel: newStats.level,
                    newLevelName: newStats.levelName,
                },
                isLevelUpModalOpen: true,
            });
        }

        set({
            stats: newStats,
            previousLevel: newStats.level,
        });
    },

    // 테스트용 레벨업 트리거
    triggerTestLevelUp: (newLevel = 2) => {
        const levelNames: Record<number, string> = {
            1: '새싹',
            2: '학습자',
            3: '열정가',
            4: '성실러',
            5: '마스터',
            6: '그랜드마스터',
        };

        set({
            levelUpInfo: {
                previousLevel: newLevel - 1,
                newLevel: newLevel,
                newLevelName: levelNames[newLevel] || '학습자',
            },
            isLevelUpModalOpen: true,
        });
    },
}});
