import api from '../axios';
import { AuthResponse, AuthUrlResponse, OAuth2CallbackRequest, UserDTO } from '@/features/auth/types';

// 프로필 설정 요청 타입
interface ProfileSetupRequest {
    name: string;
    nickname: string;
    password: string;
}

// 일반 로그인 요청 타입
interface LoginRequest {
    email: string;
    password: string;
}

export const authApi = {
    /**
     * 일반 로그인 (이메일/비밀번호)
     * POST /api/v1/auth/login
     */
    login: async (email: string, password: string) => {
        const request: LoginRequest = { email, password };
        const response = await api.post<any>('/api/v1/auth/login', request);
        // 백엔드에서 ApiResponse<AuthResponse> 반환함
        return response.data.data as AuthResponse;
    },

    /**
     * 카카오 로그인 URL 가져오기
     */
    getKakaoAuthUrl: async () => {
        const response = await api.get<any>('/api/v1/auth/oauth/kakao');
        // 백엔드에서 ApiResponse<AuthUrlResponse> 반환함
        return response.data.data as AuthUrlResponse;
    },

    /**
     * 카카오 로그인 콜백 처리 (인가 코드를 JWT로 교환)
     */
    handleKakaoCallback: async (code: string) => {
        const request: OAuth2CallbackRequest = { code };
        const response = await api.post<any>('/api/v1/auth/oauth/kakao/callback', request);
        // 백엔드에서 ApiResponse<AuthResponse> 반환함
        return response.data.data as AuthResponse;
    },

    /**
     * 네이버 로그인 URL 가져오기
     */
    getNaverAuthUrl: async () => {
        const response = await api.get<any>('/api/v1/auth/oauth/naver');
        // 백엔드에서 ApiResponse<AuthUrlResponse> 반환함
        return response.data.data as AuthUrlResponse;
    },

    /**
     * 네이버 로그인 콜백 처리 (인가 코드를 JWT로 교환)
     */
    handleNaverCallback: async (code: string, state: string) => {
        const response = await api.post<any>(`/api/v1/auth/oauth/naver/callback?state=${encodeURIComponent(state)}`, { code });
        // 백엔드에서 ApiResponse<AuthResponse> 반환함
        return response.data.data as AuthResponse;
    },

    /**
     * 구글 로그인 URL 가져오기
     */
    getGoogleAuthUrl: async () => {
        const response = await api.get<any>('/api/v1/auth/oauth/google');
        // 백엔드에서 ApiResponse<AuthUrlResponse> 반환함
        return response.data.data as AuthUrlResponse;
    },

    /**
     * 구글 로그인 콜백 처리 (인가 코드를 JWT로 교환)
     */
    handleGoogleCallback: async (code: string) => {
        const request: OAuth2CallbackRequest = { code };
        const response = await api.post<any>('/api/v1/auth/oauth/google/callback', request);
        // 백엔드에서 ApiResponse<AuthResponse> 반환함
        return response.data.data as AuthResponse;
    },

    /**
     * OAuth 회원가입 완료 (추가 정보 입력)
     * POST /api/v1/users/me/profile
     */
    setupProfile: async (name: string, nickname: string, password: string) => {
        const request: ProfileSetupRequest = { name, nickname, password };
        const response = await api.post<any>('/api/v1/users/me/profile', request);
        // 백엔드에서 ApiResponse<UserDTO> 반환함
        return response.data.data as UserDTO;
    },

    /**
     * 현재 로그인된 사용자 정보 조회 (세션 복구용)
     * GET /api/v1/users/me
     */
    getMe: async () => {
        const response = await api.get<any>('/api/v1/users/me');
        // 백엔드에서 ApiResponse<UserDTO> 반환함
        return response.data.data as UserDTO;
    },

    /**
     * 비밀번호 재설정 이메일 발송 요청
     * POST /api/v1/auth/password/reset-request
     */
    requestPasswordReset: async (email: string) => {
        const response = await api.post<any>('/api/v1/auth/password/reset-request', { email });
        return response.data;
    },

    /**
     * 비밀번호 실제 변경 (토큰 이용)
     * POST /api/v1/auth/password/reset
     */
    resetPassword: async (token: string, newPassword: string) => {
        const response = await api.post<any>('/api/v1/auth/password/reset', { token, newPassword });
        return response.data;
    },

    /**
     * Access Token 갱신
     * POST /api/v1/auth/token/refresh
     */
    refreshToken: async (refreshToken: string) => {
        const response = await api.post<any>('/api/v1/auth/token/refresh', { refreshToken });
        return response.data.data as { accessToken: string; expiresIn: number };
    },
};
