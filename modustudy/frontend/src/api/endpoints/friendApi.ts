// Friend API - 친구 관련 API 함수들
import api from '../axios';

// ===== 타입 정의 =====
export interface Friend {
    id: number;
    friendId: number;
    nickname: string;
    profileImage: string | null;
    status: 'ONLINE' | 'AWAY' | 'OFFLINE';
    createdAt: string;
}

export interface FriendRequest {
    id: number;
    senderId: number;
    senderNickname: string;
    senderProfileImage: string | null;
    message: string;
    createdAt: string;
}

export interface UserSearchResult {
    id: number;
    nickname: string;
    profileImage: string | null;
    isFriend: boolean;
    isPending: boolean;
}

// ===== API 함수들 =====

/**
 * 친구 목록 조회
 */
export const getFriends = async (): Promise<Friend[]> => {
    const response = await api.get('/api/v1/friends');
    return response.data;
};

/**
 * 사용자 검색
 */
export const searchUsers = async (query: string): Promise<UserSearchResult[]> => {
    const response = await api.get('/api/v1/friends/search', {
        params: { query }
    });
    return response.data;
};

/**
 * 친구 요청 보내기
 */
export const sendFriendRequest = async (userId: number, message?: string): Promise<void> => {
    await api.post('/api/v1/friends/request', {
        receiverId: userId,
        message
    });
};

/**
 * 받은 친구 요청 목록 조회
 */
export const getReceivedRequests = async (): Promise<FriendRequest[]> => {
    const response = await api.get('/api/v1/friends/requests/received');
    return response.data;
};

/**
 * 보낸 친구 요청 목록 조회
 */
export const getSentRequests = async (): Promise<FriendRequest[]> => {
    const response = await api.get('/api/v1/friends/requests/sent');
    return response.data;
};

/**
 * 친구 요청 수락
 */
export const acceptFriendRequest = async (requestId: number): Promise<void> => {
    await api.put(`/api/v1/friends/requests/${requestId}/accept`);
};

/**
 * 친구 요청 거절
 */
export const rejectFriendRequest = async (requestId: number): Promise<void> => {
    await api.put(`/api/v1/friends/requests/${requestId}/reject`);
};

/**
 * 친구 삭제
 */
export const deleteFriend = async (friendshipId: number): Promise<void> => {
    await api.delete(`/api/v1/friends/${friendshipId}`);
};

/**
 * 사용자 차단
 */
export const blockUser = async (userId: number): Promise<void> => {
    await api.post(`/api/v1/friends/block/${userId}`);
};

/**
 * 차단 해제
 */
export const unblockUser = async (userId: number): Promise<void> => {
    await api.delete(`/api/v1/friends/block/${userId}`);
};

/**
 * 차단 목록 조회
 */
export const getBlockedUsers = async (): Promise<UserSearchResult[]> => {
    const response = await api.get('/api/v1/friends/block');
    return response.data;
};
