// 로그인 필수 가드 - 비로그인 시 /login으로 리다이렉트
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface PrivateRouteProps {
    children: React.ReactNode;
}

export const PrivateRoute = ({ children }: PrivateRouteProps) => {
    const { isLoggedIn } = useAuthStore();
    const location = useLocation();

    if (!isLoggedIn) {
        // 로그인 후 원래 페이지로 돌아갈 수 있도록 현재 경로 저장
        sessionStorage.setItem('redirectAfterLogin', location.pathname + location.search);
        return <Navigate to="/login" replace />;
    }

    return <>{children}</>;
};
