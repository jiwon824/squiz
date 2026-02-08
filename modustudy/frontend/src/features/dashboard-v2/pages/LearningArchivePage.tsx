import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Archive,
    ChevronRight,
    Calendar,
    Search,
    Grid,
    List,
    BookOpen,
    Brain,
    Tag,
    Clock,
    BarChart3,
    FileText
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Spinner } from '@/shared/components/Spinner';
import { PageNavHeader } from '@/shared/components/layouts';
import '../styles/DashboardV2.css';
import { studyApi } from '@/api/endpoints/studyApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import { studyQuizApi } from '@/api/endpoints/studyQuizApi';

// 학습 기록 타입
interface LearningArchive {
    id: number;
    title: string;
    date: string;
    tags: string[];
    studyName: string;
    studyId: number;
    meetingId: number;
    keyPoints: string[];
    quizCount: number;
    quizCorrectRate: number;
    summary: string;
    duration: string;
    participants: string[];
}

type TabType = 'all' | 'recent' | 'favorite' | 'stats';

export const LearningArchivePage: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<TabType>('all');
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedTag, setSelectedTag] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [selectedArchive, setSelectedArchive] = useState<LearningArchive | null>(null);

    // 데이터 상태
    const [archives, setArchives] = useState<LearningArchive[]>([]);
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

                const allArchives: LearningArchive[] = [];

                for (const study of studyResponse.content) {
                    try {
                        // 미팅 목록 조회
                        const meetingsPage = await meetingApi.listMeetings(study.id, { size: 20 });

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

                            // 미팅 시간 계산
                            let duration = '';
                            if (meeting.startedAt && meeting.endedAt) {
                                const start = new Date(meeting.startedAt);
                                const end = new Date(meeting.endedAt);
                                const diffMs = end.getTime() - start.getTime();
                                const diffMins = Math.floor(diffMs / 1000 / 60);
                                if (diffMins >= 60) {
                                    duration = `${Math.floor(diffMins / 60)}시간 ${diffMins % 60}분`;
                                } else {
                                    duration = `${diffMins}분`;
                                }
                            }

                            allArchives.push({
                                id: meeting.id,
                                title: meeting.title,
                                date: meeting.startedAt?.split('T')[0] || '',
                                tags: [], // 상세 조회 시 keywords 로드
                                studyName: study.name,
                                studyId: study.id,
                                meetingId: meeting.id,
                                keyPoints: [], // 상세 조회 시 로드
                                quizCount,
                                quizCorrectRate: 0, // TODO: 사용자별 퀴즈 결과 저장 시 업데이트
                                summary: '', // 상세 조회 시 로드
                                duration,
                                participants: [], // 상세 조회 시 로드
                            });
                        }
                    } catch (err) {
                    }
                }

                // 날짜순 정렬 (최신 먼저)
                allArchives.sort((a, b) =>
                    new Date(b.date).getTime() - new Date(a.date).getTime()
                );

                setArchives(allArchives);
            } catch (err) {
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
            archive.studyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            archive.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));

        const matchesTag = selectedTag === null || archive.tags.includes(selectedTag);

        return matchesSearch && matchesTag;
    });

    const handleBack = () => {
        if (selectedArchive) {
            setSelectedArchive(null);
        } else {
            navigate(-1);
        }
    };

    // 로딩 상태
    if (isLoading) {
        return (
            <div className="py-8">
                <div className="max-w-[1400px] mx-auto px-8">
                    <Spinner variant="center" size="lg" label="로딩 중..." />
                </div>
            </div>
        );
    }

    return (
        <div className="py-8">
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 헤더 */}
                <PageNavHeader
                    title={selectedArchive ? selectedArchive.title : '학습 보관함'}
                    breadcrumbs={[
                        { label: '마이홈', path: '/dashboard' },
                        ...(selectedArchive
                            ? [
                                { label: '학습 보관함', onClick: () => setSelectedArchive(null) },
                                { label: selectedArchive.title },
                            ]
                            : [{ label: '학습 보관함' }]
                        ),
                    ]}
                    onBack={handleBack}
                    badge={selectedArchive ? { text: selectedArchive.studyName, className: 'bg-accent/10 text-accent' } : undefined}
                />

                {selectedArchive ? (
                    // 상세 뷰
                    <ArchiveDetailView archive={selectedArchive} />
                ) : (
                    /* 좌측 탭 + 우측 콘텐츠 통합 레이아웃 */
                    <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
                        <div className="flex">
                            {/* 좌측 탭 네비게이션 */}
                            <div className="w-60 flex-shrink-0 bg-gray-50/70 relative">
                                {[
                                    { id: 'all' as TabType, label: '전체 보관함', icon: Archive, count: archives.length },
                                    { id: 'recent' as TabType, label: '최근 학습', icon: Clock, count: Math.min(3, archives.length) },
                                    { id: 'favorite' as TabType, label: '즐겨찾기', icon: BookOpen, count: 0 },
                                    { id: 'stats' as TabType, label: '학습 통계', icon: BarChart3 },
                                ].map(tab => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setActiveTab(tab.id)}
                                        className={cn(
                                            'w-full flex items-center gap-2 px-4 py-4 text-sm font-medium transition-colors relative',
                                            activeTab === tab.id
                                                ? 'text-primary bg-white -mr-px z-10'
                                                : 'text-text-secondary hover:text-text-primary hover:bg-gray-100 border-b border-gray-100'
                                        )}
                                    >
                                        {/* 왼쪽 인디케이터 */}
                                        {activeTab === tab.id && (
                                            <div
                                                className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
                                            />
                                        )}
                                        <tab.icon size={18} />
                                        <span>{tab.label}</span>
                                        {tab.count !== undefined && (
                                            <span className={cn(
                                                'ml-auto px-1.5 py-0.5 rounded-full text-xs',
                                                activeTab === tab.id
                                                    ? 'bg-primary/10 text-primary'
                                                    : 'bg-gray-200 text-text-tertiary'
                                            )}>
                                                {tab.count}
                                            </span>
                                        )}
                                    </button>
                                ))}
                                {/* 우측 border line */}
                                <div className="absolute top-0 right-0 bottom-0 w-px bg-gray-200" />
                            </div>

                            {/* 우측 콘텐츠 영역 */}
                            <div className="flex-1 p-8">
                                {activeTab === 'stats' ? (
                                        <div>
                                            <LearningStatsView archives={archives} />
                                        </div>
                                    ) : (
                                        <div>
                                            {/* 검색 및 필터 */}
                                            <div className="mb-6 space-y-4">
                                                <div className="flex items-center gap-4">
                                                    {/* 검색창 */}
                                                    <div className="relative flex-1">
                                                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                                                        <input
                                                            type="text"
                                                            placeholder="제목, 스터디명 검색..."
                                                            value={searchQuery}
                                                            onChange={(e) => setSearchQuery(e.target.value)}
                                                            className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl focus:border-primary focus:outline-none transition-colors"
                                                        />
                                                    </div>
                                                    {/* 뷰 모드 토글 */}
                                                    <div className="flex gap-1">
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
                                                    </div>
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
                                                <div className={cn(
                                                    viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 gap-4' : 'space-y-3'
                                                )}>
                                                    {filteredArchives.map((archive) => (
                                                        <div
                                                            key={`${archive.studyId}-${archive.meetingId}`}
                                                            onClick={() => setSelectedArchive(archive)}
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
                                                                        archive.tags.slice(0, 3).map((tag) => (
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
                                                                    {archive.tags.length > 3 && (
                                                                        <span className="px-2 py-0.5 bg-gray-100 text-text-tertiary text-xs rounded-full">
                                                                            +{archive.tags.length - 3}
                                                                        </span>
                                                                    )}
                                                                </div>
                                                                <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                                                    {archive.duration && <span>{archive.duration}</span>}
                                                                    {archive.quizCount > 0 && <span>퀴즈 {archive.quizCount}</span>}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            ) : (
                                                <div className="text-center py-12">
                                                    <Archive className="mx-auto mb-3 text-gray-300" size={48} />
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
                                    )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

// 아카이브 상세 뷰
interface ArchiveDetailViewProps {
    archive: LearningArchive;
}

const ArchiveDetailView: React.FC<ArchiveDetailViewProps> = ({ archive }) => {
    return (
        <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden p-8">
            <div className="space-y-6">
                {/* 메타 정보 */}
                <div className="flex items-center gap-5 text-sm text-text-secondary pb-6 border-b border-gray-100">
                    <div className="flex items-center gap-1.5">
                        <Calendar size={14} />
                        <span>{archive.date}</span>
                    </div>
                    {archive.duration && (
                        <div className="flex items-center gap-1.5">
                            <Clock size={14} />
                            <span>{archive.duration}</span>
                        </div>
                    )}
                    {archive.quizCount > 0 && (
                        <div className="flex items-center gap-1.5">
                            <Brain size={14} />
                            <span>퀴즈 {archive.quizCount}개</span>
                        </div>
                    )}
                </div>

                {/* 태그 */}
                {archive.tags.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                        {archive.tags.map((tag) => (
                            <span
                                key={tag}
                                className="px-3 py-1.5 bg-primary/10 text-primary text-sm font-medium rounded-full"
                            >
                                {tag}
                            </span>
                        ))}
                    </div>
                )}

                {/* 스터디 정보 */}
                <div className="rounded-xl border border-gray-100 overflow-hidden">
                    <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                        <h3 className="font-semibold text-text-primary mb-0">📚 스터디 정보</h3>
                    </div>
                    <div className="p-5">
                        <p className="text-text-secondary">{archive.studyName}</p>
                    </div>
                </div>

                {/* 요약 */}
                {archive.summary && (
                    <div className="rounded-xl border border-gray-100 overflow-hidden">
                        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                            <h3 className="font-semibold text-text-primary mb-0">📝 요약</h3>
                        </div>
                        <div className="p-5">
                            <p className="text-text-secondary leading-relaxed">{archive.summary}</p>
                        </div>
                    </div>
                )}

                {/* 키포인트 */}
                {archive.keyPoints.length > 0 && (
                    <div className="rounded-xl border border-gray-100 overflow-hidden">
                        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                            <h3 className="font-semibold text-text-primary mb-0">💡 핵심 포인트</h3>
                        </div>
                        <div className="p-5">
                            <ul className="space-y-3">
                                {archive.keyPoints.map((point, idx) => (
                                    <li key={idx} className="flex items-start gap-3 text-text-secondary">
                                        <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                        <span>{point}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </div>
                )}

                {/* 참여자 */}
                {archive.participants.length > 0 && (
                    <div className="rounded-xl border border-gray-100 overflow-hidden">
                        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                            <h3 className="font-semibold text-text-primary mb-0">👥 참여자</h3>
                        </div>
                        <div className="p-5">
                            <div className="flex flex-wrap gap-2">
                                {archive.participants.map((name, idx) => (
                                    <span
                                        key={idx}
                                        className="px-3 py-1.5 bg-gray-50 text-text-secondary text-sm rounded-full"
                                    >
                                        {name}
                                    </span>
                                ))}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

// 색상 스타일 상수
const BG_COLOR_STYLES = {
    primary: 'bg-primary/10',
    secondary: 'bg-secondary/10',
    accent: 'bg-accent/10',
    warning: 'bg-warning/10',
} as const;

const TEXT_COLOR_STYLES = {
    primary: 'text-primary',
    secondary: 'text-secondary',
    accent: 'text-accent',
    warning: 'text-warning',
} as const;

type ColorType = keyof typeof BG_COLOR_STYLES;

// 학습 통계 뷰
interface LearningStatsViewProps {
    archives: LearningArchive[];
}

const LearningStatsView: React.FC<LearningStatsViewProps> = ({ archives }) => {
    // 통계 계산
    const totalArchives = archives.length;
    const totalQuizzes = archives.reduce((sum, a) => sum + a.quizCount, 0);
    const archivesWithRate = archives.filter(a => a.quizCorrectRate > 0);
    const avgCorrectRate = archivesWithRate.length > 0
        ? Math.round(archivesWithRate.reduce((sum, a) => sum + a.quizCorrectRate, 0) / archivesWithRate.length)
        : 0;
    const totalKeyPoints = archives.reduce((sum, a) => sum + a.keyPoints.length, 0);

    // 스터디별 학습 횟수
    const studyStats = archives.reduce((acc, a) => {
        acc[a.studyName] = (acc[a.studyName] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    // 태그별 빈도
    const tagStats = archives.flatMap(a => a.tags).reduce((acc, t) => {
        acc[t] = (acc[t] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    const topTags = Object.entries(tagStats)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 10);

    return (
        <div className="space-y-6">
            {/* 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                {[
                    { label: '총 학습 기록', value: totalArchives, icon: FileText, color: 'primary' as ColorType },
                    { label: '풀이한 퀴즈', value: totalQuizzes, icon: Brain, color: 'secondary' as ColorType },
                    { label: '평균 정답률', value: avgCorrectRate > 0 ? `${avgCorrectRate}%` : '-', icon: BarChart3, color: 'accent' as ColorType },
                    { label: '학습한 키포인트', value: totalKeyPoints, icon: BookOpen, color: 'warning' as ColorType },
                ].map((stat) => (
                    <div
                        key={stat.label}
                        className="rounded-xl border border-gray-100 px-4 py-4"
                    >
                        <div className="flex items-center gap-3">
                            <div className={cn(
                                'w-10 h-10 rounded-xl flex items-center justify-center',
                                BG_COLOR_STYLES[stat.color]
                            )}>
                                <stat.icon className={TEXT_COLOR_STYLES[stat.color]} size={18} />
                            </div>
                            <div>
                                <div className="text-xl font-bold text-text-primary">{stat.value}</div>
                                <div className="text-xs text-text-tertiary">{stat.label}</div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* 스터디별 학습 */}
            {Object.keys(studyStats).length > 0 && (
                <div className="rounded-xl border border-gray-100 overflow-hidden">
                    <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                        <h3 className="font-semibold text-text-primary mb-0">스터디별 학습 횟수</h3>
                    </div>
                    <div className="p-5 space-y-4">
                        {Object.entries(studyStats).map(([study, count]) => (
                            <div key={study}>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-sm font-medium text-text-primary">{study}</span>
                                    <span className="text-sm text-text-tertiary">{count}회</span>
                                </div>
                                <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                                    <div
                                        style={{ width: `${(count / totalArchives) * 100}%` }}
                                        className="h-2.5 bg-primary rounded-full"
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* 자주 학습한 주제 */}
            {topTags.length > 0 && (
                <div className="rounded-xl border border-gray-100 overflow-hidden">
                    <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                        <h3 className="font-semibold text-text-primary mb-0">자주 학습한 주제</h3>
                    </div>
                    <div className="p-5">
                        <div className="flex flex-wrap gap-2">
                            {topTags.map(([tag, count]) => (
                                <span
                                    key={tag}
                                    className="px-3 py-1.5 bg-gray-50 text-text-secondary text-sm rounded-full flex items-center gap-1.5"
                                >
                                    <Tag size={12} />
                                    {tag}
                                    <span className="text-xs text-text-tertiary">({count})</span>
                                </span>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* 빈 상태 */}
            {totalArchives === 0 && (
                <div className="text-center py-12">
                    <BarChart3 className="mx-auto mb-3 text-gray-300" size={48} />
                    <p className="text-text-tertiary">아직 학습 통계가 없습니다</p>
                    <p className="text-sm text-text-tertiary mt-1">스터디 미팅에 참여하면 통계가 생성됩니다</p>
                </div>
            )}
        </div>
    );
};

export default LearningArchivePage;
