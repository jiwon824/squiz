import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '',
    headers: {
        'Content-Type': 'application/json',
    },
    // 쿠키 전송 허용: withCredentials를 통해 브라우저가
    // "이 사이트(3000번)는 서버(8080번)와 쿠키를 주고받아도 된다"는 허락을 받았습니다.
    withCredentials: true,
});

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

export default api;
