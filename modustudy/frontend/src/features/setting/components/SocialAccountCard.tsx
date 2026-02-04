/**
 * SocialAccountCard 컴포넌트
 * 연동된 소셜 계정 정보를 표시하는 카드입니다.
 */

import { Button } from '@/shared/components/Button';
import { cn } from '@/shared/utils/cn';
import type { SocialAccount, SocialProvider } from '../types';

// 소셜 프로바이더별 설정
const providerConfig: Record<
    SocialProvider,
    { name: string; color: string; bgColor: string; icon: JSX.Element }
> = {
    KAKAO: {
        name: '카카오',
        color: '#3C1E1E',
        bgColor: '#FEE500',
        icon: (
            <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3C6.477 3 2 6.463 2 10.691c0 2.652 1.767 5.001 4.429 6.338l-.715 2.635c-.086.321.199.607.507.445l3.146-1.664c.827.131 1.696.2 2.633.2 5.523 0 10-3.463 10-7.754S17.523 3 12 3z" />
            </svg>
        ),
    },
    NAVER: {
        name: '네이버',
        color: '#FFFFFF',
        bgColor: '#03C75A',
        icon: (
            <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M16.273 12.845L7.376 3H3v18h4.727V12.155L16.624 21H21V3h-4.727v9.845z" />
            </svg>
        ),
    },
    GOOGLE: {
        name: '구글',
        color: '#4285F4',
        bgColor: '#FFFFFF',
        icon: (
            <svg viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
        ),
    },
};

interface SocialAccountCardProps {
    /** 소셜 계정 정보 */
    account: SocialAccount;
    /** 연동 해제 핸들러 */
    onUnlink: (provider: SocialProvider) => void;
    /** 연동 해제 비활성화 여부 */
    disabled?: boolean;
    /** 저장 중 여부 */
    isSaving?: boolean;
    /** 현재 로그인 방식인지 여부 */
    isCurrentLoginMethod?: boolean;
}

export const SocialAccountCard = ({
    account,
    onUnlink,
    disabled = false,
    isSaving = false,
    isCurrentLoginMethod = false,
}: SocialAccountCardProps) => {
    const config = providerConfig[account.provider];

    // 날짜 포맷팅
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
        });
    };

    return (
        <div className={cn(
            'social-account-card',
            isCurrentLoginMethod && 'ring-2 ring-[var(--color-primary)] ring-opacity-50'
        )}>
            <div
                className={cn('social-account-icon', account.provider.toLowerCase())}
                style={{
                    backgroundColor: config.bgColor,
                    color: config.color,
                }}
            >
                {config.icon}
            </div>
            <div className="social-account-info">
                <div className="social-account-name">
                    {config.name}
                    {isCurrentLoginMethod && (
                        <span
                            className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                            style={{
                                backgroundColor: 'var(--color-primary-alpha-10)',
                                color: 'var(--color-primary)',
                            }}
                        >
                            현재 로그인
                        </span>
                    )}
                </div>
                <div className="social-account-email">{account.email}</div>
                <div className="social-account-date">{formatDate(account.linkedAt)} 연동</div>
            </div>
            <div className="social-account-action">
                {isCurrentLoginMethod ? (
                    <span
                        className="text-xs px-3 py-1.5 rounded-lg font-medium"
                        style={{
                            backgroundColor: '#f1f5f9',
                            color: '#64748b',
                        }}
                    >
                        현재 로그인 방식
                    </span>
                ) : (
                    <Button
                        variant="danger"
                        size="sm"
                        onClick={() => onUnlink(account.provider)}
                        disabled={disabled}
                        isLoading={isSaving}
                    >
                        연동 해제
                    </Button>
                )}
            </div>
        </div>
    );
};

// 연동 안 된 소셜 계정 카드
interface UnlinkedSocialCardProps {
    provider: SocialProvider;
    onLink: (provider: SocialProvider) => void;
}

export const UnlinkedSocialCard = ({ provider, onLink }: UnlinkedSocialCardProps) => {
    const config = providerConfig[provider];

    return (
        <div className="social-account-card" style={{ opacity: 0.6 }}>
            <div
                className={cn('social-account-icon', provider.toLowerCase())}
                style={{
                    backgroundColor: config.bgColor,
                    color: config.color,
                }}
            >
                {config.icon}
            </div>
            <div className="social-account-info">
                <div className="social-account-name">{config.name}</div>
                <div className="social-account-email" style={{ color: '#94a3b8' }}>
                    연동되지 않음
                </div>
            </div>
            <div className="social-account-action">
                <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onLink(provider)}
                >
                    연동하기
                </Button>
            </div>
        </div>
    );
};
