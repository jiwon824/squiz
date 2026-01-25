// UI 상태 관리 스토어
import { create } from 'zustand';

// Toast 타입 정의
interface Toast {
    id: string;
    message: string;
    type: 'success' | 'error' | 'info' | 'warning';
}

interface UIState {
    isSidebarOpen: boolean;
    activeRightTab: 'friend' | 'dm' | null;
    toasts: Toast[];
    toggleSidebar: () => void;
    closeSidebar: () => void;
    toggleRightTab: (tab: 'friend' | 'dm') => void;
    setActiveRightTab: (tab: 'friend' | 'dm' | null) => void;
    showToast: (message: string, type?: Toast['type']) => void;
    removeToast: (id: string) => void;
}

export const useUIStore = create<UIState>((set) => ({
    isSidebarOpen: false, // 기본값: 닫힘
    activeRightTab: null,
    toasts: [],
    toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
    closeSidebar: () => set({ isSidebarOpen: false }),
    toggleRightTab: (tab) =>
        set((state) => ({
            activeRightTab: state.activeRightTab === tab ? null : tab,
        })),
    setActiveRightTab: (tab) => set({ activeRightTab: tab }),
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
