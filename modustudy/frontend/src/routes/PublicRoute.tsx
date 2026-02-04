// 비로그인 사용자 전용 - 로그인 상태면 리다이렉트
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface PublicRouteProps {
    children: React.ReactNode;
}

export const PublicRoute = ({ children }: PublicRouteProps) => {
    const { isLoggedIn } = useAuthStore();

    if (isLoggedIn) {
        // 신규 사용자면 설정 페이지로 리다이렉트
        const isNewUser = sessionStorage.getItem('isNewUser');
        if (isNewUser) {
            sessionStorage.removeItem('isNewUser');
            return <Navigate to="/setting" replace />;
        }
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
};
