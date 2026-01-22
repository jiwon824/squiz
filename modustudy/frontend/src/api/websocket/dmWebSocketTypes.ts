// DM WebSocket 타입 정의

// WebSocket 이벤트 타입
export type DmEventType = 'MESSAGE' | 'READ' | 'TYPING' | 'ONLINE' | 'OFFLINE';

// 메시지 전송용 DTO
export interface DmWebSocketMessage {
    receiverId: number;
    content: string;
}

// 서버에서 받는 메시지 응답
export interface DirectMessageResponse {
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

// WebSocket 이벤트 DTO
export interface DmWebSocketEvent {
    type: DmEventType;
    conversationId?: number;
    senderId?: number;
    senderNickname?: string;
    message?: DirectMessageResponse;
    timestamp: string;
}

// WebSocket 연결 상태
export type ConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'ERROR';

// WebSocket 이벤트 핸들러 타입
export interface DmWebSocketHandlers {
    onMessage?: (event: DmWebSocketEvent) => void;
    onTyping?: (event: DmWebSocketEvent) => void;
    onRead?: (event: DmWebSocketEvent) => void;
    onOnline?: (event: DmWebSocketEvent) => void;
    onOffline?: (event: DmWebSocketEvent) => void;
    onError?: (error: string) => void;
    onConnectionChange?: (status: ConnectionStatus) => void;
}
