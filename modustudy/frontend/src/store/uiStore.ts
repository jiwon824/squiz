// UI 상태 관리 스토어
import { create } from 'zustand';

// Toast 타입 정의
interface Toast {
    id: string;
    message: string;
    type: 'success' | 'error' | 'info' | 'warning';
}

// 사이드바 모드: full(전체) | mini(아이콘만) | closed(완전 닫힘)
export type SidebarMode = 'full' | 'mini' | 'closed';

interface UIState {
    sidebarMode: SidebarMode;
    activeRightTab: 'friend' | 'dm' | 'meeting' | null;
    toasts: Toast[];
    toggleSidebar: () => void;
    setSidebarMode: (mode: SidebarMode) => void;
    closeSidebar: () => void;
    toggleRightTab: (tab: 'friend' | 'dm' | 'meeting') => void;
    setActiveRightTab: (tab: 'friend' | 'dm' | 'meeting' | null) => void;
    showToast: (message: string, type?: Toast['type']) => void;
    removeToast: (id: string) => void;
}

export const useUIStore = create<UIState>((set, get) => ({
    sidebarMode: 'mini', // 기본값: 미니 모드
    activeRightTab: null,
    toasts: [],
    toggleSidebar: () => {
        const current = get().sidebarMode;
        // full → mini → full 순환
        set({ sidebarMode: current === 'full' ? 'mini' : 'full' }, false);
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
}));
