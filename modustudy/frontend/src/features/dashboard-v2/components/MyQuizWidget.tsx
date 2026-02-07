import React, { useState, useEffect, useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Brain, ChevronLeft, ChevronRight, Clock, CheckCircle2, Play, Trophy, RotateCcw } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import {
    QuizSingleChoice,
    QuizMultipleChoice,
    QuizShortAnswer,
    QuizProgress,
    WidgetHeader,
    WidgetContainer
} from '@/shared/components';
import { getTodayReviews, submitReview, ReviewItemDto } from '@/features/dashboard-v2/api/reviewApi';
import { useTimer } from '@/features/quiz/hooks/useTimer';
import { transformToQuizQuestion, indexToOptionId } from '@/shared/utils/quizUtils';

type ViewMode = 'list' | 'quiz';

export const MyQuizWidget: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    // 퀴즈 세션 상태 (MeetingTestWidget 패턴)
    const [quizSessionItems, setQuizSessionItems] = useState<ReviewItemDto[]>([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [selectedAnswers, setSelectedAnswers] = useState<number[]>([]);
    const [shortAnswer, setShortAnswer] = useState('');
    const [showResult, setShowResult] = useState(false);
    const [correctCount, setCorrectCount] = useState(0);
    const [isComplete, setIsComplete] = useState(false);

    // 데이터 로딩 상태
    const [reviewItems, setReviewItems] = useState<ReviewItemDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // 마운트 시 오늘의 복습 목록 조회
    useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);
                const response = await getTodayReviews();
                // question이 null이거나 주관식(SHORT_ANSWER)인 항목 임시 제외
                const filtered = (response.items || []).filter(
                    item => item.question != null && item.question.questionType !== 'SHORT_ANSWER'
                );
                setReviewItems(filtered);
            } catch (err) {
                console.error('[MyQuizWidget] 데이터 로딩 실패:', err);
                setReviewItems([]);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, []);

    // 퀴즈 세션 시작 (클릭한 아이템부터 순차 진행)
    const handleStartQuiz = useCallback((startIndex: number = 0) => {
        const sessionItems = reviewItems.slice(startIndex);
        setQuizSessionItems(sessionItems);
        setViewMode('quiz');
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setSelectedAnswers([]);
        setShortAnswer('');
        setShowResult(false);
        setCorrectCount(0);
        setIsComplete(false);
    }, [reviewItems]);

    const handleBackToList = () => {
        setViewMode('list');
        setQuizSessionItems([]);
    };

    // 현재 풀이 중인 아이템
    const currentReviewItem = quizSessionItems[currentIndex] || null;
    const currentQuiz = currentReviewItem ? transformToQuizQuestion(currentReviewItem) : undefined;
    const isLastQuestion = currentIndex + 1 >= quizSessionItems.length;

    // 타이머 훅 사용
    const { start: startTimer, stop: stopTimer } = useTimer();

    // 퀴즈/문제 변경 시 타이머 시작
    useEffect(() => {
        if (viewMode === 'quiz' && !isComplete && quizSessionItems[currentIndex]) {
            startTimer();
        }
    }, [viewMode, currentIndex, isComplete, startTimer]);

    // 다음 문제로 이동
    const handleNext = useCallback(() => {
        if (currentIndex + 1 >= quizSessionItems.length) {
            setIsComplete(true);
        } else {
            setCurrentIndex(prev => prev + 1);
            setSelectedAnswer(null);
            setSelectedAnswers([]);
            setShortAnswer('');
            setShowResult(false);
        }
    }, [quizSessionItems, currentIndex]);

    // 다시 풀기
    const handleRetry = useCallback(() => {
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setSelectedAnswers([]);
        setShortAnswer('');
        setShowResult(false);
        setCorrectCount(0);
        setIsComplete(false);
    }, []);

    const handleSubmission = async (userAnswer: string) => {
        if (!currentReviewItem || !currentQuiz) return;

        try {
            const responseTimeMs = stopTimer();

            const result = await submitReview({
                contentType: currentReviewItem.contentType,
                contentId: currentReviewItem.contentId,
                userAnswer: userAnswer,
                responseTimeMs
            });

            if (result.isCorrect) {
                setCorrectCount(prev => prev + 1);
            }

            setShowResult(true);

            // 완료된 아이템 원본 리스트에서 제거 (목록으로 돌아갔을 때 반영)
            setReviewItems(prev => prev.filter(item => item.reviewItemId !== currentReviewItem.reviewItemId));

        } catch (err) {
            console.error('[MyQuizWidget] 답안 제출 실패:', err);
            setShowResult(true);
        }
    };

    // 복수 선택 토글
    const handleToggleAnswer = (index: number) => {
        setSelectedAnswers(prev =>
            prev.includes(index)
                ? prev.filter(i => i !== index)
                : [...prev, index].sort((a, b) => a - b)
        );
    };

    const handleSubmitMultiple = () => {
        if (!currentReviewItem) return;

        const isSingle = currentReviewItem.question.questionType === 'MULTIPLE_CHOICE';
        const hasAnswer = isSingle ? selectedAnswer !== null : selectedAnswers.length > 0;

        if (!hasAnswer) return;

        const getOptionId = (idx: number) => {
            const option = currentReviewItem.question.options[idx];
            return option?.id || indexToOptionId(idx);
        };

        const answerString = isSingle
            ? getOptionId(selectedAnswer as number)
            : selectedAnswers.sort((a, b) => a - b).map(getOptionId).join(',');

        handleSubmission(answerString);
    };

    const handleSubmitShort = () => {
        if (!shortAnswer.trim()) return;
        handleSubmission(shortAnswer.trim());
    };

    // 로딩 중
    if (isLoading && viewMode === 'list') {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={Brain}
                    iconColor="neutral"
                    title="오늘의 복습"
                    subtitle="FSRS 알고리즘 기반"
                    maximizePath="/quiz/my-quiz"
                />
                <div className="flex items-center justify-center h-64">
                    <Spinner variant="center" size="md" label="복습 목록 로딩 중..." />
                </div>
            </WidgetContainer>
        );
    }

    return (
        <WidgetContainer>
            {/* 헤더 */}
            <WidgetHeader
                icon={Brain}
                iconColor="neutral"
                title={viewMode === 'list' ? '오늘의 복습' : (currentReviewItem?.question.category || '복습 퀴즈')}
                subtitle={viewMode === 'list' ? `${reviewItems.length}개의 복습 대기 중` : 'AI 맞춤형 복습'}
                showBackButton={viewMode === 'quiz'}
                onBack={handleBackToList}
                maximizePath="/quiz/my-quiz"
                rightActions={
                    viewMode === 'quiz' && !isComplete && correctCount > 0 ? (
                        <div className="text-sm font-bold text-text-primary">
                            {correctCount} / {currentIndex + (showResult ? 1 : 0)}
                        </div>
                    ) : undefined
                }
            />

            <div className="p-6">
                <AnimatePresence mode="wait">
                    {viewMode === 'list' ? (
                        <motion.div
                            key="list"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                        >
                            <ReviewItemList
                                items={reviewItems}
                                onSelect={(item) => {
                                    const idx = reviewItems.findIndex(i => i.reviewItemId === item.reviewItemId);
                                    handleStartQuiz(idx >= 0 ? idx : 0);
                                }}
                            />
                        </motion.div>
                    ) : currentQuiz && currentReviewItem && !isComplete ? (
                        <motion.div
                            key={`quiz-${currentIndex}`}
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.3 }}
                        >
                            {/* 진행률 */}
                            <QuizProgress
                                current={currentIndex + 1}
                                total={quizSessionItems.length}
                                className="pt-0 border-t-0 mb-4"
                            />

                            {/* 문제 풀이 화면 */}
                            {currentReviewItem.question.questionType === 'MULTIPLE_CHOICE' ? (
                                <QuizSingleChoice
                                    quiz={currentQuiz}
                                    questionNumber={currentIndex + 1}
                                    selectedAnswer={selectedAnswer}
                                    showResult={showResult}
                                    onSelectAnswer={setSelectedAnswer}
                                    onSubmit={handleSubmitMultiple}
                                    onNext={handleNext}
                                    isLastQuestion={isLastQuestion}
                                />
                            ) : currentReviewItem.question.questionType === 'MULTIPLE_CHOICE_MULTIPLE' ? (
                                <QuizMultipleChoice
                                    quiz={currentQuiz}
                                    questionNumber={currentIndex + 1}
                                    selectedAnswers={selectedAnswers}
                                    showResult={showResult}
                                    onToggleAnswer={handleToggleAnswer}
                                    onSubmit={handleSubmitMultiple}
                                    onNext={handleNext}
                                    isLastQuestion={isLastQuestion}
                                />
                            ) : (
                                <QuizShortAnswer
                                    quiz={currentQuiz}
                                    questionNumber={currentIndex + 1}
                                    userAnswer={shortAnswer}
                                    showResult={showResult}
                                    onChangeAnswer={(val) => setShortAnswer(val || '')}
                                    onSubmit={handleSubmitShort}
                                    onNext={handleNext}
                                    isLastQuestion={isLastQuestion}
                                />
                            )}
                        </motion.div>
                    ) : isComplete ? (
                        /* 완료 화면 */
                        <motion.div
                            key="complete"
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="text-center py-8"
                        >
                            <div className="w-16 h-16 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-4">
                                <Trophy size={28} className="text-accent" />
                            </div>
                            <h3 className="text-lg font-bold text-text-primary mb-1">복습 완료!</h3>
                            <p className="text-sm text-text-secondary mb-4">오늘의 복습을 모두 마쳤습니다</p>

                            <div className="flex justify-center gap-4 mb-6">
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-text-primary">{quizSessionItems.length}</div>
                                    <div className="text-xs text-text-tertiary">총 문제</div>
                                </div>
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-secondary">{correctCount}</div>
                                    <div className="text-xs text-text-tertiary">정답</div>
                                </div>
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-accent">
                                        {quizSessionItems.length > 0 ? Math.round((correctCount / quizSessionItems.length) * 100) : 0}%
                                    </div>
                                    <div className="text-xs text-text-tertiary">정답률</div>
                                </div>
                            </div>

                            <div className="flex justify-center gap-3">
                                <button
                                    onClick={handleRetry}
                                    className={cn(
                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                        'border border-accent/30 text-accent hover:bg-accent/5'
                                    )}
                                >
                                    <RotateCcw size={14} />
                                    다시 풀기
                                </button>
                                <button
                                    onClick={handleBackToList}
                                    className={cn(
                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                        'bg-accent text-white hover:bg-accent/90'
                                    )}
                                >
                                    목록으로
                                </button>
                            </div>
                        </motion.div>
                    ) : null}
                </AnimatePresence>
            </div>
        </WidgetContainer>
    );
};

// 페이지당 아이템 수
const ITEMS_PER_PAGE = 5;

// 리뷰 아이템 리스트 컴포넌트
interface ReviewItemListProps {
    items: ReviewItemDto[];
    onSelect: (item: ReviewItemDto) => void;
}

const ReviewItemList: React.FC<ReviewItemListProps> = ({ items, onSelect }) => {
    const [currentPage, setCurrentPage] = useState(1);
    const totalPages = Math.ceil(items.length / ITEMS_PER_PAGE);
    const showPagination = items.length > ITEMS_PER_PAGE;

    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const currentItems = items.slice(startIndex, startIndex + ITEMS_PER_PAGE);

    if (items.length === 0) {
        return (
            <div className="text-center py-12">
                <CheckCircle2 className="mx-auto text-green-500 mb-4" size={48} />
                <p className="text-text-primary font-bold">오늘의 복습 완료!</p>
                <p className="text-sm text-text-tertiary mt-1">
                    지금은 복습할 내용이 없습니다. 나중에 다시 확인해주세요.
                </p>
            </div>
        );
    }

    return (
        <div className="flex flex-col">
            <div className="max-h-[400px] overflow-y-auto space-y-3 pr-2">
                {currentItems.map((item) => {
                    // 난이도 표시용
                    const diffLabel = item.difficulty < 3 ? '쉬움' : item.difficulty < 7 ? '보통' : '어려움';
                    const diffColor = item.difficulty < 3 ? 'bg-green-100 text-green-700' : item.difficulty < 7 ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700';

                    // D-day 계산 (nextReviewAt vs now)
                    // 오늘 복습 목록에 있다는건 이미 지났거나 오늘이라는 뜻.
                    const dueDate = new Date(item.nextReviewAt);
                    const now = new Date();
                    const diffMs = now.getTime() - dueDate.getTime();
                    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
                    const overdueLabel = diffDays > 0 ? `${diffDays}일 전` : '오늘';

                    return (
                        <button
                            key={item.reviewItemId}
                            onClick={() => onSelect(item)}
                            className={cn(
                                'w-full flex items-center justify-between text-left group',
                                'rounded-xl p-3 transition-all duration-200',
                                'hover:bg-white hover:shadow-sm border border-transparent hover:border-blue-100'
                            )}
                        >
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-2">
                                    <span className={cn('px-2 py-0.5 rounded text-xs font-medium', diffColor)}>
                                        {diffLabel}
                                    </span>
                                    <span className="text-xs text-text-tertiary bg-gray-100 px-2 py-0.5 rounded">
                                        {item.question?.category || '일반'}
                                    </span>
                                    <span className="text-xs text-error font-medium flex items-center gap-1 ml-auto sm:ml-2">
                                        <Clock size={12} />
                                        {overdueLabel}
                                    </span>
                                </div>
                                <h4 className="font-medium text-text-primary line-clamp-2 text-sm leading-relaxed">
                                    {item.question?.questionText || '문제를 불러올 수 없습니다'}
                                </h4>
                            </div>
                            <div className="flex-shrink-0 self-center pl-3">
                                <Play size={18} className="text-primary/70 group-hover:text-primary transition-colors" />
                            </div>
                        </button>
                    );
                })}
            </div>

            {showPagination && (
                <nav className="flex items-center justify-center gap-1 mt-2 pt-2 border-t border-gray-100">
                    <button
                        type="button"
                        onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                        disabled={currentPage === 1}
                        className={cn(
                            'p-2 rounded-lg text-text-secondary hover:text-text-primary disabled:opacity-30'
                        )}
                    >
                        <ChevronLeft size={18} />
                    </button>
                    <span className="text-sm text-text-secondary font-medium px-2">
                        {currentPage} / {totalPages}
                    </span>
                    <button
                        type="button"
                        onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                        disabled={currentPage === totalPages}
                        className={cn(
                            'p-2 rounded-lg text-text-secondary hover:text-text-primary disabled:opacity-30'
                        )}
                    >
                        <ChevronRight size={18} />
                    </button>
                </nav>
            )}
        </div>
    );
};
