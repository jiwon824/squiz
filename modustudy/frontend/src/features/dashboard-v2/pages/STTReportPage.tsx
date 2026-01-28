import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    FileText,
    ChevronLeft,
    ChevronRight,
    Calendar,
    Users,
    Clock,
    MessageSquare,
    ListChecks,
    BarChart3,
    Play,
    Download,
    Search
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import '../styles/DashboardV2.css';

// 미팅 리포트 타입
interface MeetingReport {
    id: number;
    studyName: string;
    meetingTitle: string;
    date: string;
    duration: string;
    participants: string[];
    summary: string;
    keywords: string[];
    highlights: string[];
    actionItems: string[];
    transcript: TranscriptItem[];
}

interface TranscriptItem {
    speaker: string;
    time: string;
    text: string;
}

// Mock 데이터
const MOCK_REPORTS: MeetingReport[] = [
    {
        id: 1,
        studyName: 'React 스터디',
        meetingTitle: '주간 회의 - React Hooks 심화',
        date: '2026-01-25',
        duration: '1시간 30분',
        participants: ['김철수', '이영희', '박민수', '정지원', '최현우'],
        summary: 'useState와 useEffect의 실행 순서, 클린업 함수의 동작 원리에 대해 논의했습니다. 특히 의존성 배열의 올바른 사용법과 일반적인 실수에 대해 깊이 있게 다루었습니다.',
        keywords: ['React Hooks', 'useEffect', '클린업 함수', '의존성 배열', 'useState'],
        highlights: [
            '의존성 배열이 빈 배열일 때 컴포넌트 마운트 시 한 번만 실행',
            'useEffect 클린업 함수는 언마운트 시 또는 다음 effect 실행 전에 호출',
            'useState의 함수형 업데이트를 사용하면 이전 상태에 안전하게 접근 가능',
            'custom hook을 활용하여 로직 재사용성 높이기',
        ],
        actionItems: [
            '다음 주까지 각자 custom hook 예제 1개씩 작성해오기',
            'useReducer vs useState 비교 자료 준비 (김철수)',
            'React 18 동시성 기능 발표 준비 (이영희)',
        ],
        transcript: [
            { speaker: '김철수', time: '00:00', text: '오늘은 React Hooks 심화 내용을 다뤄보겠습니다.' },
            { speaker: '이영희', time: '02:30', text: 'useEffect의 클린업 함수가 언제 실행되는지 정확히 모르겠어요.' },
            { speaker: '김철수', time: '03:15', text: '클린업 함수는 두 가지 경우에 실행됩니다. 컴포넌트가 언마운트될 때와 다음 effect가 실행되기 전에요.' },
            { speaker: '박민수', time: '05:00', text: '그럼 의존성 배열에 값이 있으면 그 값이 바뀔 때마다 클린업이 먼저 실행되는 거네요?' },
            { speaker: '김철수', time: '05:45', text: '맞습니다. 그래서 이벤트 리스너 등록 같은 경우 클린업에서 제거해줘야 해요.' },
        ],
    },
    {
        id: 2,
        studyName: 'TypeScript 스터디',
        meetingTitle: '제네릭과 유틸리티 타입',
        date: '2026-01-24',
        duration: '2시간',
        participants: ['김철수', '이영희', '박민수', '정지원'],
        summary: 'TypeScript의 제네릭 문법과 Partial, Pick 같은 유틸리티 타입 활용법을 학습했습니다. 실제 프로젝트에서 타입 안전성을 높이는 방법을 논의했습니다.',
        keywords: ['제네릭', 'Partial', 'Pick', '타입 추론', 'Omit', 'Record'],
        highlights: [
            '제네릭을 사용하면 재사용 가능한 컴포넌트를 만들 수 있음',
            'Partial<T>는 모든 속성을 선택적으로 만듦',
            'Pick<T, K>는 특정 속성만 선택하여 새 타입 생성',
            'Omit<T, K>는 특정 속성을 제외한 타입 생성',
        ],
        actionItems: [
            '기존 프로젝트에서 any 타입 제거하기',
            '유틸리티 타입 활용 예제 코드 공유하기',
        ],
        transcript: [
            { speaker: '이영희', time: '00:00', text: '오늘은 TypeScript 제네릭에 대해 알아보겠습니다.' },
            { speaker: '박민수', time: '01:00', text: '제네릭이 정확히 뭔가요?' },
            { speaker: '이영희', time: '01:30', text: '제네릭은 타입을 변수처럼 사용하는 기능이에요. 함수나 클래스를 정의할 때 타입을 나중에 지정할 수 있습니다.' },
        ],
    },
    {
        id: 3,
        studyName: 'JavaScript 스터디',
        meetingTitle: '비동기 프로그래밍 마스터',
        date: '2026-01-22',
        duration: '1시간 45분',
        participants: ['김철수', '최현우', '박민수'],
        summary: 'JavaScript의 비동기 처리 방식인 Promise, async/await에 대해 심도 있게 학습했습니다. 에러 핸들링과 동시성 제어에 대해서도 다루었습니다.',
        keywords: ['Promise', 'async/await', '비동기', 'try-catch', 'Promise.all'],
        highlights: [
            'Promise는 비동기 작업의 완료 또는 실패를 나타내는 객체',
            'async/await는 Promise를 더 읽기 쉽게 사용하는 문법',
            'Promise.all()로 여러 비동기 작업을 병렬 처리',
            'Promise.allSettled()는 모든 Promise가 처리될 때까지 대기',
        ],
        actionItems: [
            'API 호출 코드를 async/await로 리팩토링하기',
            '에러 핸들링 패턴 정리해서 공유하기',
        ],
        transcript: [
            { speaker: '최현우', time: '00:00', text: 'JavaScript 비동기 처리에 대해 복습해봅시다.' },
            { speaker: '박민수', time: '00:30', text: 'Promise와 async/await의 차이가 뭔가요?' },
            { speaker: '최현우', time: '01:00', text: 'async/await는 Promise를 더 동기적으로 보이게 작성하는 문법적 설탕이에요.' },
        ],
    },
];

type TabType = 'summary' | 'transcript' | 'action' | 'stats';

export const STTReportPage: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<TabType>('summary');
    const [selectedReport, setSelectedReport] = useState<MeetingReport>(MOCK_REPORTS[0]);
    const [searchQuery, setSearchQuery] = useState('');

    // 검색 필터링
    const filteredReports = MOCK_REPORTS.filter(report =>
        searchQuery === '' ||
        report.meetingTitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
        report.studyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        report.keywords.some(k => k.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    const handleBack = () => {
        navigate(-1);
    };

    return (
        <div className="py-8">
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 미니멀 헤더 */}
                <div className="mb-6">
                    {/* 브레드크럼 */}
                    <nav className="flex items-center gap-1.5 text-sm mb-2">
                        <button
                            onClick={() => navigate('/dashboard-v2')}
                            className="text-text-tertiary hover:text-primary transition-colors"
                        >
                            대시보드
                        </button>
                        <ChevronRight size={14} className="text-text-tertiary" />
                        <span className="text-text-primary font-medium">STT 미팅 리포트</span>
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
                            STT 미팅 리포트
                        </h1>
                    </div>
                </div>

                {/* 좌측 탭 + 우측 콘텐츠 통합 레이아웃 */}
                <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
                    <div className="flex">
                        {/* 좌측: 미팅 리스트 + 탭 */}
                        <div className="w-72 flex-shrink-0 bg-gray-50/70 relative flex flex-col">
                            {/* 검색 */}
                            <div className="p-4 border-b border-gray-100">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                                    <input
                                        type="text"
                                        placeholder="미팅 검색..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        className="w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:border-primary focus:outline-none transition-colors"
                                    />
                                </div>
                            </div>

                            {/* 미팅 리스트 */}
                            <div className="flex-1 overflow-y-auto max-h-[500px]">
                                {filteredReports.map((report) => (
                                    <button
                                        key={report.id}
                                        onClick={() => setSelectedReport(report)}
                                        className={cn(
                                            'w-full p-4 text-left transition-all border-b border-gray-100',
                                            selectedReport.id === report.id
                                                ? 'bg-white border-l-4 border-l-primary -mr-px z-10'
                                                : 'hover:bg-gray-100'
                                        )}
                                    >
                                        <h4 className="font-bold text-text-primary text-sm mb-0.5">{report.studyName}</h4>
                                        <p className="text-xs text-text-secondary truncate">{report.meetingTitle}</p>
                                        <div className="flex items-center gap-2 mt-2 text-xs text-text-tertiary">
                                            <Calendar size={12} />
                                            <span>{report.date}</span>
                                        </div>
                                    </button>
                                ))}
                            </div>

                            {/* 탭 네비게이션 */}
                            <div className="border-t border-gray-200">
                                {[
                                    { id: 'summary' as TabType, label: '요약', icon: FileText },
                                    { id: 'transcript' as TabType, label: '전체 기록', icon: MessageSquare },
                                    { id: 'action' as TabType, label: '액션 아이템', icon: ListChecks },
                                    { id: 'stats' as TabType, label: '통계', icon: BarChart3 },
                                ].map(tab => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setActiveTab(tab.id)}
                                        className={cn(
                                            'w-full flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors relative',
                                            activeTab === tab.id
                                                ? 'text-primary bg-white -mr-px z-10'
                                                : 'text-text-secondary hover:text-text-primary hover:bg-gray-100 border-b border-gray-100'
                                        )}
                                    >
                                        {/* 왼쪽 인디케이터 */}
                                        {activeTab === tab.id && (
                                            <motion.div
                                                layoutId="stt-tab-indicator"
                                                className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
                                            />
                                        )}
                                        <tab.icon size={16} />
                                        <span>{tab.label}</span>
                                    </button>
                                ))}
                            </div>

                            {/* 우측 border line */}
                            <div className="absolute top-0 right-0 bottom-0 w-px bg-gray-200" />
                        </div>

                        {/* 우측 콘텐츠 영역 */}
                        <div className="flex-1 p-8">
                            {/* 선택된 미팅 헤더 */}
                            <div className="mb-6 pb-6 border-b border-gray-100">
                                <h2 className="text-xl font-bold text-text-primary mb-2">
                                    {selectedReport.meetingTitle}
                                </h2>
                                <div className="flex items-center gap-5 text-sm text-text-secondary">
                                    <div className="flex items-center gap-1.5">
                                        <Calendar size={14} />
                                        <span>{selectedReport.date}</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <Clock size={14} />
                                        <span>{selectedReport.duration}</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <Users size={14} />
                                        <span>{selectedReport.participants.length}명 참여</span>
                                    </div>
                                </div>
                                {/* 참여자 목록 */}
                                <div className="flex flex-wrap gap-2 mt-3">
                                    {selectedReport.participants.map((name, idx) => (
                                        <span
                                            key={idx}
                                            className="px-2.5 py-1 bg-gray-50 text-text-secondary text-xs rounded-full"
                                        >
                                            {name}
                                        </span>
                                    ))}
                                </div>
                            </div>

                            <AnimatePresence mode="wait">
                                {activeTab === 'summary' && (
                                    <motion.div
                                        key="summary"
                                        initial={{ opacity: 0, x: 10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -10 }}
                                    >
                                        <SummaryView report={selectedReport} />
                                    </motion.div>
                                )}

                                {activeTab === 'transcript' && (
                                    <motion.div
                                        key="transcript"
                                        initial={{ opacity: 0, x: 10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -10 }}
                                    >
                                        <TranscriptView report={selectedReport} />
                                    </motion.div>
                                )}

                                {activeTab === 'action' && (
                                    <motion.div
                                        key="action"
                                        initial={{ opacity: 0, x: 10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -10 }}
                                    >
                                        <ActionItemsView report={selectedReport} />
                                    </motion.div>
                                )}

                                {activeTab === 'stats' && (
                                    <motion.div
                                        key="stats"
                                        initial={{ opacity: 0, x: 10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -10 }}
                                    >
                                        <StatsView reports={MOCK_REPORTS} />
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

// 요약 뷰
interface SummaryViewProps {
    report: MeetingReport;
}

const SummaryView: React.FC<SummaryViewProps> = ({ report }) => {
    return (
        <div className="space-y-6">
            {/* 요약 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">📝 요약</h3>
                </div>
                <div className="p-5">
                    <p className="text-text-secondary leading-relaxed">{report.summary}</p>
                </div>
            </div>

            {/* 키워드 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">🔑 핵심 키워드</h3>
                </div>
                <div className="p-5">
                    <div className="flex flex-wrap gap-2">
                        {report.keywords.map((keyword, idx) => (
                            <span
                                key={idx}
                                className="px-3 py-1.5 bg-primary/10 text-primary text-sm font-medium rounded-full"
                            >
                                {keyword}
                            </span>
                        ))}
                    </div>
                </div>
            </div>

            {/* 주요 내용 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">💡 주요 내용</h3>
                </div>
                <div className="p-5">
                    <ul className="space-y-3">
                        {report.highlights.map((highlight, idx) => (
                            <li key={idx} className="flex items-start gap-3 text-text-secondary">
                                <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                <span>{highlight}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        </div>
    );
};

// 전체 기록 뷰
interface TranscriptViewProps {
    report: MeetingReport;
}

const TranscriptView: React.FC<TranscriptViewProps> = ({ report }) => {
    const [searchQuery, setSearchQuery] = useState('');

    const filteredTranscript = report.transcript.filter(item =>
        searchQuery === '' ||
        item.text.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.speaker.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="space-y-4">
            {/* 검색 */}
            <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <input
                    type="text"
                    placeholder="대화 내용 검색..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:border-primary focus:outline-none transition-colors"
                />
            </div>

            {/* 전체 기록 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50 flex items-center justify-between">
                    <h3 className="font-semibold text-text-primary mb-0">대화 기록</h3>
                    <button className="flex items-center gap-1.5 text-sm text-text-secondary hover:text-primary transition-colors">
                        <Download size={14} />
                        내보내기
                    </button>
                </div>
                <div className="p-5 space-y-4 max-h-[400px] overflow-y-auto">
                    {filteredTranscript.map((item, idx) => (
                        <div key={idx} className="flex gap-4">
                            <div className="flex-shrink-0 w-16 text-xs text-text-tertiary pt-0.5">
                                {item.time}
                            </div>
                            <div className="flex-1">
                                <span className="font-medium text-text-primary">{item.speaker}</span>
                                <p className="text-text-secondary mt-1">{item.text}</p>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* 재생 버튼 (가상) */}
            <div className="flex justify-center pt-4">
                <button className="flex items-center gap-2 px-6 py-3 bg-primary text-white rounded-xl font-medium hover:bg-primary-dark transition-colors">
                    <Play size={18} />
                    음성 재생
                </button>
            </div>
        </div>
    );
};

// 액션 아이템 뷰
interface ActionItemsViewProps {
    report: MeetingReport;
}

const ActionItemsView: React.FC<ActionItemsViewProps> = ({ report }) => {
    const [completedItems, setCompletedItems] = useState<number[]>([]);

    const toggleComplete = (idx: number) => {
        setCompletedItems(prev =>
            prev.includes(idx)
                ? prev.filter(i => i !== idx)
                : [...prev, idx]
        );
    };

    return (
        <div className="space-y-4">
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50 flex items-center justify-between">
                    <h3 className="font-semibold text-text-primary mb-0">✅ 액션 아이템</h3>
                    <span className="text-sm text-text-tertiary">
                        {completedItems.length} / {report.actionItems.length} 완료
                    </span>
                </div>
                <div className="p-5 space-y-3">
                    {report.actionItems.map((item, idx) => (
                        <label
                            key={idx}
                            className={cn(
                                'flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition-all',
                                completedItems.includes(idx)
                                    ? 'bg-accent/5 border-accent/30'
                                    : 'border-gray-100 hover:border-gray-200'
                            )}
                        >
                            <input
                                type="checkbox"
                                checked={completedItems.includes(idx)}
                                onChange={() => toggleComplete(idx)}
                                className="mt-0.5 w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
                            />
                            <span className={cn(
                                'flex-1 text-text-secondary',
                                completedItems.includes(idx) && 'line-through text-text-tertiary'
                            )}>
                                {item}
                            </span>
                        </label>
                    ))}
                </div>
            </div>

            {/* 진행률 */}
            <div className="rounded-xl border border-gray-100 p-5">
                <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-text-primary">진행률</span>
                    <span className="text-sm text-text-tertiary">
                        {Math.round((completedItems.length / report.actionItems.length) * 100)}%
                    </span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                    <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${(completedItems.length / report.actionItems.length) * 100}%` }}
                        transition={{ duration: 0.5 }}
                        className="h-2.5 bg-accent rounded-full"
                    />
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

// 통계 뷰
interface StatsViewProps {
    reports: MeetingReport[];
}

const StatsView: React.FC<StatsViewProps> = ({ reports }) => {
    // 통계 계산
    const totalMeetings = reports.length;
    const totalParticipants = reports.reduce((sum, r) => sum + r.participants.length, 0);
    const avgParticipants = (totalParticipants / totalMeetings).toFixed(1);
    const totalKeywords = reports.reduce((sum, r) => sum + r.keywords.length, 0);

    // 스터디별 미팅 횟수
    const studyStats = reports.reduce((acc, r) => {
        acc[r.studyName] = (acc[r.studyName] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    // 키워드 빈도
    const keywordStats = reports.flatMap(r => r.keywords).reduce((acc, k) => {
        acc[k] = (acc[k] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    const topKeywords = Object.entries(keywordStats)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 8);

    return (
        <div className="space-y-6">
            {/* 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                {[
                    { label: '총 미팅 수', value: totalMeetings, icon: FileText, color: 'primary' as ColorType },
                    { label: '평균 참여자', value: avgParticipants, icon: Users, color: 'secondary' as ColorType },
                    { label: '총 키워드', value: totalKeywords, icon: MessageSquare, color: 'accent' as ColorType },
                    { label: '스터디 그룹', value: Object.keys(studyStats).length, icon: BarChart3, color: 'warning' as ColorType },
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

            {/* 스터디별 미팅 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">스터디별 미팅 횟수</h3>
                </div>
                <div className="p-5 space-y-4">
                    {Object.entries(studyStats).map(([study, count]) => (
                        <div key={study}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-sm font-medium text-text-primary">{study}</span>
                                <span className="text-sm text-text-tertiary">{count}회</span>
                            </div>
                            <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                                <motion.div
                                    initial={{ width: 0 }}
                                    animate={{ width: `${(count / totalMeetings) * 100}%` }}
                                    transition={{ duration: 0.8 }}
                                    className="h-2.5 bg-primary rounded-full"
                                />
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* 자주 언급된 키워드 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">자주 언급된 키워드</h3>
                </div>
                <div className="p-5">
                    <div className="flex flex-wrap gap-2">
                        {topKeywords.map(([keyword, count]) => (
                            <span
                                key={keyword}
                                className="px-3 py-1.5 bg-gray-50 text-text-secondary text-sm rounded-full flex items-center gap-1.5"
                            >
                                {keyword}
                                <span className="text-xs text-text-tertiary">({count})</span>
                            </span>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default STTReportPage;
