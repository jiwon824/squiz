import React from 'react';
import { Crown, Trophy, Clock } from 'lucide-react';

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
    return (
        <div className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm overflow-hidden min-h-[600px] flex flex-col">
            <div className="flex items-center gap-3 mb-6">
                <div className="bg-warning/10 p-2 rounded-xl text-warning-dark">
                    <Crown size={20} />
                </div>
                <h3 className="text-xl font-bold text-text-primary">명예의 전당</h3>
                <span className="ml-auto text-xs font-bold text-text-tertiary">TOP 10</span>
            </div>

            {leaderboard.length > 0 ? (
                <div className="space-y-2">
                    {leaderboard.map((entry, index) => (
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
                    ))}
                </div>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-center text-text-secondary">
                    <p className="text-xl font-medium leading-relaxed">
                        🏆 아직 정복자가 없습니다!<br />첫 번째 주인공이 되어보세요.
                    </p>
                </div>
            )}
        </div>
    );
};
