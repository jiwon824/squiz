import React, { useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Check, X, ChevronRight, CheckSquare, Square, Circle, CheckCircle2, AlertCircle } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { normalizeCorrectAnswer } from '@/shared/utils/quizUtils';

// 퀴즈 타입 정의
export interface QuizQuestion {
    id: number | string;
    question: string;
    type: 'multiple' | 'short';
    options?: string[];
    correctAnswer: number | number[] | string; // 객관식(단일): index, 객관식(복수): index[], 주관식: 정답 문자열
    explanation?: string;
    difficulty?: 'easy' | 'medium' | 'hard';
    category?: string;
}

// 난이도 라벨
const difficultyLabels = {
    easy: '쉬움',
    medium: '보통',
    hard: '어려움',
};

// 난이도 색상
const difficultyColors = {
    easy: 'bg-accent/20 text-accent-dark',
    medium: 'bg-secondary/20 text-secondary-dark',
    hard: 'bg-error/20 text-error',
};

// ============================================
// 객관식 퀴즈 컴포넌트
// ============================================
export interface QuizSingleChoiceProps {
    quiz: QuizQuestion;
    questionNumber?: number;
    selectedAnswer: number | null;
    showResult: boolean;
    onSelectAnswer: (index: number) => void;
    onSubmit: () => void;
    onNext?: () => void;
    isLastQuestion?: boolean;
    className?: string;
}

export const QuizSingleChoice: React.FC<QuizSingleChoiceProps> = ({
    quiz,
    questionNumber,
    selectedAnswer,
    showResult,
    onSelectAnswer,
    onSubmit,
    onNext,
    isLastQuestion = false,
    className,
}) => {
    const correctAnswerIndex = typeof quiz.correctAnswer === 'number' ? quiz.correctAnswer : -1;

    return (
        <div className={cn('space-y-4', className)}>
            {/* 퀴즈 메타정보 */}
            {(quiz.difficulty || quiz.category) && (
                <div className="flex items-center gap-2">
                    {quiz.difficulty && (
                        <span
                            className={cn(
                                'px-3 py-1 rounded-full text-xs font-medium',
                                difficultyColors[quiz.difficulty]
                            )}
                        >
                            {difficultyLabels[quiz.difficulty]}
                        </span>
                    )}
                    {quiz.category && (
                        <span className="px-3 py-1 bg-gray-100 rounded-full text-xs font-medium text-gray-700">
                            {quiz.category}
                        </span>
                    )}
                </div>
            )}

            {/* 질문 */}
            <h4 className="text-lg font-bold text-text-primary">
                {questionNumber !== undefined && `Q${questionNumber}. `}
                {quiz.question}
            </h4>

            {/* 선택지 */}
            <div className="space-y-3">
                {quiz.options?.map((option, index) => (
                    <button
                        key={index}
                        onClick={() => !showResult && onSelectAnswer(index)}
                        disabled={showResult}
                        className={cn(
                            'w-full p-4 text-left rounded-xl transition-all border-2 group',
                            'hover:shadow-md',
                            !showResult && selectedAnswer === index && 'border-primary bg-primary/5',
                            !showResult && selectedAnswer !== index && 'border-gray-200 hover:border-gray-300',
                            showResult && index === correctAnswerIndex && 'border-secondary bg-secondary/10',
                            showResult &&
                            index === selectedAnswer &&
                            index !== correctAnswerIndex &&
                            'border-error bg-error/10',
                            showResult &&
                            index !== correctAnswerIndex &&
                            index !== selectedAnswer &&
                            'border-gray-200 opacity-50'
                        )}
                    >
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                {/* Radio Icon */}
                                <div className={cn(
                                    "transition-colors",
                                    !showResult && selectedAnswer === index ? "text-primary" : "text-gray-400",
                                    showResult && index === correctAnswerIndex ? "text-secondary" : "",
                                    showResult && selectedAnswer === index && index !== correctAnswerIndex ? "text-error" : ""
                                )}>
                                    {selectedAnswer === index || (showResult && index === correctAnswerIndex) ? (
                                        <CheckCircle2 size={20} />
                                    ) : (
                                        <Circle size={20} />
                                    )}
                                </div>
                                <span className="text-text-primary">{option}</span>
                            </div>
                            {showResult && index === correctAnswerIndex && (
                                <Check className="text-secondary" size={20} />
                            )}
                            {showResult && index === selectedAnswer && index !== correctAnswerIndex && (
                                <X className="text-error" size={20} />
                            )}
                        </div>
                    </button>
                ))}
            </div>

            {/* 해설 */}
            {showResult && quiz.explanation && (
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-secondary/10 rounded-xl p-4"
                >
                    <h5 className="font-bold text-text-primary mb-2 flex items-center gap-2">
                        <ChevronRight size={16} className="text-secondary" />
                        해설
                    </h5>
                    <p className="text-sm text-text-secondary leading-relaxed">{quiz.explanation}</p>
                </motion.div>
            )}

            {/* 버튼 */}
            <div className="flex gap-3">
                {!showResult ? (
                    <button
                        onClick={onSubmit}
                        disabled={selectedAnswer === null}
                        className={cn(
                            'flex-1 py-3 rounded-xl font-bold transition-all',
                            'bg-primary hover:bg-primary-dark text-white',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        제출하기
                    </button>
                ) : onNext ? (
                    <button
                        onClick={onNext}
                        disabled={false}
                        className={cn(
                            'flex-1 py-3 rounded-xl font-bold transition-all',
                            'bg-secondary hover:bg-secondary-dark text-white',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        {isLastQuestion ? '완료' : '다음 문제'}
                    </button>
                ) : null}
            </div>
        </div>
    );
};

// ============================================
// 객관식 퀴즈 컴포넌트 (다중 선택)
// ============================================
export interface QuizMultipleChoiceProps {
    quiz: QuizQuestion;
    questionNumber?: number;
    selectedAnswers: number[];
    showResult: boolean;
    onToggleAnswer: (index: number) => void;
    onSubmit: () => void;
    onNext?: () => void;
    isLastQuestion?: boolean;
    className?: string;
}

export const QuizMultipleChoice: React.FC<QuizMultipleChoiceProps> = ({
    quiz,
    questionNumber,
    selectedAnswers,
    showResult,
    onToggleAnswer,
    onSubmit,
    onNext,
    isLastQuestion = false,
    className,
}) => {
    // 정답 배열 파싱 - normalizeCorrectAnswer 유틸리티 사용 (항상 number[] 반환)
    const correctAnswers: number[] = normalizeCorrectAnswer(quiz.correctAnswer);

    // 전체 정답 여부 판정: 선택한 답과 정답이 정확히 일치하는지 확인
    const isAllCorrect = showResult && (
        selectedAnswers.length === correctAnswers.length &&
        correctAnswers.every(a => selectedAnswers.includes(a))
    );

    return (
        <div className={cn('space-y-4', className)}>
            {/* 퀴즈 메타정보 */}
            {(quiz.difficulty || quiz.category) && (
                <div className="flex items-center gap-2">
                    {quiz.difficulty && (
                        <span
                            className={cn(
                                'px-3 py-1 rounded-full text-xs font-medium',
                                difficultyColors[quiz.difficulty]
                            )}
                        >
                            {difficultyLabels[quiz.difficulty]}
                        </span>
                    )}
                    {quiz.category && (
                        <span className="px-3 py-1 bg-gray-100 rounded-full text-xs font-medium text-gray-700">
                            {quiz.category}
                        </span>
                    )}
                    <span className="px-3 py-1 bg-primary/10 text-primary-dark rounded-full text-xs font-medium">
                        복수 선택 가능
                    </span>
                </div>
            )}

            {/* 질문 */}
            <h4 className="text-lg font-bold text-text-primary">
                {questionNumber !== undefined && `Q${questionNumber}. `}
                {quiz.question}
            </h4>

            {/* 전체 정답/오답 판정 배너 */}
            {showResult && (
                <motion.div
                    initial={{ opacity: 0, y: -5 }}
                    animate={{ opacity: 1, y: 0 }}
                    className={cn(
                        'p-3 rounded-xl text-sm font-medium flex items-center gap-2',
                        isAllCorrect ? 'bg-secondary/10 text-secondary-dark' : 'bg-error/10 text-error'
                    )}
                >
                    {isAllCorrect ? <Check size={16} /> : <AlertCircle size={16} />}
                    {isAllCorrect ? '정답입니다!' : '오답입니다. 모든 정답을 선택해야 합니다.'}
                </motion.div>
            )}

            {/* 선택지 */}
            <div className="space-y-3">
                {quiz.options?.map((option, index) => {
                    const isSelected = selectedAnswers.includes(index);
                    const isCorrect = correctAnswers.includes(index);
                    // 선택하지 않은 정답 (놓친 항목)
                    const isMissed = showResult && isCorrect && !isSelected;

                    return (
                        <button
                            key={index}
                            onClick={() => !showResult && onToggleAnswer(index)}
                            disabled={showResult}
                            className={cn(
                                'w-full p-4 text-left rounded-xl transition-all border-2 group',
                                'hover:shadow-md',
                                // 결과 보기 전: 선택된 항목 강조
                                !showResult && isSelected && 'border-primary bg-primary/5',
                                !showResult && !isSelected && 'border-gray-200 hover:border-gray-300',
                                // 결과 보기: 선택한 정답 (맞춘 항목)
                                showResult && isCorrect && isSelected && 'border-secondary bg-secondary/10',
                                // 결과 보기: 선택하지 않은 정답 (놓친 항목 - 경고 스타일)
                                showResult && isCorrect && !isSelected && 'border-amber-400 bg-amber-50',
                                // 결과 보기: 내가 잘못 선택한 오답
                                showResult && isSelected && !isCorrect && 'border-error bg-error/10',
                                // 결과 보기: 선택 안 했고 정답도 아닌 나머지
                                showResult && !isSelected && !isCorrect && 'border-gray-200 opacity-50'
                            )}
                        >
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className={cn(
                                        "transition-colors",
                                        !showResult && isSelected ? "text-primary" : "text-gray-400",
                                        showResult && isCorrect && isSelected ? "text-secondary" : "",
                                        showResult && isCorrect && !isSelected ? "text-amber-500" : "",
                                        showResult && isSelected && !isCorrect ? "text-error" : ""
                                    )}>
                                        {isSelected ? <CheckSquare size={20} /> : <Square size={20} />}
                                    </div>
                                    <span className="text-text-primary">{option}</span>
                                </div>

                                {/* 선택한 정답 */}
                                {showResult && isCorrect && isSelected && (
                                    <Check className="text-secondary" size={20} />
                                )}
                                {/* 놓친 정답 */}
                                {showResult && isMissed && (
                                    <span className="text-xs font-medium text-amber-600 flex items-center gap-1">
                                        <AlertCircle size={14} />
                                        미선택
                                    </span>
                                )}
                                {/* 잘못 선택한 오답 */}
                                {showResult && isSelected && !isCorrect && (
                                    <X className="text-error" size={20} />
                                )}
                            </div>
                        </button>
                    );
                })}
            </div>

            {/* 해설 */}
            {showResult && quiz.explanation && (
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-secondary/10 rounded-xl p-4"
                >
                    <h5 className="font-bold text-text-primary mb-2 flex items-center gap-2">
                        <ChevronRight size={16} className="text-secondary" />
                        해설
                    </h5>
                    <p className="text-sm text-text-secondary leading-relaxed">{quiz.explanation}</p>
                </motion.div>
            )}

            {/* 버튼 */}
            <div className="flex gap-3">
                {!showResult ? (
                    <button
                        onClick={onSubmit}
                        disabled={selectedAnswers.length === 0}
                        className={cn(
                            'flex-1 py-3 rounded-xl font-bold transition-all',
                            'bg-primary hover:bg-primary-dark text-white',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        제출하기
                    </button>
                ) : onNext ? (
                    <button
                        onClick={onNext}
                        disabled={false}
                        className={cn(
                            'flex-1 py-3 rounded-xl font-bold transition-all',
                            'bg-secondary hover:bg-secondary-dark text-white',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        {isLastQuestion ? '완료' : '다음 문제'}
                    </button>
                ) : null}
            </div>
        </div>
    );
};

// ============================================
// 주관식 퀴즈 컴포넌트
// ============================================
export interface QuizShortAnswerProps {
    quiz: QuizQuestion;
    questionNumber?: number;
    userAnswer: string | null;
    showResult: boolean;
    onChangeAnswer: (value: string | null) => void;
    onSubmit: () => void;
    onNext?: () => void;
    isLastQuestion?: boolean;
    className?: string;
}

export const QuizShortAnswer: React.FC<QuizShortAnswerProps> = ({
    quiz,
    questionNumber,
    userAnswer,
    showResult,
    onChangeAnswer,
    onSubmit,
    onNext,
    isLastQuestion = false,
    className,
}) => {
    const inputRef = useRef<HTMLInputElement>(null);
    const correctAnswer = String(quiz.correctAnswer);
    const isCorrect = showResult && userAnswer?.trim().toLowerCase() === correctAnswer.toLowerCase();

    // 마운트 시 자동 포커스
    useEffect(() => {
        if (!showResult && inputRef.current) {
            inputRef.current.focus();
        }
    }, [showResult]);

    // 엔터 키로 제출 (IME 입력 고려)
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        // IME 조합 중일 때는 무시 (한글 입력 등)
        if (e.nativeEvent.isComposing) return;
        if (e.key !== 'Enter' || showResult || !userAnswer?.trim()) return;

        e.preventDefault();
        e.stopPropagation();
        onSubmit();
    };

    return (
        <div className={cn('space-y-4', className)}>
            {/* 퀴즈 메타정보 */}
            {(quiz.difficulty || quiz.category) && (
                <div className="flex items-center gap-2">
                    {quiz.difficulty && (
                        <span
                            className={cn(
                                'px-3 py-1 rounded-full text-xs font-medium',
                                difficultyColors[quiz.difficulty]
                            )}
                        >
                            {difficultyLabels[quiz.difficulty]}
                        </span>
                    )}
                    {quiz.category && (
                        <span className="px-3 py-1 bg-gray-100 rounded-full text-xs font-medium text-gray-700">
                            {quiz.category}
                        </span>
                    )}
                </div>
            )}

            {/* 질문 */}
            <h4 className="text-lg font-bold text-text-primary">
                {questionNumber !== undefined && `Q${questionNumber}. `}
                {quiz.question}
            </h4>

            {/* 입력 필드 */}
            <div className="space-y-2">
                <input
                    ref={inputRef}
                    type="text"
                    value={userAnswer || ''}
                    onChange={(e) => !showResult && onChangeAnswer(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={showResult}
                    placeholder="답을 입력하세요..."
                    className={cn(
                        'w-full p-4 rounded-xl border-2 transition-all',
                        'focus:outline-none focus:ring-2 focus:ring-primary/20',
                        !showResult && 'border-gray-200 focus:border-primary',
                        showResult && isCorrect && 'border-secondary bg-secondary/10',
                        showResult && !isCorrect && 'border-error bg-error/10',
                        'disabled:cursor-not-allowed'
                    )}
                />
                {showResult && (
                    <div className="flex items-center gap-2 text-sm">
                        {isCorrect ? (
                            <>
                                <Check className="text-secondary" size={16} />
                                <span className="text-secondary font-medium">정답입니다!</span>
                            </>
                        ) : (
                            <>
                                <X className="text-error" size={16} />
                                <span className="text-error">
                                    오답입니다. 정답: <strong>{correctAnswer}</strong>
                                </span>
                            </>
                        )}
                    </div>
                )}
            </div>

            {/* 해설 */}
            {showResult && quiz.explanation && (
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-secondary/10 rounded-xl p-4"
                >
                    <h5 className="font-bold text-text-primary mb-2 flex items-center gap-2">
                        <ChevronRight size={16} className="text-secondary" />
                        해설
                    </h5>
                    <p className="text-sm text-text-secondary leading-relaxed">{quiz.explanation}</p>
                </motion.div>
            )}

            {/* 버튼 */}
            <div className="flex gap-3">
                {!showResult ? (
                    <>
                        <button
                            onClick={onSubmit}
                            disabled={!userAnswer?.trim()}
                            className={cn(
                                'flex-1 py-3 rounded-xl font-bold transition-all',
                                'bg-primary hover:bg-primary-dark text-white',
                                'disabled:opacity-50 disabled:cursor-not-allowed'
                            )}
                        >
                            제출하기
                        </button>

                    </>
                ) : onNext ? (
                    <button
                        onClick={onNext}
                        disabled={false}
                        className={cn(
                            'flex-1 py-3 rounded-xl font-bold transition-all',
                            'bg-secondary hover:bg-secondary-dark text-white',
                            'disabled:opacity-50 disabled:cursor-not-allowed'
                        )}
                    >
                        {isLastQuestion ? '완료' : '다음 문제'}
                    </button>
                ) : null}
            </div>
        </div>
    );
};

// ============================================
// 퀴즈 진행률 표시 컴포넌트
// ============================================
export interface QuizProgressProps {
    current: number;
    total: number;
    className?: string;
}

export const QuizProgress: React.FC<QuizProgressProps> = ({ current, total, className }) => (
    <div className={cn('pt-4 border-t border-gray-100', className)}>
        <div className="flex items-center justify-between text-sm text-text-secondary mb-2">
            <span>진행률</span>
            <span>
                {current} / {total}
            </span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
            <div
                className="bg-primary rounded-full h-2 transition-all duration-300"
                style={{ width: `${(current / total) * 100}%` }}
            />
        </div>
    </div>
);
