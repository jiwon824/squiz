import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Send, Trophy, Flame, ArrowRight, Sparkles, Target } from 'lucide-react';
import { checkSimilarity, fetchDailyWord } from '@/features/quiz/services/quizService';
import '../styles/CommentleHero.css';

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
                setProblemLoaded(true); // 에러 시에도 UI 표시
            }
        };
        loadProblem();
    }, []);

    // 점수별 색상 (시멘틱 변수 사용)
    const getScoreColor = (score: number) => {
        if (score >= 90) return 'var(--color-success)';
        if (score >= 75) return 'var(--color-info)';
        if (score >= 50) return 'var(--color-secondary)';
        if (score >= 25) return 'var(--color-warning)';
        return 'var(--color-error)';
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
        <section className="commentle-hero commentle-hero-mini">
            {/* 좌측: 게임 영역 */}
            <motion.div
                className="hero-content"
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6 }}
            >
                <div className="mini-header">
                    <div className="badge-new">
                        <Sparkles size={12} />
                        오늘의 퀴즈
                    </div>
                    <span className="category-badge">{category}</span>
                </div>

                <h2 className="hero-title-mini">
                    꼬멘틀 <span className="text-highlight-accent">CS ver</span>
                </h2>
                <p className="hero-description-mini">
                    AI가 분석하는 의미 유사도 퀴즈! 100점에 가까울수록 정답입니다.
                </p>

                {/* 미니 입력 폼 */}
                <form onSubmit={handleSubmit} className="mini-input-form">
                    <div className="mini-input-wrapper">
                        <Target size={18} className="input-icon" />
                        <input
                            ref={inputRef}
                            type="text"
                            value={guess}
                            onChange={(e) => setGuess(e.target.value)}
                            placeholder={isCorrect ? "정답을 맞추셨습니다! 🎉" : "CS 용어 입력..."}
                            disabled={isCorrect || loading}
                            className="mini-input"
                            autoComplete="off"
                        />
                        <button
                            type="submit"
                            disabled={!guess.trim() || loading || isCorrect}
                            className="mini-submit-btn"
                        >
                            {loading ? (
                                <div className="mini-spinner" />
                            ) : (
                                <Send size={16} />
                            )}
                        </button>
                    </div>
                </form>

                {/* 시도 결과 미니 리스트 */}
                <div className="mini-results">
                    <AnimatePresence>
                        {guesses.slice(0, 3).map((g, idx) => (
                            <motion.div
                                key={g.id}
                                className="mini-result-item"
                                initial={{ opacity: 0, x: -20, scale: 0.9 }}
                                animate={{ opacity: 1, x: 0, scale: 1 }}
                                exit={{ opacity: 0, x: 20 }}
                                transition={{ delay: idx * 0.05 }}
                            >
                                <span className="result-word">{g.word}</span>
                                <span 
                                    className="result-score"
                                    style={{ color: getScoreColor(g.score) }}
                                >
                                    {g.score.toFixed(0)}점
                                </span>
                            </motion.div>
                        ))}
                    </AnimatePresence>
                </div>

                {/* 최고 점수 & 시도 횟수 */}
                {guesses.length > 0 && (
                    <div className="mini-stats">
                        <div className="stat-item">
                            <Flame size={14} />
                            <span>최고 {bestScore.toFixed(0)}점</span>
                        </div>
                        <div className="stat-item">
                            <Trophy size={14} />
                            <span>{guesses.length}회 시도</span>
                        </div>
                        {guesses.length > 0 && (
                            <span className="score-hint" style={{ color: getScoreColor(guesses[0].score) }}>
                                {getScoreMessage(guesses[0].score)}
                            </span>
                        )}
                    </div>
                )}
            </motion.div>

            {/* 우측: 비주얼 + CTA */}
            <motion.div
                className="hero-animation-side"
                initial={{ opacity: 0, scale: 0.8 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 0.8 }}
            >
                <div className="quiz-grid-preview">
                    {[...Array(9)].map((_, i) => (
                        <div key={i} className={`grid-box box-${(i % 3) + 1} delay-${i}`}>
                            <span className="material-icons">
                                {i === 4 ? 'question_mark' : 'code'}
                            </span>
                        </div>
                    ))}
                </div>
                <div className="floating-term term-1">Binary Tree</div>
                <div className="floating-term term-2">Recursion</div>
                <div className="floating-term term-3">Deadlock</div>
            </motion.div>

            {/* 전체 게임 이동 버튼 */}
            <button
                className="btn-hero-corner"
                onClick={() => navigate('/commentle')}
            >
                <span>전체 게임 도전</span>
                <ArrowRight size={18} />
            </button>
        </section>
    );
};

export default CommentleHero;
