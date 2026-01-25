import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import { PasswordResetModal } from './PasswordResetModal';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';

export const LoginPage = () => {
    const navigate = useNavigate();
    const login = useAuthStore((state) => state.login);

    // 폼 상태
    const [formData, setFormData] = useState({
        email: '',
        password: '',
    });
    const [rememberEmail, setRememberEmail] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    // 비밀번호 재설정 모달 상태
    const [showPasswordResetModal, setShowPasswordResetModal] = useState(false);

    // 저장된 이메일 불러오기
    useEffect(() => {
        const savedEmail = localStorage.getItem('rememberedEmail');
        if (savedEmail) {
            setFormData(prev => ({ ...prev, email: savedEmail }));
            setRememberEmail(true);
        }
    }, []);

    // 입력 핸들러
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        // 입력 시 에러 메시지 초기화
        if (errorMessage) setErrorMessage('');
    };

    // 일반 로그인 제출
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.email || !formData.password) {
            alert('이메일과 비밀번호를 입력해주세요.');
            return;
        }

        setIsLoading(true);

        try {
            console.log('[INFO] 일반 로그인 시도');
            const data = await authApi.login(formData.email, formData.password);

            // 아이디 기억 처리
            if (rememberEmail) {
                localStorage.setItem('rememberedEmail', formData.email);
            } else {
                localStorage.removeItem('rememberedEmail');
            }

            // 토큰 저장
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);

            // authStore 업데이트
            login({
                id: String(data.user.id),
                name: data.user.name,
                nickname: data.user.nickname || undefined,
                email: data.user.email,
                avatar: data.user.profileImage || undefined
            });

            console.log('[INFO] 로그인 성공!');
            navigate('/dashboard');
        } catch (error: any) {
            console.error('Login error:', error);

            if (error.response?.status === 401 || error.response?.status === 500) {
                setErrorMessage('이메일 또는 비밀번호가 올바르지 않습니다.');
            } else {
                setErrorMessage('로그인 중 오류가 발생했습니다. 다시 시도해주세요.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthLayout>
            <div className="form-header">
                <h3>로그인</h3>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
                {/* 에러 메시지 표시 */}
                {errorMessage && (
                    <div style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: '#fee2e2',
                        border: '1px solid #ef4444',
                        borderRadius: '8px',
                        marginBottom: '1rem',
                        color: '#dc2626',
                        fontSize: '0.875rem',
                        fontWeight: 500,
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem'
                    }}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" y1="8" x2="12" y2="12" />
                            <line x1="12" y1="16" x2="12.01" y2="16" />
                        </svg>
                        <span>{errorMessage}</span>
                    </div>
                )}

                <div className="input-group">
                    <label htmlFor="email">이메일</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="example@email.com"
                        required
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="password">비밀번호</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="••••••••"
                        required
                    />
                </div>

                <div className="flex-between" style={{ marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                    <label className="flex items-center gap-2 cursor-pointer">
                        <input
                            type="checkbox"
                            className="rounded-sm border-study-blue/30"
                            checked={rememberEmail}
                            onChange={(e) => setRememberEmail(e.target.checked)}
                        />
                        <span className="text-study-text/70">아이디 기억</span>
                    </label>
                    <button
                        type="button"
                        onClick={() => setShowPasswordResetModal(true)}
                        className="text-study-blue hover:underline bg-transparent border-none cursor-pointer"
                    >
                        비밀번호 찾기
                    </button>
                </div>

                <button
                    type="submit"
                    className="btn-primary"
                    disabled={isLoading}
                >
                    {isLoading ? '로그인 중...' : '로그인'}
                </button>

                <div className="auth-divider">
                    <span>또는</span>
                </div>

                {/* 소셜 로그인 버튼 그룹 - 동그란 아이콘 버튼 */}
                <div className="social-login-circle-group">
                    {/* 카카오 로그인 버튼 */}
                    <button
                        type="button"
                        className="social-circle-btn kakao"
                        onClick={async () => {
                            try {
                                console.log('[INFO] 카카오 로그인 시도');
                                sessionStorage.setItem('oauth_provider', 'kakao');
                                const { authUrl } = await authApi.getKakaoAuthUrl();
                                window.location.href = authUrl;
                            } catch (error) {
                                console.error('Failed to get Kakao Auth URL:', error);
                                alert('카카오 로그인 페이지를 불러오는데 실패했습니다.');
                            }
                        }}
                        title="카카오 로그인"
                    >
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                            <path d="M12 3C6.48 3 2 6.58 2 11c0 2.8 1.86 5.26 4.64 6.68-.15.56-.52 2.02-.6 2.33-.09.38.14.42.29.31.12-.09 1.94-1.32 2.73-1.86.56.08 1.13.12 1.94.12 5.52 0 10-3.58 10-8 0-4.42-4.48-8-10-8z" />
                        </svg>
                    </button>

                    {/* 구글 로그인 버튼 */}
                    <button
                        type="button"
                        className="social-circle-btn google"
                        onClick={async () => {
                            try {
                                console.log('[INFO] 구글 로그인 시도');
                                sessionStorage.setItem('oauth_provider', 'google');
                                const { authUrl } = await authApi.getGoogleAuthUrl();
                                window.location.href = authUrl;
                            } catch (error) {
                                console.error('Failed to get Google Auth URL:', error);
                                alert('구글 로그인 페이지를 불러오는데 실패했습니다.');
                            }
                        }}
                        title="구글 로그인"
                    >
                        <svg viewBox="0 0 24 24" width="20" height="20">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                        </svg>
                    </button>

                    {/* 네이버 로그인 버튼 */}
                    <button
                        type="button"
                        className="social-circle-btn naver"
                        onClick={async () => {
                            try {
                                console.log('[INFO] 네이버 로그인 시도');
                                sessionStorage.setItem('oauth_provider', 'naver');
                                const { authUrl } = await authApi.getNaverAuthUrl();
                                window.location.href = authUrl;
                            } catch (error) {
                                console.error('Failed to get Naver Auth URL:', error);
                                alert('네이버 로그인 페이지를 불러오는데 실패했습니다.');
                            }
                        }}
                        title="네이버 로그인"
                    >
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                            <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" />
                        </svg>
                    </button>
                </div>
            </form>

            <p style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                계정이 없으신가요?
                <br />
                소셜 인증 후 회원가입 가능합니다!
            </p>

            {/* 비밀번호 재설정 모달 */}
            <PasswordResetModal
                isOpen={showPasswordResetModal}
                onClose={() => setShowPasswordResetModal(false)}
            />
        </AuthLayout>
    );
};
