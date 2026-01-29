// 워크스페이스 WebSocket 클라이언트 서비스
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  WorkspaceWebSocketMessage,
  WorkspaceWebSocketEvent,
  WorkspaceWebSocketHandlers,
  ConnectionStatus,
} from './workspaceWebSocketTypes';

// WebSocket URL 결정
const getWsUrl = (): string => {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }
  if (import.meta.env.VITE_API_URL) {
    return `${import.meta.env.VITE_API_URL}/ws`;
  }
  return `${window.location.origin}/ws`;
};

const WS_URL = getWsUrl();

class WorkspaceWebSocketService {
  private client: Client | null = null;
  private userId: number | null = null;
  private nickname: string = '';
  private workspaceId: number | null = null;
  private handlers: WorkspaceWebSocketHandlers = {};
  private connectionStatus: ConnectionStatus = 'DISCONNECTED';

  /**
   * 워크스페이스에 WebSocket 연결
   */
  connect(
    workspaceId: number,
    userId: number,
    nickname: string,
    handlers: WorkspaceWebSocketHandlers = {}
  ): void {
    if (this.client?.connected && this.workspaceId === workspaceId) {
      console.warn('[Workspace WS] 이미 연결됨');
      return;
    }

    // 기존 연결이 있으면 종료
    if (this.client?.connected) {
      this.disconnect();
    }

    this.userId = userId;
    this.nickname = nickname;
    this.workspaceId = workspaceId;
    this.handlers = handlers;
    this.setConnectionStatus('CONNECTING');

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        userId: userId.toString(),
        nickname: nickname,
      },
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[Workspace WS]', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => this.onConnected(),
      onDisconnect: () => this.onDisconnected(),
      onStompError: (frame) => this.onError(frame.headers['message'] || 'Unknown error'),
    });

    this.client.activate();
  }

  /**
   * 연결 성공 시
   */
  private onConnected(): void {
    console.log('[Workspace WS] 연결됨');
    this.setConnectionStatus('CONNECTED');

    // 워크스페이스 입장 메시지 전송
    this.client?.publish({
      destination: `/app/workspace/join/${this.workspaceId}`,
      headers: {
        userId: this.userId!.toString(),
        nickname: this.nickname,
      },
    });

    // 워크스페이스 채널 구독
    this.subscribe();
  }

  /**
   * 워크스페이스 채널 구독
   */
  private subscribe(): void {
    if (!this.client || !this.workspaceId) return;

    // 워크스페이스 메시지 수신 (브로드캐스트)
    this.client.subscribe(`/topic/workspace/${this.workspaceId}`, (message: IMessage) => {
      this.handleEvent(message);
    });

    console.log(`[Workspace WS] 구독 시작: /topic/workspace/${this.workspaceId}`);
  }

  /**
   * 이벤트 처리
   */
  private handleEvent(message: IMessage): void {
    try {
      const event: WorkspaceWebSocketEvent = JSON.parse(message.body);

      // 본인이 보낸 메시지는 이미 로컬에서 추가했으므로 무시 (선택적)
      // if (event.senderId === this.userId && event.type === 'MESSAGE') return;

      switch (event.type) {
        case 'MESSAGE':
          this.handlers.onMessage?.(event);
          break;
        case 'TYPING':
          // 본인 타이핑은 무시
          if (event.senderId !== this.userId) {
            this.handlers.onTyping?.(event);
          }
          break;
        case 'JOIN':
          this.handlers.onJoin?.(event);
          break;
        case 'LEAVE':
          this.handlers.onLeave?.(event);
          break;
        case 'DELETE':
          this.handlers.onDelete?.(event);
          break;
        case 'UPDATE':
          this.handlers.onUpdate?.(event);
          break;
      }
    } catch (e) {
      console.error('[Workspace WS] 이벤트 파싱 실패:', e);
    }
  }

  /**
   * 연결 해제 시
   */
  private onDisconnected(): void {
    console.log('[Workspace WS] 연결 해제됨');
    this.setConnectionStatus('DISCONNECTED');
  }

  /**
   * 에러 발생 시
   */
  private onError(errorMessage: string): void {
    console.error('[Workspace WS] 에러:', errorMessage);
    this.setConnectionStatus('ERROR');
    this.handlers.onError?.(errorMessage);
  }

  /**
   * 연결 상태 변경
   */
  private setConnectionStatus(status: ConnectionStatus): void {
    this.connectionStatus = status;
    this.handlers.onConnectionChange?.(status);
  }

  /**
   * 메시지 전송
   */
  sendMessage(content: string, messageType: string = 'TEXT'): void {
    if (!this.client?.connected || !this.workspaceId) {
      console.error('[Workspace WS] 연결되지 않음');
      return;
    }

    const message: WorkspaceWebSocketMessage = {
      workspaceId: this.workspaceId,
      content,
      messageType,
    };

    this.client.publish({
      destination: `/app/workspace/send/${this.workspaceId}`,
      headers: {
        userId: this.userId!.toString(),
      },
      body: JSON.stringify(message),
    });
  }

  /**
   * 입력 중 표시 전송
   */
  sendTyping(): void {
    if (!this.client?.connected || !this.workspaceId) return;

    this.client.publish({
      destination: `/app/workspace/typing/${this.workspaceId}`,
      headers: {
        userId: this.userId!.toString(),
        nickname: this.nickname,
      },
    });
  }

  /**
   * 연결 해제
   */
  disconnect(): void {
    if (this.client?.connected && this.workspaceId) {
      // 퇴장 메시지 전송
      this.client.publish({
        destination: `/app/workspace/leave/${this.workspaceId}`,
        headers: {
          userId: this.userId!.toString(),
          nickname: this.nickname,
        },
      });
    }

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.userId = null;
    this.nickname = '';
    this.workspaceId = null;
    this.handlers = {};
    this.setConnectionStatus('DISCONNECTED');
  }

  /**
   * 연결 상태 확인
   */
  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  /**
   * 현재 연결 상태 조회
   */
  getConnectionStatus(): ConnectionStatus {
    return this.connectionStatus;
  }

  /**
   * 현재 워크스페이스 ID 조회
   */
  getWorkspaceId(): number | null {
    return this.workspaceId;
  }

  /**
   * 이벤트 핸들러 업데이트
   */
  updateHandlers(handlers: Partial<WorkspaceWebSocketHandlers>): void {
    this.handlers = { ...this.handlers, ...handlers };
  }
}

// 싱글톤 인스턴스
export const workspaceWebSocket = new WorkspaceWebSocketService();
export default workspaceWebSocket;
