import React, { useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUIStore } from '@/store/uiStore';
import { Users, MessageSquare, Plus } from 'lucide-react';
import FriendListMini from '@/features/friend/components/FriendListMini';
import DMListMini from '@/features/dm/components/DMListMini';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useFriendStore } from '@/features/friend/store/friendStore';

export const RightSideBar: React.FC = () => {
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
                // 클릭된 요소가 아이콘 바의 버튼인지 확인 (아이콘 바 버튼 클릭 시에는 toggle 자체에서 처리)
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
                        className="w-72 bg-study-bg pointer-events-auto"
                    >
                        {activeRightTab === 'friend' && <FriendListMini />}
                        {activeRightTab === 'dm' && <DMListMini />}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 고정 아이콘 바 (구글 캘린더 스타일) */}
            <div className="w-14 h-full bg-study-bg flex flex-col items-center py-4 gap-4 pointer-events-auto">
                <button
                    onClick={() => toggleRightTab('friend')}
                    className={`icon-bar-btn p-2.5 rounded-full transition-all hover:bg-study-blue/10 group ${activeRightTab === 'friend' ? 'bg-study-blue/15 text-study-blue' : 'text-gray-400'
                        }`}
                    title="친구"
                >
                    <Users size={20} className="group-hover:scale-110 transition-transform" />
                </button>

                <button
                    onClick={() => toggleRightTab('dm')}
                    className={`icon-bar-btn p-2.5 rounded-full transition-all hover:bg-study-blue/10 group relative ${activeRightTab === 'dm' ? 'bg-study-blue/15 text-study-blue' : 'text-gray-400'
                        }`}
                    title="DM"
                >
                    <MessageSquare size={20} className="group-hover:scale-110 transition-transform" />
                    <div className="absolute top-2 right-2 w-2 h-2 bg-red-400 rounded-full border-2 border-white" />
                </button>

                <div className="w-8 h-px bg-gray-100 my-2" />

                <button
                    className="p-2.5 text-gray-300 hover:text-gray-400 rounded-full hover:bg-gray-50 transition-all"
                    title="추가 예정"
                >
                    <Plus size={20} />
                </button>
            </div>
        </div>
    );
};
