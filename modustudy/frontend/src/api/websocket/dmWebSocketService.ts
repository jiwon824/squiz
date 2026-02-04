// DM WebSocket 클라이언트 서비스
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
    DmWebSocketMessage,
    DmWebSocketEvent,
    DmWebSocketHandlers,
    ConnectionStatus,
} from './dmWebSocketTypes';

// WebSocket URL 결정: 환경변수 > API URL 기반 > 현재 origin 기반
const getWsUrl = (): string => {
    if (import.meta.env.VITE_WS_URL) {
        return import.meta.env.VITE_WS_URL;
    }
    if (import.meta.env.VITE_API_URL) {
        return `${import.meta.env.VITE_API_URL}/ws`;
    }
    // 프로덕션: 현재 origin 사용
    return `${window.location.origin}/ws`;
};

const WS_URL = getWsUrl();

class DmWebSocketService {
    private client: Client | null = null;
    private userId: number | null = null;
    private nickname: string = '';
    private handlers: DmWebSocketHandlers = {};
    private connectionStatus: ConnectionStatus = 'DISCONNECTED';

    /**
     * WebSocket 연결
     */
    connect(userId: number, nickname: string, handlers: DmWebSocketHandlers = {}): void {
        if (this.client?.connected) {
            console.warn('DM WebSocket already connected');
            return;
        }

        this.userId = userId;
        this.nickname = nickname;
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
                    console.log('[DM WS]', str);
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
        console.log('DM WebSocket connected');
        this.setConnectionStatus('CONNECTED');

        // DM 연결 메시지 전송
        this.client?.publish({
            destination: '/app/dm/connect',
            headers: {
                userId: this.userId!.toString(),
            },
        });

        // 구독 설정
        this.subscribe();
    }

    /**
     * 채널 구독 (Principal 기반 /user/queue 방식)
     */
    private subscribe(): void {
        if (!this.client || !this.userId) return;

        // 개인 DM 메시지 수신 (Principal 기반 user queue)
        this.client.subscribe('/user/queue/dm', (message: IMessage) => {
            this.handleEvent(message);
        });

        // DM 이벤트 (typing, read 등)
        this.client.subscribe('/user/queue/dm/events', (message: IMessage) => {
            this.handleEvent(message);
        });
    }

    /**
     * 이벤트 처리
     */
    private handleEvent(message: IMessage): void {
        try {
            const event: DmWebSocketEvent = JSON.parse(message.body);

            switch (event.type) {
                case 'MESSAGE':
                    this.handlers.onMessage?.(event);
                    break;
                case 'TYPING':
                    this.handlers.onTyping?.(event);
                    break;
                case 'READ':
                    this.handlers.onRead?.(event);
                    break;
                case 'ONLINE':
                    this.handlers.onOnline?.(event);
                    break;
                case 'OFFLINE':
                    this.handlers.onOffline?.(event);
                    break;
            }
        } catch (e) {
            console.error('Failed to parse DM event:', e);
        }
    }

    /**
     * 연결 해제 시
     */
    private onDisconnected(): void {
        console.log('DM WebSocket disconnected');
        this.setConnectionStatus('DISCONNECTED');
    }

    /**
     * 에러 발생 시
     */
    private onError(errorMessage: string): void {
        console.error('DM WebSocket error:', errorMessage);
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
    sendMessage(receiverId: number, content: string): void {
        if (!this.client?.connected) {
            console.error('WebSocket not connected');
            return;
        }

        const message: DmWebSocketMessage = {
            receiverId,
            content,
        };

        this.client.publish({
            destination: '/app/dm/send',
            headers: {
                userId: this.userId!.toString(),
            },
            body: JSON.stringify(message),
        });
    }

    /**
     * 입력 중 표시 전송
     */
    sendTyping(conversationId: number): void {
        if (!this.client?.connected) return;

        this.client.publish({
            destination: `/app/dm/typing/${conversationId}`,
            headers: {
                userId: this.userId!.toString(),
                nickname: this.nickname,
            },
        });
    }

    /**
     * 읽음 처리 전송
     */
    sendRead(conversationId: number): void {
        if (!this.client?.connected) return;

        this.client.publish({
            destination: `/app/dm/read/${conversationId}`,
            headers: {
                userId: this.userId!.toString(),
            },
        });
    }

    /**
     * 연결 해제
     */
    disconnect(): void {
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
        this.userId = null;
        this.nickname = '';
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
     * 이벤트 핸들러 업데이트
     */
    updateHandlers(handlers: Partial<DmWebSocketHandlers>): void {
        this.handlers = { ...this.handlers, ...handlers };
    }
}

// 싱글톤 인스턴스
export const dmWebSocket = new DmWebSocketService();
export default dmWebSocket;
