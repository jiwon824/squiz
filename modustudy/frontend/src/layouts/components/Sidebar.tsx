import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { cn } from '@/shared/utils/cn';
import { useCallback, useState, useRef, useEffect } from 'react';
import { studyApi } from '@/api/endpoints/studyApi';
import {
    QuizIcon,
    StudyIcon,
    DashboardIcon,
    CalendarIcon,
    RecruitmentIcon,
    SettingIcon,
} from '@/shared/components/icons/SidebarIcons';

// 사이드바 너비 상수
const SIDEBAR_WIDTH = 80;

// 사이드바 너비 전환 스프링 애니메이션
const sidebarSpring = { type: 'spring' as const, damping: 26, stiffness: 300 };

export const Sidebar = () => {
    const sidebarMode = useUIStore((state) => state.sidebarMode);
    const toggleSidebar = useUIStore((state) => state.toggleSidebar);
    const { isLoggedIn } = useAuthStore();
    const location = useLocation();
    const navigate = useNavigate();

    const isClosed = sidebarMode === 'closed';

    // 유저가 가입한 스터디 목록 (API에서 가져옴)
    const [userStudies, setUserStudies] = useState<{ id: number; name: string; topic: string }[]>([]);

    // 로그인 상태 변경 시 내 스터디 목록 조회
    useEffect(() => {
        if (isLoggedIn) {
            studyApi.getMyStudies(0, 10)
                .then((response: any) => {
                    // 백엔드에서 StudyResponse DTO로 반환하므로 정상 객체 처리
                    const content = response?.content || [];
                    const studies = content.map((study: any) => ({
                        id: study.id,
                        name: study.name,
                        topic: study.topic?.name || '스터디',
                    }));
                    setUserStudies(studies);
                })
                .catch((error) => {
                    console.error('[Sidebar] 내 스터디 목록 조회 실패:', error);
                    setUserStudies([]);
                });
        } else {
            setUserStudies([]);
        }
    }, [isLoggedIn]);

    // 스터디 팝오버 아이템 클릭 시 워크스페이스로 이동
    const handleStudyPopoverClick = useCallback((studyId: number) => {
        navigate(`/study/${studyId}/workspace`);
    }, [navigate]);

    // 대시보드에서 다른 페이지로 전환 시 애니메이션 처리
    const handleTransitionNavigate = useCallback((path: string) => {
        // 대시보드가 아닌 경우 바로 이동
        if (location.pathname !== '/dashboard') {
            navigate(path);
            return;
        }

        // sessionStorage에 플래그 설정
        sessionStorage.setItem('fromDashboard', 'true');

        // 퇴장 애니메이션을 위한 이벤트 발생
        window.dispatchEvent(new CustomEvent('dashboardExit'));

        // 애니메이션 완료 후 네비게이션 (500ms)
        setTimeout(() => {
            navigate(path);
        }, 500);
    }, [location.pathname, navigate]);

    return (
        <motion.aside
            className={cn(
                "h-full flex flex-col bg-slate-200 flex-shrink-0",
                isClosed ? "overflow-hidden" : "overflow-visible"
            )}
            animate={{ width: isClosed ? 0 : SIDEBAR_WIDTH, minWidth: isClosed ? 0 : SIDEBAR_WIDTH }}
            transition={sidebarSpring}
        >
            {/* 햄버거 토글 버튼 */}
            <div
                className="flex items-center justify-center pt-5 pb-4 flex-shrink-0"
                style={{ width: SIDEBAR_WIDTH }}
            >
                <button
                    onClick={toggleSidebar}
                    className="w-12 h-12 flex items-center justify-center rounded-2xl hover:bg-white/60 transition-colors"
                    aria-label="사이드바 닫기"
                >
                    <span className="material-icons text-gray-600 text-[24px]">
                        menu_open
                    </span>
                </button>
            </div>

            {/* 메뉴 아이템들 */}
            <nav
                className="flex-1 space-y-3"
                style={{ width: SIDEBAR_WIDTH }}
            >
                <SidebarItem
                    icon={<DashboardIcon className="w-6 h-6" />}
                    label="대시보드"
                    path="/dashboard"
                    isActive={location.pathname === '/dashboard'}
                />

                <SidebarItem
                    icon={<QuizIcon className="w-6 h-6" />}
                    label="퀴즈"
                    path="/quiz"
                    isActive={location.pathname === '/quiz'}
                    badge={5}
                    useTransition
                    onTransitionNavigate={handleTransitionNavigate}
                />

                <SidebarItem
                    icon={<StudyIcon className="w-6 h-6" />}
                    label="스터디"
                    path="/study"
                    isActive={location.pathname === '/study'}
                    showDot={userStudies.length > 0}
                    showPopover={userStudies.length > 0}
                    popoverStudies={userStudies}
                    onPopoverItemClick={handleStudyPopoverClick}
                />

                <SidebarItem
                    icon={<RecruitmentIcon className="w-6 h-6" />}
                    label="팀원 모집"
                    path="/recruitment"
                    isActive={location.pathname === '/recruitment'}
                />

                <SidebarItem
                    icon={<CalendarIcon className="w-6 h-6" />}
                    label="캘린더"
                    path="/calendar"
                    isActive={location.pathname === '/calendar'}
                />

                <SidebarItem
                    icon={<SettingIcon className="w-6 h-6" />}
                    label="설정"
                    path="/setting"
                    isActive={location.pathname === '/setting'}
                />
            </nav>
        </motion.aside>
    );
};

// 스터디 팝오버 아이템 타입
interface StudyPopoverItem {
    id: number;
    name: string;
    topic: string;
}

// 사이드바 아이템 컴포넌트
interface SidebarItemProps {
    icon: React.ReactNode;
    label: string;
    path: string;
    isActive?: boolean;
    badge?: number;
    showDot?: boolean;
    /** 페이지 전환 애니메이션 사용 여부 */
    useTransition?: boolean;
    /** 전환 애니메이션 네비게이션 핸들러 */
    onTransitionNavigate?: (path: string) => void;
    /** 호버 시 팝오버 표시 여부 */
    showPopover?: boolean;
    /** 팝오버에 표시할 스터디 목록 */
    popoverStudies?: StudyPopoverItem[];
    /** 팝오버 아이템 클릭 핸들러 */
    onPopoverItemClick?: (studyId: number) => void;
}

const SidebarItem: React.FC<SidebarItemProps> = ({
    icon,
    label,
    path,
    isActive,
    badge,
    showDot,
    useTransition,
    onTransitionNavigate,
    showPopover,
    popoverStudies,
    onPopoverItemClick,
}) => {
    const [isHovered, setIsHovered] = useState(false);
    const enterTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const leaveTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const popoverRef = useRef<HTMLDivElement>(null);

    // 호버 시작 시 딜레이 후 팝오버 표시
    const handleMouseEnter = () => {
        // leave 타이머가 있으면 취소 (팝오버로 이동 중)
        if (leaveTimeoutRef.current) {
            clearTimeout(leaveTimeoutRef.current);
            leaveTimeoutRef.current = null;
        }

        if (showPopover && popoverStudies && popoverStudies.length > 0) {
            enterTimeoutRef.current = setTimeout(() => {
                setIsHovered(true);
            }, 150);
        }
    };

    // 호버 종료 시 딜레이 후 팝오버 숨김
    const handleMouseLeave = () => {
        if (enterTimeoutRef.current) {
            clearTimeout(enterTimeoutRef.current);
            enterTimeoutRef.current = null;
        }
        // 딜레이를 줘서 팝오버로 이동할 시간 확보
        leaveTimeoutRef.current = setTimeout(() => {
            setIsHovered(false);
        }, 150);
    };

    // 팝오버에 마우스 진입 시 leave 타이머 취소
    const handlePopoverEnter = () => {
        if (leaveTimeoutRef.current) {
            clearTimeout(leaveTimeoutRef.current);
            leaveTimeoutRef.current = null;
        }
    };

    // 컴포넌트 언마운트 시 타이머 정리
    useEffect(() => {
        return () => {
            if (enterTimeoutRef.current) {
                clearTimeout(enterTimeoutRef.current);
            }
            if (leaveTimeoutRef.current) {
                clearTimeout(leaveTimeoutRef.current);
            }
        };
    }, []);

    // 전환 애니메이션이 필요한 경우 버튼 클릭 핸들러
    const handleClick = () => {
        if (onTransitionNavigate) {
            onTransitionNavigate(path);
        }
    };

    // 내부 컨텐츠
    const content = (
        <>
            <div
                className={cn(
                    'flex items-center justify-center h-14 rounded-2xl mx-3 relative',
                    'transition-colors duration-200',
                    isActive
                        ? 'bg-[var(--color-google-blue)]/10 text-[var(--color-google-blue)]'
                        : 'text-gray-600 hover:bg-white/60 hover:text-gray-900'
                )}
            >
                {/* 활성 상태 인디케이터 바 */}
                <span
                    className={cn(
                        'absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-6 bg-[var(--color-google-blue)] rounded-r-full',
                        'transition-opacity duration-150',
                        isActive ? 'opacity-100' : 'opacity-0'
                    )}
                />

                {/* 아이콘 영역 */}
                <div
                    className={cn(
                        'relative flex items-center justify-center transition-colors',
                        isActive
                            ? 'text-[var(--color-google-blue)]'
                            : 'text-gray-500 group-hover:text-gray-700'
                    )}
                >
                    {icon}

                    {/* 배지 점 */}
                    {badge !== undefined && (
                        <span className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-[var(--color-google-blue)] border-2 border-slate-200" />
                    )}

                    {/* 활성 상태 점 */}
                    {showDot && !isActive && (
                        <span className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-[var(--color-google-green)] border-2 border-slate-200" />
                    )}
                </div>
            </div>

            {/* 아이콘 아래 라벨 */}
            <div className="flex justify-center mt-1">
                <span className={cn(
                    'text-xs leading-tight font-medium',
                    isActive ? 'text-[var(--color-google-blue)] font-semibold' : 'text-gray-500'
                )}>
                    {label}
                </span>
            </div>
        </>
    );

    // 팝오버 렌더링
    const renderPopover = () => {
        if (!showPopover || !isHovered || !popoverStudies || popoverStudies.length === 0) {
            return null;
        }

        return (
            <AnimatePresence>
                <motion.div
                    ref={popoverRef}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    transition={{ duration: 0.15 }}
                    className="absolute left-full top-0 z-50 flex items-start"
                    onMouseEnter={handlePopoverEnter}
                    onMouseLeave={handleMouseLeave}
                >
                    {/* 투명한 브릿지 영역 - 아이템과 팝오버 사이 연결 */}
                    <div className="w-3 h-16 bg-transparent" />
                    <div className="bg-white rounded-xl shadow-lg border border-gray-100 py-2 min-w-[200px] max-w-[280px]">
                        <div className="px-4 py-2 border-b border-gray-100">
                            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                                내 스터디
                            </span>
                        </div>
                        <div className="max-h-[300px] overflow-y-auto">
                            {popoverStudies.map((study) => (
                                <button
                                    key={study.id}
                                    type="button"
                                    onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        onPopoverItemClick?.(study.id);
                                        setIsHovered(false);
                                    }}
                                    className="w-full px-4 py-3 text-left hover:bg-gray-50 transition-colors flex flex-col gap-0.5"
                                >
                                    <span className="text-sm font-medium text-gray-800 truncate">
                                        {study.name}
                                    </span>
                                    <span className="text-xs text-gray-500">
                                        {study.topic}
                                    </span>
                                </button>
                            ))}
                        </div>
                    </div>
                </motion.div>
            </AnimatePresence>
        );
    };

    // 팝오버가 있는 경우 wrapper로 감싸기
    const itemWrapper = (children: React.ReactNode) => (
        <div
            className="relative"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
        >
            {children}
            {renderPopover()}
        </div>
    );

    // 전환 애니메이션 사용 시 버튼, 아니면 Link
    if (useTransition) {
        return itemWrapper(
            <button
                type="button"
                onClick={handleClick}
                className="block no-underline group w-full text-left bg-transparent border-none cursor-pointer p-0"
            >
                {content}
            </button>
        );
    }

    return itemWrapper(
        <Link to={path} className="block no-underline group">
            {content}
        </Link>
    );
};
