// 로그인 사용자 전용 레이아웃 V2 - 학습 관리 중심

import '@/features/dashboard-v2/styles/DashboardV2.css';
import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { RightSideBarV2 } from './components-v2/RightSideBarV2';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';
import { cn } from '@/shared/utils/cn';

interface UserLayoutV2Props {
    children: React.ReactNode;
}

export const UserLayoutV2: React.FC<UserLayoutV2Props> = ({ children }) => {
    const { isSidebarOpen, toggleSidebar, closeSidebar, activeRightTab, setActiveRightTab } = useUIStore();
    const { user, logout } = useAuthStore();
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);
    const location = useLocation();

    // 반응형 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // 반응형 1000px 이하에서 사이드바 자동 닫기
    useEffect(() => {
        if (windowWidth <= 1000 && isSidebarOpen) {
            closeSidebar();
        }
    }, [windowWidth, isSidebarOpen, closeSidebar]);

    // 반응형 600px 이하에서 우측 사이드바 자동 닫기
    useEffect(() => {
        if (windowWidth <= 600 && activeRightTab) {
            setActiveRightTab(null);
        }
    }, [windowWidth, activeRightTab, setActiveRightTab]);

    // 회의 룸 진입 시 좌측 사이드바 자동 닫기
    useEffect(() => {
        const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
        if (isMeetingRoom && isSidebarOpen) {
            closeSidebar();
        }
    }, [location.pathname, isSidebarOpen, closeSidebar]);

    const isCompactMode = windowWidth <= 600;
    const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
    const shouldHideHeader = isMeetingRoom;

    return (
        <div className="flex flex-col h-screen bg-slate-200 overflow-hidden">
            {/* 헤더 - 회의 룸에서는 숨김 */}
            {!shouldHideHeader && (
                <header className="h-16 w-full bg-slate-200 flex items-center justify-between px-6 flex-shrink-0 z-50">
                    <div className="flex items-center gap-4">
                        {/* 사이드바 토글 버튼 */}
                        <button
                            onClick={toggleSidebar}
                            className="p-2 rounded-google hover:bg-study-blue/10 transition-colors"
                            aria-label="Toggle sidebar"
                        >
                            <span className="material-icons text-study-blue">
                                {isSidebarOpen ? 'menu_open' : 'menu'}
                            </span>
                        </button>

                        {/* 로고 영역 */}
                        <Link to="/dashboard-v2" className="flex items-center">
                            <SquizLogoNew width={160} height={55} className="scale-110 origin-left" />
                        </Link>
                    </div>

                    {/* 우측 인증 영역 */}
                    <div className="flex items-center gap-3 pr-14 h-full">
                        <div className="w-px h-6 bg-study-blue/20 mx-1" />

                        <div className="flex items-center gap-3 ml-2">
                            <Link
                                to="/profile"
                                className={cn(
                                    'flex items-center gap-3 p-1 px-4',
                                    'bg-white/60 hover:bg-white hover:shadow-md hover:-translate-y-0.5',
                                    'transition-all rounded-full border border-study-blue/20',
                                    'backdrop-blur-md shadow-sm group cursor-pointer no-underline active:scale-95'
                                )}
                            >
                                <div className={cn(
                                    'w-9 h-9 rounded-full',
                                    'bg-gradient-to-br from-study-blue to-study-blue-dark',
                                    'flex items-center justify-center',
                                    'text-white text-sm font-bold overflow-hidden',
                                    'shadow-inner ring-2 ring-white/50'
                                )}>
                                    {user?.avatar ? (
                                        <img
                                            src={user.avatar}
                                            alt="프로필"
                                            className="w-full h-full object-cover group-hover:scale-110 transition-transform"
                                        />
                                    ) : (
                                        (user?.nickname || user?.name)?.charAt(0) || 'U'
                                    )}
                                </div>
                                {(user?.nickname || user?.name) && (
                                    <span className="text-base font-bold text-study-text-dark tracking-tight">
                                        {user.nickname || user.name}님
                                    </span>
                                )}
                            </Link>
                            <button
                                onClick={logout}
                                className={cn(
                                    'p-2 px-4 bg-gray-100 hover:bg-gray-200',
                                    'text-gray-600 rounded-pill text-xs font-bold',
                                    'transition-all hover:scale-105 active:scale-95'
                                )}
                            >
                                로그아웃
                            </button>
                        </div>
                    </div>
                </header>
            )}

            <div className="flex flex-1 overflow-hidden relative">
                <Sidebar />

                {/* 페이지 콘텐츠 */}
                <main
                    className={cn(
                        'flex-1 flex flex-col overflow-hidden',
                        'pb-6 bg-slate-200 transition-all duration-300 ease-out',
                        shouldHideHeader ? 'pt-0' : 'pt-2',
                        isSidebarOpen ? 'pl-6' : 'pl-4',
                        isCompactMode ? 'pr-2' : activeRightTab ? 'pr-80' : 'pr-14'
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
