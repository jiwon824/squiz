import React, { useState, useMemo } from 'react';
import { Guess } from '../hooks/useCommentleGame';
import { History, Hash, Target } from 'lucide-react';
import { cn, classBuilder } from '@/shared/utils/cn';

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
        <section className={cn(classBuilder.card('elevated'), 'p-4')}>
            <div className="flex items-center justify-between mb-3 pb-2.5 border-b border-border-light">
                <div className="flex items-center gap-2.5">
                    <div className="bg-primary/10 p-1.5 rounded-lg text-primary">
                        <History size={16} />
                    </div>
                    <h3 className="text-base font-bold text-text-primary">도전 기록</h3>
                </div>
                <div className="flex items-center gap-1.5">
                    <div className="px-3 py-1 bg-background-secondary rounded-full text-xs font-bold text-text-tertiary">
                        TOTAL {guesses.length}
                    </div>
                    {/* 정렬 토글 */}
                    <button
                        onClick={() => setSortOrder('input')}
                        className={cn(
                            'px-3 py-1 rounded-full text-xs font-bold transition-all',
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
                            'px-3 py-1 rounded-full text-xs font-bold transition-all',
                            sortOrder === 'similarity'
                                ? 'bg-primary text-white'
                                : 'bg-background-secondary text-text-tertiary hover:text-text-secondary'
                        )}
                    >
                        유사도순
                    </button>
                </div>
            </div>

            <div className="space-y-2">
                {guesses.length > 0 ? (
                    sortedGuesses.map((guess, index) => (
                        <div
                            key={`${sortOrder}-${guess.id}-${index}`}
                            className="flex items-center justify-between p-3 bg-background-secondary/50 hover:bg-white hover:shadow-md hover:border-primary/20 border border-transparent rounded-xl transition-all group animate-[slideInUp_0.3s_ease-out]"
                        >
                            <div className="flex items-center gap-3">
                                <span className="font-mono text-xs font-bold text-text-tertiary w-6">#{guess.attemptNum}</span>
                                <span className="text-base font-bold text-text-primary">{guess.word}</span>
                            </div>

                            <div className="flex items-center gap-3">
                                <div className="flex flex-col items-end">
                                    <span className={cn("text-lg font-black tracking-tighter", getScoreColor(guess.score))}>
                                        {guess.score.toFixed(2)}
                                    </span>
                                    <div className="flex items-center gap-1 text-[10px] font-bold text-text-tertiary uppercase">
                                        <Target size={10} />
                                        Similarity
                                    </div>
                                </div>
                                <div className={cn("w-1.5 h-9 rounded-full", getScoreColor(guess.score).replace('text-', 'bg-'))} />
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="flex flex-col items-center justify-center text-center p-8 text-text-tertiary">
                        <div className="w-14 h-14 bg-background-secondary rounded-full flex items-center justify-center mb-3">
                            <Hash size={28} />
                        </div>
                        <p className="text-sm font-medium">첫 번째 단어를 입력하여<br />게임을 시작하세요!</p>
                    </div>
                )}
            </div>
        </section>
    );
};
