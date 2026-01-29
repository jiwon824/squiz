import { Link, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';
import {
    QuizIcon,
    StudyIcon,
    DashboardIcon,
    CalendarIcon,
    RecruitmentIcon,
    SettingIcon,
} from '@/shared/components/icons/SidebarIcons';

// 사이드바 너비 상수
const SIDEBAR_FULL_WIDTH = 240;
const SIDEBAR_MINI_WIDTH = 80;

// 아이콘 영역 너비 (미니 사이드바와 동일 → 아이콘 위치 고정)
const ICON_AREA_WIDTH = SIDEBAR_MINI_WIDTH;

// 스프링 애니메이션 설정
const sidebarSpring = { type: 'spring' as const, damping: 26, stiffness: 300 };
const fadeTransition = { duration: 0.15, ease: [0.4, 0, 0.2, 1] };

export const Sidebar = () => {
    const sidebarMode = useUIStore((state) => state.sidebarMode);
    const toggleSidebar = useUIStore((state) => state.toggleSidebar);
    const location = useLocation();

    const isFull = sidebarMode === 'full';
    const isClosed = sidebarMode === 'closed';

    const currentWidth = isClosed ? 0 : isFull ? SIDEBAR_FULL_WIDTH : SIDEBAR_MINI_WIDTH;

    return (
        <motion.aside
            className="h-full flex flex-col bg-slate-200 flex-shrink-0 overflow-hidden"
            animate={{ width: currentWidth, minWidth: currentWidth }}
            transition={sidebarSpring}
        >
            {/* 햄버거 토글 버튼 - 항상 ICON_AREA 중앙 고정 */}
            <div
                className="flex items-center justify-center pt-5 pb-4 flex-shrink-0"
                style={{ width: ICON_AREA_WIDTH }}
            >
                <button
                    onClick={toggleSidebar}
                    className="w-12 h-12 flex items-center justify-center rounded-2xl hover:bg-white/60 transition-colors"
                    aria-label="Toggle sidebar"
                >
                    <span className="material-icons text-gray-600 text-[24px]">
                        {isFull ? 'menu_open' : 'menu'}
                    </span>
                </button>
            </div>

            {/* 메뉴 아이템들 */}
            <nav
                className="flex-1 space-y-3"
                style={{ width: isFull ? SIDEBAR_FULL_WIDTH : SIDEBAR_MINI_WIDTH }}
            >
                <SidebarItem
                    icon={<DashboardIcon className="w-6 h-6" />}
                    label="대시보드"
                    path="/dashboard"
                    isActive={location.pathname === '/dashboard'}
                    isFull={isFull}
                />

                <SidebarItem
                    icon={<QuizIcon className="w-6 h-6" />}
                    label="퀴즈"
                    path="/quiz"
                    isActive={location.pathname === '/quiz'}
                    isFull={isFull}
                    badge={5}
                />

                <SidebarItem
                    icon={<StudyIcon className="w-6 h-6" />}
                    label="스터디"
                    path="/study"
                    isActive={location.pathname === '/study'}
                    isFull={isFull}
                    showDot
                />

                <SidebarItem
                    icon={<RecruitmentIcon className="w-6 h-6" />}
                    label="팀원 모집"
                    path="/recruitment"
                    isActive={location.pathname === '/recruitment'}
                    isFull={isFull}
                />

                <SidebarItem
                    icon={<CalendarIcon className="w-6 h-6" />}
                    label="캘린더"
                    path="/calendar"
                    isActive={location.pathname === '/calendar'}
                    isFull={isFull}
                />

                <SidebarItem
                    icon={<SettingIcon className="w-6 h-6" />}
                    label="설정"
                    path="/setting"
                    isActive={location.pathname === '/setting'}
                    isFull={isFull}
                />
            </nav>
        </motion.aside>
    );
};

// 사이드바 아이템 컴포넌트
interface SidebarItemProps {
    icon: React.ReactNode;
    label: string;
    path: string;
    isActive?: boolean;
    isFull: boolean;
    badge?: number;
    showDot?: boolean;
}

const SidebarItem: React.FC<SidebarItemProps> = ({
    icon,
    label,
    path,
    isActive,
    isFull,
    badge,
    showDot,
}) => {
    return (
        <Link to={path} className="block no-underline group">
            <div
                className={cn(
                    'flex items-center h-14 rounded-2xl mx-3 relative',
                    'transition-colors duration-200',
                    isActive
                        ? 'bg-[var(--color-google-blue)]/10 text-[var(--color-google-blue)]'
                        : 'text-gray-600 hover:bg-white/60 hover:text-gray-900'
                )}
            >
                {/* 활성 상태 인디케이터 바 - layoutId 제거하여 라우트 전환 버벅임 해소 */}
                {isActive && (
                    <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-6 bg-[var(--color-google-blue)] rounded-r-full" />
                )}

                {/* 아이콘 영역 - 항상 고정 너비, 세로 중앙 정렬 */}
                <div
                    className="flex items-center justify-center flex-shrink-0 h-full"
                    style={{ width: ICON_AREA_WIDTH - 20 }}
                >
                    <div
                        className={cn(
                            'relative flex items-center justify-center transition-colors',
                            isActive
                                ? 'text-[var(--color-google-blue)]'
                                : 'text-gray-500 group-hover:text-gray-700'
                        )}
                    >
                        {icon}

                        {/* 배지 점 (미니 모드) */}
                        <AnimatePresence>
                            {!isFull && badge !== undefined && (
                                <motion.span
                                    className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-[var(--color-google-blue)] border-2 border-slate-200"
                                    initial={{ scale: 0 }}
                                    animate={{ scale: 1 }}
                                    exit={{ scale: 0 }}
                                    transition={fadeTransition}
                                />
                            )}
                        </AnimatePresence>

                        {/* 활성 상태 점 (미니 모드) */}
                        <AnimatePresence>
                            {!isFull && showDot && !isActive && (
                                <motion.span
                                    className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-[var(--color-google-green)] border-2 border-slate-200"
                                    initial={{ scale: 0 }}
                                    animate={{ scale: 1 }}
                                    exit={{ scale: 0 }}
                                    transition={fadeTransition}
                                />
                            )}
                        </AnimatePresence>
                    </div>
                </div>

                {/* 라벨 + 배지 영역 (full 모드 - opacity만 전환하여 버벅임 방지) */}
                <AnimatePresence>
                    {isFull && (
                        <motion.div
                            className="flex items-center flex-1 h-full overflow-hidden"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            transition={fadeTransition}
                        >
                            <span className={cn(
                                'text-base whitespace-nowrap leading-none',
                                isActive ? 'text-[var(--color-google-blue)] font-semibold' : 'font-medium'
                            )}>
                                {label}
                            </span>

                            {/* 배지 */}
                            {badge !== undefined && (
                                <span className="ml-auto mr-4 px-2.5 py-0.5 rounded-full text-[10px] font-bold text-white bg-[var(--color-google-blue)] shadow-sm">
                                    {badge}
                                </span>
                            )}

                            {/* 활성 상태 점 */}
                            {showDot && !isActive && (
                                <span className="ml-auto mr-4 w-2 h-2 rounded-full bg-[var(--color-google-green)]" />
                            )}
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* 스크린 리더 전용 라벨 (미니 모드) */}
                {!isFull && (
                    <span className="sr-only">
                        {label}
                    </span>
                )}
            </div>

            {/* 미니 모드 라벨 (아이템 아래) */}
            <AnimatePresence>
                {!isFull && (
                    <motion.div
                        className="flex justify-center mt-1"
                        initial={{ opacity: 0, y: -4 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -4 }}
                        transition={fadeTransition}
                    >
                        <span className={cn(
                            'text-xs leading-tight font-medium',
                            isActive ? 'text-[var(--color-google-blue)] font-semibold' : 'text-gray-500'
                        )}>
                            {label}
                        </span>
                    </motion.div>
                )}
            </AnimatePresence>
        </Link>
    );
};
