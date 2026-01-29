// 요약 뷰: 대시보드형 메인 (InsightHeader + 주요 내용 + 액션 아이템 미리보기)
// PieChart 화자 선택 시 주요 내용에서 해당 화자 관련 항목 하이라이트

import React, { useState, useMemo } from 'react';
import { ChevronRight, ListChecks, Sparkles } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { MeetingReport, TabType } from './types';
import { getSpeakerClasses } from './constants';
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
    // 화자 필터 상태 (PieChart/범례 클릭으로 제어)
    const [selectedSpeaker, setSelectedSpeaker] = useState<string | null>(null);

    // 선택된 화자의 발언에서 언급된 키워드가 포함된 하이라이트를 찾아 강조
    const highlightedIndices = useMemo(() => {
        if (!selectedSpeaker) return new Set<number>();

        // 선택된 화자의 발언 텍스트 모음
        const speakerTexts = report.transcript
            .filter(t => t.speaker === selectedSpeaker)
            .map(t => t.text.toLowerCase());

        const indices = new Set<number>();
        report.highlights.forEach((highlight, idx) => {
            const h = highlight.toLowerCase();
            // 화자의 발언에 하이라이트 관련 단어가 포함되어 있는지 확인
            const isRelated = speakerTexts.some(text =>
                // 하이라이트의 핵심 단어(4자 이상)가 발언에 포함되면 관련으로 판정
                h.split(/\s+/).filter(w => w.length >= 4).some(word => text.includes(word))
            );
            if (isRelated) indices.add(idx);
        });
        return indices;
    }, [selectedSpeaker, report.transcript, report.highlights]);

    // 선택된 화자의 색상 클래스
    const speakerColor = selectedSpeaker
        ? getSpeakerClasses(selectedSpeaker, report.participants)
        : null;

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
                    selectedSpeaker={selectedSpeaker}
                    onSpeakerSelect={setSelectedSpeaker}
                />
            </div>

            {/* 하단: 2컬럼 그리드 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 좌: 주요 내용 */}
                <div className="rounded-xl border border-border overflow-hidden">
                    <div className={cn(
                        'px-5 py-4 border-b border-border',
                        'bg-background/50 flex items-center justify-between'
                    )}>
                        <div className="flex items-baseline gap-2">
                            <h3 className="font-semibold text-text-primary mb-0">주요 내용</h3>
                            <span className={cn(
                                'inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-semibold rounded-pill',
                                'bg-primary/10 text-[var(--color-primary)]'
                            )}>
                                <Sparkles size={10} />
                                AI 분석
                            </span>
                        </div>
                        {selectedSpeaker && speakerColor && (
                            <span className={cn(
                                'text-xs px-2.5 py-1 rounded-pill font-semibold',
                                speakerColor.text
                            )}>
                                {selectedSpeaker} 관련
                            </span>
                        )}
                    </div>
                    <div className="p-5 max-h-[320px] overflow-y-auto">
                        <ul className="space-y-4">
                            {report.highlights.map((highlight, idx) => {
                                const isHighlighted = selectedSpeaker && highlightedIndices.has(idx);
                                const isDimmed = selectedSpeaker && !highlightedIndices.has(idx);
                                return (
                                    <li
                                        key={idx}
                                        className={cn(
                                            'flex items-start gap-3 text-sm rounded-google p-2 -m-2 transition-all',
                                            {
                                                'bg-primary/5 ring-1 ring-primary/20': isHighlighted,
                                                'opacity-40': isDimmed,
                                            }
                                        )}
                                    >
                                        <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                        <span className={cn(
                                            isHighlighted ? 'text-text-primary' : 'text-text-secondary'
                                        )}>
                                            {highlight}
                                        </span>
                                    </li>
                                );
                            })}
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
                        <span className="text-xs px-2.5 py-1 font-semibold text-text-tertiary">
                            {report.actionItems.length}개
                        </span>
                    </div>
                    <div className="p-5 max-h-[320px] overflow-y-auto">
                        <ul className="space-y-5">
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
