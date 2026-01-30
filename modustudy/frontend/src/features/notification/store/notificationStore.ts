/**
 * Notification Store (Zustand)
 * 알림 상태 관리
 */

import { create } from 'zustand';
import {
    getNotifications,
    getUnreadCount,
    markAsRead,
    markAllAsRead,
    type NotificationItem,
    type NotificationType,
} from '../api/notificationApi';

interface NotificationState {
    // 알림 목록
    notifications: NotificationItem[];
    // 읽지 않은 알림 수
    unreadCount: number;
    // 타입별 읽지 않은 알림 수
    unreadByType: Record<NotificationType, number>;
    // 로딩 상태
    isLoading: boolean;
    // 에러
    error: string | null;
    // fetch 완료 플래그
    hasFetched: boolean;

    // 액션
    fetchNotifications: (page?: number, size?: number, type?: NotificationType) => Promise<void>;
    fetchUnreadCount: () => Promise<void>;
    markNotificationAsRead: (notificationId: number) => Promise<void>;
    markAllNotificationsAsRead: () => Promise<void>;
    resetNotifications: () => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
    notifications: [],
    unreadCount: 0,
    unreadByType: {} as Record<NotificationType, number>,
    isLoading: false,
    error: null,
    hasFetched: false,

    fetchNotifications: async (page = 0, size = 20, type) => {
        if (get().isLoading) return;
        set({ isLoading: true, error: null });
        try {
            const response = await getNotifications(page, size, type);
            set({
                notifications: response.content,
                unreadCount: response.unreadCount,
                isLoading: false,
                hasFetched: true,
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '알림을 불러오는데 실패했습니다.',
                isLoading: false,
                hasFetched: true,
            });
        }
    },

    fetchUnreadCount: async () => {
        try {
            console.log('[알림 Store] getUnreadCount API 호출 시작');
            const response = await getUnreadCount();
            console.log('[알림 Store] getUnreadCount 응답:', response);
            set({
                unreadCount: response.unreadCount,
                unreadByType: response.byType,
            });
        } catch (error) {
            console.error('[알림 Store] getUnreadCount 에러:', error);
        }
    },

    markNotificationAsRead: async (notificationId) => {
        try {
            await markAsRead(notificationId);
            const { notifications, unreadCount } = get();
            const notification = notifications.find((n) => n.id === notificationId);
            if (notification && !notification.isRead) {
                set({
                    notifications: notifications.map((n) =>
                        n.id === notificationId ? { ...n, isRead: true } : n
                    ),
                    unreadCount: Math.max(0, unreadCount - 1),
                });
            }
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '알림 읽음 처리에 실패했습니다.',
            });
        }
    },

    markAllNotificationsAsRead: async () => {
        try {
            await markAllAsRead();
            const { notifications } = get();
            set({
                notifications: notifications.map((n) => ({ ...n, isRead: true })),
                unreadCount: 0,
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '전체 읽음 처리에 실패했습니다.',
            });
        }
    },

    resetNotifications: () => {
        set({
            notifications: [],
            unreadCount: 0,
            unreadByType: {} as Record<NotificationType, number>,
            isLoading: false,
            error: null,
            hasFetched: false,
        });
    },
}));
