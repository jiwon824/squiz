import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '',
    headers: {
        'Content-Type': 'application/json',
    },
    // 쿠키 전송 허용: withCredentials를 통해 브라우저가
    // "이 사이트(3000번)는 서버(8080번)와 쿠키를 주고받아도 된다"는 허락을 받았습니다.
    withCredentials: true,
});

// 토큰 갱신 중복 방지를 위한 플래그
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
}> = [];

// 대기 중인 요청들 처리
const processQueue = (error: unknown, token: string | null = null) => {
    failedQueue.forEach((prom) => {
        if (token) {
            prom.resolve(token);
        } else {
            prom.reject(error);
        }
    });
    failedQueue = [];
};

// 요청 인터셉터: 토큰과 User-Id 헤더 부착
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // User-Id 헤더 추가 (백엔드 메시지 API 등에서 필요)
        const authStorage = localStorage.getItem('auth-storage');
        if (authStorage) {
            try {
                const authData = JSON.parse(authStorage);
                const userId = authData?.state?.user?.id;
                if (userId) {
                    config.headers['User-Id'] = String(userId);
                }
            } catch (e) {
                // JSON 파싱 실패 시 무시
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러 시 자동 토큰 갱신
api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        // 401 에러이고, 재시도하지 않은 요청이며, 토큰 갱신 요청이 아닌 경우
        if (
            error.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes('/auth/token/refresh')
        ) {
            // 이미 갱신 중이면 대기열에 추가
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then((token) => {
                    originalRequest.headers.Authorization = `Bearer ${token}`;
                    return api(originalRequest);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshToken = localStorage.getItem('refreshToken');

            if (!refreshToken) {
                // Refresh Token이 없으면 로그아웃 처리
                isRefreshing = false;
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                window.location.href = '/login';
                return Promise.reject(error);
            }

            try {
                // 토큰 갱신 요청
                const response = await axios.post(
                    `${import.meta.env.VITE_API_URL || ''}/api/v1/auth/token/refresh`,
                    { refreshToken },
                    { headers: { 'Content-Type': 'application/json' } }
                );

                const newAccessToken = response.data.data.accessToken;
                localStorage.setItem('accessToken', newAccessToken);

                // 대기 중인 요청들 처리
                processQueue(null, newAccessToken);
                isRefreshing = false;

                // 원래 요청 재시도
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return api(originalRequest);
            } catch (refreshError) {
                // 갱신 실패 시 로그아웃 처리
                processQueue(refreshError, null);
                isRefreshing = false;
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        // 403, 500, 503 에러 시 에러 페이지로 리다이렉트
        const status = error.response?.status;
        if (status === 403 || status === 500 || status === 503) {
            window.location.href = `/error/${status}`;
            return Promise.reject(error);
        }

        return Promise.reject(error);
    }
);

export default api;
