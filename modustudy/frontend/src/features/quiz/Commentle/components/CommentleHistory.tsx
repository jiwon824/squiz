import React from 'react';
import { Guess } from '../hooks/useCommentleGame';
import { History, Hash, Target } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface CommentleHistoryProps {
    guesses: Guess[];
}

export const CommentleHistory: React.FC<CommentleHistoryProps> = ({ guesses }) => {
    // 점수에 따른 색상 매핑
    const getScoreColor = (score: number) => {
        if (score >= 90) return 'text-quiz-success';
        if (score >= 75) return 'text-quiz-info';
        if (score >= 50) return 'text-quiz-focus';
        if (score >= 25) return 'text-quiz-warning';
        return 'text-quiz-danger';
    };


    return (
        <section className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm overflow-hidden flex flex-col h-full min-h-[400px]">
            <div className="flex items-center justify-between mb-6 pb-4 border-b border-border-light">
                <div className="flex items-center gap-3">
                    <div className="bg-primary/10 p-2 rounded-xl text-primary">
                        <History size={20} />
                    </div>
                    <h3 className="font-bold text-text-primary">도전 기록</h3>
                </div>
                <div className="px-3 py-1 bg-background-secondary rounded-full text-xs font-bold text-text-tertiary">
                    TOTAL {guesses.length}
                </div>
            </div>

            <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-3">
                {guesses.length > 0 ? (
                    guesses.map((guess) => (
                        <div
                            key={guess.id}
                            className="flex items-center justify-between p-4 bg-background-secondary/50 hover:bg-white hover:shadow-md hover:border-primary/20 border border-transparent rounded-2xl transition-all group animate-[slideInUp_0.3s_ease-out]"
                        >
                            <div className="flex items-center gap-4">
                                <span className="font-mono text-xs font-bold text-text-tertiary w-6">#{guess.attemptNum}</span>
                                <span className="text-lg font-bold text-text-primary">{guess.word}</span>
                            </div>

                            <div className="flex items-center gap-4">
                                <div className="flex flex-col items-end">
                                    <span className={cn("text-xl font-black tracking-tighter", getScoreColor(guess.score))}>
                                        {guess.score.toFixed(2)}
                                    </span>
                                    <div className="flex items-center gap-1 text-[10px] font-bold text-text-tertiary uppercase">
                                        <Target size={10} />
                                        Similarity
                                    </div>
                                </div>
                                <div className={cn("w-1.5 h-10 rounded-full", getScoreColor(guess.score).replace('text-', 'bg-'))} />
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="h-full flex flex-col items-center justify-center text-center p-8 text-text-tertiary">
                        <div className="w-16 h-16 bg-background-secondary rounded-full flex items-center justify-center mb-4">
                            <Hash size={32} />
                        </div>
                        <p className="font-medium">첫 번째 단어를 입력하여<br />게임을 시작하세요!</p>
                    </div>
                )}
            </div>
        </section>
    );
};
