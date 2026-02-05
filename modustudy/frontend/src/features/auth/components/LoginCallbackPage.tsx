import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { useSettingStore } from '@/features/setting/store/settingStore';
import type { SocialProvider } from '@/features/setting/types';
import AuthLayout from './AuthLayout';
import { Spinner } from '@/shared/components/Spinner';

export const LoginCallbackPage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const login = useAuthStore((state) => state.login);
    const showToast = useUIStore((state) => state.showToast);
    const { completeSocialLink, refetchSocialAccounts } = useSettingStore();

    // 연동 모드인지 확인하는 상태
    const [isLinkMode, setIsLinkMode] = useState(false);
    const [isExiting, setIsExiting] = useState(false);

    // exit 애니메이션 후 페이지 이동
    const navigateWithExit = (path: string) => {
        setIsExiting(true);
        setTimeout(() => navigate(path, { replace: true }), 500);
    };

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
                console.log('[INFO] 이미 처리 중입니다.');
                return;
            }
            isProcessingRef.current = true;

            const oauthMode = sessionStorage.getItem('oauth_mode');
            const provider = sessionStorage.getItem('oauth_provider') || 'kakao';
            const redirectPath = sessionStorage.getItem('oauth_redirect_path');

            // 연동 모드인 경우 (기존 로그인 상태에서 추가 계정 연동)
            if (oauthMode === 'link') {
                setIsLinkMode(true);
                console.log(`[INFO] ${provider} 계정 연동 처리 시작`);

                try {
                    await completeSocialLink(
                        provider.toUpperCase() as SocialProvider,
                        code
                    );

                    // 연동 성공 후 소셜 계정 목록 새로고침
                    await refetchSocialAccounts();

                    showToast(`${provider === 'kakao' ? '카카오' : provider === 'naver' ? '네이버' : '구글'} 계정이 연동되었습니다.`, 'success');

                    // 세션 스토리지 정리
                    sessionStorage.removeItem('oauth_mode');
                    sessionStorage.removeItem('oauth_provider');
                    sessionStorage.removeItem('oauth_redirect_path');

                    navigateWithExit(redirectPath || '/setting');
                } catch (error: any) {
                    console.error('Social link error:', error);
                    isProcessingRef.current = false;

                    // 에러 메시지 처리
                    const errorMessage = error.response?.data?.error?.message
                        || error.message
                        || '계정 연동에 실패했습니다.';
                    showToast(errorMessage, 'error');

                    // 세션 스토리지 정리
                    sessionStorage.removeItem('oauth_mode');
                    sessionStorage.removeItem('oauth_provider');
                    sessionStorage.removeItem('oauth_redirect_path');

                    navigate(redirectPath || '/setting', { replace: true });
                }
                return;
            }

            // 일반 로그인 모드
            try {
                const provider = sessionStorage.getItem('oauth_provider') || 'kakao'; // 기본값은 kakao (하위 호환)
                const oauthAction = sessionStorage.getItem('oauth_action'); // 'link' = 계정 연동, null = 로그인
                console.log(`[INFO] ${provider} ${oauthAction === 'link' ? '계정 연동' : '로그인'} 처리 시작`);

                // 이미 로그인된 상태에서 Google 계정 연동하는 경우 (캘린더 연동)
                if (oauthAction === 'link' && provider === 'google') {
                    console.log('[INFO] Google 계정 연동 (기존 사용자에게 추가)');
                    try {
                        await authApi.linkGoogleAccount(code);
                        console.log('[INFO] Google 계정 연동 성공!');

                        // 연동 후 원래 페이지로 복귀
                        const redirectUrl = sessionStorage.getItem('oauth_redirect_path') || '/calendar';
                        sessionStorage.removeItem('oauth_action');
                        sessionStorage.removeItem('oauth_provider');
                        sessionStorage.removeItem('oauth_redirect_path');

                        showToast('Google Calendar가 연동되었습니다.', 'success');
                        navigateWithExit(redirectUrl);
                        return;
                    } catch (linkError: any) {
                        console.error('Google 계정 연동 실패:', linkError);
                        showToast(linkError?.response?.data?.error?.message || 'Google 계정 연동에 실패했습니다.', 'error');

                        const redirectUrl = sessionStorage.getItem('oauth_redirect_path') || '/calendar';
                        sessionStorage.removeItem('oauth_action');
                        sessionStorage.removeItem('oauth_provider');
                        sessionStorage.removeItem('oauth_redirect_path');

                        navigate(redirectUrl, { replace: true });
                        return;
                    }
                }
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

                    navigateWithExit('/signup?oauth=true');
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
                        loginProvider: provider.toUpperCase() as 'KAKAO' | 'GOOGLE' | 'NAVER',
                        role: data.user.role as 'USER' | 'ADMIN' || 'USER'
                    });

                    console.log('[INFO] 기존 소셜 유저 로그인 성공!');

                    // 로그인 전 페이지로 리다이렉트 (저장된 URL이 있으면)
                    const loginRedirectUrl = sessionStorage.getItem('redirectAfterLogin')
                        || sessionStorage.getItem('oauth_redirect_path');
                    if (loginRedirectUrl) {
                        sessionStorage.removeItem('redirectAfterLogin');
                        sessionStorage.removeItem('oauth_redirect_path');
                    }

                    navigateWithExit(loginRedirectUrl || '/dashboard');
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
    }, [searchParams, navigate, login, completeSocialLink, refetchSocialAccounts, showToast]);

    return (
        <AuthLayout pageState={isExiting ? 'page-exit' : ''}>
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '300px',
                textAlign: 'center'
            }}>
                <Spinner size="xl" />
                <h3 style={{ marginTop: '1.5rem', fontWeight: 700 }}>
                    {isLinkMode ? '계정 연동 중입니다' : '로그인 중입니다'}
                </h3>
                <p style={{ color: '#64748b', marginTop: '0.5rem' }}>잠시만 기다려 주세요...</p>
            </div>
        </AuthLayout>
    );
};
