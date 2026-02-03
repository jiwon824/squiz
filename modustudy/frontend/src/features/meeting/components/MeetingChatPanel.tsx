import React, { useCallback, useEffect, useRef, useState } from 'react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { MeetingRoomChatMessage } from '../types';
import { Send, Trash2, MessageCircle, X } from 'lucide-react';
import { useVirtualizer } from '@tanstack/react-virtual';

interface MeetingChatPanelProps {
    messages: MeetingRoomChatMessage[];
    onSend: (text: string) => void;
    onDelete: (message: MeetingRoomChatMessage) => void;
    currentUserId?: number | null;
    currentSender: string;
    /** 최대화 모드에서 플로팅 패널로 표시될 때 compact UI 적용 */
    compact?: boolean;
    /** 닫기 버튼 클릭 시 */
    onClose?: () => void;
}

const MeetingChatPanel: React.FC<MeetingChatPanelProps> = ({
    messages,
    onSend,
    onDelete,
    currentUserId,
    currentSender,
    compact = false,
    onClose,
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
            'bg-white rounded-2xl shadow-[0_4px_15px_rgba(0,0,0,0.05)] flex flex-col min-h-0',
            compact ? 'p-3 gap-2 flex-1' : 'p-4 gap-3 flex-[1_1_58%]'
        )}>
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <h3 className={cn(
                    'font-bold text-gray-900',
                    compact ? 'text-lg' : 'text-xl'
                )}>채팅</h3>
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

            {/* 메시지 목록 (가상 스크롤) */}
            <div
                className="flex-1 overflow-y-auto min-h-0"
                ref={scrollContainerRef}
            >
                {messages.length === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-2 py-8 text-gray-300">
                        <MessageCircle size={compact ? 28 : 36} />
                        <span className={compact ? 'text-sm' : 'text-base'}>아직 채팅이 없습니다.</span>
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
                            const prevMessage = virtualItem.index > 0 ? messages[virtualItem.index - 1] : null;
                            const nextMessage = virtualItem.index < messages.length - 1 ? messages[virtualItem.index + 1] : null;

                            const isOwn =
                                (currentUserId !== null &&
                                    currentUserId !== undefined &&
                                    message.userId !== null &&
                                    message.userId === currentUserId) ||
                                message.sender === currentSender;

                            // 연속 메시지 그룹화: 같은 사람이 연속으로 보낸 메시지인지 확인
                            const isSameSenderAsPrev = prevMessage && prevMessage.sender === message.sender;
                            const isSameSenderAsNext = nextMessage && nextMessage.sender === message.sender;

                            // 그룹의 첫 번째 메시지: 이름 표시 (상대방만)
                            const showSenderName = !isOwn && !isSameSenderAsPrev;
                            // 그룹의 마지막 메시지: 시간 표시
                            const showTime = !isSameSenderAsNext;

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
                                        compact ? 'pb-1' : 'pb-1.5',
                                        conditionalClasses.state(isOwn, 'ml-auto items-end', 'mr-auto items-start')
                                    )}>
                                        {/* 발신자 이름 (상대방, 그룹 첫 메시지만) */}
                                        {showSenderName && (
                                            <span className={cn(
                                                'text-gray-400 mb-0.5 ml-1',
                                                compact ? 'text-xs' : 'text-sm'
                                            )}>
                                                {message.sender}
                                            </span>
                                        )}

                                        {/* 말풍선 */}
                                        <div className={cn(
                                            'relative rounded-2xl break-words',
                                            compact ? 'px-3 py-2 text-sm' : 'px-3.5 py-2.5 text-base',
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
                                                    className="absolute -left-8 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-full hover:bg-gray-100 cursor-pointer"
                                                    onClick={() => onDelete(message)}
                                                >
                                                    <Trash2 size={compact ? 12 : 14} className="text-gray-400" />
                                                </button>
                                            )}
                                        </div>

                                        {/* 시간 (그룹 마지막 메시지만) */}
                                        {showTime && (
                                            <span className={cn(
                                                'text-gray-300 mt-0.5',
                                                compact ? 'text-[10px]' : 'text-xs',
                                                conditionalClasses.state(isOwn, 'mr-1', 'ml-1')
                                            )}>
                                                {new Date(message.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* 입력 영역 */}
            <form className="flex items-center gap-2.5" onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={text}
                    onChange={(event) => setText(event.target.value)}
                    placeholder="메시지를 입력하세요"
                    className={cn(
                        'flex-1 min-w-0 bg-gray-50 rounded-full placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 transition-all',
                        compact ? 'px-4 py-2 text-sm' : 'px-5 py-2.5 text-base'
                    )}
                />
                <button
                    type="submit"
                    className={cn(
                        'flex-shrink-0 rounded-full flex items-center justify-center transition-all cursor-pointer',
                        compact ? 'w-9 h-9' : 'w-10 h-10',
                        conditionalClasses.state(
                            !!text.trim(),
                            'bg-blue-600 text-white hover:bg-blue-700 shadow-sm',
                            'bg-blue-200 text-blue-400'
                        )
                    )}
                    disabled={!text.trim()}
                >
                    <Send size={compact ? 16 : 18} />
                </button>
            </form>
        </section>
    );
};

export default MeetingChatPanel;
