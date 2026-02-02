import React from 'react';
import { cn, classBuilder, conditionalClasses } from '@/shared/utils/cn';
import { MeetingRoomParticipant } from '../types';
import { Crown, Mic } from 'lucide-react';

interface MeetingParticipantsProps {
    participants: MeetingRoomParticipant[];
    presenterId: number | null;
    presenterName: string | null;
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

const MeetingParticipants: React.FC<MeetingParticipantsProps> = ({ participants, presenterId, presenterName }) => {
    return (
        <section className={cn(classBuilder.card('default'), 'rounded-2xl p-4 shadow-sm flex flex-col gap-3 min-h-0 flex-[0_1_42%]')}>
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-gray-900">참가자</h3>
                <span className="inline-flex items-center justify-center min-w-[24px] h-6 px-2 bg-blue-100 text-blue-700 text-xs font-medium rounded-full">
                    {participants.length}
                </span>
            </div>

            {/* 참가자 목록 */}
            <div className="flex flex-col gap-1.5 overflow-y-auto">
                {participants.map((participant) => {
                    const isPresenter =
                        (presenterId !== null && participant.id === presenterId) ||
                        (presenterName !== null && participant.displayName === presenterName);

                    return (
                        <div
                            key={participant.id}
                            className={cn(
                                'flex items-center gap-3 px-3 py-2 rounded-xl transition-all duration-200',
                                conditionalClasses.state(
                                    participant.isSpeaking,
                                    'bg-blue-50 ring-1 ring-blue-200',
                                    'hover:bg-gray-50'
                                )
                            )}
                        >
                            {/* 아바타 */}
                            <div className="relative flex-shrink-0">
                                <div className={cn(
                                    'w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold',
                                    getAvatarColor(participant.displayName)
                                )}>
                                    {getInitial(participant.displayName)}
                                </div>
                                {/* 접속 상태 도트 */}
                                <div className={cn(
                                    'absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-white',
                                    conditionalClasses.state(!!participant.active, 'bg-green-500', 'bg-gray-300')
                                )} />
                            </div>

                            {/* 이름 + 상태 */}
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-1.5">
                                    <span className="text-sm font-medium text-gray-900 truncate">
                                        {participant.displayName}
                                    </span>
                                    {isPresenter && (
                                        <Crown size={12} className="text-amber-500 flex-shrink-0" />
                                    )}
                                </div>
                                <span className="text-xs text-gray-400">
                                    {isPresenter ? '발표 중' : participant.isPresent === false ? '자리 없음' : '자리 있음'}
                                </span>
                            </div>

                            {/* 말하기 인디케이터 */}
                            {participant.isSpeaking && (
                                <div className="flex-shrink-0">
                                    <Mic size={14} className="text-blue-500 animate-pulse" />
                                </div>
                            )}
                        </div>
                    );
                })}
                {participants.length === 0 && (
                    <p className="text-sm text-gray-400 text-center py-4">참가자가 없습니다.</p>
                )}
            </div>
        </section>
    );
};

export default MeetingParticipants;
