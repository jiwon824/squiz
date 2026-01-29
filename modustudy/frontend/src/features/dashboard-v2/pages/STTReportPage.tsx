// STT 미팅 리포트 페이지
// 서브컴포넌트로 분리된 대시보드형 레이아웃

import React, { useState, useCallback } from 'react';
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
} from 'lucide-react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
import {
    SummaryView,
    TranscriptView,
    ActionItemsView,
    StatsView,
    MOCK_REPORTS,
} from '../components/stt-report';
import type { TabType, MeetingReport, TranscriptItem } from '../components/stt-report';
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

    // 요약 변경 핸들러
    const handleSummaryChange = useCallback((newSummary: string) => {
        setSelectedReport(prev => ({ ...prev, summary: newSummary }));
    }, []);

    // 대화 기록 변경 핸들러
    const handleTranscriptChange = useCallback((updatedTranscript: TranscriptItem[]) => {
        setSelectedReport(prev => ({ ...prev, transcript: updatedTranscript }));
    }, []);

    return (
        <div className="py-8">
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 뒤로가기 헤더 */}
                <PageNavHeader
                    title="STT 미팅 리포트"
                    breadcrumbs={[
                        { label: '대시보드', path: '/dashboard' },
                        { label: 'STT 미팅 리포트' },
                    ]}
                    onBack={() => navigate(-1)}
                />

                {/* 좌측 사이드바 + 우측 콘텐츠 */}
                <div className={cn(
                    'bg-surface rounded-2xl shadow-md',
                    'border border-border overflow-hidden'
                )}>
                    <div className="flex">
                        {/* 좌측: 미팅 리스트 + 탭 */}
                        <div className={cn(
                            'w-72 flex-shrink-0 relative flex flex-col',
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
                                    const isSelected = selectedReport.id === report.id;
                                    return (
                                        <button
                                            key={report.id}
                                            onClick={() => setSelectedReport(report)}
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
                            {/* 선택된 미팅 헤더 */}
                            <div className="mb-6 pb-6 border-b border-border">
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

                            {/* 탭 콘텐츠 */}
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

export default STTReportPage;
