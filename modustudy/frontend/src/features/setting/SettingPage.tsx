/**
 * SettingPage 컴포넌트
 * 설정 페이지의 메인 컴포넌트입니다.
 * 사이드바 네비게이션과 각 섹션을 렌더링합니다.
 */

import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
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
import type { SettingSection } from './types';

export const SettingPage = () => {
    const location = useLocation();
    const {
        activeSection,
        setActiveSection,
        fetchNotificationSettings,
        fetchSocialAccounts,
        isLoading,
    } = useSettingStore();

    // URL state로 전달된 섹션이 있으면 해당 섹션으로 이동
    useEffect(() => {
        const state = location.state as { section?: SettingSection } | null;
        if (state?.section) {
            setActiveSection(state.section);
        }
    }, [location.state, setActiveSection]);

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
