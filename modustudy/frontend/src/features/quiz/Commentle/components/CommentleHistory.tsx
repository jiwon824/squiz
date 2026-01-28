import React, { useState, useMemo } from 'react';
import { Guess } from '../hooks/useCommentleGame';
import { History, Hash, Target } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface CommentleHistoryProps {
    guesses: Guess[];
}

type SortOrder = 'input' | 'similarity';

export const CommentleHistory: React.FC<CommentleHistoryProps> = ({ guesses }) => {
    const [sortOrder, setSortOrder] = useState<SortOrder>('input');

    // 점수에 따른 색상 매핑
    const getScoreColor = (score: number) => {
        if (score >= 90) return 'text-quiz-success';
        if (score >= 75) return 'text-quiz-info';
        if (score >= 50) return 'text-quiz-focus';
        if (score >= 25) return 'text-quiz-warning';
        return 'text-quiz-danger';
    };

    // 정렬된 guesses
    const sortedGuesses = useMemo(() => {
        const sorted = [...guesses];
        if (sortOrder === 'similarity') {
            sorted.sort((a, b) => b.score - a.score);
        }
        return sorted;
    }, [guesses, sortOrder]);

    return (
        <section className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-6 pb-4 border-b border-border-light">
                <div className="flex items-center gap-3">
                    <div className="bg-primary/10 p-2 rounded-xl text-primary">
                        <History size={20} />
                    </div>
                    <h3 className="text-xl font-bold text-text-primary">도전 기록</h3>
                </div>
                <div className="flex items-center gap-2">
                    <div className="px-4 py-1.5 bg-background-secondary rounded-full text-sm font-bold text-text-tertiary">
                        TOTAL {guesses.length}
                    </div>
                    {/* 정렬 토글 */}
                    <button
                        onClick={() => setSortOrder('input')}
                        className={cn(
                            'px-4 py-1.5 rounded-full text-sm font-bold transition-all',
                            sortOrder === 'input'
                                ? 'bg-primary text-white'
                                : 'bg-background-secondary text-text-tertiary hover:text-text-secondary'
                        )}
                    >
                        입력순
                    </button>
                    <button
                        onClick={() => setSortOrder('similarity')}
                        className={cn(
                            'px-4 py-1.5 rounded-full text-sm font-bold transition-all',
                            sortOrder === 'similarity'
                                ? 'bg-primary text-white'
                                : 'bg-background-secondary text-text-tertiary hover:text-text-secondary'
                        )}
                    >
                        유사도순
                    </button>
                </div>
            </div>

            <div className="space-y-3">
                {guesses.length > 0 ? (
                    sortedGuesses.map((guess, index) => (
                        <div
                            key={`${sortOrder}-${guess.id}-${index}`}
                            className="flex items-center justify-between p-5 bg-background-secondary/50 hover:bg-white hover:shadow-md hover:border-primary/20 border border-transparent rounded-2xl transition-all group animate-[slideInUp_0.3s_ease-out]"
                        >
                            <div className="flex items-center gap-4">
                                <span className="font-mono text-sm font-bold text-text-tertiary w-7">#{guess.attemptNum}</span>
                                <span className="text-xl font-bold text-text-primary">{guess.word}</span>
                            </div>

                            <div className="flex items-center gap-4">
                                <div className="flex flex-col items-end">
                                    <span className={cn("text-2xl font-black tracking-tighter", getScoreColor(guess.score))}>
                                        {guess.score.toFixed(2)}
                                    </span>
                                    <div className="flex items-center gap-1 text-xs font-bold text-text-tertiary uppercase">
                                        <Target size={12} />
                                        Similarity
                                    </div>
                                </div>
                                <div className={cn("w-2 h-12 rounded-full", getScoreColor(guess.score).replace('text-', 'bg-'))} />
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="flex flex-col items-center justify-center text-center p-12 text-text-tertiary">
                        <div className="w-20 h-20 bg-background-secondary rounded-full flex items-center justify-center mb-4">
                            <Hash size={40} />
                        </div>
                        <p className="text-lg font-medium">첫 번째 단어를 입력하여<br />게임을 시작하세요!</p>
                    </div>
                )}
            </div>
        </section>
    );
};
