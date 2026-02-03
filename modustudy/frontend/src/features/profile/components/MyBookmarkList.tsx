import React, { useState, useEffect } from 'react';
import { studyApi, StudyBookmarkResponse } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';
import { Bookmark, Users, Calendar, Loader2, BookmarkX } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const MyBookmarkList: React.FC = () => {
    const [bookmarks, setBookmarks] = useState<StudyBookmarkResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const { showToast } = useUIStore();
    const navigate = useNavigate();

    useEffect(() => {
        fetchBookmarks();
    }, []);

    const fetchBookmarks = async () => {
        setLoading(true);
        try {
            const response = await studyApi.getMyBookmarks(0, 50);
            const content = response?.content || [];
            setBookmarks(content);
        } catch (error) {
            console.error('[MyBookmarkList] 북마크 목록 조회 실패:', error);
            showToast('북마크 목록을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleStudyClick = (studyId: number) => {
        navigate(`/study/${studyId}`);
    };

    const handleRemoveBookmark = async (e: React.MouseEvent, studyId: number) => {
        e.stopPropagation();
        try {
            await studyApi.toggleBookmark(studyId);
            setBookmarks(prev => prev.filter(b => b.studyId !== studyId));
            showToast('북마크가 해제되었습니다.', 'success');
        } catch (error) {
            console.error('[MyBookmarkList] 북마크 해제 실패:', error);
            showToast('북마크 해제에 실패했습니다.', 'error');
        }
    };

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'RECRUITING':
            case 'RECRUIT_CLOSED':
                return 'bg-blue-50 text-blue-600 border-blue-200';
            case 'IN_PROGRESS':
                return 'bg-green-50 text-green-600 border-green-200';
            case 'COMPLETED':
                return 'bg-gray-50 text-gray-500 border-gray-200';
            default:
                return 'bg-amber-50 text-amber-600 border-amber-200';
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return '모집중';
            case 'RECRUIT_CLOSED':
                return '모집완료';
            case 'IN_PROGRESS':
                return '진행중';
            case 'COMPLETED':
                return '완료';
            case 'SCHEDULED':
                return '예정';
            case 'PENDING':
                return '확정대기';
            default:
                return status;
        }
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">북마크 목록을 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary flex items-center gap-2">
                        <Bookmark size={20} className="text-amber-500" />
                        스터디 북마크
                    </h2>
                    <p className="text-sm text-text-secondary mt-1">관심있는 스터디를 저장해두세요.</p>
                </div>
                <span className="text-sm font-medium text-text-tertiary">{bookmarks.length}개</span>
            </div>

            {/* 북마크 목록 */}
            {bookmarks.length === 0 ? (
                <div className="text-center py-12 bg-background-secondary rounded-2xl">
                    <BookmarkX size={48} className="mx-auto text-text-muted mb-4" />
                    <p className="text-text-secondary">북마크한 스터디가 없습니다</p>
                    <p className="text-sm text-text-tertiary mt-1">관심있는 스터디를 북마크해보세요!</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {bookmarks.map((bookmark) => (
                        <div
                            key={bookmark.id}
                            className="bg-background-secondary rounded-2xl p-5 border border-border-light hover:shadow-md transition-all cursor-pointer group"
                            onClick={() => handleStudyClick(bookmark.studyId)}
                        >
                            <div className="flex items-start justify-between gap-4">
                                {/* 스터디 정보 */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-2">
                                        <h3 className="font-bold text-text-primary line-clamp-1 group-hover:text-primary transition-colors">
                                            {bookmark.studyName}
                                        </h3>
                                        <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${getStatusStyle(bookmark.studyStatus)}`}>
                                            {getStatusLabel(bookmark.studyStatus)}
                                        </span>
                                    </div>
                                    <p className="text-sm text-text-secondary mb-3 line-clamp-2">
                                        {bookmark.studyDescription || '설명이 없습니다.'}
                                    </p>
                                    <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                        {bookmark.topic && (
                                            <span className="px-2 py-1 bg-primary/10 text-primary rounded-md">
                                                {bookmark.topic.name}
                                            </span>
                                        )}
                                        {bookmark.maxMembers && (
                                            <span className="flex items-center gap-1">
                                                <Users size={12} />
                                                최대 {bookmark.maxMembers}명
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* 북마크 해제 버튼 */}
                                <button
                                    onClick={(e) => handleRemoveBookmark(e, bookmark.studyId)}
                                    className="p-2 rounded-lg hover:bg-amber-50 text-amber-500 hover:text-amber-600 transition-colors"
                                    title="북마크 해제"
                                >
                                    <Bookmark size={20} fill="currentColor" />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
