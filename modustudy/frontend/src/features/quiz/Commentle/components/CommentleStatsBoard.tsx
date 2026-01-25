import React from 'react';
import { Crown, Info, Trophy, Clock } from 'lucide-react';

interface LeaderboardEntry {
    nickname: string;
    attempts: number;
    time: number;
    rank?: number;
}

interface CommentleStatsBoardProps {
    leaderboard: LeaderboardEntry[];
}

export const CommentleStatsBoard: React.FC<CommentleStatsBoardProps> = ({ leaderboard }) => {
    const scoreGuide = [
        { range: '90~100', color: 'var(--color-google-green)', label: '정답!' },
        { range: '75~89', color: 'var(--color-google-blue)', label: '아주 좋음' },
        { range: '50~74', color: '#a855f7', label: '좋은 방향' },
        { range: '25~49', color: 'var(--color-google-yellow)', label: '보통' },
        { range: '0~24', color: 'var(--color-google-red)', label: '멀어요' },
    ];

    return (
        <div className="flex flex-col gap-6">
            {/* 리더보드 카드 */}
            <div className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm overflow-hidden">
                <div className="flex items-center gap-3 mb-6">
                    <div className="bg-warning/10 p-2 rounded-xl text-warning-dark">
                        <Crown size={20} />
                    </div>
                    <h3 className="font-bold text-text-primary">명예의 전당</h3>
                    <span className="ml-auto text-xs font-bold text-text-tertiary">TOP 10</span>
                </div>

                <div className="space-y-2">
                    {leaderboard.length > 0 ? (
                        leaderboard.map((entry, index) => (
                            <div
                                key={index}
                                className="flex items-center gap-3 p-3 rounded-2xl hover:bg-background-secondary transition-colors"
                            >
                                <div className="w-8 h-8 flex items-center justify-center rounded-lg font-black text-sm bg-white border border-border-light shadow-xs">
                                    {entry.rank || index + 1}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="font-bold text-text-primary truncate">{entry.nickname}</div>
                                    <div className="flex items-center gap-3 text-[10px] font-bold text-text-tertiary uppercase">
                                        <span className="flex items-center gap-1"><Trophy size={10} /> {entry.attempts}회</span>
                                        <span className="flex items-center gap-1"><Clock size={10} /> {entry.time}초</span>
                                    </div>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="text-center py-8 text-text-tertiary text-sm italic">
                            🏆 아직 정복자가 없습니다!<br />첫 번째 주인공이 되어보세요.
                        </div>
                    )}
                </div>
            </div>

            {/* 점수 기준 가이드 카드 */}
            <div className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                    <Info size={16} className="text-text-tertiary" />
                    <h4 className="text-sm font-bold text-text-primary uppercase tracking-tight">유사도 점수 기준</h4>
                </div>
                <div className="grid grid-cols-1 gap-2">
                    {scoreGuide.map((item, i) => (
                        <div key={i} className="flex items-center justify-between p-2.5 rounded-xl bg-background-secondary/50 border border-transparent hover:border-border-light transition-all">
                            <div className="flex items-center gap-3">
                                <div className="w-2.5 h-2.5 rounded-full" style={{ background: item.color }} />
                                <span className="text-xs font-bold text-text-secondary">{item.range}</span>
                            </div>
                            <span className="text-xs font-black text-text-primary uppercase">{item.label}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};
