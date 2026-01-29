import React from 'react';
import { Users, UserCheck, ClipboardCheck, LayoutDashboard, FileWarning } from 'lucide-react';
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
    const menuItems: { id: ManageTab; label: string; icon: React.ReactNode; badge?: number }[] = [
        { id: 'dashboard', label: '팀 대시보드', icon: <LayoutDashboard size={18} /> },
        { id: 'applicants', label: '지원자 관리', icon: <UserCheck size={18} />, badge: pendingApplicantCount },
        { id: 'members', label: '멤버 관리', icon: <Users size={18} /> },
        { id: 'attendance', label: '출석 관리', icon: <ClipboardCheck size={18} /> },
        { id: 'excuse', label: '소명 관리', icon: <FileWarning size={18} />, badge: pendingExcuseCount },
    ];

    const handleTabClick = (tab: ManageTab) => {
        setActiveTab(tab);
        // 탭 전환 시 카운트 새로고침 (옵션)
        if (onRefreshCounts) {
            onRefreshCounts();
        }
    };

    return (
        <aside className="w-56 flex-shrink-0">
            <nav className="bg-surface rounded-3xl border border-border-light p-3 shadow-sm sticky top-6">
                <ul className="space-y-1">
                    {menuItems.map((item) => (
                        <li key={item.id}>
                            <button
                                onClick={() => handleTabClick(item.id)}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all
                                    ${activeTab === item.id
                                        ? 'bg-primary text-white shadow-md'
                                        : 'text-text-secondary hover:bg-background-secondary hover:text-text-primary'
                                    }`}
                            >
                                {item.icon}
                                <span className="flex-1 text-left">{item.label}</span>
                                {item.badge !== undefined && item.badge > 0 && (
                                    <span className={`px-2 py-0.5 text-xs font-bold rounded-full
                                        ${activeTab === item.id
                                            ? 'bg-white/20 text-white'
                                            : 'bg-error/10 text-error'
                                        }`}>
                                        {item.badge}
                                    </span>
                                )}
                            </button>
                        </li>
                    ))}
                </ul>
            </nav>
        </aside>
    );
};

export default ManagementSidebar;
