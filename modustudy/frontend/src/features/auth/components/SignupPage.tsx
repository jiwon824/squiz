import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PasswordInput } from './PasswordInput';
import { TermsModal } from './TermsModal';
import { OAuthTempData } from '../types';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import AuthLayout from './AuthLayout';
import '../styles/AuthLayout.css';

// md 파일을 raw text로 import
import termsText from '../terms-of-use.md?raw';
import privacyText from '../privacy-policy.md?raw';

export const SignupPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const login = useAuthStore((state) => state.login);
    const showToast = useUIStore((state) => state.showToast);

    // OAuth 모드 감지
    const isOAuthMode = searchParams.get('oauth') === 'true';
    const [oauthData, setOauthData] = useState<OAuthTempData | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    // 약관 동의 상태
    const [agreeTerms, setAgreeTerms] = useState(false);
    const [agreePrivacy, setAgreePrivacy] = useState(false);

    // 약관 모달 상태
    const [modalContent, setModalContent] = useState<{ title: string; content: string; type: 'terms' | 'privacy' } | null>(null);

    // 입력 상태
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
    }, [isOAuthMode, navigate, showToast]);

    // 입력 핸들러
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    // 가입 제출
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // 약관 동의 확인
        if (!agreeTerms || !agreePrivacy) {
            showToast('서비스 이용약관과 개인정보처리방침에 동의해주세요.', 'warning');
            return;
        }

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
                showToast('회원가입이 완료되었습니다. 스터디 선호 설정을 완료해주세요.', 'success');

                // 스터디 선호 설정 페이지로 강제 이동
                window.location.href = '/setting?section=study';
            } else {
                console.log('[INFO] 일반 회원가입', formData);
                showToast('회원가입이 완료되었습니다.', 'success');
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
                        ? '서비스 이용을 위해 추가 정보를 입력해주세요.'
                        : 'SQUIZ 스터디 커뮤니티에 참여해보세요.'}
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
                        placeholder="이름을 입력해주세요"
                        required
                    />
                    {isOAuthMode && (
                        <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-tertiary)', marginTop: '0.4rem' }}>
                            소셜 계정 이름이 기본 입력되어 있습니다. 필요 시 수정해주세요.
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
                            {oauthData?.loginProvider === 'KAKAO' && '카카오 계정 이메일을 사용합니다.'}
                            {oauthData?.loginProvider === 'NAVER' && '네이버 계정 이메일을 사용합니다.'}
                            {oauthData?.loginProvider === 'GOOGLE' && '구글 계정 이메일을 사용합니다.'}
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
                        placeholder="사용할 닉네임을 입력해주세요"
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

                {/* 약관 동의 영역 */}
                <div className="terms-agreement">
                    <div className="terms-agreement-item">
                        <input
                            type="checkbox"
                            id="agreeTerms"
                            checked={agreeTerms}
                            onChange={(e) => setAgreeTerms(e.target.checked)}
                        />
                        <label htmlFor="agreeTerms">
                            서비스 이용약관에 동의합니다 (필수)
                        </label>
                        <button
                            type="button"
                            className="terms-view-btn"
                            onClick={() => setModalContent({ title: '서비스 이용약관', content: termsText, type: 'terms' })}
                        >
                            보기
                        </button>
                    </div>
                    <div className="terms-agreement-item">
                        <input
                            type="checkbox"
                            id="agreePrivacy"
                            checked={agreePrivacy}
                            onChange={(e) => setAgreePrivacy(e.target.checked)}
                        />
                        <label htmlFor="agreePrivacy">
                            개인정보처리방침에 동의합니다 (필수)
                        </label>
                        <button
                            type="button"
                            className="terms-view-btn"
                            onClick={() => setModalContent({ title: '개인정보처리방침', content: privacyText, type: 'privacy' })}
                        >
                            보기
                        </button>
                    </div>
                </div>

                <button
                    type="submit"
                    className="btn-primary"
                    style={{ marginTop: '1rem' }}
                    disabled={isLoading || !agreeTerms || !agreePrivacy}
                >
                    {isLoading ? '처리 중...' : (isOAuthMode ? '가입 완료' : '가입하기')}
                </button>
            </form>

            {!isOAuthMode && (
                <p style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                    이미 계정이 있으신가요? <a href="/login" className="text-study-blue font-bold hover:underline">로그인</a>
                </p>
            )}

            {/* 약관 모달 */}
            {modalContent && (
                <TermsModal
                    title={modalContent.title}
                    content={modalContent.content}
                    onClose={() => setModalContent(null)}
                />
            )}
        </AuthLayout>
    );
};
