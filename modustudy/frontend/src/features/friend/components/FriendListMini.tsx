import React, { useEffect, useState } from 'react';
import { Users, Search, UserPlus, Check, X, Loader2 } from 'lucide-react';
import { useFriendStore } from '../store/friendStore';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useUIStore } from '@/store/uiStore';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

const FriendListMini: React.FC = () => {
    const {
        friends,
        receivedRequests,
        searchResults,
        isLoading,
        fetchFriends,
        fetchReceivedRequests,
        searchUsers,
        sendRequest,
        acceptRequest,
        rejectRequest,
        clearSearch
    } = useFriendStore();

    const { startConversationWith, fetchConversations } = useDMStore();
    const { setActiveRightTab } = useUIStore();

    const [searchQuery, setSearchQuery] = useState('');
    const [showSearch, setShowSearch] = useState(false);

    // 초기 데이터 로드
    useEffect(() => {
        fetchFriends();
        fetchReceivedRequests();
    }, [fetchFriends, fetchReceivedRequests]);

    // 검색 디바운스
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchQuery.trim()) {
                searchUsers(searchQuery);
            } else {
                clearSearch();
            }
        }, 300);
        return () => clearTimeout(timer);
    }, [searchQuery, searchUsers, clearSearch]);

    // 친구 클릭 시 DM 시작
    const handleFriendClick = (friend: { friendId: number; nickname: string; profileImage: string | null }) => {
        // 대화 목록 새로고침
        fetchConversations();
        // DM 시작 (기존 대화가 있으면 열고, 없으면 새 대화 모드)
        startConversationWith({
            id: friend.friendId,
            nickname: friend.nickname,
            profileImage: friend.profileImage
        });
        // DM 탭으로 전환
        setActiveRightTab('dm');
    };

    // 상태별 색상
    const getStatusColor = (status: string) => {
        switch (status) {
            case 'ONLINE': return 'bg-green-500';
            case 'AWAY': return 'bg-yellow-500';
            default: return 'bg-gray-400';
        }
    };

    return (
        <div className="py-4 pl-4 pr-4 h-full flex flex-col bg-transparent">
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2 text-study-blue">
                    <Users size={20} />
                    <h3 className="font-bold text-sm">친구 목록</h3>
                    {Array.isArray(receivedRequests) && receivedRequests.length > 0 && (
                        <span className="bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                            {receivedRequests.length}
                        </span>
                    )}
                </div>
                <button
                    onClick={() => setShowSearch(!showSearch)}
                    className="p-1.5 hover:bg-gray-100 rounded-full transition-colors"
                >
                    {showSearch ? <X size={16} /> : <UserPlus size={16} />}
                </button>
            </div>

            {/* 검색 영역 */}
            {showSearch && (
                <div className="mb-4">
                    <div className="relative">
                        <Search size={14} className="absolute left-2 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="친구 검색..."
                            className="w-full pl-8 pr-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:border-study-blue"
                        />
                    </div>

                    {/* 검색 결과 */}
                    {searchResults.length > 0 && (
                        <div className="mt-2 border border-gray-200 rounded-lg overflow-hidden">
                            {searchResults.map(user => (
                                <div key={user.id} className="flex items-center justify-between p-2 hover:bg-gray-50">
                                    <div className="flex items-center gap-2">
                                        <div className="w-7 h-7 rounded-full bg-study-blue/10 flex items-center justify-center text-xs font-bold text-study-blue overflow-hidden">
                                            <img src={getProfileImageUrl(null)} alt={user.nickname} className="w-full h-full object-cover" />
                                        </div>
                                        <span className="text-sm">{user.nickname}</span>
                                    </div>
                                    {user.isFriend ? (
                                        <span className="text-xs text-gray-400">친구</span>
                                    ) : user.isPending ? (
                                        <span className="text-xs text-yellow-500">요청중</span>
                                    ) : (
                                        <button
                                            onClick={() => sendRequest(user.id)}
                                            className="p-1 text-study-blue hover:bg-study-blue/10 rounded"
                                        >
                                            <UserPlus size={14} />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* 친구 요청 목록 */}
            {Array.isArray(receivedRequests) && receivedRequests.length > 0 && (
                <div className="mb-4">
                    <p className="text-xs text-gray-500 mb-2">친구 요청</p>
                    <div className="space-y-2">
                        {receivedRequests.map(request => (
                            <div key={request.id} className="flex items-center justify-between p-2 bg-yellow-50 rounded-lg">
                                <div className="flex items-center gap-2">
                                    <div className="w-7 h-7 rounded-full bg-yellow-200 flex items-center justify-center text-xs font-bold text-yellow-700 overflow-hidden">
                                        <img src={getProfileImageUrl(null)} alt={request.senderNickname} className="w-full h-full object-cover" />
                                    </div>
                                    <span className="text-sm font-medium">{request.senderNickname}</span>
                                </div>
                                <div className="flex gap-1">
                                    <button
                                        onClick={() => acceptRequest(request.id)}
                                        className="p-1 text-green-600 hover:bg-green-100 rounded"
                                    >
                                        <Check size={14} />
                                    </button>
                                    <button
                                        onClick={() => rejectRequest(request.id)}
                                        className="p-1 text-red-500 hover:bg-red-100 rounded"
                                    >
                                        <X size={14} />
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* 친구 목록 */}
            <div className="flex-1 overflow-y-auto">
                {isLoading ? (
                    <div className="flex items-center justify-center py-8">
                        <Loader2 size={24} className="animate-spin text-gray-400" />
                    </div>
                ) : Array.isArray(friends) && friends.length > 0 ? (
                    <div className="space-y-1">
                        {friends.map(friend => (
                            <div
                                key={friend.id}
                                onClick={() => handleFriendClick({ friendId: friend.friendId, nickname: friend.nickname, profileImage: friend.profileImage })}
                                className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer"
                            >
                                <div className="relative flex-shrink-0">
                                    <div className="w-9 h-9 rounded-full bg-study-blue/10 flex items-center justify-center font-bold text-xs text-study-blue overflow-hidden">
                                        <img src={getProfileImageUrl(friend.profileImage)} alt={friend.nickname} className="w-full h-full object-cover" />
                                    </div>
                                    <div className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 border-white ${getStatusColor(friend.status)}`} />
                                </div>
                                <span className="text-sm font-bold text-gray-800">{friend.nickname}</span>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-8 text-gray-400">
                        <Users size={32} className="mx-auto mb-2 opacity-50" />
                        <p className="text-sm">친구가 없습니다</p>
                        <p className="text-xs mt-1">위 버튼으로 친구를 추가하세요</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default FriendListMini;
