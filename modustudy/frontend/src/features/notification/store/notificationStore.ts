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

            // 중복 알림 필터링 및 제목 통일
            // 백엔드에서 승인/거절 시 알림을 2번 보내는 문제 해결
            const filteredNotifications = response.content
                .filter((notification) => {
                    // "스터디 신청 승인" 또는 "스터디 신청 결과" 제목의 알림은 제외
                    // (STUDY_APPLICATION 타입으로 오는 중복 알림)
                    if (notification.type === 'STUDY_APPLICATION' &&
                        (notification.title === '스터디 신청 승인' || notification.title === '스터디 신청 결과')) {
                        return false;
                    }
                    return true;
                })
                .map((notification) => {
                    // "스터디 가입 승인/거절" 제목을 "스터디 신청이 승인/거절되었습니다"로 변경
                    if (notification.title === '스터디 가입 승인') {
                        return { ...notification, title: '스터디 신청이 승인되었습니다' };
                    }
                    if (notification.title === '스터디 가입 거절') {
                        return { ...notification, title: '스터디 신청이 거절되었습니다' };
                    }
                    return notification;
                });

            // unreadCount는 fetchUnreadCount()에서만 갱신 (덮어쓰기 방지)
            // 드롭다운에서 표시할 알림 개수와 배지 숫자 불일치 버그 수정
            set({
                notifications: filteredNotifications,
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
            const response = await getUnreadCount();
            set({
                unreadCount: response.unreadCount,
                unreadByType: response.byType,
            });
        } catch {
            // 조용히 실패 처리
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
