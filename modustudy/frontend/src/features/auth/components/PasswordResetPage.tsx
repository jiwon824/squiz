import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import { authApi } from '@/api/endpoints/authApi';
import { PasswordInput } from './PasswordInput';
import { getErrorMessage } from '@/shared/utils/errorUtils';

export const PasswordResetPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

    useEffect(() => {
        if (!token) {
            setMessage({ type: 'error', text: '유효하지 않은 접근입니다. 메일에 포함된 최신 링크를 다시 확인해주세요.' });
        }
    }, [token]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // 실시간 검증 로직은 PasswordInput에서 수행되지만, 제출 시 최종 확인
        const hasNumber = /\d/.test(password);
        const hasSpecialChar = /[!@#$%^&*(),?":{}|<>]/.test(password);

        if (password.length < 8 || !hasNumber || !hasSpecialChar) {
            setMessage({ type: 'error', text: '비밀번호를 규격(8자 이상, 숫자 및 특수문자 포함)에 맞게 입력해주세요.' });
            return;
        }

        if (password !== confirmPassword) {
            setMessage({ type: 'error', text: '비밀번호가 일치하지 않습니다.' });
            return;
        }

        setIsLoading(true);
        setMessage(null);

        try {
            await authApi.resetPassword(token || '', password);
            setMessage({ type: 'success', text: '비밀번호가 성공적으로 변경되었습니다. 잠시 후 로그인 페이지로 이동합니다.' });
            setTimeout(() => {
                navigate('/login');
            }, 3000);
        } catch (error: unknown) {
            console.error('Password reset error:', error);
            setMessage({
                type: 'error',
                text: getErrorMessage(error, '비밀번호 변경에 실패했습니다. 링크가 만료되었을 수 있습니다.')
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthLayout>
            <div className="form-header">
                <h3 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--color-text-primary)' }}>비밀번호 재설정</h3>
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
                    안전한 학습을 위해 보안 등급이 높은 새 비밀번호를 설정해주세요.
                </p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit} style={{ marginTop: '2rem' }}>
                {message && (
                    <div style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: message.type === 'success' ? '#d1fae5' : '#fee2e2',
                        border: `1px solid ${message.type === 'success' ? '#10b981' : '#ef4444'}`,
                        borderRadius: '12px',
                        marginBottom: '1.5rem',
                        color: message.type === 'success' ? '#065f46' : '#dc2626',
                        fontSize: '0.875rem',
                        fontWeight: 500,
                        lineHeight: '1.5'
                    }}>
                        {message.text}
                    </div>
                )}

                {!token || message?.type === 'error' ? (
                    <button
                        type="button"
                        onClick={() => navigate('/login')}
                        className="btn-primary"
                        style={{
                            width: '100%',
                            background: 'white',
                            color: 'var(--color-primary)',
                            border: '1px solid var(--color-primary)'
                        }}
                    >
                        다시 요청하기 (로그인 페이지로)
                    </button>
                ) : (
                    <>
                        <PasswordInput
                            password={password}
                            confirmPassword={confirmPassword}
                            onPasswordChange={setPassword}
                            onConfirmPasswordChange={setConfirmPassword}
                        />

                        <button
                            type="submit"
                            className="btn-primary"
                            disabled={isLoading || (message?.type === 'success')}
                            style={{ marginTop: '2rem', width: '100%' }}
                        >
                            {isLoading ? '변경 중...' : '확인 및 변경하기'}
                        </button>
                    </>
                )}
            </form>

            <div style={{ marginTop: '3rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                혹시 로그인 정보를 알고 계신가요?
                <br />
                <span
                    onClick={() => navigate('/login')}
                    style={{
                        display: 'inline-block',
                        marginTop: '0.5rem',
                        color: 'var(--color-primary)',
                        cursor: 'pointer',
                        fontWeight: 700,
                        textDecoration: 'underline'
                    }}
                >
                    로그인 페이지로 돌아가기
                </span>
            </div>
        </AuthLayout>
    );
};
