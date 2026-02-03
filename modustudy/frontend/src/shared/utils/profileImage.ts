// 프로필 이미지 URL 처리 유틸리티

// 기본 프로필 이미지 경로
export const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

/**
 * 프로필 이미지 URL 변환 함수
 * - /uploads/로 시작하는 로컬 경로는 API 서버 URL을 붙임
 * - http/https로 시작하는 외부 URL은 그대로 사용
 * - 빈 값이면 기본 이미지 반환
 */
export const getProfileImageUrl = (imageUrl?: string | null): string => {
    if (!imageUrl) return DEFAULT_PROFILE_IMAGE;

    // 외부 URL인 경우 그대로 반환
    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
        return imageUrl;
    }

    // /uploads/로 시작하는 로컬 경로인 경우 API URL 붙이기
    if (imageUrl.startsWith('/uploads/')) {
        const apiUrl = import.meta.env.VITE_API_URL || '';
        return `${apiUrl}${imageUrl}`;
    }

    return imageUrl;
};
