import React, { useState } from 'react';
import { Archive, Search, Calendar, Grid, List } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';

// Mock 데이터
const MOCK_ARCHIVES = [
    {
        id: 1,
        title: 'React Hooks 정리',
        date: '2026-01-25',
        tags: ['React', 'Hooks'],
        studyName: 'React 스터디',
        keyPoints: 5,
        quizCount: 3,
        summary: 'useState, useEffect, useRef 등 주요 Hook 정리',
    },
    {
        id: 2,
        title: 'TypeScript 제네릭',
        date: '2026-01-24',
        tags: ['TypeScript', '제네릭'],
        studyName: 'TypeScript 스터디',
        keyPoints: 7,
        quizCount: 5,
        summary: '제네릭 문법과 실전 활용법',
    },
    {
        id: 3,
        title: 'Zustand 상태관리',
        date: '2026-01-23',
        tags: ['React', 'Zustand', '상태관리'],
        studyName: 'React 스터디',
        keyPoints: 4,
        quizCount: 2,
        summary: 'Zustand를 활용한 전역 상태관리',
    },
];

export const LearningArchiveWidget: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedTag, setSelectedTag] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

    // 모든 태그 추출
    const allTags = Array.from(new Set(MOCK_ARCHIVES.flatMap((archive) => archive.tags)));

    // 필터링
    const filteredArchives = MOCK_ARCHIVES.filter((archive) => {
        const matchesSearch =
            searchQuery === '' ||
            archive.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            archive.summary.toLowerCase().includes(searchQuery.toLowerCase());

        const matchesTag = selectedTag === null || archive.tags.includes(selectedTag);

        return matchesSearch && matchesTag;
    });

    // 뷰 모드 토글 버튼
    const viewModeButtons = (
        <>
            <button
                onClick={() => setViewMode('grid')}
                className={cn(
                    'p-2 rounded-lg transition-all',
                    viewMode === 'grid' ? 'bg-primary/20 text-primary' : 'text-gray-400 hover:bg-gray-100'
                )}
            >
                <Grid size={18} />
            </button>
            <button
                onClick={() => setViewMode('list')}
                className={cn(
                    'p-2 rounded-lg transition-all',
                    viewMode === 'list' ? 'bg-primary/20 text-primary' : 'text-gray-400 hover:bg-gray-100'
                )}
            >
                <List size={18} />
            </button>
            <div className="w-px h-5 bg-gray-200 mx-1" />
        </>
    );

    return (
        <WidgetContainer>
            {/* 헤더 - 공통 컴포넌트 사용 */}
            <WidgetHeader
                icon={Archive}
                iconColor="accent"
                title="학습 보관함"
                subtitle="과거 스터디 기록"
                maximizePath="/learning-archive"
                rightActions={viewModeButtons}
            />

            <div className="p-6">
                {/* 검색 및 필터 */}
                <div className="mb-6 space-y-4">
                    {/* 검색창 */}
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                        <input
                            type="text"
                            placeholder="제목, 내용 검색..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-xl focus:border-primary focus:outline-none transition-colors"
                        />
                    </div>

                    {/* 태그 필터 */}
                    <div className="flex flex-wrap gap-2">
                        <button
                            onClick={() => setSelectedTag(null)}
                            className={cn(
                                'px-3 py-1.5 rounded-full text-xs font-medium transition-all',
                                selectedTag === null
                                    ? 'bg-primary text-white'
                                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            )}
                        >
                            전체
                        </button>
                        {allTags.map((tag) => (
                            <button
                                key={tag}
                                onClick={() => setSelectedTag(tag)}
                                className={cn(
                                    'px-3 py-1.5 rounded-full text-xs font-medium transition-all',
                                    selectedTag === tag
                                        ? 'bg-primary text-white'
                                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                )}
                            >
                                {tag}
                            </button>
                        ))}
                    </div>
                </div>

                {/* 아카이브 리스트 */}
                <div
                    className={cn(
                        viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 gap-4' : 'space-y-3'
                    )}
                >
                    {filteredArchives.map((archive) => (
                        <div
                            key={archive.id}
                            className={cn(
                                'bg-gray-50 rounded-xl p-4 cursor-pointer',
                                'hover:shadow-md hover:bg-white border border-gray-100'
                            )}
                        >
                            <div className="flex items-start justify-between mb-2">
                                <h4 className="font-bold text-text-primary mb-0">{archive.title}</h4>
                                <span className="text-xs text-text-tertiary flex items-center gap-1">
                                    <Calendar size={12} />
                                    {archive.date}
                                </span>
                            </div>

                            <p className="text-sm text-text-secondary mb-3 line-clamp-2">{archive.summary}</p>

                            <div className="flex items-center justify-between">
                                <div className="flex flex-wrap gap-1.5">
                                    {archive.tags.map((tag) => (
                                        <span
                                            key={tag}
                                            className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full"
                                        >
                                            {tag}
                                        </span>
                                    ))}
                                </div>
                                <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                    <span>키포인트 {archive.keyPoints}</span>
                                    <span>퀴즈 {archive.quizCount}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {filteredArchives.length === 0 && (
                    <div className="text-center py-12">
                        <Archive className="mx-auto mb-3 text-gray-300" size={48} />
                        <p className="text-text-tertiary">검색 결과가 없습니다</p>
                    </div>
                )}
            </div>
        </WidgetContainer>
    );
};
