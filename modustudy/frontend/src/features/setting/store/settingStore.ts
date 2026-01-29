/**
 * Setting Store (Zustand)
 * 설정 페이지의 전역 상태를 관리합니다.
 */

import { create } from 'zustand';
import {
    getNotificationSettings,
    updateNotificationSettings,
    getSocialAccounts,
    unlinkSocialAccount,
    setPassword,
    changePassword,
    getGoogleCalendarStatus,
    getGoogleCalendarAuthUrl,
    disconnectGoogleCalendar,
} from '../api/settingApi';
import type {
    NotificationSetting,
    NotificationType,
    SocialAccount,
    GoogleCalendarStatus,
    SettingSection,
} from '../types';

// ============================================
// 상태 인터페이스
// ============================================

interface SettingState {
    // 현재 활성 섹션
    activeSection: SettingSection;

    // 알림 설정 데이터
    notificationSettings: NotificationSetting[];

    // 소셜 계정 데이터
    socialAccounts: SocialAccount[];
    hasPassword: boolean;

    // Google 캘린더 연동 상태
    googleCalendarStatus: GoogleCalendarStatus | null;

    // 로딩/저장 상태
    isLoading: boolean;
    isSaving: boolean;
    error: string | null;

    // 액션: 섹션 변경
    setActiveSection: (section: SettingSection) => void;

    // 액션: 알림 설정
    fetchNotificationSettings: () => Promise<void>;
    updateNotificationSetting: (type: NotificationType, isEnabled: boolean) => Promise<void>;

    // 액션: 소셜 계정
    fetchSocialAccounts: () => Promise<void>;
    unlinkSocialAccount: (provider: string) => Promise<void>;

    // 액션: 비밀번호
    setNewPassword: (password: string, passwordConfirm: string) => Promise<void>;
    changeExistingPassword: (
        currentPassword: string,
        newPassword: string,
        newPasswordConfirm: string
    ) => Promise<void>;

    // 액션: Google 캘린더
    fetchGoogleCalendarStatus: () => Promise<void>;
    connectGoogleCalendar: () => Promise<void>;
    disconnectGoogleCalendar: () => Promise<void>;

    // 액션: 초기화
    resetError: () => void;
}

// ============================================
// 스토어 생성
// ============================================

export const useSettingStore = create<SettingState>((set, get) => ({
    // 초기 상태
    activeSection: 'notification',
    notificationSettings: [],
    socialAccounts: [],
    hasPassword: false,
    googleCalendarStatus: null,
    isLoading: false,
    isSaving: false,
    error: null,

    // 섹션 변경
    setActiveSection: (section) => {
        set({ activeSection: section });
    },

    // ============================================
    // 알림 설정 액션
    // ============================================

    /**
     * 알림 설정 조회
     */
    fetchNotificationSettings: async () => {
        set({ isLoading: true, error: null });
        try {
            const settings = await getNotificationSettings();
            set({ notificationSettings: settings, isLoading: false });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '알림 설정을 불러오는데 실패했습니다.',
                isLoading: false,
            });
        }
    },

    /**
     * 개별 알림 설정 업데이트
     */
    updateNotificationSetting: async (type, isEnabled) => {
        const { notificationSettings } = get();

        // 낙관적 업데이트: UI 먼저 변경
        const updatedSettings = notificationSettings.map((setting) =>
            setting.type === type ? { ...setting, isEnabled } : setting
        );
        set({ notificationSettings: updatedSettings, isSaving: true });

        try {
            // 전체 설정을 서버에 저장
            await updateNotificationSettings({
                settings: updatedSettings.map((s) => ({ type: s.type, isEnabled: s.isEnabled })),
            });
            set({ isSaving: false });
        } catch (error) {
            // 실패 시 롤백
            set({
                notificationSettings,
                error: error instanceof Error ? error.message : '알림 설정 저장에 실패했습니다.',
                isSaving: false,
            });
        }
    },

    // ============================================
    // 소셜 계정 액션
    // ============================================

    /**
     * 소셜 계정 목록 조회
     */
    fetchSocialAccounts: async () => {
        set({ isLoading: true, error: null });
        try {
            const response = await getSocialAccounts();
            set({
                socialAccounts: response.linkedAccounts,
                hasPassword: response.hasPassword,
                isLoading: false,
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '소셜 계정 정보를 불러오는데 실패했습니다.',
                isLoading: false,
            });
        }
    },

    /**
     * 소셜 계정 연동 해제
     */
    unlinkSocialAccount: async (provider) => {
        const { socialAccounts, hasPassword } = get();

        // 최소 1개 로그인 수단 유지 체크
        if (socialAccounts.length <= 1 && !hasPassword) {
            set({ error: '최소 1개의 로그인 방법은 유지해야 합니다.' });
            return;
        }

        set({ isSaving: true, error: null });
        try {
            await unlinkSocialAccount(provider);
            // 연동 해제된 계정 목록에서 제거
            set({
                socialAccounts: socialAccounts.filter(
                    (account) => account.provider !== provider
                ),
                isSaving: false,
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '소셜 연동 해제에 실패했습니다.',
                isSaving: false,
            });
        }
    },

    // ============================================
    // 비밀번호 액션
    // ============================================

    /**
     * 비밀번호 최초 설정
     */
    setNewPassword: async (password, passwordConfirm) => {
        set({ isSaving: true, error: null });
        try {
            await setPassword({ password, passwordConfirm });
            set({ hasPassword: true, isSaving: false });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '비밀번호 설정에 실패했습니다.',
                isSaving: false,
            });
            throw error;
        }
    },

    /**
     * 비밀번호 변경
     */
    changeExistingPassword: async (currentPassword, newPassword, newPasswordConfirm) => {
        set({ isSaving: true, error: null });
        try {
            await changePassword({ currentPassword, newPassword, newPasswordConfirm });
            set({ isSaving: false });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : '비밀번호 변경에 실패했습니다.',
                isSaving: false,
            });
            throw error;
        }
    },

    // ============================================
    // Google 캘린더 액션
    // ============================================

    /**
     * Google 캘린더 연동 상태 조회
     */
    fetchGoogleCalendarStatus: async () => {
        try {
            const status = await getGoogleCalendarStatus();
            set({ googleCalendarStatus: status });
        } catch {
            // 캘린더 상태 조회 실패는 조용히 처리 (미연동 상태로 간주)
            set({ googleCalendarStatus: { connected: false, email: null, lastSyncAt: null } });
        }
    },

    /**
     * Google 캘린더 연동 (OAuth 팝업)
     */
    connectGoogleCalendar: async () => {
        try {
            const authUrl = await getGoogleCalendarAuthUrl();
            window.open(authUrl, 'google-calendar-auth', 'width=500,height=600');
        } catch {
            set({ error: 'Google 캘린더 연동 URL을 가져오는데 실패했습니다.' });
        }
    },

    /**
     * Google 캘린더 연동 해제
     */
    disconnectGoogleCalendar: async () => {
        set({ isSaving: true, error: null });
        try {
            await disconnectGoogleCalendar();
            set({
                googleCalendarStatus: { connected: false, email: null, lastSyncAt: null },
                isSaving: false,
            });
        } catch (error) {
            set({
                error: error instanceof Error ? error.message : 'Google 캘린더 연동 해제에 실패했습니다.',
                isSaving: false,
            });
        }
    },

    // ============================================
    // 유틸리티 액션
    // ============================================

    /**
     * 에러 초기화
     */
    resetError: () => {
        set({ error: null });
    },
}));
