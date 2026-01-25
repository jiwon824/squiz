// 모달, 사이드바 토글 상태
import { create } from 'zustand';

interface UIState {
    isSidebarOpen: boolean;
    activeRightTab: 'friend' | 'dm' | null;
    toggleSidebar: () => void;
    closeSidebar: () => void;
    toggleRightTab: (tab: 'friend' | 'dm') => void;
    setActiveRightTab: (tab: 'friend' | 'dm' | null) => void;
}

export const useUIStore = create<UIState>((set) => ({
    isSidebarOpen: false, // 기본값: 닫힘
    activeRightTab: null,
    toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
    closeSidebar: () => set({ isSidebarOpen: false }),
    toggleRightTab: (tab) =>
        set((state) => ({
            activeRightTab: state.activeRightTab === tab ? null : tab,
        })),
    setActiveRightTab: (tab) => set({ activeRightTab: tab }),
}));
