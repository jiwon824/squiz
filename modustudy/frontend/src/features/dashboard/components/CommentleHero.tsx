import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Send, Trophy, Flame, ArrowRight, Sparkles, Target } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { checkSimilarity, fetchDailyWord } from '@/features/quiz/services/quizService';

interface Guess {
    id: number;
    word: string;
    score: number;
}

const CommentleHero: React.FC = () => {
    const navigate = useNavigate();
    const inputRef = useRef<HTMLInputElement>(null);
    
    const [guess, setGuess] = useState('');
    const [guesses, setGuesses] = useState<Guess[]>([]);
    const [loading, setLoading] = useState(false);
    const [isCorrect, setIsCorrect] = useState(false);
    const [problemLoaded, setProblemLoaded] = useState(false);
    const [category, setCategory] = useState('CS');
    const [bestScore, setBestScore] = useState(0);

    // 컴포넌트 마운트 시 오늘의 문제 로드
    useEffect(() => {
        const loadProblem = async () => {
            try {
                const data = await fetchDailyWord();
                setCategory(data.category || 'CS');
                setProblemLoaded(true);
            } catch (error) {
                console.error('Failed to load problem:', error);
                setProblemLoaded(true);
            }
        };
        loadProblem();
    }, []);

    // 점수별 색상
    const getScoreColor = (score: number) => {
        if (score >= 90) return 'text-accent';
        if (score >= 75) return 'text-secondary';
        if (score >= 50) return 'text-primary';
        if (score >= 25) return 'text-warning';
        return 'text-error';
    };

    // 점수별 메시지
    const getScoreMessage = (score: number) => {
        if (score >= 90) return '🎉 정답!';
        if (score >= 75) return '🔥 아주 가까워요!';
        if (score >= 50) return '👍 좋은 방향!';
        if (score >= 25) return '🤔 힌트: 더 구체적으로';
        return '❄️ 조금 멀어요';
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!guess.trim() || loading || isCorrect) return;

        setLoading(true);
        try {
            const result = await checkSimilarity(guess.trim());
            
            const newGuess: Guess = {
                id: Date.now(),
                word: result.userWord || guess,
                score: result.score,
            };

            setGuesses(prev => [newGuess, ...prev]);
            setBestScore(prev => Math.max(prev, result.score));
            setGuess('');

            if (result.isCorrect || result.score >= 90) {
                setIsCorrect(true);
            }
        } catch (error) {
            console.error('Error checking similarity:', error);
        } finally {
            setLoading(false);
            inputRef.current?.focus();
        }
    };

    return (
        <section className="relative bg-gradient-to-br from-primary/5 via-secondary/5 to-accent/5 rounded-2xl overflow-hidden border border-gray-100 shadow-md">
            <div className="p-8">
                {/* 헤더 */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-3">
                        <div className={cn(
                            'flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold',
                            'bg-primary/10 text-primary'
                        )}>
                            <Sparkles size={14} />
                            오늘의 퀴즈
                        </div>
                        <span className="px-3 py-1.5 bg-gray-100 rounded-full text-xs font-medium text-gray-700">
                            {category}
                        </span>
                    </div>
                    <button
                        onClick={() => navigate('/commentle')}
                        className={cn(
                            'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold transition-all',
                            'bg-white hover:bg-gray-50 text-primary border border-gray-200',
                            'hover:shadow-md hover:scale-105'
                        )}
                    >
                        <span>전체 게임</span>
                        <ArrowRight size={16} />
                    </button>
                </div>

                {/* 타이틀 */}
                <div className="mb-6">
                    <h2 className="text-3xl font-black text-text-primary mb-2">
                        꼬멘틀 <span className="text-primary">CS ver</span>
                    </h2>
                    <p className="text-text-secondary">
                        AI가 분석하는 의미 유사도 퀴즈! 100점에 가까울수록 정답입니다.
                    </p>
                </div>

                {/* 입력 폼 */}
                <form onSubmit={handleSubmit} className="mb-6">
                    <div className={cn(
                        'flex items-center gap-3 p-2 rounded-xl border-2 transition-all',
                        'bg-white',
                        isCorrect ? 'border-accent' : 'border-gray-200 hover:border-gray-300 focus-within:border-primary'
                    )}>
                        <Target className="ml-3 text-gray-400" size={20} />
                        <input
                            ref={inputRef}
                            type="text"
                            value={guess}
                            onChange={(e) => setGuess(e.target.value)}
                            placeholder={isCorrect ? "정답을 맞추셨습니다! 🎉" : "CS 용어를 입력하세요..."}
                            disabled={isCorrect || loading}
                            className={cn(
                                'flex-1 px-3 py-3 bg-transparent border-none outline-none',
                                'text-text-primary placeholder:text-gray-400',
                                'disabled:opacity-60'
                            )}
                            autoComplete="off"
                        />
                        <button
                            type="submit"
                            disabled={!guess.trim() || loading || isCorrect}
                            className={cn(
                                'w-11 h-11 rounded-lg flex items-center justify-center transition-all',
                                'bg-primary hover:bg-primary-dark text-white',
                                'disabled:opacity-50 disabled:cursor-not-allowed',
                                'hover:scale-105 active:scale-95'
                            )}
                        >
                            {loading ? (
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                            ) : (
                                <Send size={18} />
                            )}
                        </button>
                    </div>
                </form>

                {/* 최근 시도 결과 */}
                {guesses.length > 0 && (
                    <div className="space-y-4">
                        <div className="flex flex-wrap gap-2">
                            <AnimatePresence>
                                {guesses.slice(0, 3).map((g, idx) => (
                                    <motion.div
                                        key={g.id}
                                        initial={{ opacity: 0, x: -10, scale: 0.9 }}
                                        animate={{ opacity: 1, x: 0, scale: 1 }}
                                        exit={{ opacity: 0, x: 10 }}
                                        transition={{ delay: idx * 0.05 }}
                                        className="flex items-center gap-2 px-4 py-2 bg-white rounded-full border border-gray-200 shadow-sm"
                                    >
                                        <span className="font-semibold text-text-primary text-sm">{g.word}</span>
                                        <span className={cn('font-black text-sm', getScoreColor(g.score))}>
                                            {g.score.toFixed(0)}점
                                        </span>
                                    </motion.div>
                                ))}
                            </AnimatePresence>
                        </div>

                        {/* 통계 */}
                        <div className="flex items-center gap-4 pt-4 border-t border-gray-200">
                            <div className="flex items-center gap-2 text-sm text-text-secondary">
                                <Flame className="text-accent" size={16} />
                                <span>최고 <span className="font-bold text-text-primary">{bestScore.toFixed(0)}점</span></span>
                            </div>
                            <div className="flex items-center gap-2 text-sm text-text-secondary">
                                <Trophy className="text-secondary" size={16} />
                                <span><span className="font-bold text-text-primary">{guesses.length}회</span> 시도</span>
                            </div>
                            <span className={cn('ml-auto text-sm font-bold px-3 py-1 rounded-full', getScoreColor(guesses[0].score), 'bg-gray-100')}>
                                {getScoreMessage(guesses[0].score)}
                            </span>
                        </div>
                    </div>
                )}
            </div>
        </section>
    );
};

export default CommentleHero;
