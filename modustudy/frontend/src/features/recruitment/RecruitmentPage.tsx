import { useState } from 'react';
import { MainLayout } from '@/layouts/MainLayout';
import { RecruitmentList } from './components/RecruitmentList';
import { RecruitmentForm } from './components/RecruitmentForm';
import { RecruitmentReportModal } from './components/RecruitmentReportModal';
import { useRecruitmentStore } from './useRecruitmentStore';
import { MessageSquare, Share2, MoreVertical, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button, ArrowButton } from '@/shared/components';

type ViewMode = 'list' | 'create' | 'edit' | 'detail';

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
        <MainLayout>
            <div className="max-w-7xl mx-auto py-12 px-6">

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

                {/* 3. Detail Mode */}
                {viewMode === 'detail' && selectedPost && (
                    <div className="max-w-4xl mx-auto animate-fadeIn">
                        <header className="flex items-center justify-between mb-8">
                            <div className="flex items-center gap-4">
                                <ArrowButton
                                    direction="left"
                                    onClick={() => setViewMode('list')}
                                    size="md"
                                />
                                <span className="text-sm font-bold text-text-secondary">목록으로 돌아가기</span>
                            </div>

                            <div className="flex gap-2">
                                <Button
                                    variant="google-outline"
                                    size="md"
                                    isCircle
                                    onClick={() => {/* share logic */ }}
                                >
                                    <Share2 size={18} />
                                </Button>
                                <Button
                                    variant="google-outline"
                                    size="md"
                                    isCircle
                                    onClick={() => handleReport(selectedPost.id)}
                                    className="text-text-secondary hover:text-error hover:border-error/20"
                                >
                                    <MoreVertical size={18} />
                                </Button>
                            </div>
                        </header>

                        <article className="bg-white border border-border-light rounded-[32px] overflow-hidden shadow-sm">
                            {/* Hero Header */}
                            <div className="p-10 bg-background-secondary/30 border-b border-border-light">
                                <span className={cn(
                                    "inline-block px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border mb-6",
                                    selectedPost.isCompleted ? "bg-text-tertiary/10 text-text-tertiary border-text-tertiary/20" : "bg-success/10 text-success border-success/20"
                                )}>
                                    {selectedPost.isCompleted ? '모집 완료된 팀' : '현재 팀원 모집 중'}
                                </span>

                                <h1 className="text-4xl font-black text-text-primary tracking-tight leading-tight mb-6">
                                    {selectedPost.title}
                                </h1>

                                <div className="flex flex-wrap items-center justify-between gap-6">
                                    <div className="flex items-center gap-4">
                                        <img src={selectedPost.authorAvatar} alt="" className="w-12 h-12 rounded-2xl border-2 border-white shadow-md" />
                                        <div>
                                            <p className="font-black text-text-primary uppercase tracking-tighter text-sm">{selectedPost.authorName}</p>
                                            <p className="text-xs text-text-tertiary font-medium">작성일: {new Date(selectedPost.createdAt).toLocaleDateString()}</p>
                                        </div>
                                    </div>

                                    <div className="flex gap-4">
                                        <div className="px-4 py-2 bg-white rounded-xl border border-border-light/50 flex flex-col items-center">
                                            <span className="text-[10px] font-black text-text-tertiary uppercase">Views</span>
                                            <span className="font-bold text-text-primary">{selectedPost.views}</span>
                                        </div>
                                        <div className="px-4 py-2 bg-white rounded-xl border border-border-light/50 flex flex-col items-center">
                                            <span className="text-[10px] font-black text-text-tertiary uppercase">Members</span>
                                            <span className="font-bold text-text-primary">{selectedPost.memberCount}/{selectedPost.maxMembers}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Content Section */}
                            <div className="p-10">
                                <div className="flex flex-wrap gap-2 mb-10">
                                    {selectedPost.tags.map(tag => (
                                        <span key={tag} className="px-4 py-2 bg-primary/5 text-primary text-xs font-black rounded-xl border border-primary/10 tracking-tight">#{tag}</span>
                                    ))}
                                </div>

                                <div className="prose max-w-none text-text-secondary leading-[1.8] text-lg mb-12 whitespace-pre-wrap">
                                    {selectedPost.content}
                                </div>

                                {/* Author Console (Owner only simulation) */}
                                {selectedPost.authorId === 'me' || true && (
                                    <div className="mt-16 p-6 bg-background-secondary rounded-2xl border border-border-light/50 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <CheckCircle2 className="text-primary" size={24} />
                                            <span className="font-bold text-text-primary">작성한 게시글 관리</span>
                                        </div>
                                        <div className="flex gap-3">
                                            <Button
                                                variant="google-outline"
                                                size="sm"
                                                onClick={() => toggleComplete(selectedPost.id)}
                                                className="bg-white border-border-light"
                                            >
                                                {selectedPost.isCompleted ? '모집 재개하기' : '모집 완료 처리'}
                                            </Button>
                                            <Button
                                                variant="google-outline"
                                                size="sm"
                                                onClick={() => handleEdit(selectedPost.id)}
                                                className="bg-white border-border-light text-primary hover:bg-primary/5"
                                            >
                                                수정
                                            </Button>
                                            <Button
                                                variant="google-outline"
                                                size="sm"
                                                onClick={() => handleDelete(selectedPost.id)}
                                                className="bg-white border-border-light text-error hover:bg-error/5"
                                            >
                                                삭제
                                            </Button>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Comment Section Placeholder */}
                            <div className="p-10 bg-background-secondary/20 border-t border-border-light">
                                <div className="flex items-center gap-3 mb-8">
                                    <MessageSquare className="text-text-tertiary" size={22} />
                                    <h4 className="text-xl font-bold text-text-primary">문의 및 댓글</h4>
                                </div>

                                <div className="bg-white border border-border-light rounded-2xl p-6 mb-4">
                                    <textarea
                                        placeholder="관심 있으시다면 간단한 소개와 함께 댓글을 남겨주세요."
                                        className="w-full outline-none resize-none text-sm leading-relaxed"
                                        rows={3}
                                    />
                                    <div className="flex justify-end pt-4">
                                        <Button variant="google-primary" size="md">댓글 등록</Button>
                                    </div>
                                </div>
                            </div>
                        </article>
                    </div>
                )}

                {/* Report Modal Integration */}
                <RecruitmentReportModal
                    isOpen={isReportModalOpen}
                    onClose={() => setIsReportModalOpen(false)}
                    onSubmit={submitReport}
                    targetTitle={posts.find(p => p.id === reportTargetId)?.title}
                />
            </div>
        </MainLayout>
    );
};
