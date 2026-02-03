import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { MessageCircle, Send, CornerDownRight, MoreVertical, Pencil, Trash2, X } from 'lucide-react';
import { Spinner, ButtonSpinner } from '@/shared/components/Spinner';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';
import {
    StudyCommentResponse,
    getStudyComments,
    createStudyComment,
    updateStudyComment,
    deleteStudyComment,
    getStudyCommentCount,
} from '@/api/endpoints/studyApi';

interface StudyCommentSectionProps {
    studyId: number;
    studyLeaderId?: number; // 스터디장 ID (삭제 권한 체크용)
}

/**
 * 스터디 댓글 섹션 컴포넌트
 * - 댓글 목록 표시 (대댓글 포함)
 * - 댓글 작성/수정/삭제
 * - 로그인 체크
 */
const StudyCommentSection: React.FC<StudyCommentSectionProps> = ({
    studyId,
    studyLeaderId,
}) => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, isLoggedIn } = useAuthStore();
    const { showToast } = useUIStore();

    // 상태
    const [comments, setComments] = useState<StudyCommentResponse[]>([]);
    const [commentCount, setCommentCount] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [newComment, setNewComment] = useState('');
    const [replyingTo, setReplyingTo] = useState<number | null>(null); // 대댓글 대상 댓글 ID
    const [replyContent, setReplyContent] = useState('');
    const [editingId, setEditingId] = useState<number | null>(null); // 수정 중인 댓글 ID
    const [editContent, setEditContent] = useState('');
    const [openMenuId, setOpenMenuId] = useState<number | null>(null); // 열린 메뉴 ID
    const [currentPage, setCurrentPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);

    // 댓글 목록 조회
    const fetchComments = useCallback(async (page = 0, append = false) => {
        try {
            setIsLoading(true);
            const response = await getStudyComments(studyId, page, 20);

            if (append) {
                setComments(prev => [...prev, ...response.comments]);
            } else {
                setComments(response.comments);
            }
            setHasMore(response.hasNext);
            setCurrentPage(page);
        } catch (error) {
            console.error('댓글 조회 실패:', error);
            showToast('댓글을 불러오는데 실패했습니다.', 'error');
        } finally {
            setIsLoading(false);
        }
    }, [studyId, showToast]);

    // 댓글 개수 조회
    const fetchCommentCount = useCallback(async () => {
        try {
            const count = await getStudyCommentCount(studyId);
            setCommentCount(count);
        } catch (error) {
            console.error('댓글 개수 조회 실패:', error);
        }
    }, [studyId]);

    // 초기 로드
    useEffect(() => {
        fetchComments();
        fetchCommentCount();
    }, [fetchComments, fetchCommentCount]);

    // 로그인 체크 및 리다이렉트
    const checkLoginAndRedirect = () => {
        if (!isLoggedIn) {
            sessionStorage.setItem('redirectAfterLogin', location.pathname);
            showToast('로그인이 필요합니다.', 'info');
            navigate('/login');
            return false;
        }
        return true;
    };

    // 댓글 작성
    const handleSubmitComment = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!checkLoginAndRedirect()) return;
        if (!newComment.trim()) {
            showToast('댓글 내용을 입력해주세요.', 'warning');
            return;
        }

        setIsSubmitting(true);
        try {
            await createStudyComment(studyId, { content: newComment.trim() });
            setNewComment('');
            await fetchComments();
            await fetchCommentCount();
            showToast('댓글이 등록되었습니다.', 'success');
        } catch (error) {
            console.error('댓글 작성 실패:', error);
            showToast('댓글 작성에 실패했습니다.', 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    // 대댓글 작성
    const handleSubmitReply = async (parentId: number) => {
        if (!checkLoginAndRedirect()) return;
        if (!replyContent.trim()) {
            showToast('답글 내용을 입력해주세요.', 'warning');
            return;
        }

        setIsSubmitting(true);
        try {
            await createStudyComment(studyId, {
                parentId,
                content: replyContent.trim(),
            });
            setReplyContent('');
            setReplyingTo(null);
            await fetchComments();
            await fetchCommentCount();
            showToast('답글이 등록되었습니다.', 'success');
        } catch (error) {
            console.error('답글 작성 실패:', error);
            showToast('답글 작성에 실패했습니다.', 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    // 댓글 수정
    const handleUpdateComment = async (commentId: number) => {
        if (!editContent.trim()) {
            showToast('수정할 내용을 입력해주세요.', 'warning');
            return;
        }

        setIsSubmitting(true);
        try {
            await updateStudyComment(studyId, commentId, { content: editContent.trim() });
            setEditingId(null);
            setEditContent('');
            await fetchComments();
            showToast('댓글이 수정되었습니다.', 'success');
        } catch (error) {
            console.error('댓글 수정 실패:', error);
            showToast('댓글 수정에 실패했습니다.', 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    // 댓글 삭제
    const handleDeleteComment = async (commentId: number) => {
        if (!confirm('댓글을 삭제하시겠습니까?')) return;

        try {
            await deleteStudyComment(studyId, commentId);
            await fetchComments();
            await fetchCommentCount();
            showToast('댓글이 삭제되었습니다.', 'success');
        } catch (error) {
            console.error('댓글 삭제 실패:', error);
            showToast('댓글 삭제에 실패했습니다.', 'error');
        }
    };

    // 더 보기
    const handleLoadMore = () => {
        fetchComments(currentPage + 1, true);
    };

    // 시간 포맷팅 (상대 시간)
    const formatRelativeTime = (dateStr: string) => {
        const date = new Date(dateStr);
        const now = new Date();
        const diffMs = now.getTime() - date.getTime();
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return '방금 전';
        if (diffMins < 60) return `${diffMins}분 전`;
        if (diffHours < 24) return `${diffHours}시간 전`;
        if (diffDays < 7) return `${diffDays}일 전`;

        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });
    };

    // 수정/삭제 권한 확인
    const canEditComment = (comment: StudyCommentResponse) => {
        return user && Number(user.id) === comment.userId;
    };

    const canDeleteComment = (comment: StudyCommentResponse) => {
        if (!user) return false;
        // 작성자 또는 스터디장만 삭제 가능
        return Number(user.id) === comment.userId || Number(user.id) === studyLeaderId;
    };

    // 단일 댓글 렌더링
    const renderComment = (comment: StudyCommentResponse, isReply = false) => (
        <div
            key={comment.id}
            className={cn(
                'group',
                isReply && 'ml-10 pl-4 border-l-2 border-[var(--color-border)]'
            )}
        >
            <div className="flex gap-3">
                {/* 프로필 이미지 */}
                <img
                    src={getProfileImageUrl(comment.userProfileImage)}
                    alt={comment.userNickname}
                    className="w-10 h-10 rounded-full object-cover flex-shrink-0"
                />

                <div className="flex-1 min-w-0">
                    {/* 헤더 */}
                    <div className="flex items-center justify-between mb-1">
                        <div className="flex items-center gap-2">
                            <span className="font-semibold text-[var(--color-text-primary)]">
                                {comment.userNickname}
                            </span>
                            {comment.userId === studyLeaderId && (
                                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-[var(--color-primary-alpha-10)] text-[var(--color-primary)] rounded">
                                    스터디장
                                </span>
                            )}
                            <span className="text-xs text-[var(--color-text-tertiary)]">
                                {formatRelativeTime(comment.createdAt)}
                            </span>
                            {comment.updatedAt !== comment.createdAt && !comment.isDeleted && (
                                <span className="text-xs text-[var(--color-text-tertiary)]">(수정됨)</span>
                            )}
                        </div>

                        {/* 수정/삭제 메뉴 (헤더 우측) */}
                        {!comment.isDeleted && (canEditComment(comment) || canDeleteComment(comment)) && (
                            <div className="relative">
                                <button
                                    onClick={() => setOpenMenuId(openMenuId === comment.id ? null : comment.id)}
                                    className="p-1 text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)] rounded hover:bg-[var(--color-background-secondary)] transition-colors"
                                >
                                    <MoreVertical size={16} />
                                </button>

                                {openMenuId === comment.id && (
                                    <div className="absolute right-0 top-full mt-1 w-24 bg-white rounded-lg border border-[var(--color-border)] shadow-lg z-20 overflow-hidden">
                                        {canEditComment(comment) && (
                                            <button
                                                onClick={() => {
                                                    setEditingId(comment.id);
                                                    setEditContent(comment.content);
                                                    setOpenMenuId(null);
                                                }}
                                                className="w-full px-3 py-2 text-left text-sm text-[var(--color-text-secondary)] hover:bg-[var(--color-background-secondary)] flex items-center gap-2"
                                            >
                                                <Pencil size={14} />
                                                수정
                                            </button>
                                        )}
                                        {canDeleteComment(comment) && (
                                            <button
                                                onClick={() => {
                                                    handleDeleteComment(comment.id);
                                                    setOpenMenuId(null);
                                                }}
                                                className="w-full px-3 py-2 text-left text-sm text-[var(--color-error)] hover:bg-[var(--color-error-light)] flex items-center gap-2"
                                            >
                                                <Trash2 size={14} />
                                                삭제
                                            </button>
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* 내용 (수정 모드 또는 일반) */}
                    {editingId === comment.id ? (
                        <div className="space-y-2">
                            <textarea
                                value={editContent}
                                onChange={(e) => setEditContent(e.target.value)}
                                className="w-full p-3 border border-[var(--color-border)] rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] text-sm"
                                rows={3}
                                placeholder="댓글 내용을 입력하세요..."
                            />
                            <div className="flex gap-2 justify-end">
                                <button
                                    onClick={() => {
                                        setEditingId(null);
                                        setEditContent('');
                                    }}
                                    className="px-3 py-1.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                                >
                                    취소
                                </button>
                                <button
                                    onClick={() => handleUpdateComment(comment.id)}
                                    disabled={isSubmitting}
                                    className="px-3 py-1.5 text-sm bg-[var(--color-primary)] text-white rounded-lg hover:bg-[var(--color-primary-dark)] disabled:opacity-50"
                                >
                                    {isSubmitting ? '저장 중...' : '저장'}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <p className={cn(
                            'text-sm leading-relaxed whitespace-pre-wrap',
                            comment.isDeleted
                                ? 'text-[var(--color-text-tertiary)] italic'
                                : 'text-[var(--color-text-primary)]'
                        )}>
                            {comment.content}
                        </p>
                    )}

                    {/* 이미지 */}
                    {comment.imageUrl && !comment.isDeleted && (
                        <img
                            src={comment.imageUrl}
                            alt="첨부 이미지"
                            className="mt-2 max-w-xs rounded-lg border border-[var(--color-border)]"
                        />
                    )}

                    {/* 답글 버튼 (최상위 댓글만) */}
                    {!comment.isDeleted && editingId !== comment.id && !isReply && (
                        <div className="mt-2">
                            <button
                                onClick={() => {
                                    if (!checkLoginAndRedirect()) return;
                                    setReplyingTo(replyingTo === comment.id ? null : comment.id);
                                    setReplyContent('');
                                }}
                                className="text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] flex items-center gap-1"
                            >
                                <CornerDownRight size={12} />
                                답글 {comment.replyCount > 0 && `(${comment.replyCount})`}
                            </button>
                        </div>
                    )}

                    {/* 답글 입력 폼 */}
                    {replyingTo === comment.id && (
                        <div className="mt-3 flex gap-2">
                            <img
                                src={getProfileImageUrl(user?.avatar)}
                                alt="내 프로필"
                                className="w-8 h-8 rounded-full object-cover flex-shrink-0"
                            />
                            <div className="flex-1">
                                <textarea
                                    value={replyContent}
                                    onChange={(e) => setReplyContent(e.target.value)}
                                    placeholder="답글을 입력하세요..."
                                    className="w-full p-2 border border-[var(--color-border)] rounded-lg resize-none text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
                                    rows={2}
                                />
                                <div className="flex gap-2 justify-end mt-2">
                                    <button
                                        onClick={() => {
                                            setReplyingTo(null);
                                            setReplyContent('');
                                        }}
                                        className="px-3 py-1.5 text-xs text-[var(--color-text-secondary)]"
                                    >
                                        취소
                                    </button>
                                    <button
                                        onClick={() => handleSubmitReply(comment.id)}
                                        disabled={isSubmitting || !replyContent.trim()}
                                        className="px-3 py-1.5 text-xs bg-[var(--color-primary)] text-white rounded-lg hover:bg-[var(--color-primary-dark)] disabled:opacity-50"
                                    >
                                        {isSubmitting ? '등록 중...' : '답글 등록'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* 대댓글 목록 */}
            {comment.replies && comment.replies.length > 0 && (
                <div className="mt-4 space-y-4">
                    {comment.replies.map((reply) => renderComment(reply, true))}
                </div>
            )}
        </div>
    );

    return (
        <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
            {/* 헤더 */}
            <div className="flex items-center gap-2 mb-6">
                <MessageCircle size={20} className="text-[var(--color-primary)]" />
                <h3 className="text-lg font-bold text-[var(--color-text-primary)]">
                    댓글
                </h3>
                <span className="text-sm text-[var(--color-text-tertiary)]">
                    ({commentCount})
                </span>
            </div>

            {/* 댓글 작성 폼 */}
            <form onSubmit={handleSubmitComment} className="mb-6">
                <div className="flex gap-3">
                    <img
                        src={getProfileImageUrl(user?.avatar)}
                        alt="내 프로필"
                        className="w-10 h-10 rounded-full object-cover flex-shrink-0"
                    />
                    <div className="flex-1">
                        <textarea
                            value={newComment}
                            onChange={(e) => setNewComment(e.target.value)}
                            placeholder={isLoggedIn ? "댓글을 입력하세요..." : "로그인 후 댓글을 작성할 수 있습니다."}
                            className="w-full p-3 border border-[var(--color-border)] rounded-xl resize-none text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] min-h-[80px]"
                            rows={3}
                            onClick={() => {
                                if (!isLoggedIn) {
                                    checkLoginAndRedirect();
                                }
                            }}
                        />
                        <div className="flex justify-end mt-2">
                            <button
                                type="submit"
                                disabled={isSubmitting || !newComment.trim()}
                                className={cn(
                                    'px-4 py-2 rounded-lg text-sm font-semibold flex items-center gap-2',
                                    'bg-[var(--color-primary)] text-white',
                                    'hover:bg-[var(--color-primary-dark)]',
                                    'disabled:opacity-50 disabled:cursor-not-allowed',
                                    'transition-colors'
                                )}
                            >
                                {isSubmitting ? (
                                    <>
                                        <ButtonSpinner />
                                        등록 중...
                                    </>
                                ) : (
                                    <>
                                        <Send size={14} />
                                        댓글 등록
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </form>

            {/* 댓글 목록 */}
            {isLoading && comments.length === 0 ? (
                <Spinner variant="center" label="댓글을 불러오는 중..." />
            ) : comments.length === 0 ? (
                <div className="text-center py-12 text-[var(--color-text-tertiary)]">
                    <MessageCircle size={40} className="mx-auto mb-3 opacity-50" />
                    <p className="font-medium">아직 댓글이 없습니다.</p>
                    <p className="text-sm mt-1">첫 댓글을 남겨보세요!</p>
                </div>
            ) : (
                <div className="space-y-6">
                    {comments.map((comment) => renderComment(comment))}
                </div>
            )}

            {/* 더 보기 버튼 */}
            {hasMore && (
                <div className="mt-6 text-center">
                    <button
                        onClick={handleLoadMore}
                        disabled={isLoading}
                        className="px-6 py-2 text-sm text-[var(--color-primary)] hover:bg-[var(--color-primary-alpha-10)] rounded-lg transition-colors"
                    >
                        {isLoading ? '로딩 중...' : '더 보기'}
                    </button>
                </div>
            )}

            {/* 메뉴 외부 클릭 시 닫기 */}
            {openMenuId && (
                <div
                    className="fixed inset-0 z-0"
                    onClick={() => setOpenMenuId(null)}
                />
            )}
        </div>
    );
};

export default StudyCommentSection;
