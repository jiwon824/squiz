// 워크스페이스 WebSocket 타입 정의

// 연결 상태
export type ConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'ERROR';

// 이벤트 타입
export type WorkspaceEventType = 'MESSAGE' | 'TYPING' | 'JOIN' | 'LEAVE' | 'DELETE' | 'UPDATE';

// 메시지 응답 (MessageResponse와 동일한 구조)
export interface WorkspaceMessageResponse {
  id: number;
  workspaceId: number;
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM' | 'MEETING_SUMMARY';
  fileUrl: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string | null;
}

// WebSocket 이벤트
export interface WorkspaceWebSocketEvent {
  type: WorkspaceEventType;
  workspaceId: number;
  senderId: number;
  senderNickname: string;
  senderProfileImageUrl: string | null;
  message?: WorkspaceMessageResponse;
  messageId?: number;
  timestamp: string;
}

// WebSocket 메시지 전송용
export interface WorkspaceWebSocketMessage {
  workspaceId: number;
  content: string;
  messageType?: string;
}

// 이벤트 핸들러
export interface WorkspaceWebSocketHandlers {
  onMessage?: (event: WorkspaceWebSocketEvent) => void;
  onTyping?: (event: WorkspaceWebSocketEvent) => void;
  onJoin?: (event: WorkspaceWebSocketEvent) => void;
  onLeave?: (event: WorkspaceWebSocketEvent) => void;
  onDelete?: (event: WorkspaceWebSocketEvent) => void;
  onUpdate?: (event: WorkspaceWebSocketEvent) => void;
  onConnectionChange?: (status: ConnectionStatus) => void;
  onError?: (error: string) => void;
}
