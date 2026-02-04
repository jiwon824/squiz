/**
 * Notification API
 * 알림 관련 API 호출 함수
 */

import api from '@/api/axios';

// 알림 타입
export type NotificationType =
    | 'CHAT'
    | 'SCHEDULE'
    | 'ATTENDANCE'
    | 'STUDY_UPDATE'
    | 'STUDY_APPLICATION'
    | 'STUDY_RECRUITMENT_COMPLETE'
    | 'STUDY_EXTENSION'
    | 'STUDY_START'
    | 'QUIZ'
    | 'REPORT'
    | 'SYSTEM'
    | 'FRIEND';

// 알림 응답 타입
export interface NotificationItem {
    id: number;
    type: NotificationType;
    title: string;
    content: string;
    referenceType: string | null;
    referenceId: number | null;
    isRead: boolean;
    createdAt: string;
}

// 알림 목록 응답
export interface NotificationListResponse {
    content: NotificationItem[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    unreadCount: number;
}

// 읽지 않은 알림 수 응답
export interface UnreadCountResponse {
    unreadCount: number;
    byType: Record<NotificationType, number>;
}

/**
 * 알림 목록 조회
 */
export const getNotifications = async (
    page: number = 0,
    size: number = 20,
    type?: NotificationType
): Promise<NotificationListResponse> => {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (type) {
        params.append('type', type);
    }

    const response = await api.get<{ success: boolean; data: NotificationListResponse }>(
        `/api/v1/notifications?${params.toString()}`
    );
    return response.data.data;
};

/**
 * 읽지 않은 알림 수 조회
 */
export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
    const response = await api.get<{ success: boolean; data: UnreadCountResponse }>(
        '/api/v1/notifications/unread-count'
    );
    return response.data.data;
};

/**
 * 알림 읽음 처리
 */
export const markAsRead = async (notificationId: number): Promise<void> => {
    await api.put(`/api/v1/notifications/${notificationId}/read`);
};

/**
 * 전체 읽음 처리
 */
export const markAllAsRead = async (): Promise<{ readCount: number }> => {
    const response = await api.put<{ success: boolean; data: { readCount: number } }>(
        '/api/v1/notifications/read-all'
    );
    return response.data.data;
};
