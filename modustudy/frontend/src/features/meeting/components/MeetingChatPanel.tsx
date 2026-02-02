import React, { useEffect, useRef, useState } from 'react';
import { cn, classBuilder, conditionalClasses } from '@/shared/utils/cn';
import { MeetingRoomChatMessage } from '../types';
import { Send, Trash2, MessageCircle } from 'lucide-react';

interface MeetingChatPanelProps {
    messages: MeetingRoomChatMessage[];
    onSend: (text: string) => void;
    onDelete: (message: MeetingRoomChatMessage) => void;
    currentUserId?: number | null;
    currentSender: string;
}

const MeetingChatPanel: React.FC<MeetingChatPanelProps> = ({
    messages,
    onSend,
    onDelete,
    currentUserId,
    currentSender,
}) => {
    const [text, setText] = useState('');
    const messagesRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        if (!messagesRef.current) return;
        messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }, [messages.length]);

    const handleSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        const trimmed = text.trim();
        if (!trimmed) return;
        onSend(trimmed);
        setText('');
    };

    return (
        <section className={cn(classBuilder.card('default'), 'rounded-2xl p-4 shadow-sm flex flex-col gap-3 flex-[1_1_58%] min-h-0')}>
            {/* 헤더 */}
            <h3 className="text-sm font-semibold text-gray-900">채팅</h3>

            {/* 메시지 목록 */}
            <div className="flex-1 overflow-y-auto flex flex-col gap-2 min-h-0" ref={messagesRef}>
                {messages.map((message, index) => {
                    const isOwn =
                        (currentUserId !== null &&
                            currentUserId !== undefined &&
                            message.userId !== null &&
                            message.userId === currentUserId) ||
                        message.sender === currentSender;

                    return (
                        <div
                            key={message.id ?? `${message.sentAt}-${index}`}
                            className={cn(
                                'flex flex-col max-w-[85%] group',
                                conditionalClasses.state(isOwn, 'self-end items-end', 'self-start items-start')
                            )}
                        >
                            {/* 발신자 이름 (상대방만) */}
                            {!isOwn && (
                                <span className="text-[11px] text-gray-400 mb-0.5 ml-1">
                                    {message.sender}
                                </span>
                            )}

                            {/* 말풍선 */}
                            <div className={cn(
                                'relative px-3 py-2 rounded-2xl text-sm break-words',
                                conditionalClasses.state(
                                    isOwn,
                                    'bg-blue-600 text-white rounded-br-md',
                                    'bg-gray-100 text-gray-900 rounded-bl-md'
                                )
                            )}>
                                {message.text}

                                {/* 삭제 버튼 (hover 시 표시, 내 메시지만) */}
                                {isOwn && (
                                    <button
                                        type="button"
                                        className="absolute -left-7 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded-full hover:bg-gray-100 cursor-pointer"
                                        onClick={() => onDelete(message)}
                                    >
                                        <Trash2 size={12} className="text-gray-400" />
                                    </button>
                                )}
                            </div>

                            {/* 시간 */}
                            <span className={cn(
                                'text-[10px] text-gray-300 mt-0.5',
                                conditionalClasses.state(isOwn, 'mr-1', 'ml-1')
                            )}>
                                {new Date(message.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </span>
                        </div>
                    );
                })}
                {messages.length === 0 && (
                    <div className="flex flex-col items-center justify-center gap-2 py-8 text-gray-300">
                        <MessageCircle size={32} />
                        <span className="text-sm">아직 채팅이 없습니다.</span>
                    </div>
                )}
            </div>

            {/* 입력 영역 */}
            <form className="flex items-center gap-2" onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={text}
                    onChange={(event) => setText(event.target.value)}
                    placeholder="메시지를 입력하세요"
                    className="flex-1 min-w-0 px-4 py-2 bg-gray-50 border border-gray-200 rounded-full text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-all"
                />
                <button
                    type="submit"
                    className={cn(
                        'flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center transition-all cursor-pointer',
                        conditionalClasses.state(
                            !!text.trim(),
                            'bg-blue-600 text-white hover:bg-blue-700 shadow-sm',
                            'bg-gray-100 text-gray-400'
                        )
                    )}
                    disabled={!text.trim()}
                >
                    <Send size={16} />
                </button>
            </form>
        </section>
    );
};

export default MeetingChatPanel;
