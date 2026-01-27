import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Brain, Check, X, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

// Mock 퀴즈 데이터
const MOCK_QUIZZES = [
    {
        id: 1,
        question: 'useEffect의 클린업 함수는 언제 실행되나요?',
        options: ['컴포넌트 마운트 시', '언마운트 시 또는 다음 effect 실행 전', '렌더링 직후', '상태 변경 시'],
        correctAnswer: 1,
        explanation: '클린업 함수는 컴포넌트 언마운트 시 또는 다음 effect가 실행되기 전에 호출됩니다.',
        difficulty: 'medium',
        category: 'React',
    },
    {
        id: 2,
        question: 'TypeScript의 Partial<T> 유틸리티 타입의 역할은?',
        options: [
            '모든 속성을 필수로 만듦',
            '모든 속성을 선택적으로 만듦',
            '특정 속성만 선택',
            '속성 제거',
        ],
        correctAnswer: 1,
        explanation: 'Partial<T>는 타입 T의 모든 속성을 선택적(optional)으로 만드는 유틸리티 타입입니다.',
        difficulty: 'easy',
        category: 'TypeScript',
    },
];

export const AIQuizWidget: React.FC = () => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [showResult, setShowResult] = useState(false);
    const [score, setScore] = useState({ correct: 0, total: 0 });

    const currentQuiz = MOCK_QUIZZES[currentIndex];

    const handleSubmit = () => {
        if (selectedAnswer === null) return;

        const isCorrect = selectedAnswer === currentQuiz.correctAnswer;
        setScore((prev) => ({
            correct: prev.correct + (isCorrect ? 1 : 0),
            total: prev.total + 1,
        }));
        setShowResult(true);
    };

    const handleNext = () => {
        if (currentIndex < MOCK_QUIZZES.length - 1) {
            setCurrentIndex((prev) => prev + 1);
            setSelectedAnswer(null);
            setShowResult(false);
        }
    };

    const difficultyColors = {
        easy: 'bg-accent/20 text-accent-dark',
        medium: 'bg-secondary/20 text-secondary-dark',
        hard: 'bg-error/20 text-error',
    };

    const getProgressWidth = () => {
        return `${((currentIndex + 1) / MOCK_QUIZZES.length) * 100}%`;
    };

    return (
        <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
            {/* 헤더 */}
            <div className="bg-gradient-to-r from-secondary/10 to-accent/10 px-6 py-4 flex items-center justify-between border-b border-gray-100">
                <div className="flex items-center gap-3">
                    <Brain className="text-secondary" size={24} />
                    <div>
                        <h3 className="text-lg font-bold text-text-primary">AI 퀴즈</h3>
                        <p className="text-sm text-text-secondary">학습 내용 복습</p>
                    </div>
                </div>
                {score.total > 0 && (
                    <div className="text-sm font-bold text-text-primary">
                        정답률: {Math.round((score.correct / score.total) * 100)}%
                    </div>
                )}
            </div>

            <div className="p-6">
                <AnimatePresence mode="wait">
                    <motion.div
                        key={currentIndex}
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -20 }}
                        transition={{ duration: 0.3 }}
                    >
                        {/* 퀴즈 메타정보 */}
                        <div className="flex items-center gap-2 mb-4">
                            <span
                                className={cn(
                                    'px-3 py-1 rounded-full text-xs font-medium',
                                    difficultyColors[currentQuiz.difficulty as keyof typeof difficultyColors]
                                )}
                            >
                                {currentQuiz.difficulty === 'easy' && '쉬움'}
                                {currentQuiz.difficulty === 'medium' && '보통'}
                                {currentQuiz.difficulty === 'hard' && '어려움'}
                            </span>
                            <span className="px-3 py-1 bg-gray-100 rounded-full text-xs font-medium text-gray-700">
                                {currentQuiz.category}
                            </span>
                        </div>

                        {/* 질문 */}
                        <h4 className="text-lg font-bold text-text-primary mb-4">
                            Q{currentIndex + 1}. {currentQuiz.question}
                        </h4>

                        {/* 선택지 */}
                        <div className="space-y-3 mb-6">
                            {currentQuiz.options.map((option, index) => (
                                <button
                                    key={index}
                                    onClick={() => !showResult && setSelectedAnswer(index)}
                                    disabled={showResult}
                                    className={cn(
                                        'w-full p-4 text-left rounded-xl transition-all border-2',
                                        'hover:shadow-md',
                                        !showResult && selectedAnswer === index && 'border-primary bg-primary/5',
                                        !showResult && selectedAnswer !== index && 'border-gray-200 hover:border-gray-300',
                                        showResult &&
                                            index === currentQuiz.correctAnswer &&
                                            'border-accent bg-accent/10',
                                        showResult &&
                                            index === selectedAnswer &&
                                            index !== currentQuiz.correctAnswer &&
                                            'border-error bg-error/10',
                                        showResult &&
                                            index !== currentQuiz.correctAnswer &&
                                            index !== selectedAnswer &&
                                            'border-gray-200 opacity-50'
                                    )}
                                >
                                    <div className="flex items-center justify-between">
                                        <span className="text-text-primary">{option}</span>
                                        {showResult && index === currentQuiz.correctAnswer && (
                                            <Check className="text-accent" size={20} />
                                        )}
                                        {showResult && index === selectedAnswer && index !== currentQuiz.correctAnswer && (
                                            <X className="text-error" size={20} />
                                        )}
                                    </div>
                                </button>
                            ))}
                        </div>

                        {/* 결과 및 설명 */}
                        {showResult && (
                            <motion.div
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                className="bg-accent/10 rounded-xl p-4 mb-4"
                            >
                                <h5 className="font-bold text-text-primary mb-2 flex items-center gap-2">
                                    <ChevronRight size={16} className="text-accent" />
                                    해설
                                </h5>
                                <p className="text-sm text-text-secondary leading-relaxed">{currentQuiz.explanation}</p>
                            </motion.div>
                        )}

                        {/* 액션 버튼 */}
                        <div className="flex gap-3">
                            {!showResult ? (
                                <button
                                    onClick={handleSubmit}
                                    disabled={selectedAnswer === null}
                                    className={cn(
                                        'flex-1 py-3 rounded-xl font-bold transition-all',
                                        'bg-primary hover:bg-primary-dark text-white',
                                        'disabled:opacity-50 disabled:cursor-not-allowed'
                                    )}
                                >
                                    제출하기
                                </button>
                            ) : (
                                <button
                                    onClick={handleNext}
                                    disabled={currentIndex >= MOCK_QUIZZES.length - 1}
                                    className={cn(
                                        'flex-1 py-3 rounded-xl font-bold transition-all',
                                        'bg-secondary hover:bg-secondary-dark text-white',
                                        'disabled:opacity-50 disabled:cursor-not-allowed'
                                    )}
                                >
                                    {currentIndex < MOCK_QUIZZES.length - 1 ? '다음 문제' : '완료'}
                                </button>
                            )}
                        </div>
                    </motion.div>
                </AnimatePresence>

                {/* 진행률 */}
                <div className="mt-6 pt-4 border-t border-gray-100">
                    <div className="flex items-center justify-between text-sm text-text-secondary mb-2">
                        <span>진행률</span>
                        <span>
                            {currentIndex + 1} / {MOCK_QUIZZES.length}
                        </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                            className="bg-primary rounded-full h-2 transition-all duration-300"
                            style={{ width: getProgressWidth() }}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};
