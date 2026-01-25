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

// ===== 프론트엔드 타입 정의 =====
export interface Conversation {
    id: number;
    participantId: number;
    participantNickname: string;
    participantProfileImage: string | null;
    participantIsOnline: boolean;
    lastMessage: string;
    lastMessageIsMine: boolean;
    lastMessageAt: string;
    unreadCount: number;
}

export interface Message {
    id: number;
    conversationId: number;
    senderId: number;
    senderNickname: string;
    senderProfileImage: string | null;
    content: string;
    isDeleted: boolean;
    isMine: boolean;
    createdAt: string;
}

export interface SendMessageRequest {
    receiverId: number;
    content: string;
}

// ===== 백엔드 API 응답 타입 =====
interface ConversationApiResponse {
    conversationId: number;
    partnerId: number;
    partnerNickname: string;
    partnerProfileImage: string | null;
    partnerIsOnline: boolean;
    lastMessage: string;
    lastMessageIsMine: boolean;
    unreadCount: number;
    lastMessageAt: string;
}

interface MessageApiResponse {
    messageId: number;
    conversationId: number;
    senderId: number;
    senderNickname: string;
    senderProfileImage: string | null;
    content: string;
    isDeleted: boolean;
    isMine: boolean;
    createdAt: string;
}

// ===== API 함수들 =====

/**
 * 대화방 목록 조회
 */
export const getConversations = async (): Promise<Conversation[]> => {
    const response = await api.get('/api/v1/dm/conversations');
    const apiResults: ConversationApiResponse[] = response.data.data || [];
    return apiResults.map(conv => ({
        id: conv.conversationId,
        participantId: conv.partnerId,
        participantNickname: conv.partnerNickname,
        participantProfileImage: conv.partnerProfileImage,
        participantIsOnline: conv.partnerIsOnline,
        lastMessage: conv.lastMessage,
        lastMessageIsMine: conv.lastMessageIsMine,
        lastMessageAt: conv.lastMessageAt,
        unreadCount: conv.unreadCount
    }));
};

/**
 * 특정 대화방의 메시지 목록 조회
 */
export const getMessages = async (conversationId: number, page = 0, size = 20): Promise<Message[]> => {
    const response = await api.get(`/api/v1/dm/conversations/${conversationId}/messages`, {
        params: { page, size }
    });
    const apiResults: MessageApiResponse[] = response.data.data || [];
    return apiResults.map(msg => ({
        id: msg.messageId,
        conversationId: msg.conversationId,
        senderId: msg.senderId,
        senderNickname: msg.senderNickname,
        senderProfileImage: msg.senderProfileImage,
        content: msg.content,
        isDeleted: msg.isDeleted,
        isMine: msg.isMine,
        createdAt: msg.createdAt
    }));
};

/**
 * 메시지 전송 (DM 작성)
 */
export const sendMessage = async (receiverId: number, content: string): Promise<Message> => {
    const response = await api.post('/api/v1/dm', {
        receiverId,
        content
    });
    const msg: MessageApiResponse = response.data.data;
    return {
        id: msg.messageId,
        conversationId: msg.conversationId,
        senderId: msg.senderId,
        senderNickname: msg.senderNickname,
        senderProfileImage: msg.senderProfileImage,
        content: msg.content,
        isDeleted: msg.isDeleted,
        isMine: msg.isMine,
        createdAt: msg.createdAt
    };
};

/**
 * 안읽은 메시지 총 개수 조회
 */
export const getUnreadCount = async (): Promise<number> => {
    const response = await api.get('/api/v1/dm/unread-count');
    return response.data.data || 0;
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
