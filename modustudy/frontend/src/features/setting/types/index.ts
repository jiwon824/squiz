/**
 * Setting Feature 타입 정의
 * 설정 페이지에서 사용하는 모든 타입을 정의합니다.
 */

// ============================================
// 알림 설정 타입
// ============================================

/** 알림 타입 enum */
export type NotificationType =
    | 'CHAT'
    | 'SCHEDULE'
    | 'ATTENDANCE'
    | 'STUDY_UPDATE'
    | 'QUIZ'
    | 'FRIEND'
    | 'SYSTEM';

/** 알림 설정 아이템 */
export interface NotificationSetting {
    type: NotificationType;
    typeName: string;
    isEnabled: boolean;
}

/** 알림 설정 수정 요청 */
export interface NotificationSettingUpdateRequest {
    settings: Array<{
        type: NotificationType;
        isEnabled: boolean;
    }>;
}

// ============================================
// 소셜 계정 타입
// ============================================

/** 소셜 프로바이더 타입 */
export type SocialProvider = 'KAKAO' | 'NAVER' | 'GOOGLE';

/** 연동된 소셜 계정 정보 */
export interface SocialAccount {
    provider: SocialProvider;
    email: string;
    linkedAt: string;
}

/** 소셜 계정 조회 응답 */
export interface SocialAccountsResponse {
    linkedAccounts: SocialAccount[];
    hasPassword: boolean;
}

// ============================================
// 비밀번호 타입
// ============================================

/** 비밀번호 최초 설정 요청 */
export interface SetPasswordRequest {
    password: string;
    passwordConfirm: string;
}

/** 비밀번호 변경 요청 */
export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
    newPasswordConfirm: string;
}

// ============================================
// 프로필 타입
// ============================================

/** 프로필 수정 요청 */
export interface ProfileUpdateRequest {
    nickname: string;
}

// ============================================
// 테마 설정 타입 (TODO: API 구현 후 사용)
// ============================================

/** 테마 타입 */
export type ThemeType = 'light' | 'dark' | 'system';

/** 테마 설정 */
export interface ThemeSettings {
    theme: ThemeType;
    fontSize: 'small' | 'medium' | 'large';
}

// ============================================
// 스터디 선호 설정 타입
// ============================================

/** 가용 요일 */
export type DayOfWeek = '월' | '화' | '수' | '목' | '금' | '토' | '일';

/** 가용 시간대 */
export type TimeSlot = 'morning' | 'afternoon' | 'evening' | 'night';

/** 스터디 선호 설정 */
export interface StudyPreference {
    techStack: string[];
    availableDays: DayOfWeek[];
    preferredTimeSlot: TimeSlot | null;
    preferredDurationWeeks: number; // 2~8
}

/** 스터디 선호 설정 저장 요청 */
export interface StudyPreferenceUpdateRequest {
    techStack: string[];
    availableDays: string[];
    preferredTimeSlot: string | null;
    preferredDurationWeeks: number;
}

// ============================================
// Google 캘린더 연동 타입
// ============================================

/** Google 캘린더 연동 상태 */
export interface GoogleCalendarStatus {
    connected: boolean;
    email: string | null;
    lastSyncAt: string | null;
}

// ============================================
// 설정 페이지 섹션 타입
// ============================================

/** 설정 섹션 타입 */
export type SettingSection = 'notification' | 'account' | 'profile' | 'study' | 'theme';

/** 사이드바 메뉴 아이템 */
export interface SettingMenuItem {
    id: SettingSection;
    label: string;
    icon: string;
    description: string;
}
