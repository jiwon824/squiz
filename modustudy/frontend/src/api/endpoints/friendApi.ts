// Friend API - 친구 관련 API 함수들
import api from '../axios';

// ===== 프론트엔드 타입 정의 =====
export interface Friend {
    id: number;          // friendshipId
    friendId: number;    // 친구의 userId
    nickname: string;
    profileImage: string | null;
    status: 'ONLINE' | 'AWAY' | 'OFFLINE';
    createdAt: string;
}

export interface FriendRequest {
    id: number;          // requestId
    senderId: number;
    senderNickname: string;
    senderProfileImage: string | null;
    createdAt: string;
}

export interface UserSearchResult {
    id: number;
    nickname: string;
    profileImage: string | null;
    isFriend: boolean;
    isPending: boolean;
}

// ===== 백엔드 API 응답 타입 =====
interface FriendApiResponse {
    friendshipId: number;
    userId: number;
    nickname: string;
    profileImage: string | null;
    isOnline: boolean;
    lastSeenAt: string | null;
    friendSince: string;
}

interface FriendRequestApiResponse {
    requestId: number;
    userId: number;
    nickname: string;
    profileImage: string | null;
    isOnline: boolean;
    status: string;
    createdAt: string;
}

interface UserSearchApiResponse {
    userId: number;
    nickname: string;
    profileImage: string | null;
    isOnline: boolean;
    friendStatus: 'NONE' | 'FRIEND' | 'PENDING_SENT' | 'PENDING_RECEIVED' | 'BLOCKED';
}

interface BlockedUserApiResponse {
    id: number;
    blockedId: number;
    blockedNickname: string;
    blockedProfileImage: string | null;
    createdAt: string;
}

// ===== API 함수들 =====

/**
 * 친구 목록 조회
 */
export const getFriends = async (): Promise<Friend[]> => {
    const response = await api.get('/api/v1/friends');
    const apiResults: FriendApiResponse[] = response.data.data || [];
    return apiResults.map(friend => ({
        id: friend.friendshipId,
        friendId: friend.userId,
        nickname: friend.nickname,
        profileImage: friend.profileImage,
        status: friend.isOnline ? 'ONLINE' : 'OFFLINE',
        createdAt: friend.friendSince
    }));
};

/**
 * 사용자 검색
 */
export const searchUsers = async (query: string): Promise<UserSearchResult[]> => {
    const response = await api.get('/api/v1/friends/search', {
        params: { keyword: query }
    });
    const apiResults: UserSearchApiResponse[] = response.data.data || [];
    return apiResults.map(user => ({
        id: user.userId,
        nickname: user.nickname,
        profileImage: user.profileImage,
        isFriend: user.friendStatus === 'FRIEND',
        isPending: user.friendStatus === 'PENDING_SENT' || user.friendStatus === 'PENDING_RECEIVED'
    }));
};

/**
 * 친구 요청 보내기
 */
export const sendFriendRequest = async (targetUserId: number): Promise<void> => {
    await api.post('/api/v1/friends/request', {
        userId: targetUserId
    });
};

/**
 * 받은 친구 요청 목록 조회
 */
export const getReceivedRequests = async (): Promise<FriendRequest[]> => {
    const response = await api.get('/api/v1/friends/requests/received');
    const apiResults: FriendRequestApiResponse[] = response.data.data || [];
    return apiResults.map(req => ({
        id: req.requestId,
        senderId: req.userId,
        senderNickname: req.nickname,
        senderProfileImage: req.profileImage,
        createdAt: req.createdAt
    }));
};

/**
 * 보낸 친구 요청 목록 조회
 */
export const getSentRequests = async (): Promise<FriendRequest[]> => {
    const response = await api.get('/api/v1/friends/requests/sent');
    const apiResults: FriendRequestApiResponse[] = response.data.data || [];
    return apiResults.map(req => ({
        id: req.requestId,
        senderId: req.userId,
        senderNickname: req.nickname,
        senderProfileImage: req.profileImage,
        createdAt: req.createdAt
    }));
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
    const apiResults: BlockedUserApiResponse[] = response.data.data || [];
    return apiResults.map(blocked => ({
        id: blocked.blockedId,
        nickname: blocked.blockedNickname,
        profileImage: blocked.blockedProfileImage,
        isFriend: false,
        isPending: false
    }));
};
