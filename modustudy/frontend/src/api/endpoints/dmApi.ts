// DM API - 다이렉트 메시지 관련 API 함수들
import api from '../axios';

// WebSocket 서비스 및 타입 re-export
export { dmWebSocket } from '../websocket';
export type {
    DmEventType,
    DmWebSocketMessage,
    DirectMessageResponse,
    DmWebSocketEvent,
    ConnectionStatus,
    DmWebSocketHandlers,
} from '../websocket';

// ===== 타입 정의 =====
export interface Conversation {
    id: number;
    participantId: number;
    participantNickname: string;
    participantProfileImage: string | null;
    lastMessage: string;
    lastMessageAt: string;
    unreadCount: number;
}

export interface Message {
    id: number;
    conversationId: number;
    senderId: number;
    senderNickname: string;
    content: string;
    createdAt: string;
    isRead: boolean;
}

export interface SendMessageRequest {
    receiverId: number;
    content: string;
}

// ===== API 함수들 =====

/**
 * 대화방 목록 조회
 */
export const getConversations = async (): Promise<Conversation[]> => {
    const response = await api.get('/api/v1/dm/conversations');
    return response.data;
};

/**
 * 특정 대화방의 메시지 목록 조회
 */
export const getMessages = async (conversationId: number, page = 0, size = 20): Promise<Message[]> => {
    const response = await api.get(`/api/v1/dm/conversations/${conversationId}/messages`, {
        params: { page, size }
    });
    return response.data;
};

/**
 * 메시지 전송 (DM 작성)
 */
export const sendMessage = async (receiverId: number, content: string): Promise<Message> => {
    const response = await api.post('/api/v1/dm', {
        receiverId,
        content
    });
    return response.data;
};

/**
 * 안읽은 메시지 총 개수 조회
 */
export const getUnreadCount = async (): Promise<number> => {
    const response = await api.get('/api/v1/dm/unread-count');
    return response.data.count || response.data;
};

/**
 * 대화방 메시지 읽음 처리
 */
export const markAsRead = async (conversationId: number): Promise<void> => {
    await api.post(`/api/v1/dm/conversations/${conversationId}/read`);
};

/**
 * 대화방 삭제
 */
export const deleteConversation = async (conversationId: number): Promise<void> => {
    await api.delete(`/api/v1/dm/conversations/${conversationId}`);
};
