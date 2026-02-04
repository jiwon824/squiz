import React, { useState, useEffect } from 'react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import MeetingParticipants from './MeetingParticipants';
import MeetingChatPanel from './MeetingChatPanel';
import MeetingVideoStage from './MeetingVideoStage';
import { MeetingRoomChatMessage, MeetingRoomParticipant } from '../types';
import { Maximize2, Minimize2, Users, MessageCircle, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface RemoteVideoStream {
    id: string;
    stream: MediaStream;
    label: string;
    isPresenter: boolean;
}

interface MeetingRoomContentProps {
    localStream: MediaStream | null;
    localLabel: string;
    localIsPresenter: boolean;
    isScreenSharing?: boolean;
    remoteVideoStreams: RemoteVideoStream[];
    videoStageRef: React.RefObject<HTMLDivElement>;
    aiVideoRef: React.RefObject<HTMLVideoElement>;
    participants: MeetingRoomParticipant[];
    presenterId: number | null;
    presenterName: string | null;
    chatMessages: MeetingRoomChatMessage[];
    onSendChat: (content: string) => void;
    onDeleteChat: (message: MeetingRoomChatMessage) => void;
    currentUserId: number | null;
    currentSender: string;
    /** 비디오 확장 상태 변경 시 부모에게 알림 */
    onExpandChange?: (expanded: boolean) => void;
}

const MeetingRoomContent: React.FC<MeetingRoomContentProps> = ({
    localStream,
    localLabel,
    localIsPresenter,
    isScreenSharing,
    remoteVideoStreams,
    videoStageRef,
    aiVideoRef,
    participants,
    presenterId,
    presenterName,
    chatMessages,
    onSendChat,
    onDeleteChat,
    currentUserId,
    currentSender,
    onExpandChange,
}) => {
    const [isExpanded, setIsExpanded] = useState(false);
    const [miniTab, setMiniTab] = useState<'participants' | 'chat' | null>(null);
    const [showTooltip, setShowTooltip] = useState(false);

    // 처음 미팅 입장 시 안내 툴팁 표시
    useEffect(() => {
        const tooltipKey = 'meeting_guide_shown';
        const hasShown = sessionStorage.getItem(tooltipKey);
        if (!hasShown) {
            setShowTooltip(true);
            sessionStorage.setItem(tooltipKey, 'true');
        }
    }, []);

    // 사이드패널 열림 여부
    const isSidePanelOpen = isExpanded && miniTab !== null;

    return (
        <div className="relative min-h-0 flex-1 flex gap-3 h-full overflow-hidden">
            {/* 비디오 영역 - flex-1로 남은 공간 차지 */}
            <div
                className="relative bg-gray-900 rounded-2xl shadow-sm min-h-[420px] overflow-hidden flex-1 transition-all duration-300 ease-out"
                style={{ willChange: 'width' }}
            >
                <div className="p-3 h-full">
                    <MeetingVideoStage
                        localStream={localStream}
                        localLabel={localLabel}
                        localIsPresenter={localIsPresenter}
                        isScreenSharing={isScreenSharing}
                        containerRef={videoStageRef}
                        remoteVideoStreams={remoteVideoStreams}
                    />
                </div>

                {/* 최대화/축소 토글 (우상단) */}
                <button
                    className="absolute top-3 right-3 z-10 w-9 h-9 rounded-xl bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white flex items-center justify-center transition-all cursor-pointer active:scale-95"
                    onClick={() => {
                        const next = !isExpanded;
                        setMiniTab(null); // 최대화/최소화 시 항상 miniTab 초기화
                        setIsExpanded(next);
                        onExpandChange?.(next);
                    }}
                    title={isExpanded ? '사이드패널 열기' : '비디오 최대화'}
                >
                    {isExpanded ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
                </button>

                {/* 최대화 시 미니 탭 버튼 (우하단 플로팅) */}
                {isExpanded && (
                    <div className="absolute bottom-4 right-4 z-10 flex gap-2.5">
                        <button
                            className={cn(
                                'flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium backdrop-blur-sm transition-all cursor-pointer active:scale-95',
                                conditionalClasses.state(
                                    miniTab === 'participants',
                                    'bg-white/90 text-gray-900 shadow-sm',
                                    'bg-black/40 text-white hover:bg-black/60'
                                )
                            )}
                            onClick={() => setMiniTab(miniTab === 'participants' ? null : 'participants')}
                        >
                            <Users size={16} />
                            {participants.length}
                        </button>
                        <button
                            className={cn(
                                'flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium backdrop-blur-sm transition-all cursor-pointer active:scale-95',
                                conditionalClasses.state(
                                    miniTab === 'chat',
                                    'bg-white/90 text-gray-900 shadow-sm',
                                    'bg-black/40 text-white hover:bg-black/60'
                                )
                            )}
                            onClick={() => setMiniTab(miniTab === 'chat' ? null : 'chat')}
                        >
                            <MessageCircle size={16} />
                            채팅
                        </button>
                    </div>
                )}

                <video ref={aiVideoRef} className="absolute -left-[9999px] w-px h-px" muted playsInline />
            </div>

            {/* 사이드 패널 - 일반 모드 (고정 너비) */}
            {!isExpanded && (
                <div className="hidden lg:flex flex-col gap-3 min-h-0 h-full w-[320px] shrink-0">
                    <MeetingParticipants
                        participants={participants}
                        presenterId={presenterId}
                        presenterName={presenterName}
                    />
                    <MeetingChatPanel
                        messages={chatMessages}
                        onSend={onSendChat}
                        onDelete={onDeleteChat}
                        currentUserId={currentUserId}
                        currentSender={currentSender}
                    />
                </div>
            )}

            {/* 최대화 시 사이드패널 (오른쪽에서 슬라이드, 영역 밀어내기) */}
            <AnimatePresence mode="wait">
                {isSidePanelOpen && (
                    <motion.div
                        initial={{ width: 0, opacity: 0 }}
                        animate={{ width: 340, opacity: 1 }}
                        exit={{ width: 0, opacity: 0 }}
                        transition={{ duration: 0.25, ease: 'easeOut' }}
                        className="h-full shrink-0 overflow-hidden"
                        style={{ willChange: 'width, opacity' }}
                    >
                        <div className="h-full w-[340px] bg-white rounded-2xl shadow-[0_4px_15px_rgba(0,0,0,0.05)] overflow-hidden flex flex-col">
                            {miniTab === 'participants' ? (
                                <MeetingParticipants
                                    participants={participants}
                                    presenterId={presenterId}
                                    presenterName={presenterName}
                                    onClose={() => setMiniTab(null)}
                                />
                            ) : (
                                <MeetingChatPanel
                                    messages={chatMessages}
                                    onSend={onSendChat}
                                    onDelete={onDeleteChat}
                                    currentUserId={currentUserId}
                                    currentSender={currentSender}
                                    compact
                                    onClose={() => setMiniTab(null)}
                                />
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 처음 미팅 입장 시 안내 툴팁 */}
            <AnimatePresence>
                {showTooltip && (
                    <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        className="absolute bottom-24 right-4 z-30 bg-gray-900 text-white px-5 py-4 rounded-xl shadow-lg max-w-[300px]"
                    >
                        <button
                            className="absolute -top-2 -right-2 w-7 h-7 bg-white text-gray-600 rounded-full flex items-center justify-center shadow-md hover:bg-gray-100 transition-colors cursor-pointer active:scale-95"
                            onClick={() => setShowTooltip(false)}
                        >
                            <X size={14} />
                        </button>
                        <p className="text-base font-medium mb-1.5">미팅 안내</p>
                        <p className="text-sm text-gray-300 leading-relaxed">
                            우측 하단의 버튼으로 참가자 목록과 채팅을 열 수 있어요. 비디오 최대화 시에도 사용 가능합니다.
                        </p>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default MeetingRoomContent;
