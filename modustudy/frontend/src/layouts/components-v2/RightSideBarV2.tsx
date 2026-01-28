import React, { useRef, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useUIStore } from '@/store/uiStore';
import { Users, MessageSquare, Video, Calendar, Clock, Play } from 'lucide-react';
import FriendListMini from '@/features/friend/components/FriendListMini';
import DMListMini from '@/features/dm/components/DMListMini';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useFriendStore } from '@/features/friend/store/friendStore';
import { cn } from '@/shared/utils/cn';

// Mock 데이터: 다가오는 미팅 일정
const MOCK_MEETINGS = [
    {
        id: 1,
        studyId: 101,
        studyName: 'React 스터디',
        meetingTitle: '주간 회의',
        scheduledAt: new Date(Date.now() + 1000 * 60 * 25), // 25분 후
        status: 'SCHEDULED',
    },
    {
        id: 2,
        studyId: 102,
        studyName: 'CS 스터디',
        meetingTitle: '알고리즘 세션',
        scheduledAt: new Date(Date.now() + 1000 * 60 * 60 * 2), // 2시간 후
        status: 'SCHEDULED',
    },
];

// 미팅 퀵 액세스 위젯
const MeetingQuickAccess: React.FC = () => {
    const navigate = useNavigate();
    const [meetings, setMeetings] = useState(MOCK_MEETINGS);

    // 남은 시간 계산
    const getTimeUntil = (scheduledAt: Date) => {
        const now = new Date();
        const diff = scheduledAt.getTime() - now.getTime();
        const minutes = Math.floor(diff / 1000 / 60);
        const hours = Math.floor(minutes / 60);

        if (minutes < 0) return '시작됨';
        if (minutes < 60) return `${minutes}분 후`;
        return `${hours}시간 ${minutes % 60}분 후`;
    };

    // 미팅 참여 가능 여부 (10분 전부터)
    const canJoin = (scheduledAt: Date) => {
        const now = new Date();
        const diff = scheduledAt.getTime() - now.getTime();
        const minutes = Math.floor(diff / 1000 / 60);
        return minutes <= 10 && minutes >= 0;
    };

    return (
        <div className="h-full bg-white border-l border-gray-100 flex flex-col">
            {/* 헤더 */}
            <div className="p-4 border-b border-gray-100">
                <div className="flex items-center gap-2 text-study-blue">
                    <Video size={20} />
                    <h3 className="font-bold text-sm">다가오는 미팅</h3>
                </div>
            </div>

            {/* 미팅 목록 */}
            <div className="flex-1 overflow-y-auto p-3 space-y-3">
                {meetings.length === 0 ? (
                    <div className="text-center py-8 text-gray-400 text-sm">
                        <Calendar size={32} className="mx-auto mb-2 opacity-30" />
                        <p>예정된 미팅이 없습니다</p>
                    </div>
                ) : (
                    meetings.map((meeting) => {
                        const joinable = canJoin(meeting.scheduledAt);
                        return (
                            <motion.div
                                key={meeting.id}
                                className={cn(
                                    'p-3 rounded-lg border transition-all',
                                    joinable
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
                                        <p className="text-xs text-gray-500 mt-0.5">
                                            {meeting.studyName}
                                        </p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2 text-xs text-gray-600 mb-3">
                                    <Clock size={12} />
                                    <span className={joinable ? 'text-study-green font-bold' : ''}>
                                        {getTimeUntil(meeting.scheduledAt)}
                                    </span>
                                </div>

                                <button
                                    onClick={() =>
                                        navigate(`/study/${meeting.studyId}/meetings/${meeting.id}/room`)
                                    }
                                    disabled={!joinable}
                                    className={cn(
                                        'w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-bold transition-all',
                                        joinable
                                            ? 'bg-study-green text-white hover:bg-study-green/90 hover:scale-105 active:scale-95'
                                            : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                                    )}
                                >
                                    <Play size={14} />
                                    {joinable ? '지금 참여하기' : '대기 중'}
                                </button>
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

export const RightSideBarV2: React.FC = () => {
    const { activeRightTab, toggleRightTab } = useUIStore();
    const panelRef = useRef<HTMLDivElement>(null);
    const { unreadCount, fetchUnreadCount } = useDMStore();
    const { receivedRequests, fetchReceivedRequests } = useFriendStore();

    // 초기 데이터 로드
    useEffect(() => {
        fetchUnreadCount();
        fetchReceivedRequests();
    }, [fetchUnreadCount, fetchReceivedRequests]);

    // 외부 클릭 시 닫기
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
        <div className="flex fixed right-0 top-16 bottom-0 z-40 pointer-events-none" ref={panelRef}>
            {/* 확장 패널 영역 */}
            <AnimatePresence>
                {activeRightTab && (
                    <motion.div
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: 20 }}
                        transition={{ type: 'spring', damping: 20, stiffness: 300 }}
                        className="w-72 pointer-events-auto"
                    >
                        {activeRightTab === 'friend' && <FriendListMini />}
                        {activeRightTab === 'dm' && <DMListMini />}
                        {activeRightTab === 'meeting' && <MeetingQuickAccess />}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 고정 아이콘 바 */}
            <div className="w-14 h-full flex flex-col items-center py-4 gap-4 pointer-events-auto bg-slate-200">
                {/* 미팅 버튼 (새로 추가!) */}
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
                    <div className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-study-green rounded-full border-2 border-white animate-pulse" />
                </button>

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
