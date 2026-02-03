import React, { useCallback, useEffect, useRef, useState } from 'react';
import { cn, classBuilder, conditionalClasses } from '@/shared/utils/cn';
import { MeetingRoomChatMessage } from '../types';
import { Send, Trash2, MessageCircle } from 'lucide-react';
import { useVirtualizer } from '@tanstack/react-virtual';

interface MeetingChatPanelProps {
    messages: MeetingRoomChatMessage[];
    onSend: (text: string) => void;
    onDelete: (message: MeetingRoomChatMessage) => void;
    currentUserId?: number | null;
    currentSender: string;
    /** 최대화 모드에서 플로팅 패널로 표시될 때 compact UI 적용 */
    compact?: boolean;
}

const MeetingChatPanel: React.FC<MeetingChatPanelProps> = ({
    messages,
    onSend,
    onDelete,
    currentUserId,
    currentSender,
    compact = false,
}) => {
    const [text, setText] = useState('');
    const scrollContainerRef = useRef<HTMLDivElement | null>(null);

    // 가상 스크롤 설정
    const virtualizer = useVirtualizer({
        count: messages.length,
        getScrollElement: () => scrollContainerRef.current,
        estimateSize: () => (compact ? 52 : 64),
        overscan: 10,
    });

    // 새 메시지 추가 시 하단으로 자동 스크롤
    const prevCountRef = useRef(messages.length);
    useEffect(() => {
        if (messages.length > prevCountRef.current && messages.length > 0) {
            virtualizer.scrollToIndex(messages.length - 1, { align: 'end' });
        }
        prevCountRef.current = messages.length;
    }, [messages.length, virtualizer]);

    // 초기 마운트 시 하단으로 스크롤
    useEffect(() => {
        if (messages.length > 0) {
            virtualizer.scrollToIndex(messages.length - 1, { align: 'end' });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleSubmit = useCallback((event: React.FormEvent) => {
        event.preventDefault();
        const trimmed = text.trim();
        if (!trimmed) return;
        onSend(trimmed);
        setText('');
    }, [text, onSend]);

    return (
        <section className={cn(
            classBuilder.card('default'),
            'rounded-2xl shadow-sm flex flex-col min-h-0',
            compact ? 'p-3 gap-2 flex-1' : 'p-4 gap-3 flex-[1_1_58%]'
        )}>
            {/* 헤더 */}
            <h3 className={cn(
                'font-semibold text-gray-900',
                compact ? 'text-xs' : 'text-sm'
            )}>채팅</h3>

            {/* 메시지 목록 (가상 스크롤) */}
            <div
                className="flex-1 overflow-y-auto min-h-0"
                ref={scrollContainerRef}
            >
                {messages.length === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-2 py-8 text-gray-300">
                        <MessageCircle size={compact ? 24 : 32} />
                        <span className={compact ? 'text-xs' : 'text-sm'}>아직 채팅이 없습니다.</span>
                    </div>
                ) : (
                    <div
                        style={{
                            height: `${virtualizer.getTotalSize()}px`,
                            width: '100%',
                            position: 'relative',
                        }}
                    >
                        {virtualizer.getVirtualItems().map((virtualItem) => {
                            const message = messages[virtualItem.index];
                            const isOwn =
                                (currentUserId !== null &&
                                    currentUserId !== undefined &&
                                    message.userId !== null &&
                                    message.userId === currentUserId) ||
                                message.sender === currentSender;

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
                                    <div className={cn(
                                        'flex flex-col max-w-[85%] group',
                                        compact ? 'pb-1.5' : 'pb-2',
                                        conditionalClasses.state(isOwn, 'ml-auto items-end', 'mr-auto items-start')
                                    )}>
                                        {/* 발신자 이름 (상대방만) */}
                                        {!isOwn && (
                                            <span className={cn(
                                                'text-gray-400 mb-0.5 ml-1',
                                                compact ? 'text-[10px]' : 'text-[11px]'
                                            )}>
                                                {message.sender}
                                            </span>
                                        )}

                                        {/* 말풍선 */}
                                        <div className={cn(
                                            'relative rounded-2xl break-words',
                                            compact ? 'px-2.5 py-1.5 text-xs' : 'px-3 py-2 text-sm',
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
                                                    <Trash2 size={compact ? 10 : 12} className="text-gray-400" />
                                                </button>
                                            )}
                                        </div>

                                        {/* 시간 */}
                                        <span className={cn(
                                            'text-gray-300 mt-0.5',
                                            compact ? 'text-[9px]' : 'text-[10px]',
                                            conditionalClasses.state(isOwn, 'mr-1', 'ml-1')
                                        )}>
                                            {new Date(message.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
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
                    className={cn(
                        'flex-1 min-w-0 bg-gray-50 border border-gray-200 rounded-full placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-all',
                        compact ? 'px-3 py-1.5 text-xs' : 'px-4 py-2 text-sm'
                    )}
                />
                <button
                    type="submit"
                    className={cn(
                        'flex-shrink-0 rounded-full flex items-center justify-center transition-all cursor-pointer',
                        compact ? 'w-7 h-7' : 'w-9 h-9',
                        conditionalClasses.state(
                            !!text.trim(),
                            'bg-blue-600 text-white hover:bg-blue-700 shadow-sm',
                            'bg-gray-100 text-gray-400'
                        )
                    )}
                    disabled={!text.trim()}
                >
                    <Send size={compact ? 13 : 16} />
                </button>
            </form>
        </section>
    );
};

export default MeetingChatPanel;
