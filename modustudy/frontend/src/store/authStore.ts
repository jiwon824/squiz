import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
    id: string;
    name: string;
    nickname?: string;
    email: string;
    avatar?: string;
    bio?: string;
    loginProvider?: 'KAKAO' | 'GOOGLE' | 'NAVER' | 'EMAIL';
    role?: 'USER' | 'ADMIN';
}

interface AuthState {
    isLoggedIn: boolean;
    user: User | null;
    isInitialized: boolean;
    login: (user: User) => void;
    logout: () => void;
    updateUser: (data: Partial<User>) => void;
    setInitialized: (val: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            isLoggedIn: false,
            user: null,
            isInitialized: false,
            login: (user) => set({ isLoggedIn: true, user }),
            logout: () => {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                set({ isLoggedIn: false, user: null });
            },
            updateUser: (data) => set((state) => ({
                user: state.user ? { ...state.user, ...data } : null
            })),
            setInitialized: (val) => set({ isInitialized: val }),
        }),
        {
            name: 'auth-storage', // localStorage 키 이름
            partialize: (state) => ({ isLoggedIn: state.isLoggedIn, user: state.user }), // 저장할 필드만 선택
        }
    )
);
