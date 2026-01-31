import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PasswordInput } from './PasswordInput';
import { OAuthTempData } from '../types';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import AuthLayout from './AuthLayout';
import '../styles/AuthLayout.css';

export const SignupPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const login = useAuthStore((state) => state.login);
    const showToast = useUIStore((state) => state.showToast);

    // OAuth 모드 감지
    const isOAuthMode = searchParams.get('oauth') === 'true';
    const [oauthData, setOauthData] = useState<OAuthTempData | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    // 폼 상태
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        nickname: '',
        password: '',
        confirmPassword: '',
    });

    // OAuth 데이터 로드
    useEffect(() => {
        if (isOAuthMode) {
            const tempData = localStorage.getItem('oauthTempData');
            if (tempData) {
                const data: OAuthTempData = JSON.parse(tempData);
                setOauthData(data);
                setFormData(prev => ({
                    ...prev,
                    email: data.email || '',
                    name: data.name || '',
                }));
            } else {
                showToast('잘못된 접근입니다.', 'error');
                navigate('/login');
            }
        }
    }, [isOAuthMode, navigate]);

    // 입력 핸들러
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    // 폼 제출
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (formData.password.length < 8) {
            showToast('비밀번호는 8자 이상이어야 합니다.', 'warning');
            return;
        }

        if (!/\d/.test(formData.password)) {
            showToast('비밀번호에 숫자를 포함해주세요.', 'warning');
            return;
        }

        if (!/[!@#$%^&*(),.?":{}|<>]/.test(formData.password)) {
            showToast('비밀번호에 특수문자를 포함해주세요.', 'warning');
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            showToast('비밀번호가 일치하지 않습니다.', 'warning');
            return;
        }

        if (!formData.nickname.trim()) {
            showToast('닉네임은 필수입니다.', 'warning');
            return;
        }

        setIsLoading(true);

        try {
            if (isOAuthMode && oauthData) {
                console.log('[INFO] OAuth 회원가입 완료 요청');

                localStorage.setItem('accessToken', oauthData?.accessToken || '');
                localStorage.setItem('refreshToken', oauthData?.refreshToken || '');

                const user = await authApi.setupProfile(
                    formData.name,
                    formData.nickname,
                    formData.password
                );

                login({
                    id: String(user.id),
                    name: user.name || formData.name,
                    nickname: user.nickname || formData.nickname,
                    email: user.email,
                    avatar: user.profileImage || undefined
                });

                localStorage.removeItem('oauthTempData');

                console.log('[INFO] 회원가입 완료!');
                showToast('회원가입이 완료되었습니다!', 'success');
                navigate('/dashboard');
            } else {
                console.log('[INFO] 일반 회원가입:', formData);
                showToast('회원가입이 완료되었습니다!', 'success');
                navigate('/login');
            }
        } catch (error) {
            console.error('Signup error:', error);
            showToast('회원가입 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthLayout>
            <div className="form-header">
                <h3>{isOAuthMode ? '추가 정보 입력' : '회원가입'}</h3>
                <p>
                    {isOAuthMode
                        ? '서비스 이용을 위해 추가 정보를 입력해주세요'
                        : 'SQUIZ 스터디 팀의 일원이 되어보세요'}
                </p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
                <div className="input-group">
                    <label htmlFor="name">이름</label>
                    <input
                        type="text"
                        id="name"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        placeholder="성함을 입력해주세요"
                        required
                    />
                    {isOAuthMode && (
                        <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-tertiary)', marginTop: '0.4rem' }}>
                            소셜 계정의 이름이 기본 입력되어 있습니다. 필요한 경우 수정해 주세요.
                        </p>
                    )}
                </div>

                <div className="input-group">
                    <label htmlFor="email">이메일</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="example@ssafy.com"
                        required
                        readOnly={isOAuthMode}
                        style={isOAuthMode ? {
                            backgroundColor: 'var(--color-background-tertiary)',
                            cursor: 'not-allowed'
                        } : {}}
                    />
                    {isOAuthMode && oauthData && (
                        <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-tertiary)', marginTop: '0.5rem' }}>
                            {oauthData?.loginProvider === 'KAKAO' && '카카오 계정의 이메일이 사용됩니다.'}
                            {oauthData?.loginProvider === 'NAVER' && '네이버 계정의 이메일이 사용됩니다.'}
                            {oauthData?.loginProvider === 'GOOGLE' && '구글 계정의 이메일이 사용됩니다.'}
                        </p>
                    )}
                </div>

                <div className="input-group">
                    <label htmlFor="nickname">닉네임</label>
                    <input
                        type="text"
                        id="nickname"
                        name="nickname"
                        value={formData.nickname}
                        onChange={handleChange}
                        placeholder="사용하실 닉네임을 입력해주세요"
                        maxLength={50}
                        required
                    />
                    <p style={{ fontSize: '0.75rem', color: 'var(--color-text-tertiary)', marginTop: '0.25rem' }}>
                        {formData.nickname.length}/50자
                    </p>
                </div>

                <PasswordInput
                    password={formData.password}
                    confirmPassword={formData.confirmPassword}
                    onPasswordChange={(password: string) => setFormData(prev => ({ ...prev, password }))}
                    onConfirmPasswordChange={(confirmPassword: string) => setFormData(prev => ({ ...prev, confirmPassword }))}
                />

                <button
                    type="submit"
                    className="btn-primary"
                    style={{ marginTop: '1rem' }}
                    disabled={isLoading}
                >
                    {isLoading ? '처리 중...' : (isOAuthMode ? '가입 완료' : '가입하기')}
                </button>
            </form>

            {!isOAuthMode && (
                <p style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                    이미 계정이 있으신가요? <a href="/login" className="text-study-blue font-bold hover:underline">로그인</a>
                </p>
            )}
        </AuthLayout>
    );
};
