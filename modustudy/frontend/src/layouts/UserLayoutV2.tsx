// 로그인 사용자 전용 레이아웃 V2 - 학습 관리 중심

import '@/features/dashboard-v2/styles/DashboardV2.css';
import { useEffect, useState, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { RightSideBarV2 } from './components-v2/RightSideBarV2';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';
import { Bell, User, Settings, LogOut } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface UserLayoutV2Props {
    children: React.ReactNode;
}

export const UserLayoutV2: React.FC<UserLayoutV2Props> = ({ children }) => {
    const sidebarMode = useUIStore((state) => state.sidebarMode);
    const activeRightTab = useUIStore((state) => state.activeRightTab);
    const setSidebarMode = useUIStore((state) => state.setSidebarMode);
    const setActiveRightTab = useUIStore((state) => state.setActiveRightTab);
    const { user, logout } = useAuthStore();
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);
    const [isNotificationOpen, setIsNotificationOpen] = useState(false);
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const notificationRef = useRef<HTMLDivElement>(null);
    const profileRef = useRef<HTMLDivElement>(null);
    const location = useLocation();

    // 반응형 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // 반응형 1000px 이하에서 사이드바 미니 모드로 전환
    useEffect(() => {
        if (windowWidth <= 1000 && sidebarMode === 'full') {
            setSidebarMode('mini');
        }
    }, [windowWidth, sidebarMode, setSidebarMode]);

    // 반응형 600px 이하에서 우측 사이드바 자동 닫기
    useEffect(() => {
        if (windowWidth <= 600 && activeRightTab) {
            setActiveRightTab(null);
        }
    }, [windowWidth, activeRightTab, setActiveRightTab]);

    // 회의 룸 진입 시 좌측 사이드바 자동 닫기
    useEffect(() => {
        const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
        if (isMeetingRoom && sidebarMode !== 'closed') {
            setSidebarMode('closed');
        }
    }, [location.pathname, sidebarMode, setSidebarMode]);

    // 드롭다운 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (notificationRef.current && !notificationRef.current.contains(event.target as Node)) {
                setIsNotificationOpen(false);
            }
            if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
                setIsProfileOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const isCompactMode = windowWidth <= 600;
    const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
    const shouldHideHeader = isMeetingRoom;

    return (
        <div className="flex flex-col h-screen bg-slate-200 overflow-hidden">
            {/* 헤더 - 회의 룸에서는 숨김 */}
            {!shouldHideHeader && (
                <header className="h-16 w-full bg-slate-200 flex items-center justify-between px-6 flex-shrink-0 z-50">
                    <div className="flex items-center gap-4">
                        {/* 로고 영역 */}
                        <Link to="/dashboard" className="flex items-center">
                            <SquizLogoNew width={160} height={55} className="scale-110 origin-left" />
                        </Link>
                    </div>

                    {/* 우측 인증 영역 */}
                    <div className="flex items-center gap-2 md:gap-4 pr-14">
                        {/* 알림 드롭다운 */}
                        <div ref={notificationRef} className="relative">
                            <button
                                onClick={() => {
                                    setIsNotificationOpen(!isNotificationOpen);
                                    setIsProfileOpen(false);
                                }}
                                className={cn(
                                    'p-2 rounded-lg transition-colors relative',
                                    'hover:bg-white/50',
                                    isNotificationOpen && 'bg-white/60'
                                )}
                                aria-label="Notifications"
                            >
                                <div className="relative">
                                    {/* 읽지 않은 알림 표시 */}
                                    <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full" />
                                    <Bell size={20} className="text-gray-700" />
                                </div>
                            </button>

                            {/* 알림 드롭다운 메뉴 */}
                            {isNotificationOpen && (
                                <div className={cn(
                                    'absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-gray-200',
                                    'animate-in fade-in slide-in-from-top-2 duration-200 z-50'
                                )}>
                                    {/* 헤더 */}
                                    <div className="px-4 py-3 border-b border-gray-100">
                                        <h6 className="text-base font-semibold text-gray-900">알림</h6>
                                    </div>

                                    {/* 알림 목록 */}
                                    <div className="max-h-96 overflow-y-auto">
                                        <div className="p-4 text-center text-sm text-gray-500">
                                            새로운 알림이 없습니다
                                        </div>
                                    </div>

                                    {/* 푸터 */}
                                    <div className="px-4 py-2 border-t border-gray-100">
                                        <Link
                                            to="/notifications"
                                            className="flex items-center justify-center gap-1 text-sm text-study-blue hover:text-study-blue-dark no-underline"
                                            onClick={() => setIsNotificationOpen(false)}
                                        >
                                            <span>전체 보기</span>
                                        </Link>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* 프로필 드롭다운 */}
                        <div ref={profileRef} className="relative">
                            <button
                                onClick={() => {
                                    setIsProfileOpen(!isProfileOpen);
                                    setIsNotificationOpen(false);
                                }}
                                className={cn(
                                    'flex items-center p-1 rounded-lg transition-colors',
                                    'hover:bg-white/50',
                                    isProfileOpen && 'bg-white/60'
                                )}
                                aria-label="Profile menu"
                            >
                                <div className="w-8 h-8 md:w-9 md:h-9 rounded-full bg-gradient-to-br from-study-blue to-study-blue-dark flex items-center justify-center text-white text-sm font-bold overflow-hidden">
                                    {user?.avatar ? (
                                        <img
                                            src={user.avatar}
                                            alt="Profile"
                                            className="w-full h-full object-cover"
                                        />
                                    ) : (
                                        (user?.nickname || user?.name)?.charAt(0) || 'U'
                                    )}
                                </div>
                            </button>

                            {/* 프로필 드롭다운 메뉴 */}
                            {isProfileOpen && (
                                <div className={cn(
                                    'absolute right-0 mt-2 w-60 bg-white rounded-lg shadow-lg border border-gray-200',
                                    'animate-in fade-in slide-in-from-top-2 duration-200 z-50'
                                )}>
                                    {/* 헤더 */}
                                    <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2">
                                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-study-blue to-study-blue-dark flex items-center justify-center text-white text-sm font-bold overflow-hidden">
                                            {user?.avatar ? (
                                                <img
                                                    src={user.avatar}
                                                    alt="Profile"
                                                    className="w-full h-full object-cover"
                                                />
                                            ) : (
                                                (user?.nickname || user?.name)?.charAt(0) || 'U'
                                            )}
                                        </div>
                                        <div>
                                            <h6 className="text-base font-semibold text-gray-900">
                                                {user?.nickname || user?.name || '사용자'}
                                            </h6>
                                            <small className="text-gray-500">회원</small>
                                        </div>
                                    </div>

                                    {/* 메뉴 아이템 */}
                                    <ul className="py-2">
                                        <li>
                                            <Link
                                                to="/profile"
                                                className={cn(
                                                    'flex items-center gap-2 px-4 py-2 text-sm text-gray-700',
                                                    'hover:bg-gray-50 transition-colors no-underline'
                                                )}
                                                onClick={() => setIsProfileOpen(false)}
                                            >
                                                <User size={16} />
                                                <span>마이 프로필</span>
                                            </Link>
                                        </li>
                                        <li>
                                            <Link
                                                to="/setting"
                                                className={cn(
                                                    'flex items-center gap-2 px-4 py-2 text-sm text-gray-700',
                                                    'hover:bg-gray-50 transition-colors no-underline'
                                                )}
                                                onClick={() => setIsProfileOpen(false)}
                                            >
                                                <Settings size={16} />
                                                <span>설정</span>
                                            </Link>
                                        </li>
                                    </ul>

                                    {/* 푸터 */}
                                    <div className="px-3 py-2 border-t border-gray-100">
                                        <button
                                            onClick={() => {
                                                logout();
                                                setIsProfileOpen(false);
                                            }}
                                            className={cn(
                                                'w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg',
                                                'bg-red-50 text-red-600 hover:bg-red-100 transition-colors text-sm font-medium'
                                            )}
                                        >
                                            <LogOut size={16} />
                                            <span>로그아웃</span>
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </header>
            )}

            <div className="flex flex-1 overflow-hidden">
                <Sidebar />

                {/* 페이지 콘텐츠 */}
                <main
                    className={cn(
                        'flex-1 flex flex-col overflow-hidden',
                        'pb-6 pr-0 pl-0 bg-slate-200 transition-all duration-300 ease-out',
                        shouldHideHeader ? 'pt-0' : 'pt-2'
                    )}
                >
                    {/* 둥근 메인 컨테이너 섹션 */}
                    <section
                        id="main-content-scroll"
                        className="bg-white rounded-3xl h-full overflow-auto scrollbar-hide"
                        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                    >
                        {children}
                    </section>
                </main>

                {!isCompactMode && <RightSideBarV2 />}
            </div>
        </div>
    );
};
