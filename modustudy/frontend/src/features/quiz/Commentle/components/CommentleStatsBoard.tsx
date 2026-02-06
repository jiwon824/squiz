import React from 'react';
import { Crown, Trophy, Clock, LogIn } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { cn, classBuilder } from '@/shared/utils/cn';

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
    const { isLoggedIn } = useAuthStore();

    return (
        <div className={cn(classBuilder.card('elevated'), 'p-4 overflow-hidden min-h-[400px] lg:min-h-0 flex flex-col')}>
            <div className="flex items-center gap-2.5 mb-4">
                <div className="bg-warning/10 p-1.5 rounded-lg text-warning-dark">
                    <Crown size={16} />
                </div>
                <h3 className="text-base font-bold text-text-primary">명예의 전당</h3>
                <span className="ml-auto text-[10px] font-bold text-text-tertiary">TOP 10</span>
            </div>

            {/* 비로그인 안내 배너 */}
            {!isLoggedIn && (
                <div className="mb-3 p-3 bg-white/60 backdrop-blur-sm rounded-lg border border-warning/20">
                    <div className="flex items-start gap-2.5">
                        <div className="bg-warning/10 p-1 rounded-md text-amber-500 mt-0.5">
                            <LogIn size={14} />
                        </div>
                        <div className="space-y-0.5">
                            <p className="text-xs font-bold text-text-primary">로그인하면 기록이 등록돼요</p>
                            <p className="text-[10px] text-text-tertiary leading-relaxed">
                                지금도 퀴즈를 풀 수 있지만, 리더보드에 기록을 남기려면 로그인이 필요합니다.
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {leaderboard.length > 0 ? (
                <div className="space-y-1.5 overflow-y-auto">
                    {leaderboard.map((entry, index) => (
                        <div
                            key={index}
                            className="flex items-center gap-2.5 p-2.5 rounded-xl hover:bg-background-secondary transition-colors"
                        >
                            <div className="w-7 h-7 flex items-center justify-center rounded-lg font-black text-xs bg-white border border-border-light shadow-xs">
                                {entry.rank || index + 1}
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="font-bold text-sm text-text-primary truncate">{entry.nickname}</div>
                                <div className="flex items-center gap-2.5 text-[10px] font-bold text-text-tertiary uppercase">
                                    <span className="flex items-center gap-1"><Trophy size={10} /> {entry.attempts}회</span>
                                    <span className="flex items-center gap-1"><Clock size={10} /> {entry.time}초</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-center text-text-secondary">
                    <p className="text-base font-medium leading-relaxed">
                        🏆 아직 정복자가 없습니다!<br />첫 번째 주인공이 되어보세요.
                    </p>
                </div>
            )}
        </div>
    );
};
