import { useEffect, useState } from 'react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { RecruitmentList } from './components/RecruitmentList';
import { RecruitmentForm } from './components/RecruitmentForm';
import { RecruitmentReportModal } from './components/RecruitmentReportModal';
import {
    MessageSquare, Share2, MoreVertical, CheckCircle2,
    Users, Eye, Calendar, Tag, AlertTriangle
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button, ArrowButton, Dropdown, Modal } from '@/shared/components';
import { getProfileImageUrl } from '@/shared/utils/profileImage';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
    RecruitmentPostDetail,
    RecruitmentPostSummary,
    addRecruitmentComment,
    deleteRecruitmentPost,
    getRecruitmentPostDetail,
    getRecruitmentPosts,
    reportRecruitmentPost,
    updateRecruitmentPost,
} from '@/api/endpoints/boardApi';

type ViewMode = 'list' | 'create' | 'edit' | 'detail';


export const RecruitmentPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const currentUser = useAuthStore((state) => state.user);
    const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
    const { showToast } = useUIStore();
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [posts, setPosts] = useState<RecruitmentPostSummary[]>([]);
    const [, setSelectedPostId] = useState<number | null>(null);
    const [selectedPost, setSelectedPost] = useState<RecruitmentPostDetail | null>(null);
    const [, setIsLoading] = useState(false);
    const [commentInput, setCommentInput] = useState('');
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [reportTargetId, setReportTargetId] = useState<number | null>(null);
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [isStatusUpdating, setIsStatusUpdating] = useState(false);

    const loadPosts = async () => {
        setIsLoading(true);
        try {
            const page = await getRecruitmentPosts({ page: 0, size: 50 });
            setPosts(page.content || []);
        } finally {
            setIsLoading(false);
        }
    };

    const loadPostDetail = async (id: number) => {
        const detail = await getRecruitmentPostDetail(id);
        setSelectedPost(detail);
    };

    const getMeetingTypeLabel = (type?: string | null) => {
        switch (type) {
            case 'ONLINE':
                return '온라인';
            case 'OFFLINE':
                return '오프라인';
            case 'HYBRID':
                return '온·오프라인';
            default:
                return type || '-';
        }
    };

    useEffect(() => {
        loadPosts();
    }, []);

    useEffect(() => {
        const postIdParam = searchParams.get('postId');
        if (!postIdParam) return;
        const parsedId = Number(postIdParam);
        if (Number.isNaN(parsedId)) return;
        void handleDetail(parsedId);
    }, [searchParams]);

    // Handlers
    const handleDetail = async (id: number) => {
        setSelectedPostId(id);
        await loadPostDetail(id);
        setViewMode('detail');
    };

    const handleEdit = (id: number) => {
        setSelectedPostId(id);
        setViewMode('edit');
    };

    const handleDelete = async (id: number) => {
        if (confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
            await deleteRecruitmentPost(id);
            setViewMode('list');
            loadPosts();
        }
    };

    const handleReport = (id: number) => {
        setReportTargetId(id);
        setIsReportModalOpen(true);
    };

    const handleShareLink = async () => {
        if (!selectedPost) return;
        const shareUrl = `${window.location.origin}/recruitment?postId=${selectedPost.id}`;
        await navigator.clipboard.writeText(shareUrl);
        showToast('링크가 복사되었습니다.', 'success');
    };

    const submitReport = async (reason: string) => {
        if (!reportTargetId) return;
        try {
            await reportRecruitmentPost(reportTargetId, { reason });
            showToast('신고가 접수되었습니다.', 'success');
        } catch (error: any) {
            const message = error?.response?.data?.message || '신고 접수에 실패했습니다.';
            showToast(message, 'error');
        }
    };

    const handleAdd = () => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        setViewMode('create');
    };

    const handleCommentSubmit = async () => {
        if (!selectedPost || !commentInput.trim()) return;
        await addRecruitmentComment(selectedPost.id, { content: commentInput.trim() });
        setCommentInput('');
        await loadPostDetail(selectedPost.id);
    };

    const handleStatusChange = async (nextStatus: 'RECRUITING' | 'COMPLETED') => {
        if (!selectedPost) return;
        if (selectedPost.recruitmentStatus === nextStatus) return;
        try {
            setIsStatusUpdating(true);
            const updated = await updateRecruitmentPost(selectedPost.id, {
                title: selectedPost.title,
                content: selectedPost.content,
                recruitmentField: selectedPost.recruitmentField,
                meetingType: selectedPost.meetingType,
                targetMembers: selectedPost.targetMembers ?? 1,
                recruitmentStatus: nextStatus,
            });
            setSelectedPost(updated);
            loadPosts();
        } finally {
            setIsStatusUpdating(false);
        }
    };

    return (
        <UserLayoutV2>
            <div className="max-w-7xl mx-auto px-4 md:px-6 py-6">

                {/* 1. List Mode */}
                {viewMode === 'list' && (
                    <RecruitmentList
                        posts={posts}
                        onDetail={handleDetail}
                        onAdd={handleAdd}
                        onReport={handleReport}
                    />
                )}

                {/* 2. Create/Edit Mode */}
                {(viewMode === 'create' || viewMode === 'edit') && (
                    <RecruitmentForm
                        initialData={viewMode === 'edit' ? selectedPost : null}
                        onCancel={() => setViewMode('list')}
                        onSuccess={() => {
                            setViewMode('list');
                            loadPosts();
                        }}
                    />
                )}

                {/* 3. Detail Mode - Recruitment Detail*/}
                {viewMode === 'detail' && selectedPost && (
                    <div className="max-w-5xl mx-auto animate-fadeIn">
                        {/* 상단 네비게이션*/}
                        <div className="flex justify-between items-center mb-6">
                            <div className="flex items-center gap-3">
                                <ArrowButton
                                    direction="left"
                                    onClick={() => setViewMode('list')}
                                    size="md"
                                />
                                <span className="text-sm font-semibold text-[var(--color-text-secondary)]">
                                    모집글 상세
                                </span>
                            </div>
                        </div>

                        {/* 컨테이너 카드 */}
                        <div className="bg-white rounded-2xl border border-[var(--color-border)] shadow-sm overflow-hidden">
                            {/* 헤더 섹션 */}
                            <div className="p-6 md:p-8">
                                {/* 상단: 상태 + 액션 버튼 */}
                                <div className="flex justify-between items-start gap-4 mb-4">
                                    {/* 상태 영역 */}
                                    <div className="flex flex-wrap items-center gap-2">
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-bold",
                                            selectedPost.recruitmentStatus === 'COMPLETED'
                                                ? "bg-[var(--color-text-tertiary)] text-white"
                                                : "bg-[var(--color-success)] text-white"
                                        )}>
                                            {selectedPost.recruitmentStatus === 'COMPLETED' ? '모집 완료' : '모집 중'}
                                        </span>
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-semibold",
                                            "bg-[var(--color-primary-alpha-10)] text-[var(--color-primary)]"
                                        )}>
                                            # {selectedPost.recruitmentField || '-'}
                                        </span>
                                    </div>

                                    {/* 액션 버튼 */}
                                    <div className="flex items-center gap-1 flex-shrink-0">
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={handleShareLink}
                                            className="text-[var(--color-text-tertiary)] hover:text-[var(--color-primary)] rounded-full"
                                        >
                                            <Share2 size={20} />
                                        </Button>

                                        {/* 더보기 메뉴 */}
                                        <Dropdown
                                            trigger={({ toggle }) => (
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={toggle}
                                                    className="text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)] rounded-full"
                                                >
                                                    <MoreVertical size={20} />
                                                </Button>
                                            )}
                                            align="right"
                                            menuClassName="w-48"
                                            items={[
                                                {
                                                    label: '신고하기',
                                                    value: 'report',
                                                    icon: <AlertTriangle size={16} />,
                                                    danger: true,
                                                    onClick: () => handleReport(selectedPost.id),
                                                },
                                            ]}
                                        />
                                    </div>
                                </div>

                                <div className="flex flex-col gap-4">
                                    <div className="flex flex-wrap items-start justify-between gap-3">
                                        {/* 타이틀 */}
                                        <h1 className="text-2xl md:text-3xl font-extrabold text-[var(--color-text-primary)] leading-tight">
                                            {selectedPost.title}
                                        </h1>
                                        <span className="inline-flex items-center px-3 py-1 rounded-full bg-[var(--color-background-secondary)] text-xs font-semibold text-[var(--color-text-secondary)]">
                                            {new Date(selectedPost.createdAt).toLocaleDateString('ko-KR', {
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric'
                                            })}
                                        </span>
                                    </div>

                                    {/* 작성자 정보 */}
                                    <div className="flex items-center gap-3 text-sm text-[var(--color-text-tertiary)]">
                                        <img
                                            src={getProfileImageUrl(selectedPost.authorProfileImage)}
                                            alt=""
                                            className="w-12 h-12 rounded-full border border-white shadow-md object-cover"
                                        />
                                        <span className="text-base font-bold text-[var(--color-text-primary)]">
                                            {selectedPost.authorName}
                                        </span>
                                    </div>
                                </div>
                                {selectedPost.recruitmentStatus === 'COMPLETED' && (
                                    <div className="mt-4 mb-2">
                                        <span className="inline-flex items-center px-4 py-2 rounded-2xl text-sm md:text-base font-extrabold bg-[var(--color-error)] text-white shadow-lg shadow-[var(--color-error)]/30">
                                            모집 완료
                                        </span>
                                    </div>
                                )}
                            </div>

                            {/* 구분선*/}
                            <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                            {/* 모집 정보 섹션 */}
                            <div className="p-6 md:p-8">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-8">
                                    <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                        <Tag size={18} className="text-[var(--color-primary)]" />
                                    </div>
                                    모집 정보
                                </h2>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                                    {/* 모집 인원 */}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Users size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                모집 인원
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-2">
                                                <span>{selectedPost.targetMembers ?? '-'}명</span>
                                                {selectedPost.recruitmentStatus === 'COMPLETED' && (
                                                    <span className="px-2 py-0.5 bg-[var(--color-error-light)] text-[var(--color-error)] text-xs font-bold rounded">
                                                        마감
                                                    </span>
                                                )}
                                            </p>
                                        </div>
                                    </div>

                                    {/* 조회수*/}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Eye size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                조회수
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                                {selectedPost.viewCount}회
                                            </p>
                                        </div>
                                    </div>

                                    {/* 모집 분야 */}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Tag size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                모집 분야
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                                {selectedPost.recruitmentField || '-'}
                                            </p>
                                        </div>
                                    </div>

                                    {/* 작성일*/}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Calendar size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                작성일
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                                {new Date(selectedPost.createdAt).toLocaleDateString('ko-KR')}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {/* 태그 */}
                                <div className="flex flex-wrap gap-2 mt-8">
                                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-background-secondary)] text-[var(--color-text-secondary)]">
                                        {selectedPost.recruitmentField || '-'}
                                    </span>
                                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-background-secondary)] text-[var(--color-text-secondary)]">
                                        {getMeetingTypeLabel(selectedPost.meetingType)}
                                    </span>
                                </div>
                            </div>

                            {/* 구분선*/}
                            <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                            {/* 본문 섹션 */}
                            <div className="p-6 md:p-8">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-8">
                                    <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                        <MessageSquare size={18} className="text-[var(--color-primary)]" />
                                    </div>
                                    상세 내용
                                </h2>

                                <div className="text-[var(--color-text-secondary)] leading-relaxed whitespace-pre-wrap">
                                    {selectedPost.content}
                                </div>
                            </div>

                            {/* 구분선*/}
                            <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                            {/* 게시글 관리(작성자용) */}
                            {Number(currentUser?.id) === selectedPost.authorId && (
                                <>
                                    <div className="p-6 md:p-8">
                                        <div className="p-5 bg-[var(--color-background)] rounded-xl border border-[var(--color-border-lighter)] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                                            <div className="flex items-center gap-3">
                                                <CheckCircle2 className="text-[var(--color-primary)]" size={22} />
                                                <span className="font-bold text-[var(--color-text-primary)] text-sm">작성자 게시글 관리</span>
                                            </div>
                                            <div className="flex gap-2 flex-wrap">
                                                <Button
                                                    variant={selectedPost.recruitmentStatus === 'RECRUITING' ? 'primary' : 'google-outline'}
                                                    size="sm"
                                                    onClick={() => handleStatusChange('RECRUITING')}
                                                    disabled={isStatusUpdating}
                                                    className="text-sm"
                                                >
                                                    모집 중
                                                </Button>
                                                <Button
                                                    variant={selectedPost.recruitmentStatus === 'COMPLETED' ? 'primary' : 'google-outline'}
                                                    size="sm"
                                                    onClick={() => handleStatusChange('COMPLETED')}
                                                    disabled={isStatusUpdating}
                                                    className="text-sm"
                                                >
                                                    모집 완료
                                                </Button>
                                                <Button
                                                    variant="google-outline"
                                                    size="sm"
                                                    onClick={() => handleEdit(selectedPost.id)}
                                                    className="text-sm text-[var(--color-primary)] hover:bg-[var(--color-primary-alpha-5)]"
                                                >
                                                    수정
                                                </Button>
                                                <Button
                                                    variant="google-outline"
                                                    size="sm"
                                                    onClick={() => handleDelete(selectedPost.id)}
                                                    className="text-sm text-[var(--color-error)] hover:bg-[var(--color-error-light)]"
                                                >
                                                    삭제
                                                </Button>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 구분선*/}
                                    <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />
                                </>
                            )}

                            {/* 댓글 섹션 */}
                            <div className="p-6 md:p-8">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-6">
                                    <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                        <MessageSquare size={18} className="text-[var(--color-primary)]" />
                                    </div>
                                    문의 및 댓글
                                </h2>

                                {selectedPost.recruitmentStatus === 'RECRUITING' ? (
                                    <div className="bg-[var(--color-background)] border border-[var(--color-border-lighter)] rounded-xl p-4">
                                        <textarea
                                            placeholder="문의가 있으시다면 간단하게 댓글을 남겨주세요."
                                            value={commentInput}
                                            onChange={(e) => setCommentInput(e.target.value)}
                                            className="w-full outline-none resize-none text-sm leading-relaxed bg-transparent"
                                            rows={3}
                                        />
                                        <div className="flex justify-end pt-3 border-t border-[var(--color-border-lighter)]">
                                            <Button variant="primary" size="sm" className="rounded-lg" onClick={handleCommentSubmit}>
                                                댓글 등록
                                            </Button>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="bg-[var(--color-background-secondary)] border border-[var(--color-border-lighter)] rounded-xl p-4 text-sm text-[var(--color-text-secondary)]">
                                        모집이 완료되어 댓글 작성이 비활성화되었습니다.
                                    </div>
                                )}
                                {selectedPost.comments.length > 0 && (
                                    <div className="mt-6 space-y-4">
                                        {[...selectedPost.comments]
                                            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                                            .map((comment) => (
                                            <div key={comment.id} className="flex gap-3">
                                                <img
                                                    src={getProfileImageUrl(comment.authorProfileImage)}
                                                    alt=""
                                                    className="w-8 h-8 rounded-lg border border-[var(--color-border)] object-cover"
                                                />
                                                <div className="flex-1">
                                                    <div className="flex items-center gap-2">
                                                        <span className="text-sm font-semibold text-[var(--color-text-primary)]">
                                                            {comment.authorName}
                                                        </span>
                                                        <span className="text-xs text-[var(--color-text-tertiary)]">
                                                            {new Date(comment.createdAt).toLocaleDateString('ko-KR')}
                                                        </span>
                                                    </div>
                                                    <p className="text-sm text-[var(--color-text-secondary)] mt-1 whitespace-pre-wrap">
                                                        {comment.content}
                                                    </p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                <Modal
                    isOpen={showLoginModal}
                    onClose={() => setShowLoginModal(false)}
                    title="로그인 필요"
                    maxWidth="sm"
                >
                    <p className="text-sm text-[var(--color-text-secondary)]">
                        로그인이 필요한 기능입니다.
                    </p>
                    <div className="flex gap-2 mt-6">
                        <Button
                            variant="google-outline"
                            size="sm"
                            onClick={() => setShowLoginModal(false)}
                            className="flex-1"
                        >
                            취소
                        </Button>
                        <Button
                            variant="primary"
                            size="sm"
                            onClick={() => navigate('/login')}
                            className="flex-1"
                        >
                            로그인
                        </Button>
                    </div>
                </Modal>

                {/* Report Modal */}

                <RecruitmentReportModal
                    isOpen={isReportModalOpen}
                    onClose={() => setIsReportModalOpen(false)}
                    onSubmit={submitReport}
                    targetTitle={posts.find(p => p.id === reportTargetId)?.title}
                />
            </div>
        </UserLayoutV2>
    );
};












