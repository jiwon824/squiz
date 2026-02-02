/**
 * SettingSidebar 컴포넌트
 * 설정 페이지의 좌측 네비게이션 메뉴를 담당합니다.
 */

import { Bell, ShieldUser, User, Palette, BookOpen } from 'lucide-react';
import { PageSidebar } from '@/shared/components/PageSidebar';
import { useSettingStore } from '../store/settingStore';
import type { SettingSection } from '../types';
import type { SidebarItem } from '@/shared/components/PageSidebar';

// 메뉴 아이템 정의
const menuItems: Array<SidebarItem & { id: SettingSection }> = [
    {
        id: 'notification',
        label: '알림 설정',
        description: '알림 수신 관리',
        icon: <Bell size={18} />,
    },
    {
        id: 'account',
        label: '계정 / 보안',
        description: '비밀번호, 소셜 연동',
        icon: <ShieldUser size={18} />,
    },
    {
        id: 'profile',
        label: '프로필 설정',
        description: '프로필 정보 수정',
        icon: <User size={18} />,
    },
    {
        id: 'study',
        label: '스터디 선호',
        description: '기술스택, 일정, 기간',
        icon: <BookOpen size={18} />,
    },
    {
        id: 'theme',
        label: '테마 / 디스플레이',
        description: '화면 설정',
        icon: <Palette size={18} />,
    },
];

export const SettingSidebar = () => {
    const { activeSection, setActiveSection } = useSettingStore();

    return (
        <PageSidebar
            items={menuItems}
            activeId={activeSection}
            onSelect={(id) => setActiveSection(id as SettingSection)}
            title="설정"
        />
    );
};
