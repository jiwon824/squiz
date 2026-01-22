import React, { useEffect, useState } from 'react';
import { MessageSquare, Send, ArrowLeft, Loader2 } from 'lucide-react';
import { useDMStore } from '../store/dmStore';

const DMListMini: React.FC = () => {
    const {
        conversations,
        messages,
        currentConversationId,
        unreadCount,
        isLoading,
        pendingDMUser,
        fetchConversations,
        fetchUnreadCount,
        setCurrentConversation,
        sendMessage,
        clearPendingDM
    } = useDMStore();

    const [messageInput, setMessageInput] = useState('');

    // 초기 데이터 로드
    useEffect(() => {
        fetchConversations();
        fetchUnreadCount();
    }, [fetchConversations, fetchUnreadCount]);

    // 메시지 전송
    const handleSendMessage = async () => {
        if (!messageInput.trim()) return;

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

    // 시간 포맷팅
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
                    <button
                        onClick={() => clearPendingDM()}
                        className="p-1 hover:bg-gray-100 rounded-full transition-colors"
                    >
                        <ArrowLeft size={18} />
                    </button>
                    <div className="w-8 h-8 rounded-full bg-study-blue/10 flex items-center justify-center font-bold text-xs text-study-blue">
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
                        onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:border-study-blue"
                        autoFocus
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className="p-2 bg-study-blue text-white rounded-lg hover:bg-study-blue/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        <Send size={16} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 상세 보기
    if (currentConversationId) {
        const currentConversation = conversations.find(c => c.id === currentConversationId);

        return (
            <div className="p-4 h-full flex flex-col">
                {/* 채팅 헤더 */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
                    <button
                        onClick={() => setCurrentConversation(null)}
                        className="p-1 hover:bg-gray-100 rounded-full transition-colors"
                    >
                        <ArrowLeft size={18} />
                    </button>
                    <div className="w-8 h-8 rounded-full bg-study-blue/10 flex items-center justify-center font-bold text-xs text-study-blue">
                        {currentConversation?.participantNickname.charAt(0)}
                    </div>
                    <span className="font-bold text-sm">{currentConversation?.participantNickname}</span>
                </div>

                {/* 메시지 목록 */}
                <div className="flex-1 overflow-y-auto space-y-3 mb-4">
                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 size={24} className="animate-spin text-gray-400" />
                        </div>
                    ) : Array.isArray(messages) && messages.length > 0 ? (
                        messages.map(msg => (
                            <div
                                key={msg.id}
                                className={`flex ${msg.senderId === currentConversation?.participantId ? 'justify-start' : 'justify-end'}`}
                            >
                                <div
                                    className={`max-w-[80%] px-3 py-2 rounded-lg text-sm ${msg.senderId === currentConversation?.participantId
                                        ? 'bg-gray-100 text-gray-800'
                                        : 'bg-study-blue text-white'
                                        }`}
                                >
                                    <p>{msg.content}</p>
                                    <p className={`text-[10px] mt-1 ${msg.senderId === currentConversation?.participantId
                                        ? 'text-gray-400'
                                        : 'text-blue-200'
                                        }`}>
                                        {formatTime(msg.createdAt)}
                                    </p>
                                </div>
                            </div>
                        ))
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
                        onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                        placeholder="메시지 입력..."
                        className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:border-study-blue"
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!messageInput.trim()}
                        className="p-2 bg-study-blue text-white rounded-lg hover:bg-study-blue/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        <Send size={16} />
                    </button>
                </div>
            </div>
        );
    }

    // 대화방 목록
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
                {isLoading ? (
                    <div className="flex items-center justify-center py-8">
                        <Loader2 size={24} className="animate-spin text-gray-400" />
                    </div>
                ) : Array.isArray(conversations) && conversations.length > 0 ? (
                    <div className="space-y-1">
                        {conversations.map(dm => (
                            <div
                                key={dm.id}
                                onClick={() => setCurrentConversation(dm.id)}
                                className="flex flex-col p-2 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer group"
                            >
                                <div className="flex justify-between items-start mb-0.5">
                                    <div className="flex items-center gap-2">
                                        <div className="w-7 h-7 rounded-full bg-study-blue/10 flex items-center justify-center font-bold text-xs text-study-blue">
                                            {dm.participantNickname.charAt(0)}
                                        </div>
                                        <span className="text-sm font-bold text-gray-800">{dm.participantNickname}</span>
                                    </div>
                                    <span className="text-[10px] text-gray-400">{formatTime(dm.lastMessageAt)}</span>
                                </div>
                                <div className="flex justify-between items-center pl-9">
                                    <p className="text-xs text-gray-500 truncate pr-4">{dm.lastMessage}</p>
                                    {dm.unreadCount > 0 && (
                                        <span className="min-w-[18px] h-[18px] flex items-center justify-center bg-red-500 text-white text-[10px] rounded-full">
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
