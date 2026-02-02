// STT 미팅 리포트 페이지
// 서브컴포넌트로 분리된 대시보드형 레이아웃

import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    FileText,
    Calendar,
    Users,
    Clock,
    MessageSquare,
    ListChecks,
    BarChart3,
    Search,
    Download,
    ChevronDown,
    AArrowUp,
    AArrowDown,
    Type,
} from 'lucide-react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { Dropdown } from '@/shared/components';
import { PageNavHeader } from '@/shared/components/layouts';
import {
    SummaryView,
    TranscriptView,
    ActionItemsView,
    StatsView,
    exportReports,
} from '../components/stt-report';
import type { TabType, TranscriptItem, ExportScope } from '../components/stt-report';
import { useSttStore } from '@/store/sttStore';
import '../styles/DashboardV2.css';

// 탭 정의
const TABS = [
    { id: 'summary' as TabType, label: '요약', icon: FileText },
    { id: 'transcript' as TabType, label: '전체 기록', icon: MessageSquare },
    { id: 'action' as TabType, label: '액션 아이템', icon: ListChecks },
    { id: 'stats' as TabType, label: '통계', icon: BarChart3 },
] as const;

// 탭 전환 애니메이션
const TAB_ANIMATION = {
    initial: { opacity: 0, x: 10 },
    animate: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -10 },
};

// 다운로드 드롭다운 옵션
const EXPORT_OPTIONS: { scope: ExportScope; label: string; desc: string }[] = [
    { scope: 'current', label: '현재 미팅', desc: '선택된 미팅 1건' },
    { scope: 'byStudy', label: '스터디별', desc: '스터디 그룹으로 묶어서' },
    { scope: 'byDate', label: '일자별', desc: '날짜순 정렬' },
    { scope: 'all', label: '전체', desc: '스터디별 + 일자별 통합' },
];

export const STTReportPage: React.FC = () => {
    const navigate = useNavigate();

    // STT 스토어
    const {
        reports,
        selectedReport,
        selectReport,
        isLoading,
        fetchMeetings,
        updateSummary,
        updateTranscript,
    } = useSttStore();

    const [activeTab, setActiveTab] = useState<TabType>('summary');
    const [searchQuery, setSearchQuery] = useState('');
    const [textSize, setTextSize] = useState<'sm' | 'base' | 'lg'>('base');

    // 마운트 시 미팅 목록 조회
    useEffect(() => {
        fetchMeetings();
    }, [fetchMeetings]);


    // 검색 필터링
    const filteredReports = reports.filter(report =>
        searchQuery === '' ||
        report.meetingTitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
        report.studyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        report.keywords.some(k => k.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    // 요약 변경 핸들러
    const handleSummaryChange = useCallback((newSummary: string) => {
        if (selectedReport) {
            updateSummary(selectedReport.id, newSummary);
        }
    }, [selectedReport, updateSummary]);

    // 대화 기록 변경 핸들러
    const handleTranscriptChange = useCallback((updatedTranscript: TranscriptItem[]) => {
        if (selectedReport) {
            updateTranscript(selectedReport.id, updatedTranscript);
        }
    }, [selectedReport, updateTranscript]);

    // JSON 다운로드 핸들러
    const handleExport = useCallback((scope: ExportScope) => {
        exportReports(scope, reports, selectedReport ?? undefined);
    }, [selectedReport, reports]);

    return (
        <div className="py-8">
            <div className="max-w-[1600px] mx-auto px-8">
                {/* 브레드크럼 + 뒤로가기 헤더 + 우측 다운로드 */}
                <PageNavHeader
                    title="미팅 리포트"
                    breadcrumbs={[
                        { label: '대시보드', path: '/dashboard' },
                        { label: '미팅 리포트' },
                    ]}
                    onBack={() => navigate(-1)}
                    rightActions={
                        <Dropdown
                            trigger={({ isOpen, toggle }) => (
                                <button
                                    onClick={toggle}
                                    className={cn(
                                        'inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium rounded-lg',
                                        'text-text-secondary',
                                        'hover:bg-surface-hover hover:text-text-primary transition-colors',
                                        isOpen && 'bg-surface-hover text-text-primary'
                                    )}
                                >
                                    <Download size={15} />
                                    JSON 내보내기
                                    <ChevronDown size={14} className={cn(
                                        'transition-transform duration-200',
                                        isOpen && 'rotate-180'
                                    )} />
                                </button>
                            )}
                            align="right"
                            menuClassName="w-40"
                            items={EXPORT_OPTIONS.map(opt => ({
                                label: opt.label,
                                value: opt.scope,
                                onClick: () => handleExport(opt.scope),
                            }))}
                        />
                    }
                />

                {/* 좌측 사이드바 + 우측 콘텐츠 */}
                <div className={cn(
                    'bg-surface rounded-2xl shadow-md',
                    'border border-border overflow-hidden'
                )}>
                    <div className="flex">
                        {/* 좌측: 미팅 리스트 + 탭 */}
                        <div className={cn(
                            'w-80 flex-shrink-0 relative flex flex-col',
                            'bg-background/70'
                        )}>
                            {/* 검색 */}
                            <div className="p-4 border-b border-border">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary" size={16} />
                                    <input
                                        type="text"
                                        placeholder="미팅 검색..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        className={cn(
                                            'w-full pl-9 pr-4 py-2.5 text-sm',
                                            'border border-border rounded-google',
                                            'focus:border-primary focus:outline-none transition-colors'
                                        )}
                                    />
                                </div>
                            </div>

                            {/* 미팅 리스트 */}
                            <div className="flex-1 overflow-y-auto max-h-[500px]">
                                {filteredReports.map((report) => {
                                    const isSelected = selectedReport?.id === report.id;
                                    return (
                                        <button
                                            key={report.id}
                                            onClick={() => selectReport(report)}
                                            className={cn(
                                                'w-full p-4 text-left transition-all border-b border-border',
                                                conditionalClasses.state(
                                                    isSelected,
                                                    'bg-surface border-l-4 border-l-primary -mr-px z-10',
                                                    'hover:bg-surface-hover'
                                                )
                                            )}
                                        >
                                            <h4 className="font-bold text-text-primary text-sm mb-0.5">
                                                {report.studyName}
                                            </h4>
                                            <p className="text-xs text-text-secondary truncate">
                                                {report.meetingTitle}
                                            </p>
                                            <div className="flex items-center gap-2 mt-2 text-xs text-text-tertiary">
                                                <Calendar size={12} />
                                                <span>{report.date}</span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>

                            {/* 탭 네비게이션 */}
                            <div className="border-t border-border">
                                {TABS.map(tab => {
                                    const isActive = activeTab === tab.id;
                                    return (
                                        <button
                                            key={tab.id}
                                            onClick={() => setActiveTab(tab.id)}
                                            className={cn(
                                                'w-full flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors relative',
                                                conditionalClasses.state(
                                                    isActive,
                                                    'text-primary bg-surface -mr-px z-10',
                                                    'text-text-secondary hover:text-text-primary hover:bg-surface-hover border-b border-border'
                                                )
                                            )}
                                        >
                                            {isActive && (
                                                <motion.div
                                                    layoutId="stt-tab-indicator"
                                                    className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
                                                />
                                            )}
                                            <tab.icon size={16} />
                                            <span>{tab.label}</span>
                                        </button>
                                    );
                                })}
                            </div>

                            {/* 우측 border line */}
                            <div className="absolute top-0 right-0 bottom-0 w-px bg-border" />
                        </div>

                        {/* 우측 콘텐츠 영역 */}
                        <div className="flex-1 p-8">
                            {!selectedReport ? (
                                <div className="flex items-center justify-center h-64 text-text-tertiary">
                                    {isLoading ? '로딩 중...' : '미팅을 선택해주세요'}
                                </div>
                            ) : (
                            <>
                            {/* 선택된 미팅 헤더 */}
                            <div className="mb-6 pb-6 border-b border-border">
                                <div className="flex items-center justify-between mb-2">
                                    <h2 className="text-xl font-bold text-text-primary">
                                        {selectedReport.meetingTitle}
                                    </h2>

                                    {/* 텍스트 크기 조절 */}
                                    <div className={cn(
                                        'flex items-center gap-0.5 p-0.5 rounded-google',
                                        'bg-background border border-border'
                                    )}>
                                        <button
                                            onClick={() => setTextSize('sm')}
                                            className={cn(
                                                'p-1.5 rounded transition-colors',
                                                textSize === 'sm'
                                                    ? 'bg-surface text-primary shadow-sm'
                                                    : 'text-text-tertiary hover:text-text-secondary'
                                            )}
                                            aria-label="작은 글씨"
                                            title="작게"
                                        >
                                            <AArrowDown size={14} />
                                        </button>
                                        <button
                                            onClick={() => setTextSize('base')}
                                            className={cn(
                                                'p-1.5 rounded transition-colors',
                                                textSize === 'base'
                                                    ? 'bg-surface text-primary shadow-sm'
                                                    : 'text-text-tertiary hover:text-text-secondary'
                                            )}
                                            aria-label="보통 글씨"
                                            title="보통"
                                        >
                                            <Type size={14} />
                                        </button>
                                        <button
                                            onClick={() => setTextSize('lg')}
                                            className={cn(
                                                'p-1.5 rounded transition-colors',
                                                textSize === 'lg'
                                                    ? 'bg-surface text-primary shadow-sm'
                                                    : 'text-text-tertiary hover:text-text-secondary'
                                            )}
                                            aria-label="큰 글씨"
                                            title="크게"
                                        >
                                            <AArrowUp size={14} />
                                        </button>
                                    </div>
                                </div>
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
                                            className={cn(
                                                'px-2.5 py-1 text-xs rounded-full',
                                                'bg-background text-text-secondary'
                                            )}
                                        >
                                            {name}
                                        </span>
                                    ))}
                                </div>
                            </div>

                            {/* 탭 콘텐츠 (zoom으로 텍스트 크기 조절) */}
                            <div style={{ zoom: textSize === 'sm' ? 0.9 : textSize === 'base' ? 1 : 1.12 }}>
                            <AnimatePresence mode="wait">
                                {activeTab === 'summary' && (
                                    <motion.div key="summary" {...TAB_ANIMATION}>
                                        <SummaryView
                                            report={selectedReport}
                                            onSummaryChange={handleSummaryChange}
                                            onTabChange={setActiveTab}
                                        />
                                    </motion.div>
                                )}

                                {activeTab === 'transcript' && (
                                    <motion.div key="transcript" {...TAB_ANIMATION}>
                                        <TranscriptView
                                            report={selectedReport}
                                            onTranscriptChange={handleTranscriptChange}
                                        />
                                    </motion.div>
                                )}

                                {activeTab === 'action' && (
                                    <motion.div key="action" {...TAB_ANIMATION}>
                                        <ActionItemsView report={selectedReport} />
                                    </motion.div>
                                )}

                                {activeTab === 'stats' && (
                                    <motion.div key="stats" {...TAB_ANIMATION}>
                                        <StatsView reports={reports} />
                                    </motion.div>
                                )}
                            </AnimatePresence>
                            </div>
                            </>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default STTReportPage;
