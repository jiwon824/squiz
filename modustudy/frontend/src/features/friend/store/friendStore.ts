// Friend Store - 친구 관련 상태 관리 (Zustand)
import { create } from 'zustand';
import {
    getFriends,
    getReceivedRequests,
    acceptFriendRequest,
    rejectFriendRequest,
    searchUsers,
    sendFriendRequest,
    deleteFriend,
    Friend,
    FriendRequest,
    UserSearchResult
} from '@/api/endpoints/friendApi';

interface FriendState {
    // 상태
    friends: Friend[];
    receivedRequests: FriendRequest[];
    searchResults: UserSearchResult[];
    isLoading: boolean;
    error: string | null;

    // 액션
    fetchFriends: () => Promise<void>;
    fetchReceivedRequests: () => Promise<void>;
    searchUsers: (query: string) => Promise<void>;
    sendRequest: (userId: number) => Promise<void>;
    acceptRequest: (requestId: number) => Promise<void>;
    rejectRequest: (requestId: number) => Promise<void>;
    removeFriend: (friendshipId: number) => Promise<void>;
    clearSearch: () => void;
    clearError: () => void;
}

export const useFriendStore = create<FriendState>((set, get) => ({
    // 초기 상태
    friends: [],
    receivedRequests: [],
    searchResults: [],
    isLoading: false,
    error: null,

    // 친구 목록 조회
    fetchFriends: async () => {
        set({ isLoading: true, error: null });
        try {
            const friends = await getFriends();
            set({ friends, isLoading: false });
        } catch (error: any) {
            set({
                error: error.response?.data?.message || '친구 목록을 불러오지 못했습니다.',
                isLoading: false
            });
        }
    },

    // 받은 친구 요청 목록 조회
    fetchReceivedRequests: async () => {
        try {
            const receivedRequests = await getReceivedRequests();
            set({ receivedRequests });
        } catch (error: any) {
            console.error('친구 요청 목록 조회 실패:', error);
        }
    },

    // 사용자 검색
    searchUsers: async (query: string) => {
        if (!query.trim()) {
            set({ searchResults: [] });
            return;
        }
        set({ isLoading: true });
        try {
            const results = await searchUsers(query);
            set({ searchResults: results, isLoading: false });
        } catch (error: any) {
            set({ isLoading: false });
            console.error('사용자 검색 실패:', error);
        }
    },

    // 친구 요청 보내기
    sendRequest: async (userId: number) => {
        try {
            await sendFriendRequest(userId);
            // 검색 결과 업데이트 (isPending: true)
            set(state => ({
                searchResults: state.searchResults.map(user =>
                    user.id === userId ? { ...user, isPending: true } : user
                )
            }));
        } catch (error: any) {
            set({ error: error.response?.data?.message || '친구 요청에 실패했습니다.' });
        }
    },

    // 친구 요청 수락
    acceptRequest: async (requestId: number) => {
        try {
            await acceptFriendRequest(requestId);
            // 요청 목록에서 제거 후 친구 목록 새로고침
            set(state => ({
                receivedRequests: state.receivedRequests.filter(req => req.id !== requestId)
            }));
            get().fetchFriends();
        } catch (error: any) {
            set({ error: error.response?.data?.message || '요청 수락에 실패했습니다.' });
        }
    },

    // 친구 요청 거절
    rejectRequest: async (requestId: number) => {
        try {
            await rejectFriendRequest(requestId);
            set(state => ({
                receivedRequests: state.receivedRequests.filter(req => req.id !== requestId)
            }));
        } catch (error: any) {
            set({ error: error.response?.data?.message || '요청 거절에 실패했습니다.' });
        }
    },

    // 친구 삭제
    removeFriend: async (friendshipId: number) => {
        try {
            await deleteFriend(friendshipId);
            set(state => ({
                friends: state.friends.filter(friend => friend.id !== friendshipId)
            }));
        } catch (error: any) {
            set({ error: error.response?.data?.message || '친구 삭제에 실패했습니다.' });
        }
    },

    // 검색 결과 초기화
    clearSearch: () => set({ searchResults: [] }),

    // 에러 초기화
    clearError: () => set({ error: null })
}));
