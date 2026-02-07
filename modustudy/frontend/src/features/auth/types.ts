// 인증 도메인 관련 타입 정의
export interface UserDTO {
    id: number;
    email: string;
    name: string;
    nickname: string | null;
    profileImage: string | null;
    levelName: string;
    currentLevel: number;
    role?: 'USER' | 'ADMIN';
    loginProvider?: 'KAKAO' | 'NAVER' | 'GOOGLE' | 'EMAIL';
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    isNewUser: boolean;
    user: UserDTO;
    loginProvider: 'KAKAO' | 'NAVER' | 'GOOGLE';
}

export interface OAuth2CallbackRequest {
    code: string;
}

export interface AuthUrlResponse {
    authUrl: string;
}

export interface OAuthTempData {
    email: string;
    name: string;
    profileImage: string | null;
    provider: string;
    providerId: string;
    accessToken?: string;
    refreshToken?: string;
    loginProvider?: string;
}
