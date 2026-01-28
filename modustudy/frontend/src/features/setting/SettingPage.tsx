/**
 * SettingPage 컴포넌트
 * 설정 페이지의 메인 컴포넌트입니다.
 * 사이드바 네비게이션과 각 섹션을 렌더링합니다.
 */

import { useEffect } from 'react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { useSettingStore } from './store/settingStore';
import { SettingSidebar } from './components/SettingSidebar';
import { NotificationSection } from './components/NotificationSection';
import { AccountSecuritySection } from './components/AccountSecuritySection';
import { ProfileSection } from './components/ProfileSection';
import { StudyPreferenceSection } from './components/StudyPreferenceSection';
import { ThemeDisplaySection } from './components/ThemeDisplaySection';
import './styles/SettingPage.css';
// ProfileSection에서 사용하는 ProfileHeader 스타일
import '@/features/profile/styles/ProfilePage.css';

export const SettingPage = () => {
    const {
        activeSection,
        fetchNotificationSettings,
        fetchSocialAccounts,
        isLoading,
    } = useSettingStore();

    // 초기 데이터 로드
    useEffect(() => {
        fetchNotificationSettings();
        fetchSocialAccounts();
    }, [fetchNotificationSettings, fetchSocialAccounts]);

    // 활성 섹션에 따른 컴포넌트 렌더링
    const renderSection = () => {
        switch (activeSection) {
            case 'notification':
                return <NotificationSection />;
            case 'account':
                return <AccountSecuritySection />;
            case 'profile':
                return <ProfileSection />;
            case 'study':
                return <StudyPreferenceSection />;
            case 'theme':
                return <ThemeDisplaySection />;
            default:
                return <NotificationSection />;
        }
    };

    return (
        <UserLayoutV2>
            <div className="setting-page">
                <div className="setting-container">
                    {/* 좌측 네비게이션 */}
                    <SettingSidebar />

                    {/* 우측 컨텐츠 영역 */}
                    <main className="setting-content">
                        {isLoading ? (
                            <div className="loading-spinner">
                                <div className="spinner" />
                            </div>
                        ) : (
                            renderSection()
                        )}
                    </main>
                </div>
            </div>
        </UserLayoutV2>
    );
};
