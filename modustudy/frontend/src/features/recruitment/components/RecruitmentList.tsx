import React, { useState } from 'react';
import { Search, Filter, Plus, X, Users, Eye, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';
import { RecruitmentPostSummary } from '@/api/endpoints/boardApi';

interface RecruitmentListProps {
    posts: RecruitmentPostSummary[];
    onDetail: (id: number) => void;
    onAdd: () => void;
    onReport: (id: number) => void;
}

const statusLabel: Record<string, string> = {
    RECRUITING: '모집중',
};

// 날짜 포맷
const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${month}.${day}`;
};

export const RecruitmentList: React.FC<RecruitmentListProps> = ({ posts, onDetail, onAdd, onReport: _onReport }) => {
    const [search, setSearch] = useState('');
    const [activeStatus, setActiveStatus] = useState<'all' | 'recruiting' | 'completed'>('all');

    const filteredPosts = posts.filter(post => {
        const matchesSearch = post.title.toLowerCase().includes(search.toLowerCase()) ||
            (post.topicName || '').toLowerCase().includes(search.toLowerCase());
        const isCompleted = !['SCHEDULED', 'RECRUITING', 'PENDING'].includes(post.studyStatus);
        const matchesStatus =
            activeStatus === 'all' ||
            (activeStatus === 'recruiting' && !isCompleted) ||
            (activeStatus === 'completed' && isCompleted);
        return matchesSearch && matchesStatus;
    });

    const topicColorClasses = [
        'bg-[rgba(59,130,246,0.12)] text-[#2563eb]',
        'bg-[rgba(34,197,94,0.12)] text-[#16a34a]',
        'bg-[rgba(168,85,247,0.12)] text-[#a855f7]',
        'bg-[rgba(245,158,11,0.15)] text-[#d97706]',
        'bg-[rgba(236,72,153,0.12)] text-[#db2777]',
        'bg-[rgba(20,184,166,0.12)] text-[#0f766e]',
    ];

    const getTopicColor = (topicName?: string | null) => {
        if (!topicName) return topicColorClasses[0];
        let hash = 0;
        for (let i = 0; i < topicName.length; i += 1) {
            hash = (hash * 31 + topicName.charCodeAt(i)) % topicColorClasses.length;
        }
        return topicColorClasses[hash];
    };

    // 그리드 컬럼 정의
    const gridCols = 'grid-cols-[40px_96px_1fr_96px_72px_64px_80px_72px]';
    const gridColsMobile = 'grid-cols-[56px_1fr_80px_72px_72px]';

    return (
        <div className="space-y-6 animate-fadeIn">
            {/* 헤더 */}
            <div className="flex justify-between mb-2">
                <div className="flex items-center pt-2">
                    <div>
                        <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-text-primary)]">
                            팀원 모집 게시판
                        </h1>
                        <p className="text-sm text-[var(--color-text-secondary)] mt-1">
                            총 <span className="font-bold text-[var(--color-primary)]">{filteredPosts.length}</span>개의 모집글
                        </p>
                    </div>
                </div>

                <div className="flex items-center">
                    <Button
                        onClick={onAdd}
                        variant="primary"
                        size="md"
                        leftIcon={<Plus size={18} />}
                        className="h-11 rounded-xl font-semibold shadow-md shadow-[var(--color-primary-alpha-20)]"
                    >
                        모집글 작성하기
                    </Button>
                </div>
            </div>

            {/* 검색 및 필터 바 */}
            <div className="mb-6">
                <div className="flex flex-col lg:flex-row gap-4">
                    {/* 검색 바 */}
                    <div className="flex-1 relative">
                        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-text-tertiary)]" />
                        <input
                            type="text"
                            placeholder="제목, 기술 스택 등으로 검색..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="w-full h-11 pl-11 pr-10 bg-[var(--color-background)] rounded-xl text-sm focus:outline-none ring-0 focus:ring-2 ring-[var(--color-primary-alpha-10)] transition-all duration-300 ease-in-out"
                        />
                        {search && (
                            <button
                                type="button"
                                onClick={() => setSearch('')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-[var(--color-background-secondary)] text-[var(--color-text-tertiary)]"
                            >
                                <X size={16} />
                            </button>
                        )}
                    </div>

                    {/* 상태 필터 탭 */}
                    <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1">
                        {(['all', 'recruiting', 'completed'] as const).map((status) => (
                            <button
                                key={status}
                                onClick={() => setActiveStatus(status)}
                                className={cn(
                                    "px-3 py-2 text-xs font-semibold rounded-lg transition-all whitespace-nowrap",
                                    activeStatus === status
                                        ? "bg-white text-[var(--color-primary)] shadow-sm"
                                        : "text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                                )}
                            >
                                {status === 'all' ? '전체' : status === 'recruiting' ? '모집중' : '완료'}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* 게시판 테이블 */}
            {filteredPosts.length > 0 ? (
                <div className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden">
                    {/* 테이블 헤더 - 데스크톱 */}
                    <div className={cn(
                        "hidden md:grid gap-2 items-center px-5 py-3 bg-[var(--color-background)] border-b border-[var(--color-border)]",
                        "text-xs font-semibold text-[var(--color-text-tertiary)]",
                        gridCols
                    )}>
                        <span className="text-center">번호</span>
                        <span className="text-center">카테고리</span>
                        <span>제목</span>
                        <span>작성자</span>
                        <span className="text-center">인원</span>
                        <span className="text-center">조회</span>
                        <span className="text-center">작성일</span>
                        <span className="text-center">상태</span>
                    </div>

                    {/* 테이블 헤더 - 모바일 */}
                    <div className={cn(
                        "grid md:hidden gap-2 items-center px-4 py-3 bg-[var(--color-background)] border-b border-[var(--color-border)]",
                        "text-xs font-semibold text-[var(--color-text-tertiary)]",
                        gridColsMobile
                    )}>
                        <span className="text-center">카테고리</span>
                        <span>제목</span>
                        <span>작성자</span>
                        <span className="text-center">인원</span>
                        <span className="text-center">상태</span>
                    </div>

                    {/* 게시글 행 */}
                    {filteredPosts.map((post, index) => {
                        const isCompleted = !['SCHEDULED', 'RECRUITING', 'PENDING'].includes(post.studyStatus);
                        return (
                            <React.Fragment key={post.id}>
                                {/* 데스크톱 행 */}
                                <div
                                    onClick={() => onDetail(post.id)}
                                    className={cn(
                                        "hidden md:grid gap-2 items-center px-5 py-3.5 border-b border-[var(--color-border-lighter)] cursor-pointer transition-colors",
                                        "hover:bg-[var(--color-primary-alpha-5)]",
                                        gridCols,
                                        isCompleted && "opacity-50"
                                    )}
                                >
                                    {/* 번호 */}
                                    <span className="text-center text-sm text-[var(--color-text-tertiary)]">
                                        {filteredPosts.length - index}
                                    </span>

                                    {/* 카테고리 */}
                                    <span className={cn(
                                        "text-center text-[11px] font-bold px-2 py-0.5 rounded-md",
                                        getTopicColor(post.topicName)
                                    )}>
                                        {post.topicName || '기타'}
                                    </span>

                                    {/* 제목 + 스터디 */}
                                    <div className="flex items-center gap-2 min-w-0">
                                        {post.studyName && (
                                            <span className="flex-shrink-0 text-[11px] text-[var(--color-primary)] font-medium">
                                                [{post.studyName}]
                                            </span>
                                        )}
                                        <span className="text-sm font-medium text-[var(--color-text-primary)] truncate">
                                            {post.title}
                                        </span>
                                    </div>

                                    {/* 작성자 */}
                                    <span className="text-sm text-[var(--color-text-secondary)] truncate">
                                        {post.authorName}
                                    </span>

                                    {/* 인원 */}
                                    <div className="flex items-center justify-center gap-1 text-sm">
                                        <Users size={13} className="text-[var(--color-text-muted)]" />
                                        <span className="text-[var(--color-text-primary)] font-medium">{post.currentMembers}</span>
                                        <span className="text-[var(--color-text-muted)]">/</span>
                                        <span className="text-[var(--color-text-muted)]">{post.maxMembers ?? '-'}</span>
                                    </div>

                                    {/* 조회수 */}
                                    <div className="flex items-center justify-center gap-1 text-sm text-[var(--color-text-tertiary)]">
                                        <Eye size={13} />
                                        <span>{post.viewCount}</span>
                                    </div>

                                    {/* 작성일 */}
                                    <span className="text-center text-sm text-[var(--color-text-tertiary)]">
                                        {formatDate(post.createdAt)}
                                    </span>

                                    {/* 상태 */}
                                    <div className="flex justify-center">
                                        {isCompleted ? (
                                            <span className="text-[11px] font-bold text-[var(--color-text-tertiary)] bg-[var(--color-background)] px-2 py-0.5 rounded-md">
                                                완료
                                            </span>
                                        ) : (
                                            <span className="text-[11px] font-bold text-[var(--color-success,#22c55e)] bg-[var(--color-success-alpha-10,rgba(34,197,94,0.1))] px-2 py-0.5 rounded-md">
                                                {statusLabel[post.studyStatus] || '모집중'}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* 모바일 행 */}
                                <div
                                    onClick={() => onDetail(post.id)}
                                    className={cn(
                                        "grid md:hidden gap-2 items-center px-4 py-3.5 border-b border-[var(--color-border-lighter)] cursor-pointer transition-colors",
                                        "active:bg-[var(--color-primary-alpha-5)]",
                                        gridColsMobile,
                                        isCompleted && "opacity-50"
                                    )}
                                >
                                    {/* 카테고리 */}
                                    <span className={cn(
                                        "text-center text-[10px] font-bold px-1.5 py-0.5 rounded",
                                        getTopicColor(post.topicName)
                                    )}>
                                        {post.topicName || '기타'}
                                    </span>

                                    {/* 제목 */}
                                    <span className="text-sm font-medium text-[var(--color-text-primary)] truncate">
                                        {post.studyName ? `[${post.studyName}] ${post.title}` : post.title}
                                    </span>

                                    {/* 작성자 */}
                                    <span className="text-xs text-[var(--color-text-secondary)] truncate">
                                        {post.authorName}
                                    </span>

                                    {/* 인원 */}
                                    <span className="text-center text-xs text-[var(--color-text-tertiary)]">
                                        {post.currentMembers}/{post.maxMembers ?? '-'}
                                    </span>

                                    {/* 상태 */}
                                    <div className="flex justify-center">
                                        {isCompleted ? (
                                            <span className="text-[10px] font-bold text-[var(--color-text-tertiary)]">완료</span>
                                        ) : (
                                            <span className="text-[10px] font-bold text-[var(--color-success,#22c55e)]">모집중</span>
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
