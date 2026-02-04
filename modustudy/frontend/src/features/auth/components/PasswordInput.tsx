import { useState, useEffect } from 'react';
import { Eye, EyeOff, Check, X } from 'lucide-react';

interface PasswordInputProps {
    password: string;
    confirmPassword: string;
    onPasswordChange: (password: string) => void;
    onConfirmPasswordChange: (confirmPassword: string) => void;
}

/**
 * 비밀번호 입력 컴포넌트
 * 비밀번호 + 비밀번호 확인 + 실시간 유효성 검사
 */
export const PasswordInput = ({
    password,
    confirmPassword,
    onPasswordChange,
    onConfirmPasswordChange,
}: PasswordInputProps) => {
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [validations, setValidations] = useState({
        minLength: false,
        hasNumber: false,
        hasSpecialChar: false,
    });
    const [passwordsMatch, setPasswordsMatch] = useState(true);

    // 鍮꾨?踰덊샇 ?좏슚??寃??
    useEffect(() => {
        setValidations({
            minLength: password.length >= 8,
            hasNumber: /\d/.test(password),
            hasSpecialChar: /[!@#$%^&*(),.?":{}|<>]/.test(password),
        });
    }, [password]);

    // 鍮꾨?踰덊샇 ?쇱튂 ?뺤씤
    useEffect(() => {
        if (confirmPassword.length > 0) {
            setPasswordsMatch(password === confirmPassword);
        } else {
            setPasswordsMatch(true);
        }
    }, [password, confirmPassword]);

    return (
        <div className="password-input-group">
            {/* 비밀번호 입력 */}
            <div className="input-group">
                <label htmlFor="password">비밀번호</label>
                <div className="password-field">
                    <input
                        type={showPassword ? 'text' : 'password'}
                        id="password"
                        value={password}
                        onChange={(e) => onPasswordChange(e.target.value)}
                        placeholder="8자 이상, 숫자 및 특수문자 포함"
                        required
                    />
                    <button
                        type="button"
                        className="toggle-password"
                        onClick={() => setShowPassword(!showPassword)}
                        tabIndex={-1}
                    >
                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                </div>
            </div>

            {/* 비밀번호 유효성 검사 표시 */}
            {password.length > 0 && (
                <div className="password-validations">
                    <div className={`validation-item ${validations.minLength ? 'valid' : 'invalid'}`}>
                        {validations.minLength ? <Check size={14} /> : <X size={14} />}
                        <span>8자 이상</span>
                    </div>
                    <div className={`validation-item ${validations.hasNumber ? 'valid' : 'invalid'}`}>
                        {validations.hasNumber ? <Check size={14} /> : <X size={14} />}
                        <span>숫자 포함</span>
                    </div>
                    <div className={`validation-item ${validations.hasSpecialChar ? 'valid' : 'invalid'}`}>
                        {validations.hasSpecialChar ? <Check size={14} /> : <X size={14} />}
                        <span>특수문자 포함 (!@#$%^&*)</span>
                    </div>
                </div>
            )}

            {/* 비밀번호 확인 입력 */}
            <div className="input-group">
                <label htmlFor="confirmPassword">비밀번호 확인</label>
                <div className="password-field">
                    <input
                        type={showConfirmPassword ? 'text' : 'password'}
                        id="confirmPassword"
                        value={confirmPassword}
                        onChange={(e) => onConfirmPasswordChange(e.target.value)}
                        placeholder="비밀번호를 다시 입력해주세요"
                        required
                    />
                    <button
                        type="button"
                        className="toggle-password"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        tabIndex={-1}
                    >
                        {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                </div>
                {confirmPassword.length > 0 && !passwordsMatch && (
                    <p className="error-message">비밀번호가 일치하지 않습니다.</p>
                )}
                {confirmPassword.length > 0 && passwordsMatch && (
                    <p className="success-message">비밀번호가 일치합니다.</p>
                )}
            </div>

            <style>{`
                .password-input-group {
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                }

                .password-field {
                    position: relative;
                }

                .password-field input {
                    width: 100%;
                    padding-right: 2.5rem;
                }

                .toggle-password {
                    position: absolute;
                    right: 0.75rem;
                    top: 50%;
                    transform: translateY(-50%);
                    background: none;
                    border: none;
                    color: var(--color-text-tertiary);
                    cursor: pointer;
                    padding: 0.25rem;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: color 0.2s;
                }

                .toggle-password:hover {
                    color: var(--color-text-secondary);
                }

                .password-validations {
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                    padding: 0.75rem;
                    background-color: var(--color-background-secondary);
                    border-radius: 8px;
                    margin-top: -0.5rem;
                }

                .validation-item {
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    font-size: 0.8125rem;
                    font-weight: 500;
                }

                .validation-item.valid {
                    color: #10b981;
                }

                .validation-item.invalid {
                    color: var(--color-text-tertiary);
                }

                .error-message {
                    margin-top: 0.5rem;
                    font-size: 0.8125rem;
                    color: #ef4444;
                    font-weight: 500;
                }

                .success-message {
                    margin-top: 0.5rem;
                    font-size: 0.8125rem;
                    color: #10b981;
                    font-weight: 500;
                }
            `}</style>
        </div>
    );
};

