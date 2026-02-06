import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Lock } from 'lucide-react';
import AuthLayout from './AuthLayout';
import { PasswordResetModal } from './PasswordResetModal';
import { TermsModal } from './TermsModal';
import { IconInput } from '@/shared/components';
import { cn, classBuilder } from '@/shared/utils/cn';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import '../styles/AuthLayout.css';

// md 파일을 raw text로 import
import termsText from '../terms-of-use.md?raw';
import privacyText from '../privacy-policy.md?raw';
import { getErrorStatus } from '@/shared/utils/errorUtils';

export const LoginPage = () => {
    const navigate = useNavigate();
    const login = useAuthStore((state) => state.login);
    const showToast = useUIStore((state) => state.showToast);

    // 폼 상태
    const [formData, setFormData] = useState({
        email: '',
        password: '',
    });
    const [rememberEmail, setRememberEmail] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [isExiting, setIsExiting] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    // 필드별 유효성 검사 에러
    const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

    // 비밀번호 재설정 모달 상태
    const [showPasswordResetModal, setShowPasswordResetModal] = useState(false);

    // 약관 모달 상태
    const [termsModalContent, setTermsModalContent] = useState<{ title: string; content: string } | null>(null);

    // 저장된 이메일 불러오기
    useEffect(() => {
        const savedEmail = localStorage.getItem('rememberedEmail');
        if (savedEmail) {
            setFormData(prev => ({ ...prev, email: savedEmail }));
            setRememberEmail(true);
        }
    }, []);

    // 이메일 형식 검증
    const isValidEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

    // 입력 핸들러
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        // 입력 시 해당 필드 에러 초기화
        if (fieldErrors[name as keyof typeof fieldErrors]) {
            setFieldErrors(prev => ({ ...prev, [name]: undefined }));
        }
        if (errorMessage) setErrorMessage('');
    };

    // 일반 로그인 제출
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // 필드별 유효성 검사
        const errors: { email?: string; password?: string } = {};
        if (!formData.email) {
            errors.email = '이메일을 입력해주세요.';
        } else if (!isValidEmail(formData.email)) {
            errors.email = '올바른 이메일 형식을 입력해주세요.';
        }
        if (!formData.password) {
            errors.password = '비밀번호를 입력해주세요.';
        }

        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            return;
        }

        setIsLoading(true);

        try {
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
                avatar: data.user.profileImage || undefined,
                loginProvider: data.user.loginProvider || undefined,
                role: data.user.role || 'USER'
            });

            // exit 애니메이션 후 페이지 이동
            const redirectUrl = sessionStorage.getItem('redirectAfterLogin');
            if (redirectUrl) sessionStorage.removeItem('redirectAfterLogin');

            setIsExiting(true);
            setTimeout(() => navigate(redirectUrl || '/dashboard'), 500);
        } catch (error: unknown) {
            console.error('Login error:', error);

            if (getErrorStatus(error) === 401 || getErrorStatus(error) === 500) {
                setErrorMessage('이메일 또는 비밀번호가 올바르지 않습니다.');
            } else {
                setErrorMessage('로그인 중 오류가 발생했습니다. 다시 시도해주세요.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthLayout pageState={isExiting ? 'page-exit' : ''}>
            {/* 헤더 - 하단 간격 통일 */}
            <div className="form-header" style={{ marginBottom: '2rem' }}>
                <h3>로그인</h3>
            </div>

            <form className="auth-form" onSubmit={handleSubmit} noValidate>
                {/* 에러 메시지 */}
                {errorMessage && (
                    <div className="flex items-center gap-2 px-4 py-3 mb-5 rounded-xl bg-red-50 text-red-600 text-sm font-medium">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="flex-shrink-0">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" y1="8" x2="12" y2="12" />
                            <line x1="12" y1="16" x2="12.01" y2="16" />
                        </svg>
                        <span>{errorMessage}</span>
                    </div>
                )}

                {/* 입력 필드 영역 - 이메일/비밀번호 밀착 배치 */}
                <div className="flex flex-col gap-3">
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="email" className="text-sm font-semibold text-slate-700 ml-1">이메일</label>
                        <IconInput
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="example@email.com"
                            leftIcon={<Mail />}
                            error={fieldErrors.email}
                        />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="password" className="text-sm font-semibold text-slate-700 ml-1">비밀번호</label>
                        <IconInput
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder="••••••••"
                            leftIcon={<Lock />}
                            error={fieldErrors.password}
                        />
                    </div>
                </div>

                {/* 옵션 행 - 입력 필드와 간격 확보 */}
                <div className="flex items-center justify-between mt-5 mb-6">
                    <label className="flex items-center gap-2.5 cursor-pointer select-none group">
                        <button
                            type="button"
                            role="switch"
                            aria-checked={rememberEmail}
                            onClick={() => setRememberEmail(!rememberEmail)}
                            className={cn(
                                'relative w-9 h-5 rounded-full transition-colors duration-300 flex-shrink-0',
                                rememberEmail ? 'bg-[var(--color-primary)]' : 'bg-gray-300',
                            )}
                        >
                            <span
                                className={cn(
                                    'absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow-sm',
                                    'transition-transform duration-300 ease-out',
                                    rememberEmail && 'translate-x-4',
                                )}
                            />
                        </button>
                        <span className={cn(
                            'text-sm font-medium transition-colors duration-200',
                            rememberEmail ? 'text-[var(--color-primary)]' : 'text-gray-400',
                        )}>
                            아이디 기억
                        </span>
                    </label>
                    <button
                        type="button"
                        onClick={() => setShowPasswordResetModal(true)}
                        className="text-sm text-gray-400 hover:text-[var(--color-primary)] transition-colors duration-200 bg-transparent border-none cursor-pointer"
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

                {/* 구분선 - CSS margin 그대로 사용 (1.5rem 0) */}
                <div className="auth-divider">
                    <span>또는</span>
                </div>

                {/* 소셜 로그인 버튼 그룹 */}
                <div className="social-login-circle-group" style={{ marginTop: 0 }}>
                    {/* 구글 */}
                    <div className="relative group">
                        <button
                            type="button"
                            className="social-circle-btn google"
                            onClick={async () => {
                                try {
                                    sessionStorage.setItem('oauth_provider', 'google');
                                    const { authUrl } = await authApi.getGoogleAuthUrl();
                                    window.location.href = authUrl;
                                } catch (error) {
                                    console.error('Failed to get Google Auth URL:', error);
                                    showToast('구글 로그인 페이지를 불러오는데 실패했습니다.', 'error');
                                }
                            }}
                            aria-label="구글 로그인"
                        >
                            <svg viewBox="0 0 24 24" width="20" height="20">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                            </svg>
                        </button>
                        <span className={classBuilder.tooltip('bottom')}>Google</span>
                    </div>

                    {/* 카카오 */}
                    <div className="relative group">
                        <button
                            type="button"
                            className="social-circle-btn kakao"
                            onClick={async () => {
                                try {
                                    sessionStorage.setItem('oauth_provider', 'kakao');
                                    const { authUrl } = await authApi.getKakaoAuthUrl();
                                    window.location.href = authUrl;
                                } catch (error) {
                                    console.error('Failed to get Kakao Auth URL:', error);
                                    showToast('카카오 로그인 페이지를 불러오는데 실패했습니다.', 'error');
                                }
                            }}
                            aria-label="카카오 로그인"
                        >
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                                <path d="M12 3C6.48 3 2 6.58 2 11c0 2.8 1.86 5.26 4.64 6.68-.15.56-.52 2.02-.6 2.33-.09.38.14.42.29.31.12-.09 1.94-1.32 2.73-1.86.56.08 1.13.12 1.94.12 5.52 0 10-3.58 10-8 0-4.42-4.48-8-10-8z" />
                            </svg>
                        </button>
                        <span className={classBuilder.tooltip('bottom')}>Kakao</span>
                    </div>

                    {/* 네이버 */}
                    <div className="relative group">
                        <button
                            type="button"
                            className="social-circle-btn naver"
                            onClick={async () => {
                                try {
                                    sessionStorage.setItem('oauth_provider', 'naver');
                                    const { authUrl } = await authApi.getNaverAuthUrl();
                                    window.location.href = authUrl;
                                } catch (error) {
                                    console.error('Failed to get Naver Auth URL:', error);
                                    showToast('네이버 로그인 페이지를 불러오는데 실패했습니다.', 'error');
                                }
                            }}
                            aria-label="네이버 로그인"
                        >
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                                <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" />
                            </svg>
                        </button>
                        <span className={classBuilder.tooltip('bottom')}>Naver</span>
                    </div>
                </div>
            </form>

            {/* 하단 안내 텍스트 */}
            <p className="mt-6 text-center text-sm text-gray-500">
                계정이 없으신가요?
                <br />
                <span className="text-gray-400">소셜 인증 후 회원가입 가능합니다!</span>
            </p>

            {/* 약관 링크 */}
            <div className="terms-links" style={{ marginTop: '1rem' }}>
                <button
                    type="button"
                    onClick={() => setTermsModalContent({ title: '서비스 이용약관', content: termsText })}
                >
                    이용약관
                </button>
                <span className="terms-links-divider">|</span>
                <button
                    type="button"
                    onClick={() => setTermsModalContent({ title: '개인정보처리방침', content: privacyText })}
                >
                    개인정보 보호 정책
                </button>
            </div>

            {/* 비밀번호 재설정 모달 */}
            <PasswordResetModal
                isOpen={showPasswordResetModal}
                onClose={() => setShowPasswordResetModal(false)}
            />

            {/* 약관 모달 */}
            {termsModalContent && (
                <TermsModal
                    title={termsModalContent.title}
                    content={termsModalContent.content}
                    onClose={() => setTermsModalContent(null)}
                />
            )}
        </AuthLayout>
    );
};
