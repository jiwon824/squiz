import React, { useEffect, useState, useRef } from 'react';
import { MessageSquare, Send, Loader2 } from 'lucide-react';
import { useDMStore } from '../store/dmStore';
import { BackButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';

// URL을 클릭 가능한 링크로 변환하는 함수
const renderMessageContent = (content: string) => {
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const parts = content.split(urlRegex);

    return parts.map((part, index) => {
        if (part.match(urlRegex)) {
            return (
                <a
                    key={index}
                    href={part}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="underline break-all hover:opacity-80"
                    onClick={(e) => e.stopPropagation()}
                >
                    {part}
                </a>
            );
        }
        return <span key={index}>{part}</span>;
    });
};

const DMListMini: React.FC = () => {
    const {
        conversations,
        messages,
        currentConversationId,
        unreadCount,
        isLoading,
        isLoadingConversations,
        pendingDMUser,
        fetchConversations,
        fetchUnreadCount,
        setCurrentConversation,
        sendMessage,
        clearPendingDM,
        connectWebSocket,
        disconnectWebSocket
    } = useDMStore();

    const [messageInput, setMessageInput] = useState('');
    const messagesRef = useRef<HTMLDivElement>(null);
    const [showDemoChat, setShowDemoChat] = useState(false); // 데모 채팅방 표시 여부

    // 🎨 임시 데모 데이터 (테스트용)
    const DEMO_MODE = true;
    const demoMessages = DEMO_MODE ? [
        {
            id: 1,
            senderId: 100,
            content: '안녕하세요! 오늘 스터디 몇 시에 시작하나요?',
            createdAt: new Date(Date.now() - 3600000).toISOString(),
        },
        {
            id: 2,
            senderId: 999, // 나
            content: '7시에 시작합니다! 준비 부탁드려요 😊',
            createdAt: new Date(Date.now() - 3000000).toISOString(),
        },
        {
            id: 3,
            senderId: 100,
            content: '네 알겠습니다. 자료는 미리 공유해주실 수 있나요?',
            createdAt: new Date(Date.now() - 2400000).toISOString(),
        },
        {
            id: 4,
            senderId: 999,
            content: '네, 지금 바로 공유해드릴게요. https://example.com/study-materials',
            createdAt: new Date(Date.now() - 1800000).toISOString(),
        },
        {
            id: 5,
            senderId: 100,
            content: '감사합니다! 🙏',
            createdAt: new Date(Date.now() - 1200000).toISOString(),
        },
    ] : messages;

    const demoConversation = DEMO_MODE ? {
        id: 1,
        participantId: 100,
        participantNickname: '김철수',
    } : conversations.find(c => c.id === currentConversationId);

    // 메시지 목록 자동 스크롤 (DOM 업데이트 후 실행)
    useEffect(() => {
        if (!messagesRef.current) return;
        // setTimeout으로 DOM 업데이트 완료 후 스크롤
        const timer = setTimeout(() => {
            if (messagesRef.current) {
                messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
            }
        }, 0);
        return () => clearTimeout(timer);
    }, [DEMO_MODE ? demoMessages.length : messages.length]);

    // 초기 데이터 로드 + WebSocket 연결
    useEffect(() => {
        if (DEMO_MODE) return; // 데모 모드에서는 API 호출 안함

        fetchConversations();
        fetchUnreadCount();
        connectWebSocket();

        return () => {
            disconnectWebSocket();
        };
    }, [fetchConversations, fetchUnreadCount, connectWebSocket, disconnectWebSocket]);

    // 메시지 전송
    const handleSendMessage = async () => {
        if (!messageInput.trim()) return;

        // 🎨 데모 모드: 메시지 전송 안함 (UI 확인용)
        if (DEMO_MODE) {
            alert('데모 모드입니다. 실제 메시지는 전송되지 않습니다.');
            setMessageInput('');
            return;
        }

        // 새 대화 시작 모드
        if (pendingDMUser) {
            await sendMessage(pendingDMUser.id, messageInput);
            setMessageInput('');
            clearPendingDM();
            fetchConversations();
            return;
        }

        // 기존 대화
        if (!currentConversationId) return;
        const conversation = conversations.find(c => c.id === currentConversationId);
        if (conversation) {
            await sendMessage(conversation.participantId, messageInput);
            setMessageInput('');
        }
    };

    // 시간 포맷팅 (백엔드는 서버 로컬 시간 LocalDateTime 반환)
    const formatTime = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) {
            return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
        } else if (days === 1) {
            return '어제';
        } else if (days < 7) {
            return `${days}일 전`;
        } else {
            return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        }
    };

    // 새 대화 시작 모드
    if (pendingDMUser) {
        return (
            <div className="p-4 h-full flex flex-col">
                {/* 채팅 헤더 */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
                    <BackButton
                        variant="icon-only"
                        onClick={() => clearPendingDM()}
                    />
                    <div className={cn(
                        'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs',
                        'bg-study-blue/10 text-study-blue'
                    )}>
                        {pendingDMUser.nickname.charAt(0)}
                    </div>
                    <span className="font-bold text-sm">{pendingDMUser.nickname}</span>
                    <span className="text-xs text-green-500 ml-1">새 대화</span>
                </div>

                {/* 빈 메시지 영역 */}
                <div className="flex-1 flex items-center justify-center">
                    <div className="text-center text-gray-400">
                        <MessageSquare size={32} className="mx-auto mb-2 opacity-50" />
                        <p className="text-sm">{pendingDMUser.nickname}님과의 새 대화</p>
                        <p className="text-xs mt-1">첫 메시지를 보내보세요</p>
                    </div>
                </div>

                {/* 메시지 입력 */}
                <div className="flex gap-2">
                    <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className={cn(
                            'flex-1 px-3 py-2 text-sm rounded-lg',
                            'border border-gray-200 focus:outline-none focus:border-study-blue'
                        )}
                        autoFocus
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className={cn(
                            'w-11 h-11 flex items-center justify-center rounded-lg transition-colors',
                            'bg-study-blue text-white hover:bg-study-blue/90',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        <Send size={18} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 상세 보기
    if ((DEMO_MODE && showDemoChat) || (!DEMO_MODE && currentConversationId)) {
        const currentConversation = DEMO_MODE ? demoConversation : conversations.find(c => c.id === currentConversationId);

        return (
            <div className="p-4 h-full flex flex-col">
                {/* 채팅 헤더 */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
                    <BackButton
                        variant="icon-only"
                        onClick={() => DEMO_MODE ? setShowDemoChat(false) : setCurrentConversation(null)}
                    />
                    <div className={cn(
                        'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs',
                        'bg-study-blue/10 text-study-blue'
                    )}>
                        {currentConversation?.participantNickname.charAt(0)}
                    </div>
                    <span className="font-bold text-sm">{currentConversation?.participantNickname}</span>
                </div>

                {/* 메시지 목록 */}
                <div className="overflow-y-auto space-y-4 mb-4 max-h-[calc(100vh-250px)]" ref={messagesRef}>
                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 size={24} className="animate-spin text-gray-400" />
                        </div>
                    ) : Array.isArray(DEMO_MODE ? demoMessages : messages) && (DEMO_MODE ? demoMessages : messages).length > 0 ? (
                        (DEMO_MODE ? demoMessages : messages).map(msg => {
                            const isReceiver = msg.senderId === currentConversation?.participantId;
                            return (
                                <div
                                    key={msg.id}
                                    className={cn('flex gap-2', isReceiver ? 'flex-row' : 'flex-row-reverse')}
                                >
                                    {/* 아바타 */}
                                    <div className="flex-shrink-0">
                                        <div className={cn(
                                            'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm',
                                            'bg-primary/10 text-primary'
                                        )}>
                                            {isReceiver
                                                ? currentConversation?.participantNickname.charAt(0)
                                                : 'Me'
                                            }
                                        </div>
                                    </div>

                                    {/* 메시지 콘텐츠 */}
                                    <div className={cn(
                                        'flex flex-col max-w-[70%]',
                                        isReceiver ? 'items-start' : 'items-end'
                                    )}>
                                        {/* 헤더: 이름 + 시간 */}
                                        <div className={cn(
                                            'flex items-center gap-2 mb-1',
                                            isReceiver ? 'flex-row' : 'flex-row-reverse'
                                        )}>
                                            <span className="text-xs font-semibold text-text-primary">
                                                {isReceiver ? currentConversation?.participantNickname : '나'}
                                            </span>
                                            <time className="text-[10px] text-text-secondary">
                                                {formatTime(msg.createdAt)}
                                            </time>
                                        </div>

                                        {/* 말풍선 */}
                                        <div
                                            className={cn(
                                                'px-4 py-2 rounded-2xl text-sm break-words',
                                                isReceiver
                                                    ? 'bg-gray-100 text-text-primary rounded-tl-sm'
                                                    : 'bg-primary text-white rounded-tr-sm'
                                            )}
                                        >
                                            {renderMessageContent(msg.content)}
                                        </div>

                                        {/* 푸터: 읽음 상태 (추후 확장 가능) */}
                                        {!isReceiver && (
                                            <div className="text-[10px] text-text-secondary mt-1">
                                                전송됨
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })
                    ) : (
                        <div className="text-center py-8 text-gray-400 text-sm">
                            메시지가 없습니다
                        </div>
                    )}
                </div>

                {/* 메시지 입력 */}
                <div className="flex gap-2">
                    <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className={cn(
                            'flex-1 px-3 py-2 text-sm rounded-lg',
                            'border border-gray-200 focus:outline-none focus:border-study-blue'
                        )}
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className={cn(
                            'w-11 h-11 flex items-center justify-center rounded-lg transition-colors',
                            'bg-study-blue text-white hover:bg-study-blue/90',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        <Send size={18} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 목록
    const displayConversations = DEMO_MODE
        ? [{ id: 1, participantNickname: '김철수', lastMessage: '감사합니다! 🙏', lastMessageAt: new Date(Date.now() - 1200000).toISOString(), unreadCount: 0 }]
        : conversations;

    return (
        <div className="p-4 h-full flex flex-col">
            {/* 헤더 */}
            <div className="flex items-center gap-2 mb-4 text-study-blue">
                <MessageSquare size={20} />
                <h3 className="font-bold text-sm">다이렉트 메시지</h3>
                {unreadCount > 0 && (
                    <span className="bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                        {unreadCount}
                    </span>
                )}
            </div>

            {/* 대화방 목록 */}
            <div className="flex-1 overflow-y-auto">
                {isLoadingConversations && displayConversations.length === 0 ? (
                    <div className="flex items-center justify-center py-8">
                        <Loader2 size={24} className="animate-spin text-gray-400" />
                    </div>
                ) : Array.isArray(displayConversations) && displayConversations.length > 0 ? (
                    <div className="space-y-1">
                        {displayConversations.map(dm => (
                            <div
                                key={dm.id}
                                onClick={() => DEMO_MODE ? setShowDemoChat(true) : setCurrentConversation(dm.id)}
                                className={cn(
                                    'flex flex-col p-2 rounded-lg transition-colors cursor-pointer group',
                                    'hover:bg-gray-50'
                                )}
                            >
                                <div className="flex justify-between items-start mb-0.5">
                                    <div className="flex items-center gap-2">
                                        <div className={cn(
                                            'w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs',
                                            'bg-study-blue/10 text-study-blue'
                                        )}>
                                            {dm.participantNickname.charAt(0)}
                                        </div>
                                        <span className="text-sm font-bold text-gray-800">{dm.participantNickname}</span>
                                    </div>
                                    <span className="text-[10px] text-gray-400">{formatTime(dm.lastMessageAt)}</span>
                                </div>
                                <div className="flex justify-between items-center pl-9">
                                    <p className="text-xs text-gray-500 truncate pr-4">{dm.lastMessage}</p>
                                    {dm.unreadCount > 0 && (
                                        <span className={cn(
                                            'min-w-[18px] h-[18px] flex items-center justify-center',
                                            'bg-red-500 text-white text-[10px] rounded-full'
                                        )}>
                                            {dm.unreadCount}
                                        </span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-8 text-gray-400">
                        <MessageSquare size={32} className="mx-auto mb-2 opacity-50" />
                        <p className="text-sm">대화가 없습니다</p>
                        <p className="text-xs mt-1">친구 목록에서 대화를 시작하세요</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default DMListMini;
