import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Archive,
    ChevronLeft,
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
import '../styles/DashboardV2.css';

// 학습 기록 타입
interface LearningArchive {
    id: number;
    title: string;
    date: string;
    tags: string[];
    studyName: string;
    keyPoints: string[];
    quizCount: number;
    quizCorrectRate: number;
    summary: string;
    duration: string;
    participants: string[];
}

// Mock 데이터
const MOCK_ARCHIVES: LearningArchive[] = [
    {
        id: 1,
        title: 'React Hooks 정리',
        date: '2026-01-25',
        tags: ['React', 'Hooks', 'Frontend'],
        studyName: 'React 스터디',
        keyPoints: [
            'useState는 함수형 컴포넌트에서 상태를 관리하는 기본 훅',
            'useEffect는 사이드 이펙트를 처리하는 훅',
            'useRef는 DOM 접근 및 값 유지에 사용',
            'useMemo와 useCallback은 성능 최적화에 활용',
            'custom hook으로 로직 재사용 가능',
        ],
        quizCount: 8,
        quizCorrectRate: 75,
        summary: 'useState, useEffect, useRef 등 주요 Hook의 동작 원리와 사용법을 정리했습니다. 특히 의존성 배열의 역할과 클린업 함수의 실행 시점에 대해 심도 있게 학습했습니다.',
        duration: '1시간 30분',
        participants: ['김철수', '이영희', '박민수'],
    },
    {
        id: 2,
        title: 'TypeScript 제네릭',
        date: '2026-01-24',
        tags: ['TypeScript', '제네릭', 'Type System'],
        studyName: 'TypeScript 스터디',
        keyPoints: [
            '제네릭은 타입을 변수처럼 사용하는 기능',
            'Partial<T>는 모든 속성을 선택적으로 변환',
            'Pick<T, K>로 특정 속성만 선택 가능',
            'Omit<T, K>로 특정 속성 제외 가능',
            'Record<K, V>로 객체 타입 정의',
            'Exclude와 Extract로 유니온 타입 조작',
            'infer 키워드로 타입 추론',
        ],
        quizCount: 12,
        quizCorrectRate: 65,
        summary: '제네릭 문법과 실전 활용법을 학습했습니다. 다양한 유틸리티 타입의 사용법과 커스텀 유틸리티 타입 만들기를 연습했습니다.',
        duration: '2시간',
        participants: ['김철수', '이영희', '박민수', '정지원'],
    },
    {
        id: 3,
        title: 'Zustand 상태관리',
        date: '2026-01-23',
        tags: ['React', 'Zustand', '상태관리'],
        studyName: 'React 스터디',
        keyPoints: [
            'Zustand는 간단하고 가벼운 상태관리 라이브러리',
            'Redux보다 보일러플레이트가 적음',
            'create 함수로 스토어 생성',
            'selector로 필요한 상태만 구독 가능',
        ],
        quizCount: 5,
        quizCorrectRate: 80,
        summary: 'Zustand를 활용한 전역 상태관리 방법을 학습했습니다. Redux와의 비교를 통해 각 라이브러리의 장단점을 파악했습니다.',
        duration: '1시간',
        participants: ['김철수', '박민수'],
    },
    {
        id: 4,
        title: 'JavaScript 비동기 처리',
        date: '2026-01-22',
        tags: ['JavaScript', 'async/await', 'Promise'],
        studyName: 'JavaScript 스터디',
        keyPoints: [
            'Promise는 비동기 작업의 결과를 나타내는 객체',
            'async/await는 Promise를 동기적으로 작성하는 문법',
            'try-catch로 에러 핸들링',
            'Promise.all로 병렬 처리',
            'Promise.race로 가장 빠른 결과 선택',
        ],
        quizCount: 10,
        quizCorrectRate: 70,
        summary: 'JavaScript의 비동기 처리 패턴을 학습했습니다. 콜백 지옥에서 Promise, async/await까지의 발전 과정을 이해했습니다.',
        duration: '1시간 45분',
        participants: ['최현우', '박민수', '정지원'],
    },
    {
        id: 5,
        title: 'CSS Flexbox & Grid',
        date: '2026-01-20',
        tags: ['CSS', 'Flexbox', 'Grid', 'Layout'],
        studyName: 'Frontend 스터디',
        keyPoints: [
            'Flexbox는 1차원 레이아웃 시스템',
            'Grid는 2차원 레이아웃 시스템',
            'justify-content와 align-items로 정렬',
            'gap 속성으로 간격 조절',
            'fr 단위로 유연한 크기 지정',
        ],
        quizCount: 6,
        quizCorrectRate: 85,
        summary: 'CSS의 현대적인 레이아웃 시스템인 Flexbox와 Grid를 학습했습니다. 실제 UI 컴포넌트를 구현하며 실습했습니다.',
        duration: '1시간 20분',
        participants: ['김철수', '이영희'],
    },
];

type TabType = 'all' | 'recent' | 'favorite' | 'stats';

export const LearningArchivePage: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<TabType>('all');
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedTag, setSelectedTag] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [selectedArchive, setSelectedArchive] = useState<LearningArchive | null>(null);

    // 모든 태그 추출
    const allTags = Array.from(new Set(MOCK_ARCHIVES.flatMap((archive) => archive.tags)));

    // 필터링
    const filteredArchives = MOCK_ARCHIVES.filter((archive) => {
        const matchesSearch =
            searchQuery === '' ||
            archive.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            archive.summary.toLowerCase().includes(searchQuery.toLowerCase()) ||
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

    return (
        <div className="py-8">
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 미니멀 헤더 */}
                <div className="mb-6">
                    {/* 브레드크럼 */}
                    <nav className="flex items-center gap-1.5 text-sm mb-2">
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="text-text-tertiary hover:text-primary transition-colors"
                        >
                            대시보드
                        </button>
                        <ChevronRight size={14} className="text-text-tertiary" />
                        <button
                            onClick={selectedArchive ? () => setSelectedArchive(null) : undefined}
                            className={cn(
                                selectedArchive
                                    ? 'text-text-tertiary hover:text-primary transition-colors'
                                    : 'text-text-primary font-medium'
                            )}
                        >
                            학습 보관함
                        </button>
                        {selectedArchive && (
                            <>
                                <ChevronRight size={14} className="text-text-tertiary" />
                                <span className="text-text-primary font-medium">{selectedArchive.title}</span>
                            </>
                        )}
                    </nav>

                    {/* 페이지 타이틀 + 뒤로가기 */}
                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleBack}
                            className="text-text-tertiary hover:text-text-primary transition-colors"
                        >
                            <ChevronLeft size={24} strokeWidth={1.5} />
                        </button>
                        <h1 className="text-2xl font-bold text-text-primary mb-0">
                            {selectedArchive ? selectedArchive.title : '학습 보관함'}
                        </h1>
                        {selectedArchive && (
                            <span className="px-3 py-1 bg-accent/10 text-accent rounded-full text-sm font-medium">
                                {selectedArchive.studyName}
                            </span>
                        )}
                    </div>
                </div>

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
                                    { id: 'all' as TabType, label: '전체 보관함', icon: Archive, count: MOCK_ARCHIVES.length },
                                    { id: 'recent' as TabType, label: '최근 학습', icon: Clock, count: 3 },
                                    { id: 'favorite' as TabType, label: '즐겨찾기', icon: BookOpen, count: 2 },
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
                                            <LearningStatsView archives={MOCK_ARCHIVES} />
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
                                                            placeholder="제목, 내용, 태그 검색..."
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
                                            <div className={cn(
                                                viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 gap-4' : 'space-y-3'
                                            )}>
                                                {filteredArchives.map((archive) => (
                                                    <div
                                                        key={archive.id}
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

                                                        <p className="text-sm text-text-secondary mb-3 line-clamp-2">{archive.summary}</p>

                                                        <div className="flex items-center justify-between">
                                                            <div className="flex flex-wrap gap-1.5">
                                                                {archive.tags.slice(0, 3).map((tag) => (
                                                                    <span
                                                                        key={tag}
                                                                        className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full"
                                                                    >
                                                                        {tag}
                                                                    </span>
                                                                ))}
                                                                {archive.tags.length > 3 && (
                                                                    <span className="px-2 py-0.5 bg-gray-100 text-text-tertiary text-xs rounded-full">
                                                                        +{archive.tags.length - 3}
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                                                <span>키포인트 {archive.keyPoints.length}</span>
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
                    <div className="flex items-center gap-1.5">
                        <Clock size={14} />
                        <span>{archive.duration}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                        <Brain size={14} />
                        <span>퀴즈 {archive.quizCount}개 (정답률 {archive.quizCorrectRate}%)</span>
                    </div>
                </div>

                {/* 태그 */}
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

                {/* 요약 */}
                <div className="rounded-xl border border-gray-100 overflow-hidden">
                    <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                        <h3 className="font-semibold text-text-primary mb-0">📝 요약</h3>
                    </div>
                    <div className="p-5">
                        <p className="text-text-secondary leading-relaxed">{archive.summary}</p>
                    </div>
                </div>

                {/* 키포인트 */}
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

                {/* 참여자 */}
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

                {/* 퀴즈 성적 */}
                <div className="rounded-xl border border-gray-100 overflow-hidden">
                    <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                        <h3 className="font-semibold text-text-primary mb-0">📊 퀴즈 성적</h3>
                    </div>
                    <div className="p-5">
                        <div className="flex items-center justify-between mb-3">
                            <span className="text-sm text-text-secondary">정답률</span>
                            <span className="text-sm font-medium text-text-primary">{archive.quizCorrectRate}%</span>
                        </div>
                        <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden">
                            <div
                                style={{ width: `${archive.quizCorrectRate}%` }}
                                className={cn(
                                    'h-3 rounded-full',
                                    archive.quizCorrectRate >= 80 ? 'bg-accent' :
                                    archive.quizCorrectRate >= 60 ? 'bg-primary' :
                                    'bg-warning'
                                )}
                            />
                        </div>
                    </div>
                </div>
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
    const avgCorrectRate = Math.round(archives.reduce((sum, a) => sum + a.quizCorrectRate, 0) / totalArchives);
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
                    { label: '평균 정답률', value: `${avgCorrectRate}%`, icon: BarChart3, color: 'accent' as ColorType },
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

            {/* 자주 학습한 주제 */}
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
        </div>
    );
};

export default LearningArchivePage;
