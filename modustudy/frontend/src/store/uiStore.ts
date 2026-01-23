// 모달, 사이드바 토글 상태
import { create } from 'zustand';

interface UIState {
    isSidebarOpen: boolean;
    activeRightTab: 'friend' | 'dm' | null;
    toggleSidebar: () => void;
    toggleRightTab: (tab: 'friend' | 'dm') => void;
    setActiveRightTab: (tab: 'friend' | 'dm' | null) => void;
}

export const useUIStore = create<UIState>((set) => ({
    isSidebarOpen: false,  // 기본값을 false로 변경 (사이드바 닫힌 상태)
    activeRightTab: null,
    toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
    toggleRightTab: (tab) => set((state) => ({
        activeRightTab: state.activeRightTab === tab ? null : tab
    })),
    setActiveRightTab: (tab) => set({ activeRightTab: tab }),
}));
