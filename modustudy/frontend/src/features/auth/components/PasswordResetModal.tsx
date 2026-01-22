import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';

interface PasswordResetModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const PasswordResetModal = ({ isOpen, onClose }: PasswordResetModalProps) => {
    const { user } = useAuthStore();
    const [resetEmail, setResetEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

    // 로그인된 사용자의 경우 이메일 자동 설정
    useEffect(() => {
        if (isOpen && user?.email) {
            setResetEmail(user.email);
        }
    }, [isOpen, user]);

    // 비밀번호 재설정 요청
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!resetEmail) {
            setMessage({ type: 'error', text: '이메일을 입력해주세요.' });
            return;
        }

        setIsLoading(true);
        setMessage(null);

        try {
            await authApi.requestPasswordReset(resetEmail);

            setMessage({
                type: 'success',
                text: '성공적으로 요청되었습니다! 잠시 후에도 메일이 오지 않는다면 스팸함도 확인해주세요.'
            });

            // 성공 시 입력창 초기화 및 3초 후 닫기
            setResetEmail('');
            setTimeout(() => {
                handleClose();
            }, 5000);
        } catch (error: any) {
            console.error('Password reset error:', error);
            setMessage({
                type: 'error',
                text: error.response?.data?.message || '비밀번호 재설정 요청에 실패했습니다. 다시 시도해주세요.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    // 모달 닫기
    const handleClose = () => {
        setResetEmail('');
        setMessage(null);
        onClose();
    };

    if (!isOpen) return null;

    const modalContent = (
        <div
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(0, 0, 0, 0.6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 9999,
                animation: 'fadeIn 0.2s ease'
            }}
            onClick={handleClose}
        >
            <div
                style={{
                    background: 'white',
                    borderRadius: '16px',
                    width: '90%',
                    maxWidth: '400px',
                    padding: '2rem',
                    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
                    animation: 'slideUp 0.3s ease'
                }}
                onClick={(e) => e.stopPropagation()}
            >
                <div style={{ marginBottom: '1.5rem' }}>
                    <h3 style={{ margin: 0, fontSize: '1.5rem', fontWeight: 800, color: '#1a202c' }}>
                        비밀번호 재설정
                    </h3>
                    <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.875rem', color: '#718096' }}>
                        가입하신 이메일을 입력하시면 임시 비밀번호를 발송해드립니다.
                    </p>
                </div>

                <form onSubmit={handleSubmit}>
                    {message && (
                        <div style={{
                            padding: '0.75rem 1rem',
                            backgroundColor: message.type === 'success' ? '#d1fae5' : '#fee2e2',
                            border: `1px solid ${message.type === 'success' ? '#10b981' : '#ef4444'}`,
                            borderRadius: '8px',
                            marginBottom: '1rem',
                            color: message.type === 'success' ? '#065f46' : '#dc2626',
                            fontSize: '0.875rem',
                            fontWeight: 500
                        }}>
                            {message.text}
                        </div>
                    )}

                    <div className="input-group" style={{ marginBottom: '1.5rem' }}>
                        <label htmlFor="reset-email">이메일</label>
                        <input
                            type="email"
                            id="reset-email"
                            value={resetEmail}
                            onChange={(e) => setResetEmail(e.target.value)}
                            placeholder="example@email.com"
                            required
                            style={{ width: '100%' }}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '0.75rem' }}>
                        <button
                            type="button"
                            onClick={handleClose}
                            className="btn-primary"
                            style={{
                                flex: 1,
                                background: 'white',
                                color: '#4a5568',
                                border: '1px solid #e2e8f0',
                                whiteSpace: 'nowrap',
                                fontSize: '0.875rem'
                            }}
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="btn-primary"
                            style={{
                                flex: 1,
                                whiteSpace: 'nowrap',
                                fontSize: '0.875rem'
                            }}
                        >
                            {isLoading ? '전송 중...' : '발송'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );

    // Portal을 사용하여 body에 직접 렌더링
    return createPortal(modalContent, document.body);
};
