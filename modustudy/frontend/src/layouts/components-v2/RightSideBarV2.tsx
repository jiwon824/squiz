import React, { useRef, useEffect, useState, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { Users, MessageSquare, Video, Calendar, Clock, Play, X } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import FriendListMini from '@/features/friend/components/FriendListMini';
import DMListMini from '@/features/dm/components/DMListMini';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useFriendStore } from '@/features/friend/store/friendStore';
import { cn } from '@/shared/utils/cn';
import { calendarApi, StudySessionDTO } from '@/api/endpoints/calendarApi';
import { studyApi } from '@/api/endpoints/studyApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import { formatDate } from '@/features/calendar/utils';

// 다가오는 미팅 정보
interface UpcomingMeeting {
    id: number;
    studyId: number;
    studyName: string;
    meetingTitle: string;
    scheduledAt: Date;
    durationMinutes: number | null;
    status: string;
    meetingId: number | null;
    isOnline: boolean;
}
const resolveNextSession = (sessions: StudySessionDTO[], currentTime: Date) => {
    const candidates = sessions.filter((session) => {
        if (session.status === 'CANCELLED') return false;
        const startAt = new Date(session.scheduledAt).getTime();
        const durationMinutes = session.durationMinutes || 60;
        const endAt = startAt + durationMinutes * 60 * 1000;
        return endAt >= currentTime.getTime();
    });

    const inProgress = candidates.filter((session) => {
        const startAt = new Date(session.scheduledAt).getTime();
        const durationMinutes = session.durationMinutes || 60;
        const endAt = startAt + durationMinutes * 60 * 1000;
        return startAt <= currentTime.getTime() && currentTime.getTime() <= endAt;
    });

    if (inProgress.length > 0) {
        return inProgress.sort(
            (a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime()
        )[0];
    }

    const upcoming = candidates
        .filter((session) => new Date(session.scheduledAt).getTime() > currentTime.getTime())
        .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime());

    return upcoming[0] || null;
};

// 다가오는 미팅 빠른 접근
const MeetingQuickAccess: React.FC = () => {
    const navigate = useNavigate();
    const { showToast } = useUIStore();
    const [meetings, setMeetings] = useState<UpcomingMeeting[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [now, setNow] = useState(() => new Date());

    useEffect(() => {
        const timer = window.setInterval(() => {
            setNow(new Date());
        }, 1000);
        return () => window.clearInterval(timer);
    }, []);

    const formatCountdown = (scheduledAt: Date) => {
        const diffMs = scheduledAt.getTime() - now.getTime();
        if (diffMs <= 0) return '진행 중';
        const totalSeconds = Math.floor(diffMs / 1000);
        const seconds = totalSeconds % 60;
        const totalMinutes = Math.floor(totalSeconds / 60);
        const minutes = totalMinutes % 60;
        const totalHours = Math.floor(totalMinutes / 60);
        const hours = totalHours % 24;
        const days = Math.floor(totalHours / 24);

        const time = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
        return days > 0 ? `${days}일 ${time}` : time;
    };

    useEffect(() => {
        const loadUpcomingMeetings = async () => {
            try {
                setIsLoading(true);

                const today = new Date();
                const endDate = new Date(today);
                endDate.setDate(endDate.getDate() + 365);

                const startDate = formatDate(today);
                const endDateString = formatDate(endDate);

                let sessions = await calendarApi.getMyStudySessions(startDate, endDateString);

                const studiesResponse = await studyApi.getMyStudies(0, 50);
                const studyMap = new Map<number, string>();
                studiesResponse.content.forEach((study) => {
                    studyMap.set(study.id, study.name);
                });

                if (sessions.length === 0 && studiesResponse.content.length > 0) {
                    const sessionLists = await Promise.all(
                        studiesResponse.content.map(async (study) => {
                            try {
                                return await calendarApi.getStudySessions(study.id, startDate, endDateString);
                            } catch (error) {
                                console.warn('[MeetingQuickAccess] study sessions load failed', study.id, error);
                                return [];
                            }
                        })
                    );
                    sessions = sessionLists.flat();
                }

                const sessionsByStudy = new Map<number, StudySessionDTO[]>();
                sessions.forEach((session: StudySessionDTO) => {
                    const list = sessionsByStudy.get(session.studyId) || [];
                    list.push(session);
                    sessionsByStudy.set(session.studyId, list);
                });

                const studyIds = Array.from(sessionsByStudy.keys());
                const meetingsByStudy = await Promise.all(
                    studyIds.map(async (studyId) => {
                        try {
                            const meetings = await meetingApi.listMeetings(studyId, { page: 0, size: 50 });
                            return { studyId, meetings: meetings.content };
                        } catch (error) {
                            console.warn('[MeetingQuickAccess] meeting list load failed', studyId, error);
                            return { studyId, meetings: [] };
                        }
                    })
                );

                const meetingsLookup = new Map<number, typeof meetingsByStudy[number]['meetings']>();
                meetingsByStudy.forEach((item) => {
                    meetingsLookup.set(item.studyId, item.meetings);
                });

                const upcomingMeetings: UpcomingMeeting[] = studyIds
                    .map((studyId) => {
                        const nextSession = resolveNextSession(sessionsByStudy.get(studyId) || [], today);
                        if (!nextSession) return null;

                        const meetingsList = meetingsLookup.get(studyId) || [];
                        const matchingMeeting = meetingsList.find(
                            (meeting) => meeting.session?.id === nextSession.id && !meeting.endedAt
                        );

                        return {
                            id: nextSession.id,
                            studyId,
                            studyName: studyMap.get(studyId) || `스터디 ${studyId}`,
                            meetingTitle: nextSession.title || '스터디 세션',
                            scheduledAt: new Date(nextSession.scheduledAt),
                            durationMinutes: nextSession.durationMinutes || null,
                            status: nextSession.status as string,
                            meetingId: matchingMeeting?.id ?? null,
                            isOnline: nextSession.isOnline,
                        } as UpcomingMeeting;
                    })
                    .filter((item): item is UpcomingMeeting => item !== null)
                    .sort((a, b) => a.scheduledAt.getTime() - b.scheduledAt.getTime());

                setMeetings(upcomingMeetings);
            } catch (err) {
                console.error('[MeetingQuickAccess] 미팅 일정 로딩 실패:', err);
            } finally {
                setIsLoading(false);
            }
        };

        loadUpcomingMeetings();
        const refreshTimer = window.setInterval(loadUpcomingMeetings, 60000);
        return () => window.clearInterval(refreshTimer);
    }, []);

    return (
        <div className="h-full bg-transparent flex flex-col">
            {/* 헤더 */}
            <div className="p-4 border-b border-gray-100">
                <div className="flex items-center gap-2 text-study-blue">
                    <Video size={20} />
                    <h3 className="font-bold text-sm">다가오는 미팅</h3>
                </div>
            </div>

            {/* 미팅 목록 */}
            <div className="flex-1 overflow-y-auto p-3 space-y-3">
                {isLoading ? (
                    <div className="text-center py-8 text-gray-400 text-sm">
                        <Spinner size="lg" className="mx-auto mb-2 opacity-50" />
                        <p>로딩 중...</p>
                    </div>
                ) : meetings.length === 0 ? (
                    <div className="text-center py-8 text-gray-400 text-sm">
                        <Calendar size={32} className="mx-auto mb-2 opacity-30" />
                        <p>예정된 미팅이 없습니다</p>
                    </div>
                ) : (
                    meetings.map((meeting) => {
                        const hasStarted = meeting.scheduledAt.getTime() - now.getTime() <= 0;
                        return (
                            <motion.div
                                key={meeting.id}
                                className={cn(
                                    'p-3 rounded-lg border transition-all',
                                    hasStarted
                                        ? 'border-study-green bg-study-green/5 shadow-sm'
                                        : 'border-gray-200 bg-gray-50/50'
                                )}
                                whileHover={{ scale: 1.02 }}
                            >
                                <div className="flex items-start justify-between mb-2">
                                    <div className="flex-1">
                                        <h4 className="font-bold text-sm text-study-text-dark line-clamp-1">
                                            {meeting.meetingTitle}
                                        </h4>
                                        <p className="text-xs text-gray-500 mt-0.5">{meeting.studyName}</p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2 text-xs text-gray-600 mb-3">
                                    <Clock size={12} />
                                    <span className={hasStarted ? 'text-study-green font-bold' : ''}>
                                        {formatCountdown(meeting.scheduledAt)}
                                    </span>
                                </div>

                                {/* 온라인 세션인 경우에만 미팅 참여 버튼 표시 */}
                                {hasStarted && meeting.isOnline && (
                                    <button
                                        onClick={() => {
                                            if (!meeting.meetingId) {
                                                showToast?.('미팅 생성중입니다. 잠시후 다시 시도해주세요.', 'info');
                                                return;
                                            }
                                            navigate(`/study/${meeting.studyId}/meetings/${meeting.meetingId}/room`);
                                        }}
                                        className="w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-bold transition-all bg-study-green text-white hover:bg-study-green/90 hover:scale-105 active:scale-95"
                                    >
                                        <Play size={14} />
                                        미팅 참여하기
                                    </button>
                                )}
                                {/* 오프라인 세션인 경우 장소 표시 */}
                                {hasStarted && !meeting.isOnline && (
                                    <div className="w-full py-2 text-center text-xs text-gray-500 bg-gray-50 rounded-lg">
                                        오프라인 세션
                                    </div>
                                )}
                            </motion.div>
                        );
                    })
                )}
            </div>

            {/* 하단 액션 */}
            <div className="p-3 border-t border-gray-100">
                <button
                    onClick={() => navigate('/calendar')}
                    className="w-full py-2 text-xs font-medium text-study-blue hover:bg-study-blue/5 rounded-lg transition-colors"
                >
                    전체 일정 보기
                </button>
            </div>
        </div>
    );
};

// 리사이즈 상수
const DEFAULT_PANEL_WIDTH = 256;
const MIN_PANEL_WIDTH = 256;
const MAX_PANEL_WIDTH = 480;

export const RightSideBarV2: React.FC = () => {
    const { activeRightTab, toggleRightTab } = useUIStore();
    const { isLoggedIn } = useAuthStore();
    const panelRef = useRef<HTMLDivElement>(null);
    const { unreadCount, fetchUnreadCount } = useDMStore();
    const { fetchReceivedRequests } = useFriendStore();
    const [badgeMeetings, setBadgeMeetings] = useState<UpcomingMeeting[]>([]);
    const [badgeNow, setBadgeNow] = useState(() => new Date());

    useEffect(() => {
        const timer = window.setInterval(() => {
            setBadgeNow(new Date());
        }, 1000);
        return () => window.clearInterval(timer);
    }, []);

    const loadMeetingBadge = useCallback(async () => {
        const today = new Date();
        const endDate = new Date(today);
        endDate.setDate(endDate.getDate() + 365);

        const startDate = formatDate(today);
        const endDateString = formatDate(endDate);

        let sessions = await calendarApi.getMyStudySessions(startDate, endDateString);

        const studiesResponse = await studyApi.getMyStudies(0, 50);
        const studyMap = new Map<number, string>();
        studiesResponse.content.forEach((study) => {
            studyMap.set(study.id, study.name);
        });

        if (sessions.length === 0 && studiesResponse.content.length > 0) {
            const sessionLists = await Promise.all(
                studiesResponse.content.map(async (study) => {
                    try {
                        return await calendarApi.getStudySessions(study.id, startDate, endDateString);
                    } catch (error) {
                        console.warn('[MeetingBadge] study sessions load failed', study.id, error);
                        return [];
                    }
                })
            );
            sessions = sessionLists.flat();
        }

        const sessionsByStudy = new Map<number, StudySessionDTO[]>();
        sessions.forEach((session: StudySessionDTO) => {
            const list = sessionsByStudy.get(session.studyId) || [];
            list.push(session);
            sessionsByStudy.set(session.studyId, list);
        });

        const studyIds = Array.from(sessionsByStudy.keys());
        return studyIds
            .map((studyId) => {
                const nextSession = resolveNextSession(sessionsByStudy.get(studyId) || [], today);
                if (!nextSession) return null;
                return {
                    id: nextSession.id,
                    studyId,
                    studyName: studyMap.get(studyId) || `스터디 ${studyId}`,
                    meetingTitle: nextSession.title || '스터디 세션',
                    scheduledAt: new Date(nextSession.scheduledAt),
                    durationMinutes: nextSession.durationMinutes || null,
                    status: nextSession.status as string,
                    meetingId: null,
                    isOnline: nextSession.isOnline,
                } as UpcomingMeeting;
            })
            .filter((item): item is UpcomingMeeting => item !== null)
            .sort((a, b) => a.scheduledAt.getTime() - b.scheduledAt.getTime());
    }, []);

    useEffect(() => {
        if (!isLoggedIn) {
            setBadgeMeetings([]);
            return;
        }
        let cancelled = false;
        const load = async () => {
            try {
                const data = await loadMeetingBadge();
                if (!cancelled) {
                    setBadgeMeetings(data);
                }
            } catch (error) {
                console.warn('[MeetingBadge] load failed', error);
            }
        };

        load();
        const refreshTimer = window.setInterval(load, 60000);
        return () => {
            cancelled = true;
            window.clearInterval(refreshTimer);
        };
    }, [isLoggedIn, loadMeetingBadge]);

    const hasActiveMeeting = useMemo(() => {
        const nowTime = badgeNow.getTime();
        return badgeMeetings.some((meeting) => {
            if (meeting.status === 'CANCELLED') return false;
            const startAt = meeting.scheduledAt.getTime();
            const durationMinutes = meeting.durationMinutes || 60;
            const endAt = startAt + durationMinutes * 60 * 1000;
            return startAt <= nowTime && nowTime <= endAt;
        });
    }, [badgeMeetings, badgeNow]);

    // 현재 활성화된 미팅 정보
    const activeMeeting = useMemo(() => {
        const nowTime = badgeNow.getTime();
        return badgeMeetings.find((meeting) => {
            if (meeting.status === 'CANCELLED') return false;
            const startAt = meeting.scheduledAt.getTime();
            const durationMinutes = meeting.durationMinutes || 60;
            const endAt = startAt + durationMinutes * 60 * 1000;
            return startAt <= nowTime && nowTime <= endAt;
        }) || null;
    }, [badgeMeetings, badgeNow]);

    // 팝오버 닫기 상태 - sessionStorage로 관리하여 페이지 이동 후에도 중복 표시 방지
    const DISMISSED_KEY = 'dismissedMeetingPopoverId';
    const [dismissedMeetingId, setDismissedMeetingId] = useState<number | null>(() => {
        const stored = sessionStorage.getItem(DISMISSED_KEY);
        return stored ? Number(stored) : null;
    });
    const dismissMeeting = (meetingId: number) => {
        setDismissedMeetingId(meetingId);
        sessionStorage.setItem(DISMISSED_KEY, String(meetingId));
    };
    // 마운트 직후 팝오버가 바로 뜨지 않도록 지연 (워크스페이스 진입 시 2번 뜨는 문제 방지)
    const [popoverReady, setPopoverReady] = useState(false);
    useEffect(() => {
        const timer = setTimeout(() => setPopoverReady(true), 1500);
        return () => clearTimeout(timer);
    }, []);
    // 첫 표시 후 자동으로 dismiss 처리 (한 번만 표시)
    useEffect(() => {
        if (popoverReady && hasActiveMeeting && activeMeeting && activeMeeting.id !== dismissedMeetingId) {
            const autoTimer = setTimeout(() => {
                dismissMeeting(activeMeeting.id);
            }, 5000);
            return () => clearTimeout(autoTimer);
        }
    }, [popoverReady, hasActiveMeeting, activeMeeting, dismissedMeetingId]);
    const showMeetingPopover = popoverReady && hasActiveMeeting && activeMeeting && activeMeeting.id !== dismissedMeetingId;

    // 리사이즈 상태
    const [panelWidth, setPanelWidth] = useState(DEFAULT_PANEL_WIDTH);
    const [isResizing, setIsResizing] = useState(false);
    const resizeStartXRef = useRef(0);
    const resizeStartWidthRef = useRef(DEFAULT_PANEL_WIDTH);

    // 패널 닫힐 때 너비 초기화
    useEffect(() => {
        if (!activeRightTab) {
            setPanelWidth(DEFAULT_PANEL_WIDTH);
        }
    }, [activeRightTab]);

    // 리사이즈 드래그 시작
    const handleResizeStart = useCallback((e: React.MouseEvent) => {
        e.preventDefault();
        setIsResizing(true);
        resizeStartXRef.current = e.clientX;
        resizeStartWidthRef.current = panelWidth;
    }, [panelWidth]);

    // 리사이즈 중 마우스 이벤트 (document 레벨)
    useEffect(() => {
        if (!isResizing) return;

        const handleMouseMove = (e: MouseEvent) => {
            // 좌측 드래그 → 너비 증가 (핸들이 왼쪽 가장자리에 있으므로)
            const delta = resizeStartXRef.current - e.clientX;
            const newWidth = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, resizeStartWidthRef.current + delta));
            setPanelWidth(newWidth);
        };

        const handleMouseUp = () => {
            setIsResizing(false);
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);

        return () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };
    }, [isResizing]);

    // 리사이즈 중 텍스트 선택 방지 + 커서 변경
    useEffect(() => {
        if (isResizing) {
            document.body.style.userSelect = 'none';
            document.body.style.cursor = 'col-resize';
        } else {
            document.body.style.userSelect = '';
            document.body.style.cursor = '';
        }
    }, [isResizing]);

    // 초기 데이터 로드 (로그인 상태일 때만)
    useEffect(() => {
        if (!isLoggedIn) return;
        fetchUnreadCount();
        fetchReceivedRequests();
    }, [isLoggedIn, fetchUnreadCount, fetchReceivedRequests]);

    // 바깥 클릭 시 닫기
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (activeRightTab && panelRef.current && !panelRef.current.contains(event.target as Node)) {
                const isIconButton = (event.target as HTMLElement).closest('.icon-bar-btn');
                if (!isIconButton) {
                    toggleRightTab(activeRightTab);
                }
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [activeRightTab, toggleRightTab]);

    return (
        <div className="flex h-full" ref={panelRef}>
            {/* 확장 오른쪽 영역 */}
            <AnimatePresence>
                {activeRightTab && (
                    <motion.div
                        initial={{ width: 0, opacity: 0 }}
                        animate={{ width: panelWidth, opacity: 1 }}
                        exit={{ width: 0, opacity: 0 }}
                        transition={
                            isResizing
                                ? { duration: 0 }
                                : { type: 'spring', damping: 25, stiffness: 300 }
                        }
                        className="overflow-hidden relative"
                    >
                        {/* 리사이즈 드래그 핸들 - friend/dm 탭에서만 표시 */}
                        {(activeRightTab === 'friend' || activeRightTab === 'dm') && (
                            <div
                                onMouseDown={handleResizeStart}
                                className={cn(
                                    'absolute left-0 top-0 h-full w-2 z-10 cursor-col-resize group',
                                    'flex items-center justify-center'
                                )}
                            >
                                {/* 시각적 핸들 바 */}
                                <div className={cn(
                                    'w-1 rounded-full transition-all duration-150',
                                    isResizing
                                        ? 'bg-study-blue h-20'
                                        : 'bg-gray-300 group-hover:bg-gray-400 h-12'
                                )} />
                            </div>
                        )}

                        <div className="h-full" style={{ width: panelWidth }}>
                            {activeRightTab === 'friend' && <FriendListMini />}
                            {activeRightTab === 'dm' && <DMListMini />}
                            {activeRightTab === 'meeting' && (
                                <MeetingQuickAccess />
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 고정 아이콘 바 */}
            <div className="w-14 h-full flex flex-col items-center py-4 gap-4 bg-slate-200">
                {/* 미팅 버튼 + 활성 미팅 팝오버 */}
                <div className="relative">
                    <button
                        onClick={() => toggleRightTab('meeting')}
                        className={cn(
                            'icon-bar-btn p-2.5 rounded-full transition-all hover:bg-study-green/10 group relative',
                            activeRightTab === 'meeting' ? 'bg-study-green/15 text-study-green' : 'text-gray-400'
                        )}
                        title="미팅"
                    >
                        <Video size={20} className="group-hover:scale-110 transition-transform" />
                        {/* 알림 배지 */}
                        {hasActiveMeeting && (
                            <div className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-study-green rounded-full border-2 border-white animate-pulse" />
                        )}
                    </button>

                    {/* 활성 미팅 팝오버 */}
                    <AnimatePresence>
                        {showMeetingPopover && activeMeeting && (
                            <motion.div
                                initial={{ opacity: 0, x: 10, scale: 0.95 }}
                                animate={{ opacity: 1, x: 0, scale: 1 }}
                                exit={{ opacity: 0, x: 10, scale: 0.95 }}
                                transition={{ type: 'spring', damping: 20, stiffness: 300 }}
                                className="absolute right-[calc(100%+12px)] top-0 z-50"
                            >
                                <div className="relative bg-white rounded-2xl shadow-lg border border-study-green/20 p-3 min-w-[220px] max-w-[260px]">
                                    {/* 화살표 - 비디오 아이콘 중심을 정확히 가리킴 */}
                                    <div className="absolute top-4 -right-1.5 w-3 h-3 bg-white border-r border-t border-study-green/20 rotate-45" />

                                    <div className="flex items-start gap-2.5">
                                        {/* 카메라 아이콘 */}
                                        <div className="flex-shrink-0 w-8 h-8 bg-study-green/10 rounded-lg flex items-center justify-center">
                                            <Video size={16} className="text-study-green" />
                                        </div>

                                        {/* 미팅 정보 */}
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-bold text-study-green mb-0.5">미팅 진행 중</p>
                                            <p className="text-xs text-gray-700 font-medium truncate">{activeMeeting.studyName}</p>
                                            <p className="text-[11px] text-gray-400 truncate">{activeMeeting.meetingTitle}</p>
                                        </div>

                                        {/* X 닫기 버튼 */}
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                dismissMeeting(activeMeeting.id);
                                            }}
                                            className="flex-shrink-0 p-0.5 rounded-full hover:bg-gray-100 transition-colors text-gray-300 hover:text-gray-500"
                                        >
                                            <X size={14} />
                                        </button>
                                    </div>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>

                <div className="w-8 h-px bg-gray-100 my-1" />

                <button
                    onClick={() => toggleRightTab('friend')}
                    className={cn(
                        'icon-bar-btn p-2.5 rounded-full transition-all hover:bg-study-blue/10 group',
                        activeRightTab === 'friend' ? 'bg-study-blue/15 text-study-blue' : 'text-gray-400'
                    )}
                    title="친구"
                >
                    <Users size={20} className="group-hover:scale-110 transition-transform" />
                </button>

                <button
                    onClick={() => toggleRightTab('dm')}
                    className={cn(
                        'icon-bar-btn p-2.5 rounded-full transition-all hover:bg-study-blue/10 group relative',
                        activeRightTab === 'dm' ? 'bg-study-blue/15 text-study-blue' : 'text-gray-400'
                    )}
                    title="DM"
                >
                    <MessageSquare size={20} className="group-hover:scale-110 transition-transform" />
                    {unreadCount > 0 && (
                        <div className="absolute top-2 right-2 w-2 h-2 bg-red-400 rounded-full border-2 border-white" />
                    )}
                </button>
            </div>
        </div>
    );
};





















