import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { MeetingRoomEvent, MeetingRoomChatMessage, SpeechSegment } from '../types';

export interface MeetingWebsocketClient {
    connect: () => Promise<Client>;
    disconnect: () => void;
    subscribeRoomEvents: (roomId: string, handler: (event: MeetingRoomEvent) => void) => () => void;
    subscribeChatHistory: (roomId: string, handler: (event: MeetingRoomEvent) => void) => () => void;
    subscribeSpeech: (roomId: string, handler: (segment: SpeechSegment) => void) => () => void;
    joinRoom: (roomId: string, payload: { displayName: string; roomTitle?: string }) => void;
    sendChat: (roomId: string, payload: MeetingRoomChatMessage) => void;
    setPresenter: (roomId: string, payload: { displayName: string; action?: 'claim' | 'release' }) => void;
    setSpeaking: (roomId: string, payload: { speaking: boolean }) => void;
    setPresence: (roomId: string, payload: { present: boolean }) => void;
    isConnected: () => boolean;
}

export const createMeetingWebsocket = (baseUrl?: string): MeetingWebsocketClient => {
    let client: Client | null = null;

    const connect = () =>
        new Promise<Client>((resolve, reject) => {
            let isConnected = false;
            const socketUrl = baseUrl ? `${baseUrl.replace(/\/$/, '')}/ws` : '/ws';
            client = new Client({
                webSocketFactory: () => new SockJS(socketUrl),
                reconnectDelay: 5000,
                onConnect: () => {
                    isConnected = true;
                    resolve(client as Client);
                },
                onStompError: (frame) => reject(frame),
                onWebSocketClose: () => {
                    if (!isConnected) {
                        reject(new Error('STOMP websocket closed before connect'));
                    }
                },
                onWebSocketError: () => {
                    if (!isConnected) {
                        reject(new Error('STOMP websocket error before connect'));
                    }
                },
                debug: () => {},
            });
            client.activate();
        });

    const disconnect = () => {
        if (client) {
            client.deactivate();
        }
    };

    const subscribeRoomEvents = (roomId: string, handler: (event: MeetingRoomEvent) => void) => {
        if (!client) {
            return () => {};
        }
        const subscription = client.subscribe(`/topic/rooms/${roomId}/events`, (message) => {
            handler(JSON.parse(message.body));
        });
        return () => subscription.unsubscribe();
    };

    const subscribeChatHistory = (roomId: string, handler: (event: MeetingRoomEvent) => void) => {
        if (!client) {
            return () => {};
        }
        const subscription = client.subscribe(`/user/queue/rooms/${roomId}/history`, (message) => {
            handler(JSON.parse(message.body));
        });
        return () => subscription.unsubscribe();
    };

    // 실시간 발화 자막 구독
    const subscribeSpeech = (roomId: string, handler: (segment: SpeechSegment) => void) => {
        if (!client) {
            return () => {};
        }
        const subscription = client.subscribe(`/topic/rooms/${roomId}/speech`, (message) => {
            handler(JSON.parse(message.body));
        });
        return () => subscription.unsubscribe();
    };

    const joinRoom = (roomId: string, payload: { displayName: string; roomTitle?: string }) => {
        if (!client) return;
        client.publish({
            destination: `/app/rooms/${roomId}/join`,
            body: JSON.stringify(payload),
        });
    };

    const sendChat = (roomId: string, payload: MeetingRoomChatMessage) => {
        if (!client) return;
        client.publish({
            destination: `/app/rooms/${roomId}/chat`,
            body: JSON.stringify(payload),
        });
    };

    const setPresenter = (roomId: string, payload: { displayName: string; action?: 'claim' | 'release' }) => {
        if (!client) return;
        client.publish({
            destination: `/app/rooms/${roomId}/presenter`,
            body: JSON.stringify(payload),
        });
    };

    const setSpeaking = (roomId: string, payload: { speaking: boolean }) => {
        if (!client) return;
        client.publish({
            destination: `/app/rooms/${roomId}/speaking`,
            body: JSON.stringify(payload),
        });
    };

    const setPresence = (roomId: string, payload: { present: boolean }) => {
        if (!client) return;
        client.publish({
            destination: `/app/rooms/${roomId}/presence`,
            body: JSON.stringify(payload),
        });
    };

    return {
        connect,
        disconnect,
        subscribeRoomEvents,
        subscribeChatHistory,
        subscribeSpeech,
        joinRoom,
        sendChat,
        setPresenter,
        setSpeaking,
        setPresence,
        isConnected: () => Boolean(client && client.connected),
    };
};
