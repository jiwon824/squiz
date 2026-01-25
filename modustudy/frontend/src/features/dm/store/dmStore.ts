// DM Store - 다이렉트 메시지 상태 관리 (Zustand)
import { create } from 'zustand';
import {
    getConversations,
    getMessages,
    sendMessage,
    getUnreadCount,
    markAsRead,
    deleteConversation,
    Conversation,
    Message
} from '@/api/endpoints/dmApi';

interface DMState {
    // 상태
    conversations: Conversation[];
    currentConversationId: number | null;
    messages: Message[];
    unreadCount: number;
    isLoading: boolean;
    error: string | null;

    // 액션
    fetchConversations: () => Promise<void>;
    fetchMessages: (conversationId: number) => Promise<void>;
    sendMessage: (receiverId: number, content: string) => Promise<void>;
    fetchUnreadCount: () => Promise<void>;
    markConversationAsRead: (conversationId: number) => Promise<void>;
    removeConversation: (conversationId: number) => Promise<void>;
    setCurrentConversation: (conversationId: number | null) => void;
    clearError: () => void;
}

export const useDMStore = create<DMState>((set, get) => ({
    // 초기 상태
    conversations: [],
    currentConversationId: null,
    messages: [],
    unreadCount: 0,
    isLoading: false,
    error: null,

    // 대화방 목록 조회
    fetchConversations: async () => {
        set({ isLoading: true, error: null });
        try {
            const conversations = await getConversations();
            set({ conversations, isLoading: false });
        } catch (error: any) {
            set({
                error: error.response?.data?.message || '대화 목록을 불러오지 못했습니다.',
                isLoading: false
            });
        }
    },

    // 메시지 목록 조회
    fetchMessages: async (conversationId: number) => {
        set({ isLoading: true, currentConversationId: conversationId });
        try {
            const messages = await getMessages(conversationId);
            set({ messages, isLoading: false });
            // 읽음 처리
            get().markConversationAsRead(conversationId);
        } catch (error: any) {
            set({
                error: error.response?.data?.message || '메시지를 불러오지 못했습니다.',
                isLoading: false
            });
        }
    },

    // 메시지 전송
    sendMessage: async (receiverId: number, content: string) => {
        try {
            const newMessage = await sendMessage(receiverId, content);
            set(state => ({
                messages: [...state.messages, newMessage]
            }));
            // 대화방 목록 새로고침
            get().fetchConversations();
        } catch (error: any) {
            set({ error: error.response?.data?.message || '메시지 전송에 실패했습니다.' });
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
        set({ currentConversationId: conversationId });
        if (conversationId) {
            get().fetchMessages(conversationId);
        } else {
            set({ messages: [] });
        }
    },

    // 에러 초기화
    clearError: () => set({ error: null })
}));
