import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import { authApi } from '@/api/endpoints/authApi';

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
            setMessage({ type: 'error', text: '유효하지 않은 접근입니다. 메일의 링크를 다시 확인해주세요.' });
        }
    }, [token]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (password !== confirmPassword) {
            setMessage({ type: 'error', text: '비밀번호가 일치하지 않습니다.' });
            return;
        }

        if (password.length < 8) {
            setMessage({ type: 'error', text: '비밀번호는 8자 이상이어야 합니다.' });
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
        } catch (error: any) {
            console.error('Password reset error:', error);
            setMessage({
                type: 'error',
                text: error.response?.data?.message || '비밀번호 변경에 실패했습니다. 토큰이 만료되었을 수 있습니다.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthLayout>
            <div className="form-header">
                <h3>비밀번호 재설정</h3>
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
                    새로운 비밀번호를 입력해주세요.
                </p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
                {message && (
                    <div style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: message.type === 'success' ? '#d1fae5' : '#fee2e2',
                        border: `1px solid ${message.type === 'success' ? '#10b981' : '#ef4444'}`,
                        borderRadius: '8px',
                        marginBottom: '1rem',
                        color: message.type === 'success' ? '#065f46' : '#dc2626',
                        fontSize: '0.875rem',
                        fontWeight: 500,
                        lineHeight: '1.4'
                    }}>
                        {message.text}
                    </div>
                )}

                <div className="input-group">
                    <label htmlFor="password">새 비밀번호</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="8자 이상 입력"
                        required
                        disabled={!token || (message?.type === 'success')}
                        autoComplete="new-password"
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="confirmPassword">비밀번호 확인</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="비밀번호 재입력"
                        required
                        disabled={!token || (message?.type === 'success')}
                        autoComplete="new-password"
                    />
                </div>

                <button
                    type="submit"
                    className="btn-primary"
                    disabled={isLoading || !token || (message?.type === 'success')}
                    style={{ marginTop: '1.5rem' }}
                >
                    {isLoading ? '변경 중...' : '비밀번호 변경'}
                </button>
            </form>

            <p style={{ marginTop: '2.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                기존 비밀번호가 생각나셨나요?
                <br />
                <span
                    onClick={() => navigate('/login')}
                    style={{
                        color: 'var(--color-primary)',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        marginLeft: '5px',
                        textDecoration: 'underline'
                    }}
                >
                    로그인하러 가기
                </span>
            </p>
        </AuthLayout>
    );
};
