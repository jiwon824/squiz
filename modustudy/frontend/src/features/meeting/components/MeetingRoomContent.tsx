import React, { useState } from 'react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import MeetingParticipants from './MeetingParticipants';
import MeetingChatPanel from './MeetingChatPanel';
import MeetingVideoStage from './MeetingVideoStage';
import { MeetingRoomChatMessage, MeetingRoomParticipant } from '../types';
import { Maximize2, Minimize2, Users, MessageCircle, X } from 'lucide-react';

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

    return (
        <div className="relative min-h-0 flex-1">
            {/* 벤토 그리드 */}
            <div className={cn(
                'grid gap-3 h-full transition-all duration-300',
                conditionalClasses.state(
                    isExpanded,
                    'grid-cols-1',
                    'grid-cols-1 lg:grid-cols-[1fr_320px]'
                )
            )}>
                {/* 비디오 영역 - 벤토 메인 셀 */}
                <div className="relative bg-gray-900 rounded-2xl shadow-sm min-h-[420px] h-full overflow-hidden">
                    <div className="p-3 h-full">
                        <MeetingVideoStage
                            localStream={localStream}
                            localLabel={localLabel}
                            localIsPresenter={localIsPresenter}
                            containerRef={videoStageRef}
                            remoteVideoStreams={remoteVideoStreams}
                        />
                    </div>

                    {/* 최대화/축소 토글 (우상단) */}
                    <button
                        className="absolute top-3 right-3 z-10 w-9 h-9 rounded-xl bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white flex items-center justify-center transition-all cursor-pointer"
                        onClick={() => {
                            const next = !isExpanded;
                            setIsExpanded(next);
                            onExpandChange?.(next);
                            if (next) setMiniTab(null);
                        }}
                        title={isExpanded ? '사이드패널 열기' : '비디오 최대화'}
                    >
                        {isExpanded ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
                    </button>

                    {/* 최대화 시 미니 탭 버튼 (우하단 플로팅) */}
                    {isExpanded && (
                        <div className="absolute bottom-3 right-3 z-10 flex gap-2">
                            <button
                                className={cn(
                                    'flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium backdrop-blur-sm transition-all cursor-pointer',
                                    conditionalClasses.state(
                                        miniTab === 'participants',
                                        'bg-white/90 text-gray-900 shadow-sm',
                                        'bg-black/40 text-white hover:bg-black/60'
                                    )
                                )}
                                onClick={() => setMiniTab(miniTab === 'participants' ? null : 'participants')}
                            >
                                <Users size={14} />
                                {participants.length}
                            </button>
                            <button
                                className={cn(
                                    'flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium backdrop-blur-sm transition-all cursor-pointer',
                                    conditionalClasses.state(
                                        miniTab === 'chat',
                                        'bg-white/90 text-gray-900 shadow-sm',
                                        'bg-black/40 text-white hover:bg-black/60'
                                    )
                                )}
                                onClick={() => setMiniTab(miniTab === 'chat' ? null : 'chat')}
                            >
                                <MessageCircle size={14} />
                                채팅
                            </button>
                        </div>
                    )}

                    <video ref={aiVideoRef} className="absolute -left-[9999px] w-px h-px" muted playsInline />
                </div>

                {/* 사이드 패널 - 벤토 서브 셀 (일반 모드) */}
                {!isExpanded && (
                    <div className="flex flex-col gap-3 min-h-0 h-full">
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
            </div>

            {/* 최대화 시 플로팅 사이드패널 (오버레이) */}
            {isExpanded && miniTab !== null && (
                <div className="absolute top-0 right-0 bottom-0 w-[340px] z-20 p-2">
                    <div className="relative h-full bg-white/95 backdrop-blur-md rounded-2xl shadow-xl overflow-hidden flex flex-col">
                        {/* 플로팅 패널 닫기 버튼 */}
                        <button
                            className="absolute top-2 right-2 z-10 w-7 h-7 rounded-lg bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors cursor-pointer"
                            onClick={() => setMiniTab(null)}
                            title="패널 닫기"
                        >
                            <X size={14} className="text-gray-500" />
                        </button>

                        {miniTab === 'participants' ? (
                            <MeetingParticipants
                                participants={participants}
                                presenterId={presenterId}
                                presenterName={presenterName}
                            />
                        ) : (
                            <MeetingChatPanel
                                messages={chatMessages}
                                onSend={onSendChat}
                                onDelete={onDeleteChat}
                                currentUserId={currentUserId}
                                currentSender={currentSender}
                                compact
                            />
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default MeetingRoomContent;
