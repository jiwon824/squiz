/**
 * Setting API 클라이언트
 * 설정 관련 API 호출 함수들을 정의합니다.
 */

import api from '@/api/axios';
import type {
    NotificationSetting,
    NotificationSettingUpdateRequest,
    SocialAccountsResponse,
    SetPasswordRequest,
    ChangePasswordRequest,
} from '../types';

// ============================================
// 알림 설정 API
// ============================================

/**
 * 알림 설정 조회
 * GET /api/v1/notifications/settings
 */
export const getNotificationSettings = async (): Promise<NotificationSetting[]> => {
    const response = await api.get<{ success: boolean; data: { settings: NotificationSetting[] } }>(
        '/api/v1/notifications/settings'
    );
    return response.data.data.settings;
};

/**
 * 알림 설정 수정
 * PUT /api/v1/notifications/settings
 */
export const updateNotificationSettings = async (
    request: NotificationSettingUpdateRequest
): Promise<void> => {
    await api.put('/api/v1/notifications/settings', request);
};

// ============================================
// 소셜 계정 API
// ============================================

/**
 * 연동된 소셜 계정 조회
 * GET /api/v1/auth/social/my
 */
export const getSocialAccounts = async (): Promise<SocialAccountsResponse> => {
    const response = await api.get<{ success: boolean; data: SocialAccountsResponse }>(
        '/api/v1/auth/social/my'
    );
    return response.data.data;
};

/**
 * 소셜 계정 연동 해제
 * DELETE /api/v1/auth/social/{provider}
 */
export const unlinkSocialAccount = async (provider: string): Promise<void> => {
    await api.delete(`/api/v1/auth/social/${provider.toLowerCase()}`);
};

// ============================================
// 비밀번호 API
// ============================================

/**
 * 비밀번호 최초 설정
 * POST /api/v1/auth/password
 */
export const setPassword = async (request: SetPasswordRequest): Promise<void> => {
    await api.post('/api/v1/auth/password', request);
};

/**
 * 비밀번호 변경
 * PUT /api/v1/auth/password
 */
export const changePassword = async (request: ChangePasswordRequest): Promise<void> => {
    await api.put('/api/v1/auth/password', request);
};

// ============================================
// API 객체 export
// ============================================

export const settingApi = {
    // 알림 설정
    getNotificationSettings,
    updateNotificationSettings,
    // 소셜 계정
    getSocialAccounts,
    unlinkSocialAccount,
    // 비밀번호
    setPassword,
    changePassword,
};

export default settingApi;
