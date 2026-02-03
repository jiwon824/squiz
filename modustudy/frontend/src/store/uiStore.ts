// UI 상태 관리 스토어
import { create } from 'zustand';

// Toast 타입 정의
interface Toast {
    id: string;
    message: string;
    type: 'success' | 'error' | 'info' | 'warning';
}

// 전역 로딩 상태 타입 정의
interface GlobalLoading {
    isLoading: boolean;
    progress: number; // 0-100
}

// 사이드바 모드: mini(아이콘+라벨) | closed(완전 닫힘)
export type SidebarMode = 'mini' | 'closed';

interface UIState {
    sidebarMode: SidebarMode;
    activeRightTab: 'friend' | 'dm' | 'meeting' | null;
    toasts: Toast[];
    globalLoading: GlobalLoading;
    toggleSidebar: () => void;
    setSidebarMode: (mode: SidebarMode) => void;
    closeSidebar: () => void;
    toggleRightTab: (tab: 'friend' | 'dm' | 'meeting') => void;
    setActiveRightTab: (tab: 'friend' | 'dm' | 'meeting' | null) => void;
    showToast: (message: string, type?: Toast['type']) => void;
    removeToast: (id: string) => void;
    // 전역 로딩 액션
    startLoading: () => void;
    setLoadingProgress: (progress: number) => void;
    finishLoading: () => void;
}

export const useUIStore = create<UIState>((set, get) => ({
    sidebarMode: 'mini', // 기본값: 미니 모드
    activeRightTab: null,
    toasts: [],
    globalLoading: {
        isLoading: false,
        progress: 0,
    },
    toggleSidebar: () => {
        const current = get().sidebarMode;
        // mini ↔ closed 토글
        set({ sidebarMode: current === 'mini' ? 'closed' : 'mini' }, false);
    },
    setSidebarMode: (mode) => set({ sidebarMode: mode }, false),
    closeSidebar: () => set({ sidebarMode: 'closed' }, false),
    toggleRightTab: (tab) =>
        set((state) => ({
            activeRightTab: state.activeRightTab === tab ? null : tab,
        }), false),
    setActiveRightTab: (tab) => set({ activeRightTab: tab }, false),
    showToast: (message, type = 'success') => {
        const id = Date.now().toString();
        set((state) => ({
            toasts: [...state.toasts, { id, message, type }],
        }));
        // 3초 후 자동 제거
        setTimeout(() => {
            set((state) => ({
                toasts: state.toasts.filter((t) => t.id !== id),
            }));
        }, 3000);
    },
    removeToast: (id) =>
        set((state) => ({
            toasts: state.toasts.filter((t) => t.id !== id),
        })),
    // 전역 로딩 시작
    startLoading: () =>
        set({
            globalLoading: { isLoading: true, progress: 0 },
        }),
    // 로딩 진행률 설정
    setLoadingProgress: (progress) =>
        set((state) => ({
            globalLoading: { ...state.globalLoading, progress: Math.min(progress, 100) },
        })),
    // 로딩 완료
    finishLoading: () =>
        set({
            globalLoading: { isLoading: false, progress: 100 },
        }),
}));
