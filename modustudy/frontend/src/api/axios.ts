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

// 요청 인터셉터: 토큰이 있으면 헤더에 부착
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export default api;
