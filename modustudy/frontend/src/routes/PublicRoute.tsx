// 비로그인 사용자 전용 - 로그인 상태면 /dashboard로 리다이렉트
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface PublicRouteProps {
    children: React.ReactNode;
}

export const PublicRoute = ({ children }: PublicRouteProps) => {
    const { isLoggedIn } = useAuthStore();

    if (isLoggedIn) {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
};
