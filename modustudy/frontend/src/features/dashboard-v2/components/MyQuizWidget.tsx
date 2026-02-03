import React, { useState, useEffect, useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Brain, ChevronLeft, ChevronRight, Clock, CheckCircle2, Circle, Play, Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import {
    QuizSingleChoice,
    QuizShortAnswer,
    QuizProgress,
    QuizQuestion,
    WidgetHeader,
    WidgetContainer
} from '@/shared/components';
import { getTodayReviews, submitReview, ReviewItemDto } from '@/features/dashboard-v2/api/reviewApi';
import { useTimer } from '@/features/quiz/hooks/useTimer';

// 퀴즈 문제 변환 로직 (ReviewItemDto -> QuizQuestion)
const transformToQuizQuestion = (item: ReviewItemDto): QuizQuestion => {
    const q = item.question;
    const isMultiple = q.questionType === 'MULTIPLE_CHOICE' || q.questionType === 'MULTIPLE_CHOICE_MULTIPLE';

    // options 파싱
    let options: string[] = [];
    if (q.options) {
        options = q.options.map(o => o.text);
    }

    // 정답 처리
    let correctAnswer: number | string = q.correctAnswer;
    if (isMultiple) {
        // 객관식 정답이 인덱스(문자열)로 오는 경우 숫자로 변환
        const parsed = parseInt(q.correctAnswer, 10);
        if (!isNaN(parsed)) {
            correctAnswer = parsed;
        }
    }

    return {
        id: item.reviewItemId, // 위젯 내부에서는 reviewItemId를 id로 사용
        type: isMultiple ? 'multiple' : 'short',
        question: q.questionText,
        options: isMultiple ? options : undefined,
        correctAnswer,
        explanation: q.explanation || '',
        difficulty: item.difficulty < 3 ? 'easy' : item.difficulty < 7 ? 'medium' : 'hard',
        category: q.category || '',
    } as QuizQuestion;
};

type ViewMode = 'list' | 'quiz';

export const MyQuizWidget: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [selectedReviewItem, setSelectedReviewItem] = useState<ReviewItemDto | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0); // 단일 문제 풀이이므로 0 고정일 수 있음
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [shortAnswer, setShortAnswer] = useState('');
    const [showResult, setShowResult] = useState(false);
    const [score, setScore] = useState({ correct: 0, total: 0 });

    // 데이터 로딩 상태
    const [reviewItems, setReviewItems] = useState<ReviewItemDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // 마운트 시 오늘의 복습 목록 조회
    useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);
                const response = await getTodayReviews();
                setReviewItems(response.items || []);
            } catch (err) {
                console.error('[MyQuizWidget] 데이터 로딩 실패:', err);
                setReviewItems([]);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, []);

    // 퀴즈 문제 선택
    const handleSelectReviewItem = useCallback((item: ReviewItemDto) => {
        setSelectedReviewItem(item);
        setViewMode('quiz');
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setShortAnswer('');
        setShowResult(false);
        // Note: score for this session? Maybe just track current item correctness locally
        setScore({ correct: 0, total: 0 });
    }, []);

    const handleBackToList = () => {
        setViewMode('list');
        setSelectedReviewItem(null);
    };

    const currentQuiz = selectedReviewItem ? transformToQuizQuestion(selectedReviewItem) : undefined;


    // 타이머 훅 사용
    const { start: startTimer, stop: stopTimer } = useTimer();

    // 퀴즈/문제 변경 시 타이머 시작
    useEffect(() => {
        if (viewMode === 'quiz' && currentQuiz) {
            startTimer();
        }
    }, [viewMode, currentQuiz, startTimer]);

    const handleSubmission = async (userAnswer: string) => {
        if (!selectedReviewItem || !currentQuiz) return;

        try {
            const responseTimeMs = stopTimer();

            // API 호출
            const result = await submitReview({
                contentType: selectedReviewItem.contentType,
                contentId: selectedReviewItem.contentId,
                userAnswer: userAnswer,
                responseTimeMs
            });

            // 결과 표시
            const isCorrect = currentQuiz.type === 'multiple'
                ? String(selectedAnswer) === String(currentQuiz.correctAnswer)
                : shortAnswer.trim().toLowerCase() === String(currentQuiz.correctAnswer).trim().toLowerCase();

            setScore((prev) => ({
                correct: prev.correct + (isCorrect ? 1 : 0),
                total: prev.total + 1,
            }));

            setShowResult(true);

            // 완료된 아이템 리스트에서 제거 
            // UX: Show result modal/feedback, then user clicks "Back" or "Next".
            // Here we remove it from the list so it doesn't show up again immediately.
            setReviewItems(prev => prev.filter(item => item.reviewItemId !== selectedReviewItem.reviewItemId));

        } catch (err) {
            console.error('[MyQuizWidget] 답안 제출 실패:', err);
            setShowResult(true);
        }
    };

    const handleSubmitMultiple = () => {
        if (selectedAnswer === null) return;
        handleSubmission(String(selectedAnswer));
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
                <div className="flex items-center justify-center h-64 text-text-tertiary">
                    <Loader2 className="animate-spin mr-2" size={20} />
                    복습 목록 로딩 중...
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
                title={viewMode === 'list' ? '오늘의 복습' : (selectedReviewItem?.question.category || '복습 퀴즈')}
                subtitle={viewMode === 'list' ? `${reviewItems.length}개의 복습 대기 중` : 'AI 맞춤형 복습'}
                showBackButton={viewMode === 'quiz'}
                onBack={handleBackToList}
                maximizePath="/quiz/my-quiz"
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
                                onSelect={handleSelectReviewItem}
                            />
                        </motion.div>
                    ) : currentQuiz ? (
                        <motion.div
                            key="quiz"
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.3 }}
                        >
                            {/* 문제 풀이 화면 */}
                            {currentQuiz.type === 'multiple' ? (
                                <QuizSingleChoice
                                    quiz={currentQuiz}
                                    questionNumber={1} // 단건 진행이므로 1
                                    selectedAnswer={selectedAnswer}
                                    showResult={showResult}
                                    onSelectAnswer={setSelectedAnswer}
                                    onSubmit={handleSubmitMultiple}
                                    onNext={handleBackToList} // 결과 확인 후 다음(목록으로)
                                    isLastQuestion={true} // 항상 마지막 문제 처리(단건)
                                />
                            ) : (
                                <QuizShortAnswer
                                    quiz={currentQuiz}
                                    questionNumber={1}
                                    userAnswer={shortAnswer}
                                    showResult={showResult}
                                    onChangeAnswer={(val) => setShortAnswer(val || '')}
                                    onSubmit={handleSubmitShort}
                                    onNext={handleBackToList}
                                    isLastQuestion={true}
                                />
                            )}
                            {/* 단건 풀이 시 QuizProgress는 굳이 필요 없을 수도 있지만, 남은 개수 보여주기용으로 활용 가능 */}
                            <div className="mt-4 text-center text-sm text-text-tertiary">
                                오늘 남은 복습: {reviewItems.length}개 (현재 문제 포함)
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
            <div className="max-h-[400px] overflow-y-auto space-y-3 pr-2 quiz-list-scroll">
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
                                'w-full p-4 rounded-xl border border-gray-100 text-left transition-colors',
                                'hover:border-primary/30 hover:bg-gray-50/50'
                            )}
                        >
                            <div className="flex items-start justify-between gap-3">
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className={cn('px-2 py-0.5 rounded text-xs font-medium', diffColor)}>
                                            {diffLabel}
                                        </span>
                                        <span className="text-xs text-text-tertiary bg-gray-100 px-2 py-0.5 rounded">
                                            {item.question.category || '일반'}
                                        </span>
                                        <span className="text-xs text-error font-medium flex items-center gap-1 ml-auto sm:ml-2">
                                            <Clock size={12} />
                                            {overdueLabel}
                                        </span>
                                    </div>
                                    <h4 className="font-medium text-text-primary line-clamp-2 text-sm leading-relaxed">
                                        {item.question.questionText}
                                    </h4>
                                </div>
                                <div className="flex-shrink-0 self-center">
                                    <Play size={18} className="text-primary/70" />
                                </div>
                            </div>
                        </button>
                    );
                })}
            </div>

            {showPagination && (
                <nav className="flex items-center justify-center gap-1 mt-4 pt-4 border-t border-gray-100">
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
