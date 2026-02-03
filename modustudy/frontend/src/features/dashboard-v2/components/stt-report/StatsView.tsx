// 통계 뷰: 요약 카드 + recharts BarChart 키워드 빈도

import React from 'react';
import { motion } from 'framer-motion';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    ResponsiveContainer,
    CartesianGrid,
} from 'recharts';
import {
    FileText,
    Users,
    MessageSquare,
    BarChart3,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { MeetingReport } from './types';

// 색상 스타일 상수 (디자인 토큰)
const STAT_CARD_STYLES = [
    { bgIcon: 'bg-primary/10', textIcon: 'text-primary' },
    { bgIcon: 'bg-secondary/10', textIcon: 'text-secondary' },
    { bgIcon: 'bg-accent/10', textIcon: 'text-accent' },
    { bgIcon: 'bg-warning/10', textIcon: 'text-warning' },
] as const;

interface StatsViewProps {
    /** 전체 리포트 배열 */
    reports: MeetingReport[];
    /** 추가 클래스명 */
    className?: string;
}

export const StatsView: React.FC<StatsViewProps> = ({
    reports,
    className,
}) => {
    // 통계 계산
    const totalMeetings = reports.length;
    // participantCount 우선 사용 (목록 조회 시에도 정확한 수 제공)
    const totalParticipants = reports.reduce((sum, r) =>
        sum + (r.participantCount > 0 ? r.participantCount : r.participants.length), 0);
    const avgParticipants = totalMeetings > 0 ? (totalParticipants / totalMeetings).toFixed(1) : '0';
    const totalKeywords = reports.reduce((sum, r) => sum + r.keywords.length, 0);

    // 스터디별 미팅 횟수
    const studyStats = reports.reduce((acc, r) => {
        acc[r.studyName] = (acc[r.studyName] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    // 키워드 빈도 (BarChart 데이터)
    const keywordStats = reports.flatMap(r => r.keywords).reduce((acc, k) => {
        acc[k] = (acc[k] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    const topKeywords = Object.entries(keywordStats)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 8)
        .map(([keyword, count]) => ({ keyword, count }));

    // 요약 카드 데이터
    const statCards = [
        { label: '총 미팅 수', value: totalMeetings, icon: FileText, styleIdx: 0 },
        { label: '평균 참여자', value: avgParticipants, icon: Users, styleIdx: 1 },
        { label: '총 키워드', value: totalKeywords, icon: MessageSquare, styleIdx: 2 },
        { label: '스터디 그룹', value: Object.keys(studyStats).length, icon: BarChart3, styleIdx: 3 },
    ];

    return (
        <div className={cn('space-y-6', className)}>
            {/* 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                {statCards.map((stat) => {
                    const style = STAT_CARD_STYLES[stat.styleIdx];
                    return (
                        <div
                            key={stat.label}
                            className="rounded-xl border border-border px-4 py-4"
                        >
                            <div className="flex items-center gap-3">
                                <div className={cn(
                                    'w-10 h-10 rounded-xl flex items-center justify-center',
                                    style.bgIcon
                                )}>
                                    <stat.icon className={style.textIcon} size={18} />
                                </div>
                                <div>
                                    <div className="text-xl font-bold text-text-primary">{stat.value}</div>
                                    <div className="text-xs text-text-tertiary">{stat.label}</div>
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* 스터디별 미팅 */}
            <div className="rounded-xl border border-border overflow-hidden">
                <div className={cn(
                    'px-5 py-4 border-b border-border',
                    'bg-background/50'
                )}>
                    <h3 className="font-semibold text-text-primary mb-0">스터디별 미팅 횟수</h3>
                </div>
                <div className="p-5 space-y-4">
                    {Object.entries(studyStats).map(([study, count]) => (
                        <div key={study}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-sm font-medium text-text-primary">{study}</span>
                                <span className="text-sm text-text-tertiary">{count}회</span>
                            </div>
                            <div className="w-full bg-background rounded-full h-2.5 overflow-hidden">
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

            {/* 자주 언급된 키워드 - BarChart */}
            <div className="rounded-xl border border-border overflow-hidden">
                <div className={cn(
                    'px-5 py-4 border-b border-border',
                    'bg-background/50'
                )}>
                    <h3 className="font-semibold text-text-primary mb-0">자주 언급된 키워드</h3>
                </div>
                <div className="p-5">
                    {topKeywords.length > 0 ? (
                        <div style={{ width: '100%', height: 300 }}>
                            <ResponsiveContainer>
                                <BarChart
                                    data={topKeywords}
                                    layout="vertical"
                                    margin={{ top: 5, right: 30, left: 80, bottom: 5 }}
                                >
                                    <CartesianGrid
                                        strokeDasharray="3 3"
                                        stroke="var(--color-border)"
                                    />
                                    <XAxis
                                        type="number"
                                        allowDecimals={false}
                                        tick={{ fill: 'var(--color-text-secondary)', fontSize: 12 }}
                                    />
                                    <YAxis
                                        type="category"
                                        dataKey="keyword"
                                        width={70}
                                        tick={{ fill: 'var(--color-text-primary)', fontSize: 12 }}
                                    />
                                    <Tooltip
                                        formatter={(value: number) => [`${value}회`, '언급 횟수']}
                                        contentStyle={{
                                            borderRadius: '8px',
                                            border: '1px solid var(--color-border)',
                                            fontSize: '12px',
                                        }}
                                    />
                                    <Bar
                                        dataKey="count"
                                        fill="var(--color-primary)"
                                        radius={[0, 4, 4, 0]}
                                        barSize={20}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    ) : (
                        <p className="text-text-tertiary text-sm text-center py-8">
                            키워드 데이터가 없습니다.
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};
