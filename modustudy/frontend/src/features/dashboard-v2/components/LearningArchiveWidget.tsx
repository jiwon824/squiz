import React, { useState, useEffect } from 'react';
import { BookOpen, Search, Calendar, Grid, List, Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';
import { studyApi } from '@/api/endpoints/studyApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import { studyQuizApi } from '@/api/endpoints/studyQuizApi';

// 아카이브 아이템 타입
interface ArchiveItem {
    id: number;
    title: string;
    date: string;
    tags: string[];
    studyName: string;
    keyPoints: number;
    quizCount: number;
    summary: string;
    studyId: number;
    meetingId: number;
}

export const LearningArchiveWidget: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedTag, setSelectedTag] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

    // 데이터 상태
    const [archives, setArchives] = useState<ArchiveItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // 마운트 시 데이터 로딩
    useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);

                // 내 스터디 목록 조회
                const studyResponse = await studyApi.getMyStudies(0, 20);
                if (studyResponse.content.length === 0) {
                    setArchives([]);
                    return;
                }

                const allArchives: ArchiveItem[] = [];

                for (const study of studyResponse.content) {
                    try {
                        // 미팅 목록 조회
                        const meetingsPage = await meetingApi.listMeetings(study.id, { size: 10 });

                        // 퀴즈 목록 조회
                        let quizzes: { id: number; sourceId: number | null; questionCount: number }[] = [];
                        try {
                            quizzes = await studyQuizApi.getStudyQuizzes(study.id);
                        } catch {
                            // 퀴즈 조회 실패 시 무시
                        }

                        // 미팅별 아카이브 아이템 생성
                        for (const meeting of meetingsPage.content) {
                            // 해당 미팅의 퀴즈 수 계산
                            const meetingQuiz = quizzes.find(q => q.sourceId === meeting.id);
                            const quizCount = meetingQuiz ? meetingQuiz.questionCount : 0;

                            allArchives.push({
                                id: meeting.id,
                                title: meeting.title,
                                date: meeting.startedAt?.split('T')[0] || '',
                                tags: [], // 상세 조회 시 keywords 로드 가능
                                studyName: study.name,
                                keyPoints: 0, // 상세 조회 시 highlights 수 로드 가능
                                quizCount,
                                summary: '', // 상세 조회 시 로드
                                studyId: study.id,
                                meetingId: meeting.id,
                            });
                        }
                    } catch (err) {
                        console.warn(`[LearningArchiveWidget] 스터디 ${study.id} 데이터 조회 실패:`, err);
                    }
                }

                // 날짜순 정렬 (최신 먼저)
                allArchives.sort((a, b) =>
                    new Date(b.date).getTime() - new Date(a.date).getTime()
                );

                setArchives(allArchives);
            } catch (err) {
                console.error('[LearningArchiveWidget] 데이터 로딩 실패:', err);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, []);

    // 모든 태그 추출
    const allTags = Array.from(new Set(archives.flatMap((archive) => archive.tags)));

    // 필터링
    const filteredArchives = archives.filter((archive) => {
        const matchesSearch =
            searchQuery === '' ||
            archive.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            archive.summary.toLowerCase().includes(searchQuery.toLowerCase()) ||
            archive.studyName.toLowerCase().includes(searchQuery.toLowerCase());

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

    // 로딩 중
    if (isLoading) {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={BookOpen}
                    iconColor="neutral"
                    title="학습 보관함"
                    subtitle="과거 스터디 기록"
                    maximizePath="/learning-archive"
                />
                <div className="flex items-center justify-center h-64 text-text-tertiary">
                    <Loader2 className="animate-spin mr-2" size={20} />
                    로딩 중...
                </div>
            </WidgetContainer>
        );
    }

    return (
        <WidgetContainer>
            {/* 헤더 - 공통 컴포넌트 사용 */}
            <WidgetHeader
                icon={BookOpen}
                iconColor="neutral"
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
                            placeholder="제목, 스터디명 검색..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-xl focus:border-primary focus:outline-none transition-colors"
                        />
                    </div>

                    {/* 태그 필터 */}
                    {allTags.length > 0 && (
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
                    )}
                </div>

                {/* 아카이브 리스트 */}
                {filteredArchives.length > 0 ? (
                    <div
                        className={cn(
                            viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 gap-4' : 'space-y-3'
                        )}
                    >
                        {filteredArchives.map((archive) => (
                            <div
                                key={`${archive.studyId}-${archive.meetingId}`}
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

                                <p className="text-sm text-text-secondary mb-3 line-clamp-2">
                                    {archive.studyName}
                                </p>

                                <div className="flex items-center justify-between">
                                    <div className="flex flex-wrap gap-1.5">
                                        {archive.tags.length > 0 ? (
                                            archive.tags.map((tag) => (
                                                <span
                                                    key={tag}
                                                    className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full"
                                                >
                                                    {tag}
                                                </span>
                                            ))
                                        ) : (
                                            <span className="px-2 py-0.5 bg-gray-100 text-gray-500 text-xs rounded-full">
                                                {archive.studyName}
                                            </span>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                        {archive.quizCount > 0 && (
                                            <span>퀴즈 {archive.quizCount}문제</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-12">
                        <BookOpen className="mx-auto mb-3 text-gray-300" size={48} />
                        <p className="text-text-tertiary">
                            {archives.length === 0
                                ? '아직 학습 기록이 없습니다'
                                : '검색 결과가 없습니다'}
                        </p>
                        {archives.length === 0 && (
                            <p className="text-sm text-text-tertiary mt-1">
                                스터디 미팅 후 자동으로 저장됩니다
                            </p>
                        )}
                    </div>
                )}
            </div>
        </WidgetContainer>
    );
};
