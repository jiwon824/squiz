// 인사이트 헤더: 키워드 + 요약 편집 + 화자 발언 비율 PieChart

import React, { useMemo } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { cn } from '@/shared/utils/cn';
import type { MeetingReport, SpeakerStats } from './types';
import { SPEAKER_COLORS, getSpeakerColorIndex } from './constants';
import { EditableSummary } from './EditableSummary';

interface InsightHeaderProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 요약 수정 콜백 */
    onSummaryChange: (newSummary: string) => void;
    /** 추가 클래스명 */
    className?: string;
}

export const InsightHeader: React.FC<InsightHeaderProps> = ({
    report,
    onSummaryChange,
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

    return (
        <div className={cn('grid grid-cols-1 lg:grid-cols-3 gap-6', className)}>
            {/* 좌측: 키워드 + 요약 */}
            <div className="lg:col-span-2 space-y-5">
                {/* 키워드 해시태그 */}
                <div>
                    <h3 className="text-sm font-semibold text-text-primary mb-3">핵심 키워드</h3>
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
                    <h3 className="text-sm font-semibold text-text-primary mb-3">AI 요약</h3>
                    <EditableSummary
                        summary={report.summary}
                        onSave={onSummaryChange}
                    />
                </div>
            </div>

            {/* 우측: PieChart (화자별 발언 비율) */}
            <div className="flex flex-col items-center">
                <h3 className="text-sm font-semibold text-text-primary mb-3 self-start">발언 비율</h3>
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
                            >
                                {speakerStats.map((entry) => (
                                    <Cell
                                        key={entry.name}
                                        fill={SPEAKER_COLORS[getSpeakerColorIndex(entry.name, report.participants)]}
                                    />
                                ))}
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

                {/* 커스텀 범례 */}
                <div className="flex flex-wrap justify-center gap-x-4 gap-y-1.5 mt-2">
                    {speakerStats.map((stat) => (
                        <div key={stat.name} className="flex items-center gap-1.5 text-xs text-text-secondary">
                            <span
                                className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                                style={{
                                    backgroundColor: SPEAKER_COLORS[getSpeakerColorIndex(stat.name, report.participants)],
                                }}
                            />
                            <span>{stat.name}</span>
                            <span className="text-text-tertiary">{stat.percentage}%</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};
