import { useState } from 'react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { RecruitmentList } from './components/RecruitmentList';
import { RecruitmentForm } from './components/RecruitmentForm';
import { RecruitmentReportModal } from './components/RecruitmentReportModal';
import { useRecruitmentStore } from './useRecruitmentStore';
import {
    MessageSquare, Share2, MoreVertical, CheckCircle2,
    Users, Eye, Calendar, Tag, AlertTriangle
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button, ArrowButton, Dropdown } from '@/shared/components';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

type ViewMode = 'list' | 'create' | 'edit' | 'detail';

// 카테고리 한글 매핑
const categoryLabel: Record<string, string> = {
    study: '스터디',
    project: '프로젝트',
    mentoring: '멘토링',
};

// 카테고리 색상 매핑
const categoryColor: Record<string, string> = {
    study: 'bg-[var(--color-primary-alpha-10)] text-[var(--color-primary)]',
    project: 'bg-[rgba(34,197,94,0.1)] text-[#22c55e]',
    mentoring: 'bg-[rgba(168,85,247,0.1)] text-[#a855f7]',
};

export const RecruitmentPage = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [reportTargetId, setReportTargetId] = useState<string | null>(null);
    const { posts, deletePost, toggleComplete, report } = useRecruitmentStore();

    const selectedPost = posts.find(p => p.id === selectedPostId);

    // Handlers
    const handleDetail = (id: string) => {
        setSelectedPostId(id);
        setViewMode('detail');
    };

    const handleEdit = (id: string) => {
        setSelectedPostId(id);
        setViewMode('edit');
    };

    const handleDelete = (id: string) => {
        if (confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
            deletePost(id);
            setViewMode('list');
        }
    };

    const handleReport = (id: string) => {
        setReportTargetId(id);
        setIsReportModalOpen(true);
    };

    const handleShareLink = () => {
        navigator.clipboard.writeText(window.location.href);
    };

    const submitReport = (reason: string) => {
        if (reportTargetId) {
            report({
                targetId: reportTargetId,
                targetType: 'post',
                reason,
                reporterId: 'me'
            });
        }
    };

    return (
        <UserLayoutV2>
            <div className="max-w-7xl mx-auto py-8 px-4 md:px-6">

                {/* 1. List Mode */}
                {viewMode === 'list' && (
                    <RecruitmentList
                        onDetail={handleDetail}
                        onAdd={() => setViewMode('create')}
                        onReport={handleReport}
                    />
                )}

                {/* 2. Create/Edit Mode */}
                {(viewMode === 'create' || viewMode === 'edit') && (
                    <RecruitmentForm
                        initialData={viewMode === 'edit' ? selectedPost : null}
                        onCancel={() => setViewMode('list')}
                        onSuccess={() => setViewMode('list')}
                    />
                )}

                {/* 3. Detail Mode - StudyDetailPageV3 스타일 */}
                {viewMode === 'detail' && selectedPost && (
                    <div className="max-w-5xl mx-auto animate-fadeIn">
                        {/* 상단 네비게이션 */}
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

                        {/* 통합 카드 */}
                        <div className="bg-white rounded-2xl border border-[var(--color-border)] shadow-sm overflow-hidden">
                            {/* 헤더 섹션 */}
                            <div className="p-6 md:p-8">
                                {/* 상단: 뱃지 + 액션 버튼 */}
                                <div className="flex justify-between items-start gap-4 mb-4">
                                    {/* 뱃지 영역 */}
                                    <div className="flex flex-wrap items-center gap-2">
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-bold",
                                            selectedPost.isCompleted
                                                ? "bg-[var(--color-text-tertiary)] text-white"
                                                : "bg-[var(--color-success)] text-white"
                                        )}>
                                            {selectedPost.isCompleted ? '모집 완료' : '모집중'}
                                        </span>
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-semibold",
                                            categoryColor[selectedPost.category]
                                        )}>
                                            # {categoryLabel[selectedPost.category]}
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

                                        {/* 케밥 메뉴 */}
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

                                {/* 타이틀 */}
                                <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-text-primary)] mb-4 leading-tight">
                                    {selectedPost.title}
                                </h1>

                                {/* 작성자 정보 */}
                                <div className="flex items-center gap-3">
                                    <img
                                        src={getProfileImageUrl(selectedPost.authorAvatar)}
                                        alt=""
                                        className="w-10 h-10 rounded-xl border border-[var(--color-border)] object-cover"
                                    />
                                    <div>
                                        <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                            {selectedPost.authorName}
                                        </p>
                                        <p className="text-xs text-[var(--color-text-tertiary)]">
                                            {new Date(selectedPost.createdAt).toLocaleDateString('ko-KR', {
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric'
                                            })}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* 구분선 */}
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
                                                <span>{selectedPost.memberCount} / {selectedPost.maxMembers}명</span>
                                                {selectedPost.memberCount >= selectedPost.maxMembers && (
                                                    <span className="px-2 py-0.5 bg-[var(--color-error-light)] text-[var(--color-error)] text-xs font-bold rounded">
                                                        마감
                                                    </span>
                                                )}
                                            </p>
                                        </div>
                                    </div>

                                    {/* 조회수 */}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Eye size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                조회수
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                                {selectedPost.views}회
                                            </p>
                                        </div>
                                    </div>

                                    {/* 카테고리 */}
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
                                            <Tag size={18} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                                                카테고리
                                            </p>
                                            <p className="text-sm font-bold text-[var(--color-text-primary)]">
                                                {categoryLabel[selectedPost.category]}
                                            </p>
                                        </div>
                                    </div>

                                    {/* 작성일 */}
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
                                {selectedPost.tags.length > 0 && (
                                    <div className="flex flex-wrap gap-2 mt-8">
                                        {selectedPost.tags.map(tag => (
                                            <span
                                                key={tag}
                                                className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-background-secondary)] text-[var(--color-text-secondary)]"
                                            >
                                                # {tag}
                                            </span>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* 구분선 */}
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

                            {/* 구분선 */}
                            <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                            {/* 게시글 관리 (작성자용) */}
                            <div className="p-6 md:p-8">
                                <div className="p-5 bg-[var(--color-background)] rounded-xl border border-[var(--color-border-lighter)] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                                    <div className="flex items-center gap-3">
                                        <CheckCircle2 className="text-[var(--color-primary)]" size={22} />
                                        <span className="font-bold text-[var(--color-text-primary)] text-sm">작성한 게시글 관리</span>
                                    </div>
                                    <div className="flex gap-2 flex-wrap">
                                        <Button
                                            variant="google-outline"
                                            size="sm"
                                            onClick={() => toggleComplete(selectedPost.id)}
                                            className="text-sm"
                                        >
                                            {selectedPost.isCompleted ? '모집 재개' : '모집 완료'}
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

                            {/* 구분선 */}
                            <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                            {/* 댓글 섹션 */}
                            <div className="p-6 md:p-8">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-6">
                                    <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                        <MessageSquare size={18} className="text-[var(--color-primary)]" />
                                    </div>
                                    문의 및 댓글
                                </h2>

                                <div className="bg-[var(--color-background)] border border-[var(--color-border-lighter)] rounded-xl p-4">
                                    <textarea
                                        placeholder="관심 있으시다면 간단한 소개와 함께 댓글을 남겨주세요."
                                        className="w-full outline-none resize-none text-sm leading-relaxed bg-transparent"
                                        rows={3}
                                    />
                                    <div className="flex justify-end pt-3 border-t border-[var(--color-border-lighter)]">
                                        <Button variant="primary" size="sm" className="rounded-lg">
                                            댓글 등록
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

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
