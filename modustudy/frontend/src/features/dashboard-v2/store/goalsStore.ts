// 오늘의 목표 전역 상태 관리
// 대시보드, 캘린더 등에서 양방향 동기화

import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface Goal {
    id: number;
    text: string;
    completed: boolean;
    createdAt: string;
}

interface GoalsState {
    // 상태
    goals: Goal[];
    isLoading: boolean;

    // 액션
    addGoal: (text: string) => void;
    updateGoal: (id: number, text: string) => void;
    toggleGoal: (id: number) => void;
    removeGoal: (id: number) => void;
    setGoals: (goals: Goal[]) => void;
    clearCompletedGoals: () => void;
    reorderGoals: (fromIndex: number, toIndex: number) => void;
}

// 오늘 날짜 가져오기 (YYYY-MM-DD)
const getTodayDate = () => new Date().toISOString().split('T')[0];

export const useGoalsStore = create<GoalsState>()(
    persist(
        (set) => ({
            goals: [],
            isLoading: false,

            addGoal: (text: string) => {
                const newGoal: Goal = {
                    id: Date.now(),
                    text,
                    completed: false,
                    createdAt: getTodayDate(),
                };
                set((state) => ({
                    goals: [...state.goals, newGoal],
                }));
            },

            updateGoal: (id: number, text: string) => {
                set((state) => ({
                    goals: state.goals.map((goal) =>
                        goal.id === id ? { ...goal, text } : goal
                    ),
                }));
            },

            toggleGoal: (id: number) => {
                set((state) => ({
                    goals: state.goals.map((goal) =>
                        goal.id === id ? { ...goal, completed: !goal.completed } : goal
                    ),
                }));
            },

            removeGoal: (id: number) => {
                set((state) => ({
                    goals: state.goals.filter((goal) => goal.id !== id),
                }));
            },

            setGoals: (goals: Goal[]) => {
                set({ goals });
            },

            clearCompletedGoals: () => {
                set((state) => ({
                    goals: state.goals.filter((goal) => !goal.completed),
                }));
            },

            reorderGoals: (fromIndex: number, toIndex: number) => {
                set((state) => {
                    const newGoals = [...state.goals];
                    const [removed] = newGoals.splice(fromIndex, 1);
                    newGoals.splice(toIndex, 0, removed);
                    return { goals: newGoals };
                });
            },
        }),
        {
            name: 'today-goals-storage',
            // 오늘 날짜가 아닌 목표는 로드 시 필터링
            onRehydrateStorage: () => (state) => {
                if (state) {
                    const today = getTodayDate();
                    state.goals = state.goals.filter(
                        (goal) => goal.createdAt === today
                    );
                }
            },
        }
    )
);
