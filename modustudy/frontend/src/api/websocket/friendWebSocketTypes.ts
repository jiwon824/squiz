export type FriendPresenceEvent = {
  type: 'PRESENCE';
  userId: number;
  isOnline: boolean;
  lastSeenAt: string | null;
};

export type FriendWebSocketHandlers = {
  onPresence?: (event: FriendPresenceEvent) => void;
  onConnectionChange?: (status: ConnectionStatus) => void;
  onError?: (message: string) => void;
};

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';
