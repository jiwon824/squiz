import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useUIStore } from '@/store/uiStore';
import { QuizIcon, StudyIcon, DashboardIcon, CalendarIcon, RecruitmentIcon, SettingIcon } from '@/shared/components/icons/SidebarIcons';
export const Sidebar = () => {
    const { isSidebarOpen } = useUIStore();
    const location = useLocation();

    return (
        <aside
            className="h-screen flex flex-col overflow-hidden bg-slate-200 transition-all duration-300 ease-out"
            style={{ width: isSidebarOpen ? 280 : 0 }}
        >
            {/* 메뉴 아이템들 */}
            <nav className="flex-1 p-4 space-y-2">
                {/* 대시보드 */}
                <SidebarItem
                    icon={<DashboardIcon />}
                    label="대시보드"
                    isOpen={isSidebarOpen}
                    path="/dashboard"
                    isActive={location.pathname === '/dashboard'}
                />

                {/* 퀴즈 메뉴 */}
                <SidebarItem
                    icon={<QuizIcon />}
                    label="퀴즈"
                    isOpen={isSidebarOpen}
                    path="/quiz"
                    isActive={location.pathname === '/quiz'}
                    badge={5}
                    badgeColor="study-teal"
                />

                {/* 스터디 메뉴 */}
                <SidebarItem
                    icon={<StudyIcon />}
                    label="스터디"
                    isOpen={isSidebarOpen}
                    path="/study"
                    isActive={location.pathname === '/study'}
                    showDot
                    dotColor="study-green"
                />

                {/* 팀원 모집 */}
                <SidebarItem
                    icon={<RecruitmentIcon />}
                    label="팀원 모집"
                    isOpen={isSidebarOpen}
                    path="/recruitment"
                    isActive={location.pathname === '/recruitment'}
                />

                {/* 캘린더 */}
                <SidebarItem
                    icon={<CalendarIcon />}
                    label="캘린더"
                    isOpen={isSidebarOpen}
                    path="/calendar-expand"
                    isActive={location.pathname === '/calendar-expand'}
                />

                {/*설정*/}
                <SidebarItem
                    icon={<SettingIcon />}
                    label="설정"
                    isOpen={isSidebarOpen}
                    path="/setting"
                    isActive={location.pathname === '/setting'}
                />
            </nav>
        </aside>
    );
};

// 사이드바 아이템 컴포넌트
interface SidebarItemProps {
    icon: React.ReactNode;
    label: string;
    isOpen: boolean;
    path: string;
    isActive?: boolean;
    badge?: number;
    badgeColor?: string;
    showDot?: boolean;
    dotColor?: string;
}

const SidebarItem: React.FC<SidebarItemProps> = ({
    icon,
    label,
    isOpen,
    path,
    isActive,
    badge,
    badgeColor = 'study-blue',
    showDot,
    dotColor = 'study-green',
}) => {
    const [isHovered, setIsHovered] = useState(false);

    return (
        <Link to={path} style={{ textDecoration: 'none', display: 'block' }}>
            <motion.div
                className={`w-full flex items-center justify-between p-3 rounded-google transition-colors relative group ${isActive ? 'text-study-blue' : 'text-study-text'
                    }`}
                animate={{
                    x: (isOpen && !isActive && !isHovered) ? -5 : 0,
                    opacity: isOpen ? 1 : 0,
                    backgroundColor: isActive
                        ? '#80A1BA33' // --color-primary with 20% alpha
                        : (isHovered ? '#80A1BA1A' : 'transparent'), // --color-primary with 10% alpha
                }}
                whileTap={{ scale: 0.98 }}
                onHoverStart={() => setIsHovered(true)}
                onHoverEnd={() => setIsHovered(false)}
            >
                {/* 라벨 (왼쪽) */}
                {isOpen && (
                    <motion.span
                        className="font-medium text-left whitespace-nowrap"
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -10 }}
                    >
                        {label}
                    </motion.span>
                )}

                {/* 아이콘 + 배지 (오른쪽) */}
                <div className="flex items-center gap-2">
                    {/* 배지 */}
                    {isOpen && badge !== undefined && (
                        <motion.span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold text-white shadow-sm`}
                            style={{ backgroundColor: `var(--color-${badgeColor.replace('study-', '')})` }}
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                        >
                            {badge}
                        </motion.span>
                    )}

                    {/* 아이콘 - 사이드바가 열려있을 때, 호버 시에만 선명하게 표시 (모든 항목 대상) */}
                    {isOpen && (
                        <motion.div
                            className={isActive ? "text-study-blue" : "text-study-blue/60"}
                            initial={{ opacity: 0, x: 10 }}
                            animate={{
                                opacity: isHovered ? 1 : 0,
                                x: isHovered ? 0 : 10
                            }}
                            transition={{ duration: 0.2 }}
                        >
                            {icon}
                        </motion.div>
                    )}
                </div>

                {/* 활성 상태 점 */}
                {showDot && isOpen && !isActive && (
                    <motion.div
                        className={`absolute right-3 top-3 w-1.5 h-1.5 rounded-full`}
                        style={{ backgroundColor: `var(--color-${dotColor.replace('study-', '')})` }}
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                    />
                )}
            </motion.div>
        </Link>
    );
};
