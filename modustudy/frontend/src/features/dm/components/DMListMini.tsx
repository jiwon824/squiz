import React, { useEffect, useState, useRef } from 'react';
import { MessageSquare, Send } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { useDMStore } from '../store/dmStore';
import { useAuthStore } from '@/store/authStore';
import { BackButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

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
    const currentConversation = conversations.find(c => c.id === currentConversationId);

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
    }, [messages.length]);

    // 초기 데이터 로드 + WebSocket 연결 (로그인 상태에서만, 마운트 시 1회만 실행)
    useEffect(() => {
        const authStore = useAuthStore.getState();
        if (!authStore.user) {
            console.log('[DM] User not logged in, skipping WebSocket connection');
            return;
        }

        // store에서 직접 함수 참조 (의존성 문제 방지)
        const store = useDMStore.getState();
        store.fetchConversations();
        store.fetchUnreadCount();
        store.connectWebSocket();

        return () => {
            useDMStore.getState().disconnectWebSocket();
        };
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // 마운트/언마운트 시에만 실행

    // 메시지 전송
    const handleSendMessage = async () => {
        if (!messageInput.trim()) return;

        // 새 대화 시작 모드
        if (pendingDMUser) {
            const targetUserId = pendingDMUser.id;
            await sendMessage(targetUserId, messageInput);
            setMessageInput('');
            clearPendingDM();
            await fetchConversations();
            // 새로 생성된 대화방을 자동으로 열기
            const latestConversations = useDMStore.getState().conversations;
            const newConv = latestConversations.find(c => c.participantId === targetUserId);
            if (newConv) {
                setCurrentConversation(newConv.id);
            }
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

    // 시간 포맷팅 (백엔드는 KST(Asia/Seoul)로 동작, 타임존 정보 없이 반환됨)
    const formatTime = (dateString: string) => {
        // 타임존 정보가 없는 경우 KST(+09:00)로 해석
        const normalized = dateString.includes('Z') || dateString.includes('+') ? dateString : dateString + '+09:00';
        const date = new Date(normalized);
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

    // 날짜 문자열을 Date 객체로 변환 (KST 기준)
    const parseDate = (dateString: string): Date => {
        const normalized = dateString.includes('Z') || dateString.includes('+') ? dateString : dateString + '+09:00';
        return new Date(normalized);
    };

    // 두 날짜가 다른 날인지 확인
    const isDifferentDay = (date1: Date, date2: Date): boolean => {
        return date1.getFullYear() !== date2.getFullYear() ||
               date1.getMonth() !== date2.getMonth() ||
               date1.getDate() !== date2.getDate();
    };

    // 날짜 구분선에 표시할 텍스트 포맷
    const formatDateSeparator = (dateString: string): string => {
        const date = parseDate(dateString);
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        const diffDays = Math.floor((today.getTime() - targetDay.getTime()) / (1000 * 60 * 60 * 24));

        if (diffDays === 0) {
            return '오늘';
        } else if (diffDays === 1) {
            return '어제';
        } else {
            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'short'
            });
        }
    };

    // 새 대화 시작 모드
    if (pendingDMUser) {
        return (
            <div className="py-4 pl-4 pr-4 h-full flex flex-col bg-transparent">
                {/* 채팅 헤더 */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
                    <BackButton
                        variant="icon-only"
                        onClick={() => clearPendingDM()}
                    />
                    <div className={cn(
                        'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs overflow-hidden',
                        'bg-study-blue/10 text-study-blue'
                    )}>
                        <img src={getProfileImageUrl(pendingDMUser.profileImage)} alt={pendingDMUser.nickname} className="w-full h-full object-cover" />
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
                <div className="flex-shrink-0 flex items-center gap-2 pt-3 border-t border-gray-100">
                    <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className={cn(
                            'flex-1 h-10 px-3 text-sm rounded-lg',
                            'border border-gray-200 focus:outline-none focus:border-study-blue'
                        )}
                        autoFocus
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className={cn(
                            'flex-shrink-0 h-10 w-10 flex items-center justify-center rounded-lg transition-colors',
                            'bg-study-blue text-white hover:bg-study-blue/90',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        <Send size={16} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 상세 보기
    if (currentConversationId) {

        return (
            <div className="py-4 pl-4 pr-4 h-full flex flex-col bg-transparent">
                {/* 채팅 헤더 */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
                    <BackButton
                        variant="icon-only"
                        onClick={() => setCurrentConversation(null)}
                    />
                    <div className={cn(
                        'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs overflow-hidden',
                        'bg-study-blue/10 text-study-blue'
                    )}>
                        <img src={getProfileImageUrl(currentConversation?.participantProfileImage)} alt={currentConversation?.participantNickname} className="w-full h-full object-cover" />
                    </div>
                    <span className="font-bold text-sm">{currentConversation?.participantNickname}</span>
                </div>

                {/* 메시지 목록 */}
                <div className="flex-1 overflow-y-auto min-h-0 px-1" ref={messagesRef}>
                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Spinner size="md" color="#9ca3af" />
                        </div>
                    ) : Array.isArray(messages) && messages.length > 0 ? (
                        <div className="py-2">
                            {messages.map((msg, index) => {
                                const isReceiver = msg.senderId === currentConversation?.participantId;
                                const prevMsg = index > 0 ? messages[index - 1] : null;
                                const nextMsg = index < messages.length - 1 ? messages[index + 1] : null;

                                // 날짜 구분선 표시 여부 확인
                                const currentDate = parseDate(msg.createdAt);
                                const prevDate = prevMsg ? parseDate(prevMsg.createdAt) : null;
                                const showDateSeparator = index === 0 || (prevDate && isDifferentDay(currentDate, prevDate));

                                // 날짜가 바뀌면 같은 발신자여도 새 그룹으로 처리
                                const isSameSenderAsPrev = prevMsg !== null && prevMsg.senderId === msg.senderId && !showDateSeparator;
                                const isSameSenderAsNext = nextMsg !== null && nextMsg.senderId === msg.senderId;

                                return (
                                    <React.Fragment key={msg.id}>
                                        {/* 날짜 구분선 */}
                                        {showDateSeparator && (
                                            <div className={cn(
                                                'flex items-center gap-3 my-4',
                                                index === 0 ? 'mt-0' : ''
                                            )}>
                                                <div className="flex-1 h-px bg-gradient-to-r from-transparent via-gray-200 to-transparent" />
                                                <span className="text-xs text-gray-400 font-medium px-2 py-1 bg-gray-50 rounded-full">
                                                    {formatDateSeparator(msg.createdAt)}
                                                </span>
                                                <div className="flex-1 h-px bg-gradient-to-r from-transparent via-gray-200 to-transparent" />
                                            </div>
                                        )}
                                        <div
                                            className={cn(
                                                'flex gap-2',
                                                isReceiver ? 'flex-row' : 'flex-row-reverse',
                                                isSameSenderAsPrev ? 'mt-1' : index > 0 && !showDateSeparator ? 'mt-4' : ''
                                            )}
                                        >
                                        {/* 아바타 - 같은 발신자 연속이면 빈 공간 유지 */}
                                        <div className="flex-shrink-0 w-10">
                                            {!isSameSenderAsPrev && (
                                                <div className={cn(
                                                    'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm overflow-hidden',
                                                    'bg-primary/10 text-primary'
                                                )}>
                                                    <img
                                                        src={getProfileImageUrl(isReceiver ? currentConversation?.participantProfileImage : useAuthStore.getState().user?.avatar)}
                                                        alt={isReceiver ? currentConversation?.participantNickname : '나'}
                                                        className="w-full h-full object-cover"
                                                    />
                                                </div>
                                            )}
                                        </div>

                                        {/* 메시지 콘텐츠 */}
                                        <div className={cn(
                                            'flex flex-col max-w-[70%]',
                                            isReceiver ? 'items-start' : 'items-end'
                                        )}>
                                            {/* 헤더: 이름 + 시간 (그룹 첫 메시지에서만 표시) */}
                                            {!isSameSenderAsPrev && (
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
                                            )}

                                            {/* 말풍선 */}
                                            <div
                                                className={cn(
                                                    'px-4 py-2 rounded-2xl text-sm break-words',
                                                    isReceiver
                                                        ? 'bg-gray-100 text-text-primary'
                                                        : 'bg-primary text-white',
                                                    !isSameSenderAsPrev && (isReceiver ? 'rounded-tl-sm' : 'rounded-tr-sm')
                                                )}
                                            >
                                                {renderMessageContent(msg.content)}
                                            </div>

                                            {/* 전송됨 표시 (그룹의 마지막 메시지에서만) */}
                                            {!isReceiver && !isSameSenderAsNext && (
                                                <div className="text-[10px] text-text-secondary mt-1">
                                                    전송됨
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                    </React.Fragment>
                                );
                            })}
                        </div>
                    ) : (
                        <div className="flex-1 flex items-center justify-center h-full">
                            <div className="text-center text-gray-400 text-sm">
                                메시지가 없습니다
                            </div>
                        </div>
                    )}
                </div>

                {/* 메시지 입력 */}
                <div className="flex-shrink-0 flex items-center gap-2 pt-3 border-t border-gray-100">
                    <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className={cn(
                            'flex-1 h-10 px-3 text-sm rounded-lg',
                            'border border-gray-200 focus:outline-none focus:border-study-blue'
                        )}
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className={cn(
                            'flex-shrink-0 h-10 w-10 flex items-center justify-center rounded-lg transition-colors',
                            'bg-study-blue text-white hover:bg-study-blue/90',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        <Send size={16} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 목록

    return (
        <div className="py-4 pl-4 pr-4 h-full flex flex-col bg-transparent">
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
                {isLoadingConversations && conversations.length === 0 ? (
                    <div className="flex items-center justify-center py-8">
                        <Spinner size="md" color="#9ca3af" />
                    </div>
                ) : Array.isArray(conversations) && conversations.length > 0 ? (
                    <div className="space-y-1">
                        {conversations.map(dm => (
                            <div
                                key={dm.id}
                                onClick={() => setCurrentConversation(dm.id)}
                                className={cn(
                                    'flex flex-col p-2 rounded-lg transition-colors cursor-pointer group',
                                    'hover:bg-gray-50'
                                )}
                            >
                                <div className="flex justify-between items-start mb-0.5">
                                    <div className="flex items-center gap-2">
                                        <div className={cn(
                                            'w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs overflow-hidden',
                                            'bg-study-blue/10 text-study-blue'
                                        )}>
                                            <img src={getProfileImageUrl(dm.participantProfileImage)} alt={dm.participantNickname} className="w-full h-full object-cover" />
                                        </div>
                                        <span className="text-sm font-bold text-gray-800">{dm.participantNickname}</span>
                                    </div>
                                    <span className="text-[10px] text-gray-400">{formatTime(dm.lastMessageAt)}</span>
                                </div>
                                <div className="flex justify-between items-center pl-9">
                                    <p className="text-xs text-gray-500 truncate pr-4">{dm.lastMessage}</p>
                                    {dm.unreadCount > 0 && !dm.lastMessageIsMine && (
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
