import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import AuthLayout from './AuthLayout';
import { Loader2 } from 'lucide-react';

export const LoginCallbackPage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const login = useAuthStore((state) => state.login);
    const showToast = useUIStore((state) => state.showToast);

    // 중복 요청 방지를 위한 ref (React StrictMode에서 useEffect 두 번 실행 대응)
    const isProcessingRef = useRef(false);

    useEffect(() => {
        const handleCallback = async () => {
            const code = searchParams.get('code');

            // 인가 코드가 없으면 로그인 페이지로
            if (!code) {
                console.error('No authorization code found in URL');
                navigate('/login', { replace: true });
                return;
            }

            // 이미 처리 중이면 중복 요청 방지
            if (isProcessingRef.current) {
                console.log('[INFO] 이미 로그인 처리 중입니다.');
                return;
            }
            isProcessingRef.current = true;

            try {
                const provider = sessionStorage.getItem('oauth_provider') || 'kakao'; // 기본값은 kakao (하위 호환)
                console.log(`[INFO] ${provider} 로그인 처리 시작`);

                let data;
                if (provider === 'naver') {
                    const state = searchParams.get('state') || '';
                    data = await authApi.handleNaverCallback(code, state);
                } else if (provider === 'google') {
                    data = await authApi.handleGoogleCallback(code);
                } else {
                    data = await authApi.handleKakaoCallback(code);
                }

                console.log('[DEBUG] Server Response Data:', data);

                // authStore 업데이트 (이 과정은 로그인이 완료된 경우에만 의미가 있음)
                // 신규 유저인 경우 아직 닉네임 등이 없어 추가 정보 입력이 필요함
                if (data.isNewUser) {
                    console.log('[INFO] 신규 소셜 유저 - 추가 정보 입력 페이지로 이동');

                    // 임시 데이터 저장 (SignupPage에서 사용)
                    localStorage.setItem('oauthTempData', JSON.stringify({
                        accessToken: data.accessToken,
                        refreshToken: data.refreshToken,
                        email: data.user.email,
                        name: data.user.name,
                        loginProvider: provider.toUpperCase()
                    }));

                    navigate('/signup?oauth=true', { replace: true });
                } else {
                    // 기존 유저인 경우 바로 로그인 처리 및 대시보드 이동
                    localStorage.setItem('accessToken', data.accessToken);
                    localStorage.setItem('refreshToken', data.refreshToken);

                    login({
                        id: String(data.user.id),
                        name: data.user.name,
                        nickname: data.user.nickname || undefined,
                        email: data.user.email,
                        avatar: data.user.profileImage || undefined,
                        loginProvider: provider.toUpperCase() as 'KAKAO' | 'GOOGLE' | 'NAVER'
                    });

                    console.log('[INFO] 기존 소셜 유저 로그인 성공!');

                    // 로그인 전 페이지로 리다이렉트 (저장된 URL이 있으면)
                    const redirectUrl = sessionStorage.getItem('redirectAfterLogin');
                    if (redirectUrl) {
                        sessionStorage.removeItem('redirectAfterLogin');
                        navigate(redirectUrl, { replace: true });
                    } else {
                        navigate('/dashboard', { replace: true });
                    }
                }

                // 처리 완료 후 provider 삭제
                sessionStorage.removeItem('oauth_provider');
            } catch (error) {
                console.error('Login callback error:', error);

                // 에러 발생 시 플래그 리셋 (재시도 가능하도록)
                isProcessingRef.current = false;

                showToast('로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
                navigate('/login', { replace: true });
            }
        };

        handleCallback();
    }, [searchParams, navigate, login]);

    return (
        <AuthLayout hideBranding>
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '300px',
                textAlign: 'center'
            }}>
                <Loader2 className="animate-spin" size={48} color="var(--color-primary)" />
                <h3 style={{ marginTop: '1.5rem', fontWeight: 700 }}>로그인 중입니다</h3>
                <p style={{ color: '#64748b', marginTop: '0.5rem' }}>잠시만 기다려 주세요...</p>
            </div>
        </AuthLayout>
    );
};
