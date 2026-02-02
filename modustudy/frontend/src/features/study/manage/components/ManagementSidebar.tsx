import React from 'react';
import { Users, UserCheck, ClipboardCheck, LayoutDashboard, FileWarning } from 'lucide-react';
import { PageSidebar } from '@/shared/components/PageSidebar';
import type { SidebarItem } from '@/shared/components/PageSidebar';
import type { ManageTab } from '../StudyManagementPage';

interface ManagementSidebarProps {
    studyId: number;
    activeTab: ManageTab;
    setActiveTab: (tab: ManageTab) => void;
    pendingApplicantCount?: number;
    pendingExcuseCount?: number;
    onRefreshCounts?: () => void;
}

const ManagementSidebar: React.FC<ManagementSidebarProps> = ({
    activeTab,
    setActiveTab,
    pendingApplicantCount = 0,
    pendingExcuseCount = 0,
    onRefreshCounts,
}) => {
    const menuItems: SidebarItem[] = [
        { id: 'dashboard', label: '팀 대시보드', icon: <LayoutDashboard size={18} /> },
        { id: 'applicants', label: '지원자 관리', icon: <UserCheck size={18} />, badge: pendingApplicantCount },
        { id: 'members', label: '멤버 관리', icon: <Users size={18} /> },
        { id: 'attendance', label: '출석 관리', icon: <ClipboardCheck size={18} /> },
        { id: 'excuse', label: '소명 관리', icon: <FileWarning size={18} />, badge: pendingExcuseCount },
    ];

    const handleSelect = (id: string) => {
        setActiveTab(id as ManageTab);
        if (onRefreshCounts) {
            onRefreshCounts();
        }
    };

    return (
        <PageSidebar
            items={menuItems}
            activeId={activeTab}
            onSelect={handleSelect}
        />
    );
};

export default ManagementSidebar;
