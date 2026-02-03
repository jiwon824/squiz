// 로그인 사용자 전용 레이아웃 V2 - 학습 관리 중심

import '@/features/dashboard-v2/styles/DashboardV2.css';
import { useEffect, useState, useRef, useMemo } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { RightSideBarV2 } from './components-v2/RightSideBarV2';
import { useUIStore, SidebarMode } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { useNotificationStore } from '@/features/notification/store/notificationStore';
import { useGamificationStore } from '@/features/gamification/store/gamificationStore';
import { LevelUpModal, LevelBadge } from '@/features/gamification/components';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';
import { Bell, User, Settings, LogOut, Check, X } from 'lucide-react';
import { ButtonSpinner } from '@/shared/components/Spinner';
import { studyApi } from '@/api/endpoints/studyApi';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl, DEFAULT_PROFILE_IMAGE } from '@/shared/utils/profileImage';
import { TopProgressBar } from '@/shared/components/loading';

// 반응형 브레이크포인트 기준값 (CSS 논리 픽셀 기준, 브라우저 확대/축소 자동 반영)
const BREAKPOINTS = {
    MOBILE: 600,        // 모바일: 좌측 사이드바 닫힘, 우측 사이드바 숨김
    TABLET: 1000,       // 태블릿 이상: 좌측 사이드바 mini 모드
} as const;

interface UserLayoutV2Props {
    children: React.ReactNode;
    /** 워크스페이스에서 진입 시 애니메이션 적용 */
    isEnteringFromWorkspace?: boolean;
    /** 워크스페이스로 퇴장 시 애니메이션 적용 */
    isExitingToWorkspace?: boolean;
}

export const UserLayoutV2: React.FC<UserLayoutV2Props> = ({ children, isEnteringFromWorkspace = false, isExitingToWorkspace = false }) => {
    const sidebarMode = useUIStore((state) => state.sidebarMode);
    const activeRightTab = useUIStore((state) => state.activeRightTab);
    const setSidebarMode = useUIStore((state) => state.setSidebarMode);
    const setActiveRightTab = useUIStore((state) => state.setActiveRightTab);
    const showToast = useUIStore((state) => state.showToast);
    const { user, logout, isLoggedIn } = useAuthStore();
    const { notifications, unreadCount, fetchNotifications, fetchUnreadCount, markNotificationAsRead } = useNotificationStore();
    const { stats, fetchStats, isLevelUpModalOpen, levelUpInfo, closeLevelUpModal } = useGamificationStore();
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);
    const [isNotificationOpen, setIsNotificationOpen] = useState(false);
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const [isDashboardExiting, setIsDashboardExiting] = useState(false);
    const [leavingStudyId, setLeavingStudyId] = useState<number | null>(null);  // 탈퇴 처리 중인 스터디 ID
    const notificationRef = useRef<HTMLDivElement>(null);
    const profileRef = useRef<HTMLDivElement>(null);
    const prevSidebarModeRef = useRef<SidebarMode | null>(null);
    const location = useLocation();
    const navigate = useNavigate();

    // 대시보드에서 진입 시 애니메이션 플래그 (최초 렌더 시 한 번만 확인)
    const isEnteringFromDashboard = useMemo(() => {
        const flag = sessionStorage.getItem('fromDashboard') === 'true';
        if (flag) {
            sessionStorage.removeItem('fromDashboard');
        }
        return flag;
    }, []);

    // 퀴즈 페이지에서 돌아올 때 애니메이션 플래그
    const isEnteringFromQuiz = useMemo(() => {
        const flag = sessionStorage.getItem('fromQuiz') === 'true';
        if (flag) {
            sessionStorage.removeItem('fromQuiz');
        }
        return flag;
    }, []);

    // 대시보드 퇴장 이벤트 리스너
    useEffect(() => {
        const handleDashboardExit = () => {
            setIsDashboardExiting(true);
        };

        window.addEventListener('dashboardExit', handleDashboardExit);
        return () => {
            window.removeEventListener('dashboardExit', handleDashboardExit);
        };
    }, []);

    // 페이지 이동 시 퇴장 애니메이션 상태 리셋
    useEffect(() => {
        setIsDashboardExiting(false);
    }, [location.pathname]);

    // 알림 데이터 로드 + 실시간 polling (30초마다)
    useEffect(() => {
        if (!isLoggedIn) return;

        // 초기 로드
        fetchUnreadCount();

        // 30초마다 알림 체크
        const interval = setInterval(() => {
            fetchUnreadCount();
        }, 30000);

        return () => clearInterval(interval);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoggedIn]);

    // 게이미피케이션 통계 로드 (로그인 시 + 1분마다 갱신)
    useEffect(() => {
        if (!isLoggedIn) return;

        // 초기 로드
        fetchStats();

        // 1분마다 통계 갱신 (레벨업 감지용)
        const interval = setInterval(() => {
            fetchStats();
        }, 60000);

        return () => clearInterval(interval);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoggedIn]);

    // 알림 드롭다운 열릴 때 알림 목록 로드
    useEffect(() => {
        if (isNotificationOpen && isLoggedIn) {
            fetchNotifications(0, 10);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isNotificationOpen, isLoggedIn]);

    // 반응형 리사이즈 + 브라우저 확대/축소 감지
    // window.innerWidth는 CSS 논리 픽셀 기준이므로 확대 시 값이 줄어듦
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);

        // visualViewport: 핀치 줌 등 시각적 뷰포트 변경 감지 (모바일 대응)
        const vv = window.visualViewport;
        if (vv) {
            vv.addEventListener('resize', handleResize);
        }

        return () => {
            window.removeEventListener('resize', handleResize);
            if (vv) {
                vv.removeEventListener('resize', handleResize);
            }
        };
    }, []);

    // 반응형 사이드바 자동 모드 전환 (화면 크기 변경 시에만 동작)
    const prevWidthRef = useRef(windowWidth);
    useEffect(() => {
        const prevWidth = prevWidthRef.current;
        prevWidthRef.current = windowWidth;

        // 화면 크기가 실제로 변경되지 않았으면 무시 (수동 토글 보호)
        if (prevWidth === windowWidth) return;

        const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
        if (isMeetingRoom) return;

        if (windowWidth <= BREAKPOINTS.MOBILE) {
            // 모바일로 축소: 완전 닫기
            if (sidebarMode !== 'closed') {
                setSidebarMode('closed');
            }
        } else if (prevWidth <= BREAKPOINTS.MOBILE) {
            // 모바일에서 벗어남: mini로 복원
            setSidebarMode('mini');
        }
    }, [windowWidth, sidebarMode, setSidebarMode, location.pathname]);

    // 모바일에서 우측 사이드바 자동 닫기
    useEffect(() => {
        if (windowWidth <= BREAKPOINTS.MOBILE && activeRightTab) {
            setActiveRightTab(null);
        }
    }, [windowWidth, activeRightTab, setActiveRightTab]);

    // 회의 룸 진입 시 좌측 사이드바 자동 닫기
    useEffect(() => {
        const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
        if (isMeetingRoom && sidebarMode !== 'closed') {
            prevSidebarModeRef.current = sidebarMode;
            setSidebarMode('closed');
        }
    }, [location.pathname, sidebarMode, setSidebarMode]);

    // 회의 룸에서 나올 때 사이드바 복원 (경로 변경 시에만 동작)
    const prevPathnameRef = useRef(location.pathname);
    useEffect(() => {
        const prevPathname = prevPathnameRef.current;
        prevPathnameRef.current = location.pathname;

        const wasMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(prevPathname);
        const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);

        // 회의 룸에서 벗어났을 때만 사이드바 복원
        if (wasMeetingRoom && !isMeetingRoom && prevSidebarModeRef.current) {
            setSidebarMode(prevSidebarModeRef.current);
            prevSidebarModeRef.current = null;
        }
    }, [location.pathname, setSidebarMode]);

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

    const isCompactMode = windowWidth <= BREAKPOINTS.MOBILE;
    const isMeetingRoom = /^\/study\/\d+\/meetings\/\d+\/room/.test(location.pathname);
    const shouldHideHeader = isMeetingRoom;

    return (
        <div className={cn(
            "flex flex-col h-screen bg-slate-200 overflow-hidden",
            isEnteringFromWorkspace && "layout-entering-from-workspace",
            isExitingToWorkspace && "layout-exiting-to-workspace",
            isDashboardExiting && "layout-exiting-to-workspace",
            isEnteringFromDashboard && "layout-entering-from-dashboard",
            isEnteringFromQuiz && "layout-entering-from-quiz"
        )}>
            {/* 상단 로딩 게이지바 */}
            <TopProgressBar />

            {/* 헤더 - 회의 룸에서는 숨김 */}
            {!shouldHideHeader && (
                <header className={cn(
                    "h-16 w-full bg-slate-200 flex items-center flex-shrink-0 z-50",
                    isEnteringFromWorkspace && "layout-header-enter"
                )}>
                    {/* 좌측 영역: 사이드바와 동일 너비 — closed 시 햄버거 버튼 표시 */}
                    <div
                        className="flex-shrink-0 flex items-center justify-center transition-all duration-300 ease-out"
                        style={{
                            width: sidebarMode === 'closed' ? 64 : 80,
                            height: 64,
                        }}
                    >
                        {sidebarMode === 'closed' && (
                            <button
                                onClick={() => setSidebarMode('mini')}
                                className="w-12 h-12 flex items-center justify-center rounded-2xl hover:bg-white/60 transition-colors"
                                aria-label="사이드바 열기"
                            >
                                <span className="material-icons text-gray-600 text-[24px]">menu</span>
                            </button>
                        )}
                    </div>

                    {/* 콘텐츠 영역: 흰색 메인 시작점과 x축 정렬 */}
                    <div className="flex-1 flex items-center justify-between pr-14">
                        {/* 로고 — 흰색 콘텐츠 시작 위치와 일치 */}
                        <Link to="/dashboard" className="flex items-center">
                            <SquizLogoNew width={160} height={55} className="scale-110 origin-left" />
                        </Link>

                        {/* 우측 인증 영역 */}
                        <div className="flex items-center gap-2 md:gap-4">
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
                                    {unreadCount > 0 && (
                                        <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 rounded-full flex items-center justify-center text-[10px] text-white font-bold px-1">
                                            {unreadCount > 99 ? '99+' : unreadCount}
                                        </span>
                                    )}
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
                                    <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                                        <h6 className="text-base font-semibold text-gray-900">알림</h6>
                                        {unreadCount > 0 && (
                                            <span className="text-xs text-gray-500">{unreadCount}개의 새 알림</span>
                                        )}
                                    </div>

                                    {/* 알림 목록 (읽지 않은 알림만 표시) */}
                                    <div className="max-h-96 overflow-y-auto">
                                        {notifications.filter(n => !n.isRead).length === 0 ? (
                                            <div className="p-4 text-center text-sm text-gray-500">
                                                새로운 알림이 없습니다
                                            </div>
                                        ) : (
                                            notifications.filter(n => !n.isRead).slice(0, 10).map((notification) => (
                                                <div
                                                    key={notification.id}
                                                    onClick={() => {
                                                        // 읽음 처리
                                                        if (!notification.isRead) {
                                                            markNotificationAsRead(notification.id);
                                                        }
                                                        // 드롭다운 닫기
                                                        setIsNotificationOpen(false);
                                                        // 해당 페이지로 이동
                                                        const { referenceType, referenceId, type } = notification;
                                                        if (referenceType && referenceId) {
                                                            // 알림 타입에 따른 특수 처리
                                                            if (type === 'STUDY_START') {
                                                                // 스터디 시작 알림 → 워크스페이스(대시보드)로 이동
                                                                navigate(`/study/${referenceId}/workspace`);
                                                                return;
                                                            }
                                                            if (type === 'STUDY_EXTENSION') {
                                                                // 스터디 연장 알림 → 상세 페이지로 이동 (참가/불참 모달 표시)
                                                                navigate(`/study/${referenceId}?action=extension`);
                                                                return;
                                                            }

                                                                switch (referenceType) {
                                                                    case 'STUDY_APPLICATION':
                                                                        // 지원자 관리 탭으로 바로 이동
                                                                        navigate(`/study/manage/${referenceId}?tab=applicants`);
                                                                        break;
                                                                    case 'STUDY_EXCUSE':
                                                                        navigate(`/study/manage/${referenceId}?tab=excuse`);
                                                                        break;
                                                                    case 'STUDY':
                                                                        navigate(`/study/${referenceId}`);
                                                                        break;
                                                            case 'STUDY_SESSION':
                                                            case 'MEETING':
                                                            navigate(`/study/${referenceId}/workspace`);
                                                            break;
                                                            case 'SCHEDULE':
                                                                navigate('/calendar');
                                                                break;
                                                            case 'RECRUITMENT_POST':
                                                                navigate(`/recruitment?postId=${referenceId}`);
                                                                break;
                                                        }
                                                    }
                                                }}
                                                    className={cn(
                                                        'px-4 py-3 border-b border-gray-50 cursor-pointer transition-colors',
                                                        'hover:bg-gray-50',
                                                        !notification.isRead && 'bg-blue-50/50'
                                                    )}
                                                >
                                                    <div className="flex items-start gap-3">
                                                        {!notification.isRead && (
                                                            <span className="mt-2 w-2 h-2 bg-blue-500 rounded-full flex-shrink-0" />
                                                        )}
                                                        <div className={cn('flex-1', notification.isRead && 'ml-5')}>
                                                            <p className="text-sm font-medium text-gray-900 line-clamp-1">
                                                                {notification.title}
                                                            </p>
                                                            <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">
                                                                {notification.content}
                                                            </p>
                                                            <p className="text-[10px] text-gray-400 mt-1">
                                                                {new Date(notification.createdAt).toLocaleString('ko-KR', {
                                                                    month: 'short',
                                                                    day: 'numeric',
                                                                    hour: '2-digit',
                                                                    minute: '2-digit'
                                                                })}
                                                            </p>
                                                            {/* 스터디 연장 알림일 경우 참가/불참 버튼 표시 */}
                                                            {notification.type === 'STUDY_EXTENSION' && notification.referenceId && (
                                                                <div className="flex gap-2 mt-2">
                                                                    <button
                                                                        onClick={(e) => {
                                                                            e.stopPropagation();
                                                                            // 참가 - 읽음 처리 후 드롭다운 닫기
                                                                            markNotificationAsRead(notification.id);
                                                                            showToast('스터디에 계속 참가합니다.', 'success');
                                                                        }}
                                                                        className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-green-500 hover:bg-green-600 rounded-lg transition-colors"
                                                                    >
                                                                        <Check size={14} />
                                                                        참가
                                                                    </button>
                                                                    <button
                                                                        onClick={async (e) => {
                                                                            e.stopPropagation();
                                                                            if (leavingStudyId === notification.referenceId) return;
                                                                            setLeavingStudyId(notification.referenceId);
                                                                            try {
                                                                                await studyApi.leaveStudy(notification.referenceId!);
                                                                                markNotificationAsRead(notification.id);
                                                                                showToast('스터디에서 탈퇴했습니다.', 'success');
                                                                            } catch (error: any) {
                                                                                const message = error.response?.data?.message || '스터디 탈퇴에 실패했습니다.';
                                                                                showToast(message, 'error');
                                                                            } finally {
                                                                                setLeavingStudyId(null);
                                                                            }
                                                                        }}
                                                                        disabled={leavingStudyId === notification.referenceId}
                                                                        className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-red-500 hover:bg-red-600 rounded-lg transition-colors disabled:opacity-50"
                                                                    >
                                                                        {leavingStudyId === notification.referenceId ? (
                                                                            <ButtonSpinner />
                                                                        ) : (
                                                                            <X size={14} />
                                                                        )}
                                                                        불참
                                                                    </button>
                                                                </div>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))
                                        )}
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

                        {/* 프로필 드롭다운 / 로그인 버튼 */}
                        <div ref={profileRef} className="relative">
                            {/* 로그인 상태에 따라 다른 UI 표시 */}
                            {isLoggedIn ? (
                                <>
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
                                            <img
                                                src={getProfileImageUrl(user?.avatar)}
                                                alt="Profile"
                                                className="w-full h-full object-cover"
                                                onError={(e) => { (e.target as HTMLImageElement).src = DEFAULT_PROFILE_IMAGE; }}
                                            />
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
                                                    <img
                                                        src={getProfileImageUrl(user?.avatar)}
                                                        alt="Profile"
                                                        className="w-full h-full object-cover"
                                                        onError={(e) => { (e.target as HTMLImageElement).src = DEFAULT_PROFILE_IMAGE; }}
                                                    />
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2">
                                                        <h6 className="text-base font-semibold text-gray-900 truncate">
                                                            {user?.nickname || user?.name || '사용자'}
                                                        </h6>
                                                        {stats && (
                                                            <LevelBadge
                                                                level={stats.level}
                                                                size="sm"
                                                                variant="text"
                                                            />
                                                        )}
                                                    </div>
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
                                                        <span>내 프로필</span>
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

                                            {/* 푸터 - 로그아웃 버튼 (현재 페이지 유지) */}
                                            <div className="px-3 py-2 border-t border-gray-100">
                                                <button
                                                    onClick={() => {
                                                        logout();
                                                        setIsProfileOpen(false);
                                                        // 로그인 페이지로 리다이렉트하지 않음 - 현재 페이지 유지
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
                                </>
                            ) : (
                                /* 비로그인 상태: 로그인 버튼 표시 (알림 버튼과 동일 스타일) */
                                <button
                                    onClick={() => navigate('/login')}
                                    className={cn(
                                        'p-2 rounded-lg transition-colors relative',
                                        'hover:bg-white/50'
                                    )}
                                    aria-label="로그인"
                                >
                                    <User size={20} className="text-gray-700" />
                                </button>
                            )}
                        </div>
                        </div>
                    </div>
                </header>
            )}

            <div className="flex flex-1 overflow-hidden">
                <div className={cn(isEnteringFromWorkspace && "layout-sidebar-enter")}>
                    <Sidebar />
                </div>

                {/* 페이지 콘텐츠 - 사이드바 닫힘 시 왼쪽 패딩으로 벽 붙음 방지 */}
                <main
                    className={cn(
                        'flex-1 flex flex-col overflow-hidden',
                        'pb-6 pr-0 bg-slate-200 transition-all duration-300 ease-out',
                        shouldHideHeader ? 'pt-4' : 'pt-2',
                        sidebarMode === 'closed' ? 'pl-4' : 'pl-0',
                        isEnteringFromWorkspace && 'layout-main-enter'
                    )}
                >
                    {/* 둥근 메인 컨테이너 섹션 */}
                    <section
                        id="main-content-scroll"
                        className={cn(
                            "bg-white rounded-3xl h-full overflow-auto scrollbar-hide",
                            isEnteringFromWorkspace && "layout-content-enter"
                        )}
                        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                    >
                        {children}
                    </section>
                </main>

                {!isCompactMode && (
                    <div className={cn(isEnteringFromWorkspace && "layout-right-sidebar-enter")}>
                        <RightSideBarV2 />
                    </div>
                )}
            </div>

            {/* 레벨업 축하 모달 */}
            {levelUpInfo && (
                <LevelUpModal
                    isOpen={isLevelUpModalOpen}
                    onClose={closeLevelUpModal}
                    previousLevel={levelUpInfo.previousLevel}
                    newLevel={levelUpInfo.newLevel}
                    newLevelName={levelUpInfo.newLevelName}
                />
            )}
        </div>
    );
};
