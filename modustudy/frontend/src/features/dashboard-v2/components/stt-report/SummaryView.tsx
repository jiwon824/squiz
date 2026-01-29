// 요약 뷰: 대시보드형 메인 (InsightHeader + 주요 내용 + 액션 아이템 미리보기)

import React from 'react';
import { ChevronRight, ListChecks } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { MeetingReport, TabType } from './types';
import { InsightHeader } from './InsightHeader';

interface SummaryViewProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 요약 수정 콜백 */
    onSummaryChange: (newSummary: string) => void;
    /** 탭 전환 콜백 (액션 아이템 "전체 보기" 등) */
    onTabChange?: (tab: TabType) => void;
    /** 추가 클래스명 */
    className?: string;
}

export const SummaryView: React.FC<SummaryViewProps> = ({
    report,
    onSummaryChange,
    onTabChange,
    className,
}) => {
    return (
        <div className={cn('space-y-6', className)}>
            {/* 상단: InsightHeader (키워드 + 요약 + PieChart) */}
            <div className={cn(
                'rounded-xl border border-border overflow-hidden',
                'bg-surface p-6'
            )}>
                <InsightHeader
                    report={report}
                    onSummaryChange={onSummaryChange}
                />
            </div>

            {/* 하단: 2컬럼 그리드 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 좌: 주요 내용 */}
                <div className="rounded-xl border border-border overflow-hidden">
                    <div className={cn(
                        'px-5 py-4 border-b border-border',
                        'bg-background/50'
                    )}>
                        <h3 className="font-semibold text-text-primary mb-0">주요 내용</h3>
                    </div>
                    <div className="p-5">
                        <ul className="space-y-3">
                            {report.highlights.map((highlight, idx) => (
                                <li key={idx} className="flex items-start gap-3 text-text-secondary text-sm">
                                    <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                    <span>{highlight}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>

                {/* 우: 액션 아이템 미리보기 */}
                <div className="rounded-xl border border-border overflow-hidden">
                    <div className={cn(
                        'px-5 py-4 border-b border-border',
                        'bg-background/50 flex items-center justify-between'
                    )}>
                        <h3 className="font-semibold text-text-primary mb-0">액션 아이템</h3>
                        <span className="text-xs text-text-tertiary">
                            {report.actionItems.length}개
                        </span>
                    </div>
                    <div className="p-5">
                        <ul className="space-y-3">
                            {report.actionItems.slice(0, 3).map((item, idx) => (
                                <li key={idx} className="flex items-start gap-3 text-text-secondary text-sm">
                                    <ListChecks size={16} className="text-accent flex-shrink-0 mt-0.5" />
                                    <span>{item}</span>
                                </li>
                            ))}
                        </ul>
                        {report.actionItems.length > 3 && onTabChange && (
                            <button
                                onClick={() => onTabChange('action')}
                                className={cn(
                                    'mt-4 text-sm font-medium',
                                    'text-primary hover:text-primary-dark transition-colors',
                                    'flex items-center gap-1'
                                )}
                            >
                                전체 보기
                                <ChevronRight size={14} />
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};
