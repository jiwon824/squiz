// DM Store - 다이렉트 메시지 상태 관리 (Zustand)
import { create } from 'zustand';
import {
    getConversations,
    getMessages,
    sendMessage as sendMessageApi,
    getUnreadCount,
    markAsRead,
    deleteConversation,
    Conversation,
    Message,
    dmWebSocket
} from '@/api/endpoints/dmApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';

// 새 대화 시작을 위한 사용자 정보
interface PendingDMUser {
    id: number;
    nickname: string;
    profileImage: string | null;
}

interface DMState {
    // 상태
    conversations: Conversation[];
    currentConversationId: number | null;
    messages: Message[];
    unreadCount: number;
    isLoading: boolean;           // 메시지 로딩 전용
    isLoadingConversations: boolean;  // 대화 목록 로딩 (메시지 영역에 영향 안 줌)
    error: string | null;
    pendingDMUser: PendingDMUser | null;  // 새 대화 시작 대상
    isWebSocketConnected: boolean;  // WebSocket 연결 상태

    // 액션
    fetchConversations: () => Promise<void>;
    fetchMessages: (conversationId: number) => Promise<void>;
    sendMessage: (receiverId: number, content: string) => Promise<void>;
    fetchUnreadCount: () => Promise<void>;
    markConversationAsRead: (conversationId: number) => Promise<void>;
    removeConversation: (conversationId: number) => Promise<void>;
    setCurrentConversation: (conversationId: number | null) => void;
    startConversationWith: (user: PendingDMUser) => void;  // 새 대화 시작
    clearPendingDM: () => void;  // 대기 상태 초기화
    clearError: () => void;
    connectWebSocket: () => void;  // WebSocket 연결
    disconnectWebSocket: () => void;  // WebSocket 해제
    addMessage: (message: Message) => void;  // 메시지 추가 (WebSocket용)
}

export const useDMStore = create<DMState>((set, get) => ({
    // 초기 상태
    conversations: [],
    currentConversationId: null,
    messages: [],
    unreadCount: 0,
    isLoading: false,
    isLoadingConversations: false,
    error: null,
    pendingDMUser: null,
    isWebSocketConnected: false,

    // 대화방 목록 조회 (메시지 영역에 영향 주지 않도록 별도 로딩 플래그 사용)
    fetchConversations: async () => {
        set({ isLoadingConversations: true, error: null });
        try {
            const conversations = await getConversations();
            set({ conversations, isLoadingConversations: false });
        } catch (error: any) {
            set({
                error: error.response?.data?.message || '대화 목록을 불러오지 못했습니다.',
                isLoadingConversations: false
            });
        }
    },

    // 메시지 목록 조회
    fetchMessages: async (conversationId: number) => {
        console.log('[DM] fetchMessages started for:', conversationId);
        set({ isLoading: true, currentConversationId: conversationId });
        try {
            const messages = await getMessages(conversationId);
            console.log('[DM] fetchMessages result:', messages?.length, 'messages');
            set({ messages, isLoading: false });
            // 읽음 처리
            get().markConversationAsRead(conversationId);
        } catch (error: any) {
            console.error('[DM] fetchMessages error:', error);
            set({
                error: error.response?.data?.message || '메시지를 불러오지 못했습니다.',
                isLoading: false
            });
        }
    },

    // 메시지 전송 (REST API 우선 → 에러 즉시 감지, 성공 시 WebSocket으로 실시간 수신)
    sendMessage: async (receiverId: number, content: string) => {
        try {
            // REST API로 전송하여 서버 응답(에러 포함)을 확실히 받음
            const newMessage = await sendMessageApi(receiverId, content);
            // 중복 체크 (WebSocket echo-back이 먼저 도착한 경우 방지)
            set(state => {
                const exists = state.messages.some(m => m.id === newMessage.id);
                if (exists) return state;
                return { messages: [...state.messages, newMessage] };
            });
            // 대화방 목록 새로고침
            get().fetchConversations();
        } catch (error: any) {
            const status = error.response?.status;
            const serverMessage = error.response?.data?.message || error.response?.data?.error?.message;
            // 403: 친구 관계 필요 등 비즈니스 에러 → 토스트로 안내
            if (status === 403) {
                const friendMessage = serverMessage || '친구 추가 후 DM을 보낼 수 있습니다.';
                useUIStore.getState().showToast(friendMessage, 'warning');
            } else {
                set({ error: serverMessage || '메시지 전송에 실패했습니다.' });
            }
        }
    },

    // 안읽은 메시지 개수 조회
    fetchUnreadCount: async () => {
        try {
            const unreadCount = await getUnreadCount();
            set({ unreadCount });
        } catch (error: any) {
            console.error('안읽은 메시지 개수 조회 실패:', error);
        }
    },

    // 읽음 처리
    markConversationAsRead: async (conversationId: number) => {
        try {
            await markAsRead(conversationId);
            // 대화방의 unreadCount를 0으로 업데이트
            set(state => ({
                conversations: state.conversations.map(conv =>
                    conv.id === conversationId ? { ...conv, unreadCount: 0 } : conv
                )
            }));
            // 전체 안읽은 개수 새로고침
            get().fetchUnreadCount();
        } catch (error: any) {
            console.error('읽음 처리 실패:', error);
        }
    },

    // 대화방 삭제
    removeConversation: async (conversationId: number) => {
        try {
            await deleteConversation(conversationId);
            set(state => ({
                conversations: state.conversations.filter(conv => conv.id !== conversationId),
                currentConversationId: state.currentConversationId === conversationId ? null : state.currentConversationId,
                messages: state.currentConversationId === conversationId ? [] : state.messages
            }));
        } catch (error: any) {
            set({ error: error.response?.data?.message || '대화방 삭제에 실패했습니다.' });
        }
    },

    // 현재 대화방 설정
    setCurrentConversation: (conversationId: number | null) => {
        console.log('[DM] setCurrentConversation:', conversationId);
        set({ currentConversationId: conversationId });
        if (conversationId) {
            console.log('[DM] Calling fetchMessages for:', conversationId);
            get().fetchMessages(conversationId);
        } else {
            set({ messages: [] });
        }
    },

    // 특정 사용자와 새 대화 시작
    startConversationWith: (user: PendingDMUser) => {
        const { conversations } = get();
        // 이미 대화가 있는지 확인
        const existingConv = conversations.find(c => c.participantId === user.id);
        if (existingConv) {
            // 기존 대화가 있으면 해당 대화 열기
            get().setCurrentConversation(existingConv.id);
            set({ pendingDMUser: null });
        } else {
            // 새 대화 시작 모드
            set({
                pendingDMUser: user,
                currentConversationId: null,
                messages: []
            });
        }
    },

    // 대기 상태 초기화
    clearPendingDM: () => set({ pendingDMUser: null }),

    // 에러 초기화
    clearError: () => set({ error: null }),

    // WebSocket 연결
    connectWebSocket: () => {
        const authStore = useAuthStore.getState();
        const user = authStore.user;

        if (!user) {
            console.warn('Cannot connect DM WebSocket: user not logged in');
            return;
        }

        if (dmWebSocket.isConnected()) {
            console.warn('DM WebSocket already connected');
            return;
        }

        dmWebSocket.connect(Number(user.id), user.nickname || '', {
            onMessage: (event) => {
                // 새 메시지 수신 - 백엔드는 event.message로 전송
                if (event.message) {
                    const message: Message = {
                        id: event.message.messageId,
                        conversationId: event.message.conversationId,
                        senderId: event.message.senderId,
                        senderNickname: event.message.senderNickname,
                        senderProfileImage: event.message.senderProfileImage,
                        content: event.message.content,
                        isDeleted: event.message.isDeleted,
                        isMine: event.message.isMine,
                        createdAt: event.message.createdAt
                    };
                    get().addMessage(message);
                    // 대화방 목록 새로고침 (최신 메시지 반영)
                    get().fetchConversations();
                    get().fetchUnreadCount();
                }
            },
            onTyping: (event) => {
                // 입력 중 표시 (필요시 구현)
                console.log('Typing:', event);
            },
            onRead: (event) => {
                // 읽음 처리 (필요시 구현)
                console.log('Read:', event);
            },
            onConnectionChange: (status) => {
                set({ isWebSocketConnected: status === 'CONNECTED' });
                console.log('DM WebSocket status:', status);
            },
            onError: (error) => {
                console.error('DM WebSocket error:', error);
            }
        });
    },

    // WebSocket 해제
    disconnectWebSocket: () => {
        dmWebSocket.disconnect();
        set({ isWebSocketConnected: false });
    },

    // 메시지 추가 (WebSocket 수신용 - 상대방 메시지 + 내 echo-back 처리)
    addMessage: (message: Message) => {
        const { currentConversationId, messages } = get();
        // 현재 열려있는 대화방의 메시지인 경우에만 추가
        if (currentConversationId === message.conversationId) {
            // 서버에서 온 echo-back인 경우: 낙관적 메시지를 서버 메시지로 교체
            const optimisticIdx = messages.findIndex(
                m => m.isMine && m.content === message.content && m.id !== message.id && m.id > 1e12
            );
            if (optimisticIdx !== -1) {
                // 낙관적 메시지를 서버 응답으로 교체
                set(state => ({
                    messages: state.messages.map((m, i) => i === optimisticIdx ? message : m)
                }));
                return;
            }
            // 중복 체크 (같은 서버 ID)
            const exists = messages.some(m => m.id === message.id);
            if (!exists) {
                set(state => ({
                    messages: [...state.messages, message]
                }));
            }
        }
    }
}));
