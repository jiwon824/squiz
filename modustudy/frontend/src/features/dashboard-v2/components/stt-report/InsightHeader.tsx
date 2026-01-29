// 인사이트 헤더: 키워드 + 요약 편집 + 화자 발언 비율 PieChart
// PieChart 섹터/범례 클릭 시 화자 필터링 인터랙션 제공

import React, { useMemo } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import type { MeetingReport, SpeakerStats } from './types';
import { SPEAKER_COLORS, getSpeakerColorIndex } from './constants';
import { EditableSummary } from './EditableSummary';

interface InsightHeaderProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 요약 수정 콜백 */
    onSummaryChange: (newSummary: string) => void;
    /** 현재 선택된 화자 (null이면 전체) */
    selectedSpeaker: string | null;
    /** 화자 선택/해제 콜백 */
    onSpeakerSelect: (speaker: string | null) => void;
    /** 추가 클래스명 */
    className?: string;
}

export const InsightHeader: React.FC<InsightHeaderProps> = ({
    report,
    onSummaryChange,
    selectedSpeaker,
    onSpeakerSelect,
    className,
}) => {
    // 화자별 발언 통계 계산
    const speakerStats: SpeakerStats[] = useMemo(() => {
        const counts: Record<string, number> = {};
        report.transcript.forEach((item) => {
            counts[item.speaker] = (counts[item.speaker] || 0) + 1;
        });
        const total = report.transcript.length;
        return Object.entries(counts).map(([name, count]) => ({
            name,
            count,
            percentage: total > 0 ? Math.round((count / total) * 100) : 0,
        }));
    }, [report.transcript]);

    // PieChart 섹터 클릭 핸들러
    const handlePieClick = (_: unknown, index: number) => {
        const clicked = speakerStats[index]?.name;
        if (!clicked) return;
        onSpeakerSelect(selectedSpeaker === clicked ? null : clicked);
    };

    // 범례 클릭 핸들러
    const handleLegendClick = (name: string) => {
        onSpeakerSelect(selectedSpeaker === name ? null : name);
    };

    return (
        <div className={cn('grid grid-cols-1 lg:grid-cols-3 gap-6', className)}>
            {/* 좌측: 키워드 + 요약 */}
            <div className="lg:col-span-2 space-y-5">
                {/* 키워드 해시태그 */}
                <div>
                    <h3 className="text-base font-semibold text-text-primary mb-3">핵심 키워드</h3>
                    <div className="flex flex-wrap gap-2">
                        {report.keywords.map((keyword, idx) => (
                            <span
                                key={idx}
                                className={cn(
                                    'px-3 py-1.5 text-sm font-medium rounded-pill',
                                    'bg-primary/10 text-primary'
                                )}
                            >
                                #{keyword}
                            </span>
                        ))}
                    </div>
                </div>

                {/* AI 요약 (편집 가능) */}
                <div>
                    <EditableSummary
                        summary={report.summary}
                        onSave={onSummaryChange}
                    />
                </div>
            </div>

            {/* 우측: PieChart (화자별 발언 비율) */}
            <div className="flex flex-col items-center">
                <div className="flex items-center gap-2 self-start mb-3">
                    <h3 className="text-base font-semibold text-text-primary mb-0">발언 비율</h3>
                    {selectedSpeaker && (
                        <button
                            onClick={() => onSpeakerSelect(null)}
                            className={cn(
                                'text-xs px-2.5 py-1 rounded-pill font-medium',
                                'bg-primary/10 text-[var(--color-primary)] hover:bg-primary/20 transition-colors'
                            )}
                        >
                            필터 해제
                        </button>
                    )}
                </div>
                <div className="w-full" style={{ height: 160 }}>
                    <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                            <Pie
                                data={speakerStats}
                                cx="50%"
                                cy="50%"
                                innerRadius={40}
                                outerRadius={65}
                                dataKey="count"
                                nameKey="name"
                                strokeWidth={2}
                                stroke="var(--color-surface)"
                                onClick={handlePieClick}
                                cursor="pointer"
                            >
                                {speakerStats.map((entry) => {
                                    const isSelected = !selectedSpeaker || selectedSpeaker === entry.name;
                                    return (
                                        <Cell
                                            key={entry.name}
                                            fill={SPEAKER_COLORS[getSpeakerColorIndex(entry.name, report.participants)]}
                                            opacity={isSelected ? 1 : 0.3}
                                        />
                                    );
                                })}
                            </Pie>
                            <Tooltip
                                formatter={(value: number, name: string) => [`${value}회`, name]}
                                contentStyle={{
                                    borderRadius: '8px',
                                    border: '1px solid var(--color-border)',
                                    fontSize: '12px',
                                }}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* 클릭 가능 범례 */}
                <div className="flex flex-wrap justify-center gap-x-4 gap-y-1.5 mt-2">
                    {speakerStats.map((stat) => {
                        const isSelected = !selectedSpeaker || selectedSpeaker === stat.name;
                        return (
                            <button
                                key={stat.name}
                                onClick={() => handleLegendClick(stat.name)}
                                className={cn(
                                    'flex items-center gap-1.5 text-xs rounded-pill px-2 py-0.5',
                                    'transition-all cursor-pointer',
                                    conditionalClasses.state(
                                        isSelected,
                                        'text-text-secondary hover:bg-surface-hover',
                                        'text-text-tertiary opacity-50 hover:opacity-75'
                                    ),
                                    selectedSpeaker === stat.name && 'bg-surface-hover ring-1 ring-border'
                                )}
                            >
                                <span
                                    className="w-2.5 h-2.5 rounded-full flex-shrink-0 transition-opacity"
                                    style={{
                                        backgroundColor: SPEAKER_COLORS[getSpeakerColorIndex(stat.name, report.participants)],
                                        opacity: isSelected ? 1 : 0.4,
                                    }}
                                />
                                <span>{stat.name}</span>
                                <span className="text-text-tertiary">{stat.percentage}%</span>
                            </button>
                        );
                    })}
                </div>
                <p className="text-[10px] text-text-tertiary mt-2">
                    클릭하여 화자별 내용을 필터링하세요
                </p>
            </div>
        </div>
    );
};
