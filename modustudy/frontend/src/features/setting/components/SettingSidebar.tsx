/**
 * SettingSidebar 컴포넌트
 * 설정 페이지의 좌측 네비게이션 메뉴를 담당합니다.
 */

import { Bell, Shield, User, Palette, BookOpen } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useSettingStore } from '../store/settingStore';
import type { SettingSection } from '../types';

// 메뉴 아이템 정의
const menuItems: Array<{
    id: SettingSection;
    label: string;
    description: string;
    icon: typeof Bell;
}> = [
    {
        id: 'notification',
        label: '알림 설정',
        description: '알림 수신 관리',
        icon: Bell,
    },
    {
        id: 'account',
        label: '계정 / 보안',
        description: '비밀번호, 소셜 연동',
        icon: Shield,
    },
    {
        id: 'profile',
        label: '프로필 설정',
        description: '프로필 정보 수정',
        icon: User,
    },
    {
        id: 'study',
        label: '스터디 선호',
        description: '기술스택, 일정, 기간',
        icon: BookOpen,
    },
    {
        id: 'theme',
        label: '테마 / 디스플레이',
        description: '화면 설정',
        icon: Palette,
    },
];

export const SettingSidebar = () => {
    const { activeSection, setActiveSection } = useSettingStore();

    return (
        <aside className="setting-sidebar">
            <div className="setting-sidebar-card">
                <h2 className="sidebar-title">설정</h2>
                <nav className="sidebar-menu">
                    {menuItems.map((item) => {
                        const Icon = item.icon;
                        const isActive = activeSection === item.id;

                        return (
                            <button
                                key={item.id}
                                className={cn('sidebar-item', { active: isActive })}
                                onClick={() => setActiveSection(item.id)}
                                type="button"
                            >
                                <Icon className="sidebar-item-icon" />
                                <div className="sidebar-item-content">
                                    <span className="sidebar-item-label">{item.label}</span>
                                    <span className="sidebar-item-desc">{item.description}</span>
                                </div>
                            </button>
                        );
                    })}
                </nav>
            </div>
        </aside>
    );
};
