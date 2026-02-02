import { create } from 'zustand';
import { UnifiedSchedule, ScheduleSource, Goal, Tag } from '../types';
import { calendarService } from './calendarService';
import { CreatePersonalScheduleRequest, UpdatePersonalScheduleRequest } from '@/api/endpoints/calendarApi';

/**
 * 캘린더 스토어 상태
 */
interface CalendarState {
    // 일정 데이터
    schedules: UnifiedSchedule[];
    loading: boolean;
    error: string | null;

    // 필터
    activeFilters: ScheduleSource[];

    // 선택된 날짜
    selectedDate: string | null;

    // 목표 (학습 목표 위젯용)
    goals: Goal[];

    // 태그 (학습 메모용)
    tags: Tag[];

    // 메모
    memo: string;

    // Google Calendar 연동 상태
    googleConnected: boolean;
    googleEmail: string | null;
    googleLastSyncAt: string | null;

    // Actions
    fetchSchedules: (startDate: string, endDate: string) => Promise<void>;
    createSchedule: (request: CreatePersonalScheduleRequest) => Promise<void>;
    updateSchedule: (id: number, request: UpdatePersonalScheduleRequest) => Promise<void>;
    deleteSchedule: (id: number) => Promise<void>;

    setActiveFilters: (filters: ScheduleSource[]) => void;
    setSelectedDate: (date: string | null) => void;

    addGoal: (text: string) => void;
    toggleGoal: (id: number) => void;
    removeGoal: (id: number) => void;

    addTag: (text: string) => void;
    removeTag: (id: number) => void;

    updateMemo: (memo: string) => void;

    connectGoogle: () => Promise<string>;
    disconnectGoogle: () => Promise<void>;
    syncGoogle: () => Promise<void>;
    checkGoogleStatus: () => Promise<void>;
}

/**
 * 캘린더 Zustand 스토어
 * LocalStorage 대신 API 기반으로 전환
 */
export const useCalendarStore = create<CalendarState>((set, get) => ({
    // 초기 상태
    schedules: [],
    loading: false,
    error: null,
    activeFilters: ['personal', 'study', 'google'],
    selectedDate: null,
    goals: [],
    tags: [],
    memo: '',
    googleConnected: false,
    googleEmail: null,
    googleLastSyncAt: null,

    // ==================== 일정 관리 ====================

    /**
     * 일정 목록 조회
     */
    fetchSchedules: async (startDate: string, endDate: string) => {
        set({ loading: true, error: null });
        try {
            const schedules = await calendarService.getAllSchedules(startDate, endDate);
            set({ schedules, loading: false });
        } catch (error: any) {
            set({
                error: error.message || '일정 조회에 실패했습니다.',
                loading: false
            });
            console.error('일정 조회 실패:', error);
        }
    },

    /**
     * 개인 일정 생성
     */
    createSchedule: async (request: CreatePersonalScheduleRequest) => {
        const previousSchedules = get().schedules;

        // 가상 ID 생성 (임시)
        const tempId = `temp-${Date.now()}`;
        const newSchedule: UnifiedSchedule = {
            id: tempId,
            title: request.title,
            description: request.description,
            startDate: request.startDate,
            startTime: request.startTime,
            endDate: request.endDate,
            endTime: request.endTime,
            location: request.location,
            isOnline: request.isOnline ?? false,
            source: 'personal',
            color: request.color,
            isSyncedWithGoogle: request.syncToGoogle
        };

        // UI 즉시 업데이트 (Optimistic)
        set(state => ({
            schedules: [...state.schedules, newSchedule],
        }));

        try {
            const savedSchedule = await calendarService.createPersonalSchedule(request);

            // 실제 데이터로 교체
            set(state => ({
                schedules: state.schedules.map(s => s.id === tempId ? savedSchedule : s)
            }));
        } catch (error: any) {
            // 실패 시 롤백
            set({ schedules: previousSchedules });
            console.error('일정 생성 실패 (롤백됨):', error);
            throw error;
        }
    },

    /**
     * 개인 일정 수정
     */
    updateSchedule: async (id: number | string, request: UpdatePersonalScheduleRequest) => {
        const previousSchedules = get().schedules;

        // UI 즉시 업데이트 (Optimistic)
        set(state => ({
            schedules: state.schedules.map(s =>
                s.id === id ? { ...s, ...request } : s
            ),
        }));

        try {
            const updatedSchedule = await calendarService.updatePersonalSchedule(id as number, request);

            // 실제 데이터로 확정
            set(state => ({
                schedules: state.schedules.map(s => s.id === id ? updatedSchedule : s)
            }));
        } catch (error: any) {
            // 실패 시 롤백
            set({ schedules: previousSchedules });
            console.error('일정 수정 실패 (롤백됨):', error);
            throw error;
        }
    },

    /**
     * 개인 일정 삭제
     */
    deleteSchedule: async (id: number | string) => {
        const previousSchedules = get().schedules;

        // UI 즉시 업데이트 (Optimistic)
        set(state => ({
            schedules: state.schedules.filter(s => s.id !== id),
        }));

        try {
            await calendarService.deletePersonalSchedule(id as number);
        } catch (error: any) {
            // 실패 시 롤백
            set({ schedules: previousSchedules });
            console.error('일정 삭제 실패 (롤백됨):', error);
            throw error;
        }
    },

    // ==================== 필터 및 선택 ====================

    setActiveFilters: (filters: ScheduleSource[]) => {
        set({ activeFilters: filters });
    },

    setSelectedDate: (date: string | null) => {
        set({ selectedDate: date });
    },

    // ==================== 목표 관리 ====================

    addGoal: (text: string) => {
        set(state => ({
            goals: [
                ...state.goals,
                {
                    id: Date.now(),
                    text,
                    completed: false,
                    createdAt: new Date().toISOString()
                }
            ]
        }));
        // TODO: 백엔드 API 호출 추가
    },

    toggleGoal: (id: number) => {
        set(state => ({
            goals: state.goals.map(g =>
                g.id === id ? { ...g, completed: !g.completed } : g
            )
        }));
        // TODO: 백엔드 API 호출 추가
    },

    removeGoal: (id: number) => {
        set(state => ({
            goals: state.goals.filter(g => g.id !== id)
        }));
        // TODO: 백엔드 API 호출 추가
    },

    // ==================== 태그 관리 ====================

    addTag: (text: string) => {
        set(state => {
            // 중복 체크
            if (state.tags.some(t => t.text === text)) {
                return state;
            }
            return {
                tags: [...state.tags, { id: Date.now(), text }]
            };
        });
        // TODO: 백엔드 API 호출 추가
    },

    removeTag: (id: number) => {
        set(state => ({
            tags: state.tags.filter(t => t.id !== id)
        }));
        // TODO: 백엔드 API 호출 추가
    },

    // ==================== 메모 관리 ====================

    updateMemo: (memo: string) => {
        set({ memo });
        // TODO: 백엔드 API 호출 추가 (debounce 적용)
    },

    // ==================== Google Calendar ====================

    connectGoogle: async () => {
        try {
            const authUrl = await calendarService.connectGoogleCalendar();
            return authUrl;
        } catch (error: any) {
            set({ error: error.message || 'Google Calendar 연동에 실패했습니다.' });
            console.error('Google 연동 실패:', error);
            throw error;
        }
    },

    disconnectGoogle: async () => {
        try {
            await calendarService.disconnectGoogleCalendar();
            set({
                googleConnected: false,
                googleEmail: null,
                googleLastSyncAt: null,
                schedules: get().schedules.filter(s => s.source !== 'google')
            });
        } catch (error: any) {
            set({ error: error.message || 'Google Calendar 연동 해제에 실패했습니다.' });
            console.error('Google 연동 해제 실패:', error);
            throw error;
        }
    },

    syncGoogle: async () => {
        set({ loading: true });
        try {
            // 동기화 트리거 (Watch 등록)
            await calendarService.triggerGoogleSync();
            set({
                googleLastSyncAt: new Date().toISOString(),
                loading: false
            });
        } catch (error: any) {
            set({
                error: error.message || 'Google Calendar 동기화에 실패했습니다.',
                loading: false
            });
            console.error('Google 동기화 실패:', error);
            throw error;
        }
    },

    checkGoogleStatus: async () => {
        try {
            const status = await calendarService.getGoogleCalendarStatus();
            set({
                googleConnected: status.connected,
                googleEmail: status.email || null
            });
        } catch (error) {
            console.error('Google 상태 확인 실패:', error);
        }
    }
}));

/**
 * 필터링된 일정 반환 (computed)
 */
export const useFilteredSchedules = () => {
    const schedules = useCalendarStore(state => state.schedules);
    const activeFilters = useCalendarStore(state => state.activeFilters);

    return schedules.filter(schedule => activeFilters.includes(schedule.source));
};

/**
 * 특정 날짜의 일정 반환 (computed)
 */
export const useSchedulesForDate = (date: string) => {
    const schedules = useFilteredSchedules();
    return schedules.filter(schedule => schedule.startDate === date);
};
