import React, { useState } from 'react';
import { useRecruitmentStore } from '../useRecruitmentStore';
import { RecruitmentCard } from './RecruitmentCard';
import { Search, Filter, Plus } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';

interface RecruitmentListProps {
    onDetail: (id: string) => void;
    onAdd: () => void;
    onReport: (id: string) => void;
}

export const RecruitmentList: React.FC<RecruitmentListProps> = ({ onDetail, onAdd, onReport }) => {
    const { posts } = useRecruitmentStore();
    const [search, setSearch] = useState('');
    const [activeCategory, setActiveCategory] = useState<'all' | 'study' | 'project' | 'mentoring'>('all');

    const filteredPosts = posts.filter(post => {
        const matchesSearch = post.title.toLowerCase().includes(search.toLowerCase()) ||
            post.tags.some(t => t.toLowerCase().includes(search.toLowerCase()));
        const matchesCategory = activeCategory === 'all' || post.category === activeCategory;
        return matchesSearch && matchesCategory;
    });

    return (
        <div className="space-y-8 animate-fadeIn">
            {/* Header & Controls */}
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                <div>
                    <h2 className="text-3xl font-black text-text-primary tracking-tight mb-2">팀원 모집 게시판</h2>
                    <p className="text-text-secondary font-medium">함께 목표를 달성할 최고의 동료를 찾아보세요.</p>
                </div>

                <Button
                    onClick={onAdd}
                    variant="primary"
                    size="lg"
                    leftIcon={<Plus size={20} />}
                    className="px-6 py-6"
                >
                    모집글 작성하기
                </Button>
            </div>

            {/* Filters & Search */}
            <div className="flex flex-col md:flex-row gap-4">
                <div className="relative flex-1 group">
                    <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted group-focus-within:text-primary transition-colors" size={20} />
                    <input
                        type="text"
                        placeholder="제목, 기술 스택 등으로 검색하세요..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full bg-white border border-border-light rounded-2xl py-4 pl-12 pr-6 outline-none focus:border-primary/30 focus:shadow-sm transition-all text-sm font-medium"
                    />
                </div>

                <div className="flex items-center gap-2 overflow-x-auto pb-2 md:pb-0 no-scrollbar">
                    {['all', 'study', 'project', 'mentoring'].map((cat) => (
                        <button
                            key={cat}
                            onClick={() => setActiveCategory(cat as any)}
                            className={cn(
                                "px-5 py-3 rounded-2xl text-xs font-bold whitespace-nowrap border transition-all",
                                activeCategory === cat
                                    ? "bg-primary text-white border-primary shadow-md"
                                    : "bg-white text-text-secondary border-border-light hover:border-primary/30"
                            )}
                        >
                            {cat === 'all' ? '전체보기' : cat.toUpperCase()}
                        </button>
                    ))}
                </div>
            </div>

            {/* Grid */}
            {filteredPosts.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-10">
                    {filteredPosts.map((post, index) => (
                        <div key={post.id} className="animate-slideInUp" style={{ animationDelay: `${index * 50}ms` }}>
                            <RecruitmentCard
                                post={post}
                                onClick={onDetail}
                                onReport={(e, id) => {
                                    e.stopPropagation();
                                    onReport(id);
                                }}
                            />
                        </div>
                    ))}
                </div>
            ) : (
                <div className="bg-white border border-dashed border-border-light rounded-[32px] p-20 flex flex-col items-center justify-center text-center">
                    <div className="w-20 h-20 bg-background-secondary rounded-full flex items-center justify-center mb-6 text-text-tertiary">
                        <Filter size={40} />
                    </div>
                    <h4 className="text-xl font-bold text-text-primary mb-2">검색 결과가 없습니다</h4>
                    <p className="text-text-secondary">다른 키워드나 필터를 선택해보세요.</p>
                </div>
            )}
        </div>
    );
};
