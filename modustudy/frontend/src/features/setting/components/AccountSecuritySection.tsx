/**
 * AccountSecuritySection 컴포넌트
 * 계정/보안 설정 섹션을 담당합니다.
 * 비밀번호 설정/변경, 소셜 계정 연동 관리 기능을 제공합니다.
 */

import { useState, useEffect, useRef } from 'react';
import { ShieldUser, Key, AlertTriangle, Link2, Calendar } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useSettingStore } from '../store/settingStore';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { SocialAccountCard, UnlinkedSocialCard } from './SocialAccountCard';
import type { SocialProvider } from '../types';

// 모든 소셜 프로바이더 목록
const ALL_PROVIDERS: SocialProvider[] = ['KAKAO', 'NAVER', 'GOOGLE'];

export const AccountSecuritySection = () => {
    const {
        socialAccounts,
        hasPassword,
        setNewPassword,
        changeExistingPassword,
        unlinkSocialAccount,
        startSocialLink,
        fetchSocialAccounts,
        googleCalendarStatus,
        fetchGoogleCalendarStatus,
        connectGoogleCalendar,
        disconnectGoogleCalendar,
        isLoading,
        isSaving,
        error,
    } = useSettingStore();
    const { isLoggedIn, user } = useAuthStore();
    const showToast = useUIStore((state) => state.showToast);

    // 현재 로그인 방식 (KAKAO, GOOGLE, NAVER)
    const currentLoginProvider = user?.loginProvider;

    // 최초 한 번만 fetch 실행하기 위한 ref
    const hasFetchedRef = useRef(false);

    // 로그인 상태일 때만 소셜 계정 및 캘린더 상태 조회 (최초 마운트 시 한 번만)
    useEffect(() => {
        if (!isLoggedIn || hasFetchedRef.current) return;
        hasFetchedRef.current = true;
        fetchSocialAccounts();
        fetchGoogleCalendarStatus();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoggedIn]);

    // 비밀번호 폼 상태
    const [showPasswordForm, setShowPasswordForm] = useState(false);
    const [passwordForm, setPasswordForm] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
    });
    const [passwordError, setPasswordError] = useState<string | null>(null);
    const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

    // 연동된 프로바이더 목록
    const linkedProviders = socialAccounts.map((a) => a.provider);

    // 연동되지 않은 프로바이더 목록
    const unlinkedProviders = ALL_PROVIDERS.filter((p) => !linkedProviders.includes(p));

    // 비밀번호 저장 핸들러
    const handlePasswordSubmit = async () => {
        setPasswordError(null);
        setPasswordSuccess(null);

        // 유효성 검사
        if (passwordForm.newPassword.length < 8) {
            setPasswordError('비밀번호는 8자 이상이어야 합니다.');
            return;
        }

        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            setPasswordError('새 비밀번호가 일치하지 않습니다.');
            return;
        }

        try {
            if (hasPassword) {
                // 비밀번호 변경
                await changeExistingPassword(
                    passwordForm.currentPassword,
                    passwordForm.newPassword,
                    passwordForm.confirmPassword
                );
                setPasswordSuccess('비밀번호가 변경되었습니다.');
            } else {
                // 비밀번호 최초 설정
                await setNewPassword(passwordForm.newPassword, passwordForm.confirmPassword);
                setPasswordSuccess('비밀번호가 설정되었습니다.');
            }
            // 폼 초기화
            setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
            setShowPasswordForm(false);
        } catch (err) {
            setPasswordError(err instanceof Error ? err.message : '비밀번호 처리에 실패했습니다.');
        }
    };

    // 소셜 연동 해제 핸들러
    const handleUnlink = async (provider: SocialProvider) => {
        // 최소 1개 로그인 수단 체크
        if (socialAccounts.length <= 1 && !hasPassword) {
            showToast('최소 1개의 로그인 방법은 유지해야 합니다. 비밀번호를 먼저 설정해주세요.', 'warning');
            return;
        }

        if (confirm(`${provider} 연동을 해제하시겠습니까?`)) {
            await unlinkSocialAccount(provider);
        }
    };

    // 소셜 연동 핸들러
    const handleLink = async (provider: SocialProvider) => {
        await startSocialLink(provider);
    };

    // Google 캘린더 연동 핸들러
    const handleConnectCalendar = async () => {
        await connectGoogleCalendar();
    };

    // Google 캘린더 연동 해제 핸들러
    const handleDisconnectCalendar = async () => {
        if (confirm('Google 캘린더 연동을 해제하시겠습니까?')) {
            await disconnectGoogleCalendar();
            showToast('Google 캘린더 연동이 해제되었습니다.', 'success');
        }
    };

    return (
        <section className="setting-section">
            {/* 섹션 헤더 */}
            <div className="section-header">
                <h2 className="section-title">
                    <ShieldUser className="section-title-icon" />
                    계정 / 보안
                </h2>
                <p className="section-description">
                    계정 보안을 강화하고 로그인 방법을 관리합니다.
                </p>
            </div>

            {/* 비밀번호 설정 영역 */}
            <div className="setting-item" style={{ flexDirection: 'column', alignItems: 'stretch' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                    <div className="setting-item-info">
                        <div className="setting-item-icon" style={{ color: '#3b82f6' }}>
                            <Key />
                        </div>
                        <div className="setting-item-text">
                            <span className="setting-item-label">비밀번호</span>
                            <span className="setting-item-desc">
                                {hasPassword
                                    ? '비밀번호가 설정되어 있습니다.'
                                    : '비밀번호를 설정하면 이메일로 로그인할 수 있습니다.'}
                            </span>
                        </div>
                    </div>
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setShowPasswordForm(!showPasswordForm)}
                    >
                        {hasPassword ? '비밀번호 변경' : '비밀번호 설정'}
                    </Button>
                </div>

                {/* 비밀번호 입력 폼 */}
                {showPasswordForm && (
                    <div style={{ marginTop: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid #e2e8f0' }}>
                        {/* 현재 비밀번호 (변경 시에만) */}
                        {hasPassword && (
                            <div className="input-group">
                                <label className="input-label">현재 비밀번호</label>
                                <input
                                    type="password"
                                    className="input-field"
                                    value={passwordForm.currentPassword}
                                    onChange={(e) =>
                                        setPasswordForm({ ...passwordForm, currentPassword: e.target.value })
                                    }
                                    placeholder="현재 비밀번호를 입력하세요"
                                />
                            </div>
                        )}

                        {/* 새 비밀번호 */}
                        <div className="input-group">
                            <label className="input-label">
                                {hasPassword ? '새 비밀번호' : '비밀번호'}
                            </label>
                            <input
                                type="password"
                                className="input-field"
                                value={passwordForm.newPassword}
                                onChange={(e) =>
                                    setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                                }
                                placeholder="8자 이상의 비밀번호를 입력하세요"
                            />
                        </div>

                        {/* 비밀번호 확인 */}
                        <div className="input-group">
                            <label className="input-label">비밀번호 확인</label>
                            <input
                                type="password"
                                className="input-field"
                                value={passwordForm.confirmPassword}
                                onChange={(e) =>
                                    setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
                                }
                                placeholder="비밀번호를 다시 입력하세요"
                            />
                        </div>

                        {/* 에러/성공 메시지 */}
                        {passwordError && (
                            <div className="warning-message" style={{ background: '#fef2f2', borderColor: '#fecaca' }}>
                                <AlertTriangle size={16} />
                                <span style={{ color: '#991b1b' }}>{passwordError}</span>
                            </div>
                        )}
                        {passwordSuccess && (
                            <div className="warning-message" style={{ background: '#f0fdf4', borderColor: '#86efac' }}>
                                <span style={{ color: '#166534' }}>{passwordSuccess}</span>
                            </div>
                        )}

                        {/* 저장 버튼 */}
                        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => {
                                    setShowPasswordForm(false);
                                    setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
                                    setPasswordError(null);
                                }}
                            >
                                취소
                            </Button>
                            <Button
                                variant="primary"
                                size="sm"
                                onClick={handlePasswordSubmit}
                                isLoading={isSaving}
                            >
                                저장
                            </Button>
                        </div>
                    </div>
                )}
            </div>

            {/* 소셜 계정 연동 영역 */}
            <div style={{ marginTop: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                    <Link2 size={20} style={{ color: '#64748b' }} />
                    <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>연동된 계정</h3>
                </div>

                {isLoading ? (
                    <div className="loading-spinner">
                        <div className="spinner" />
                    </div>
                ) : (
                    <div className="social-accounts-list">
                        {/* 연동된 계정 */}
                        {socialAccounts.map((account) => (
                            <SocialAccountCard
                                key={account.provider}
                                account={account}
                                onUnlink={handleUnlink}
                                disabled={socialAccounts.length <= 1 && !hasPassword}
                                isSaving={isSaving}
                                isCurrentLoginMethod={currentLoginProvider === account.provider}
                            />
                        ))}

                        {/* 연동되지 않은 계정 */}
                        {unlinkedProviders.map((provider) => (
                            <UnlinkedSocialCard
                                key={provider}
                                provider={provider}
                                onLink={handleLink}
                            />
                        ))}
                    </div>
                )}

                {/* 경고 메시지 */}
                {socialAccounts.length <= 1 && !hasPassword && !isLoading && (
                    <div className="warning-message">
                        <AlertTriangle size={20} />
                        <span>
                            최소 1개의 로그인 방법은 유지해야 합니다.
                            연동을 해제하려면 비밀번호를 먼저 설정하거나 다른 소셜 계정을 연동해주세요.
                        </span>
                    </div>
                )}

                {/* 스토어 에러 표시 */}
                {error && (
                    <div className="warning-message" style={{ background: '#fef2f2', borderColor: '#fecaca' }}>
                        <AlertTriangle size={16} />
                        <span style={{ color: '#991b1b' }}>{error}</span>
                    </div>
                )}
            </div>

            {/* Google 캘린더 연동 영역 */}
            <div className="calendar-integration-section">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                    <Calendar size={20} style={{ color: '#64748b' }} />
                    <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Google 캘린더 연동</h3>
                </div>

                <div className="calendar-integration-card">
                    <div className="calendar-integration-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                            <rect width="24" height="24" rx="4" fill="#fff" />
                            <path d="M19.5 4H17V3c0-.6-.4-1-1-1s-1 .4-1 1v1H9V3c0-.6-.4-1-1-1s-1 .4-1 1v1H4.5C3.7 4 3 4.7 3 5.5v14c0 .8.7 1.5 1.5 1.5h15c.8 0 1.5-.7 1.5-1.5v-14C21 4.7 20.3 4 19.5 4z" fill="#4285F4"/>
                            <rect x="6" y="10" width="3" height="3" rx="0.5" fill="#EA4335"/>
                            <rect x="10.5" y="10" width="3" height="3" rx="0.5" fill="#FBBC04"/>
                            <rect x="15" y="10" width="3" height="3" rx="0.5" fill="#34A853"/>
                            <rect x="6" y="15" width="3" height="3" rx="0.5" fill="#4285F4"/>
                            <rect x="10.5" y="15" width="3" height="3" rx="0.5" fill="#EA4335"/>
                        </svg>
                    </div>
                    <div className="calendar-integration-info">
                        {googleCalendarStatus?.connected ? (
                            <>
                                <span className="calendar-integration-status connected">연동됨</span>
                                {googleCalendarStatus.email && (
                                    <span className="calendar-integration-email">{googleCalendarStatus.email}</span>
                                )}
                            </>
                        ) : (
                            <>
                                <span className="calendar-integration-status">미연동</span>
                                <span className="calendar-integration-desc">
                                    Google 캘린더를 연동하면 스터디 일정을 자동으로 동기화할 수 있습니다.
                                </span>
                            </>
                        )}
                    </div>
                    <div className="calendar-integration-action">
                        {googleCalendarStatus?.connected ? (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={handleDisconnectCalendar}
                                isLoading={isSaving}
                            >
                                연동 해제
                            </Button>
                        ) : (
                            <Button
                                variant="primary"
                                size="sm"
                                onClick={handleConnectCalendar}
                            >
                                연동하기
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </section>
    );
};
