import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  FriendPresenceEvent,
  FriendWebSocketHandlers,
  ConnectionStatus,
} from './friendWebSocketTypes';

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

class FriendWebSocketService {
  private client: Client | null = null;
  private userId: number | null = null;
  private handlers: FriendWebSocketHandlers = {};
  private connectionStatus: ConnectionStatus = 'DISCONNECTED';

  connect(userId: number, handlers: FriendWebSocketHandlers = {}): void {
    if (this.client?.connected && this.userId === userId) {
      return;
    }

    if (this.client?.connected) {
      this.disconnect();
    }

    this.userId = userId;
    this.handlers = handlers;
    this.setConnectionStatus('CONNECTING');

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        userId: userId.toString(),
      },
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[Friend WS]', str);
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

  private onConnected(): void {
    this.setConnectionStatus('CONNECTED');
    this.subscribe();
  }

  private subscribe(): void {
    if (!this.client) return;
    this.client.subscribe('/user/queue/friends/presence', (message: IMessage) => {
      this.handleEvent(message);
    });
  }

  private handleEvent(message: IMessage): void {
    try {
      const event: FriendPresenceEvent = JSON.parse(message.body);
      if (event.type === 'PRESENCE') {
        this.handlers.onPresence?.(event);
      }
    } catch (e) {
      console.error('[Friend WS] event parse failed:', e);
    }
  }

  private onDisconnected(): void {
    this.setConnectionStatus('DISCONNECTED');
  }

  private onError(errorMessage: string): void {
    this.setConnectionStatus('ERROR');
    this.handlers.onError?.(errorMessage);
  }

  private setConnectionStatus(status: ConnectionStatus): void {
    this.connectionStatus = status;
    this.handlers.onConnectionChange?.(status);
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.userId = null;
    this.handlers = {};
    this.setConnectionStatus('DISCONNECTED');
  }

  updateHandlers(handlers: Partial<FriendWebSocketHandlers>): void {
    this.handlers = { ...this.handlers, ...handlers };
  }
}

export const friendWebSocket = new FriendWebSocketService();
export default friendWebSocket;
