import React, { useMemo, useState } from 'react';
import { Filter, Plus, Users, Eye } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';
import { PageListHeader, PageListSubHeader } from '@/shared/components/layouts';
import { RecruitmentPostSummary } from '@/api/endpoints/boardApi';

interface RecruitmentListProps {
    posts: RecruitmentPostSummary[];
    onDetail: (id: number) => void;
    onAdd: () => void;
    onReport: (id: number) => void;
}

const statusLabel: Record<string, string> = {
    RECRUITING: '모집 중',
    COMPLETED: '모집 완료',
};

const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${month}.${day}`;
};

export const RecruitmentList: React.FC<RecruitmentListProps> = ({ posts, onDetail, onAdd }) => {
    const [search, setSearch] = useState('');
    const [activeStatus, setActiveStatus] = useState<'all' | 'recruiting' | 'completed'>('all');

    const filteredPosts = useMemo(() => posts.filter((post) => {
        const keyword = search.toLowerCase();
        const matchesSearch =
            post.title.toLowerCase().includes(keyword) ||
            (post.recruitmentField || '').toLowerCase().includes(keyword);
        const isCompleted = post.recruitmentStatus === 'COMPLETED';
        const matchesStatus =
            activeStatus === 'all' ||
            (activeStatus === 'recruiting' && !isCompleted) ||
            (activeStatus === 'completed' && isCompleted);
        return matchesSearch && matchesStatus;
    }), [posts, search, activeStatus]);

    const gridCols = 'grid-cols-[48px_1fr_96px_90px_64px_80px_80px]';
    const gridColsMobile = 'grid-cols-[1fr_70px_72px]';

    return (
        <div className="animate-fadeIn">
            {/* 헤더 - 공통 컴포넌트 */}
            <PageListHeader
                title="자유 모집 게시판"
                subtitle={
                    <>총 <span className="font-bold text-[var(--color-primary)]">{filteredPosts.length}</span>개의 모집글</>
                }
                actions={
                    <Button
                        onClick={onAdd}
                        variant="primary"
                        size="md"
                        leftIcon={<Plus size={18} />}
                        className="h-11 rounded-xl font-semibold shadow-md shadow-[var(--color-primary-alpha-20)]"
                    >
                        모집글 작성하기
                    </Button>
                }
            />

            {/* 서브헤더 - 공통 컴포넌트 */}
            <PageListSubHeader
                searchValue={search}
                onSearchChange={setSearch}
                searchPlaceholder="제목, 모집 분야로 검색.."
                filterControls={
                    <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1">
                        {(['all', 'recruiting', 'completed'] as const).map((status) => (
                            <button
                                key={status}
                                onClick={() => setActiveStatus(status)}
                                className={cn(
                                    'px-3 py-2 text-xs font-semibold rounded-lg transition-all whitespace-nowrap',
                                    activeStatus === status
                                        ? 'bg-white text-[var(--color-primary)] shadow-sm'
                                        : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                                )}
                            >
                                {status === 'all' ? '전체' : status === 'recruiting' ? '모집 중' : '모집 완료'}
                            </button>
                        ))}
                    </div>
                }
            />

            {filteredPosts.length > 0 ? (
                <div className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden">
                    <div className={cn(
                        'hidden md:grid gap-2 items-center px-5 py-3 bg-[var(--color-background)] border-b border-[var(--color-border)]',
                        'text-xs font-semibold text-[var(--color-text-tertiary)]',
                        gridCols
                    )}>
                        <span className="text-center">번호</span>
                        <span>제목</span>
                        <span>작성자</span>
                        <span className="text-center">목표 인원</span>
                        <span className="text-center">조회</span>
                        <span className="text-center">작성일</span>
                        <span className="text-center">상태</span>
                    </div>

                    <div className={cn(
                        'grid md:hidden gap-2 items-center px-4 py-3 bg-[var(--color-background)] border-b border-[var(--color-border)]',
                        'text-xs font-semibold text-[var(--color-text-tertiary)]',
                        gridColsMobile
                    )}>
                        <span>제목</span>
                        <span className="text-center">인원</span>
                        <span className="text-center">상태</span>
                    </div>

                    {filteredPosts.map((post, index) => {
                        const isCompleted = post.recruitmentStatus === 'COMPLETED';
                        return (
                            <React.Fragment key={post.id}>
                                <div
                                    onClick={() => onDetail(post.id)}
                                    className={cn(
                                        'hidden md:grid gap-2 items-center px-5 py-3.5 border-b border-[var(--color-border-lighter)] cursor-pointer transition-colors',
                                        'hover:bg-[var(--color-primary-alpha-5)]',
                                        gridCols,
                                        isCompleted && 'opacity-50'
                                    )}
                                >
                                    <span className="text-center text-sm text-[var(--color-text-tertiary)]">
                                        {filteredPosts.length - index}
                                    </span>
                                    <div className="flex items-center gap-2 min-w-0">
                                        <span className="text-sm font-medium text-[var(--color-text-primary)] truncate">
                                            {post.title}
                                        </span>
                                    </div>
                                    <span className="text-sm text-[var(--color-text-secondary)] truncate">
                                        {post.authorName}
                                    </span>
                                    <div className="flex items-center justify-center gap-1 text-sm">
                                        <Users size={13} className="text-[var(--color-text-muted)]" />
                                        <span className="text-[var(--color-text-primary)] font-medium">
                                            {post.targetMembers ?? '-'}
                                        </span>
                                    </div>
                                    <div className="flex items-center justify-center gap-1 text-sm text-[var(--color-text-tertiary)]">
                                        <Eye size={13} />
                                        <span>{post.viewCount}</span>
                                    </div>
                                    <span className="text-center text-sm text-[var(--color-text-tertiary)]">
                                        {formatDate(post.createdAt)}
                                    </span>
                                    <div className="flex justify-center">
                                        {isCompleted ? (
                                            <span className="text-[11px] font-bold text-[var(--color-text-tertiary)] bg-[var(--color-background)] px-2 py-0.5 rounded-md">
                                                모집 완료
                                            </span>
                                        ) : (
                                            <span className="text-[11px] font-bold text-[var(--color-success,#22c55e)] bg-[var(--color-success-alpha-10,rgba(34,197,94,0.1))] px-2 py-0.5 rounded-md">
                                                {statusLabel[post.recruitmentStatus] || '모집 중'}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                <div
                                    onClick={() => onDetail(post.id)}
                                    className={cn(
                                        'grid md:hidden gap-2 items-center px-4 py-3.5 border-b border-[var(--color-border-lighter)] cursor-pointer transition-colors',
                                        'active:bg-[var(--color-primary-alpha-5)]',
                                        gridColsMobile,
                                        isCompleted && 'opacity-50'
                                    )}
                                >
                                    <div className="min-w-0">
                                        <span className="text-sm font-medium text-[var(--color-text-primary)] truncate block">
                                            {post.title}
                                        </span>
                                        <span className="text-xs text-[var(--color-text-secondary)] truncate">
                                            {post.authorName}
                                        </span>
                                    </div>
                                    <span className="text-center text-xs text-[var(--color-text-tertiary)]">
                                        {post.targetMembers ?? '-'}
                                    </span>
                                    <div className="flex justify-center">
                                        {isCompleted ? (
                                            <span className="text-[10px] font-bold text-[var(--color-text-tertiary)]">모집 완료</span>
                                        ) : (
                                            <span className="text-[10px] font-bold text-[var(--color-success,#22c55e)]">모집 중</span>
                                        )}
                                    </div>
                                </div>
                            </React.Fragment>
                        );
                    })}
                </div>
            ) : (
                <div className="text-center py-16 bg-white rounded-2xl border border-[var(--color-border)]">
                    <div className="w-16 h-16 mx-auto mb-4 bg-[var(--color-background-secondary)] rounded-full flex items-center justify-center">
                        <Filter size={28} className="text-[var(--color-text-muted)]" />
                    </div>
                    <p className="text-lg font-semibold text-[var(--color-text-primary)] mb-2">
                        검색 결과가 없습니다
                    </p>
                    <p className="text-sm text-[var(--color-text-tertiary)]">
                        다른 키워드나 필터를 선택해보세요.
                    </p>
                </div>
            )}
        </div>
    );
};
