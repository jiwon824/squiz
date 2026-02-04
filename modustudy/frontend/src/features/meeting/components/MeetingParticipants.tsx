import React, { useRef } from 'react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { MeetingRoomParticipant } from '../types';
import { Crown, Mic, X } from 'lucide-react';
import { useVirtualizer } from '@tanstack/react-virtual';

interface MeetingParticipantsProps {
    participants: MeetingRoomParticipant[];
    presenterId: number | null;
    presenterName: string | null;
    onClose?: () => void;
}

// 이니셜 기반 아바타 색상
const avatarColors = [
    'bg-blue-500', 'bg-emerald-500', 'bg-amber-500', 'bg-rose-500',
    'bg-purple-500', 'bg-cyan-500', 'bg-indigo-500', 'bg-pink-500',
];

const getAvatarColor = (name: string) => {
    const hash = name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return avatarColors[hash % avatarColors.length];
};

const getInitial = (name: string) => {
    return name.charAt(0).toUpperCase();
};

const MeetingParticipants: React.FC<MeetingParticipantsProps> = ({ participants, presenterId, presenterName, onClose }) => {
    const scrollContainerRef = useRef<HTMLDivElement | null>(null);

    // 가상 스크롤 설정
    const virtualizer = useVirtualizer({
        count: participants.length,
        getScrollElement: () => scrollContainerRef.current,
        estimateSize: () => 56,
        overscan: 5,
    });

    return (
        <section className="bg-white rounded-2xl p-4 shadow-[0_4px_15px_rgba(0,0,0,0.05)] flex flex-col gap-3 min-h-0 flex-[0_1_42%]">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                    <h3 className="text-lg font-bold text-gray-900">참가자</h3>
                    <span className="inline-flex items-center justify-center min-w-[26px] h-7 px-2.5 bg-blue-100 text-blue-700 text-sm font-medium rounded-full">
                        {participants.length}
                    </span>
                </div>
                {onClose && (
                    <button
                        className="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center transition-colors cursor-pointer active:scale-95"
                        onClick={onClose}
                        title="닫기"
                    >
                        <X size={18} className="text-gray-400" />
                    </button>
                )}
            </div>

            {/* 참가자 목록 (가상 스크롤) */}
            <div
                className="flex-1 overflow-y-auto min-h-0"
                ref={scrollContainerRef}
            >
                {participants.length === 0 ? (
                    <p className="text-base text-gray-400 text-center py-4">참가자가 없습니다.</p>
                ) : (
                    <div
                        style={{
                            height: `${virtualizer.getTotalSize()}px`,
                            width: '100%',
                            position: 'relative',
                        }}
                    >
                        {virtualizer.getVirtualItems().map((virtualItem) => {
                            const participant = participants[virtualItem.index];
                            const isPresenter =
                                (presenterId !== null && participant.id === presenterId) ||
                                (presenterName !== null && participant.displayName === presenterName);

                            return (
                                <div
                                    key={virtualItem.key}
                                    data-index={virtualItem.index}
                                    ref={virtualizer.measureElement}
                                    style={{
                                        position: 'absolute',
                                        top: 0,
                                        left: 0,
                                        width: '100%',
                                        transform: `translateY(${virtualItem.start}px)`,
                                    }}
                                >
                                    <div className="flex items-stretch gap-2 py-1">
                                        {/* 왼쪽 초록색 단계별 라인 */}
                                        <div className={cn(
                                            'w-1 rounded-full shrink-0 transition-colors',
                                            conditionalClasses.state(
                                                !!participant.isSpeaking,
                                                'bg-emerald-500',
                                                participant.active ? 'bg-emerald-300' : 'bg-gray-200'
                                            )
                                        )} />

                                        {/* 참가자 정보 */}
                                        <div className={cn(
                                            'flex-1 flex items-center gap-3 px-3 py-2 rounded-xl transition-all duration-200',
                                            conditionalClasses.state(
                                                !!participant.isSpeaking,
                                                'bg-emerald-50',
                                                'hover:bg-gray-50'
                                            )
                                        )}>
                                            {/* 아바타 */}
                                            <div className="relative flex-shrink-0">
                                                <div className={cn(
                                                    'w-9 h-9 rounded-full flex items-center justify-center text-white text-sm font-bold',
                                                    getAvatarColor(participant.displayName)
                                                )}>
                                                    {getInitial(participant.displayName)}
                                                </div>
                                                {/* 접속 상태 도트 */}
                                                <div className={cn(
                                                    'absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full border-2 border-white',
                                                    conditionalClasses.state(!!participant.active, 'bg-green-500', 'bg-gray-300')
                                                )} />
                                            </div>

                                            {/* 이름 + 상태 */}
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-base font-medium text-gray-900 truncate">
                                                        {participant.displayName}
                                                    </span>
                                                    {isPresenter && (
                                                        <Crown size={14} className="text-amber-500 flex-shrink-0" />
                                                    )}
                                                </div>
                                                <span className="text-sm text-gray-400">
                                                    {isPresenter ? '발표 중' : participant.isPresent === false ? '자리 없음' : '참여 중'}
                                                </span>
                                            </div>

                                            {/* 말하기 인디케이터 */}
                                            {participant.isSpeaking && (
                                                <div className="flex-shrink-0">
                                                    <Mic size={16} className="text-emerald-500 animate-pulse" />
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </section>
    );
};

export default MeetingParticipants;
