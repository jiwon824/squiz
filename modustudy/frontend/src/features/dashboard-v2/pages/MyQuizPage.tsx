import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Brain,
    ChevronLeft,
    ChevronRight,
    AlertTriangle,
    TrendingDown,
    RotateCcw,
    CheckCircle2,
    XCircle,
    Clock,
    BarChart3,
    BookOpen,
    Target,
    Play
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import {
    QuizSingleChoice,
    QuizMultipleChoice,
    QuizShortAnswer,
} from '@/shared/components';
import '../styles/DashboardV2.css';
import { getTodayReviews, getWrongAnswers, submitReview, ReviewItemDto, WrongAnswerSortType } from '../api/reviewApi';
import { useTimer } from '@/features/quiz/hooks/useTimer';
import { transformToQuizQuestion, indexToOptionId } from '@/shared/utils/quizUtils';

// 흔들리는 개념 타입
interface WeakConcept {
    id: number;
    concept: string;
    category: string;
    wrongCount: number;
    relatedQuestions: number[];
    lastReviewDate: string;
}

// Mock 데이터 - 흔들리는 개념들 (유지)
const MOCK_WEAK_CONCEPTS: WeakConcept[] = [
    {
        id: 1,
        concept: 'useEffect 생명주기',
        category: 'React',
        wrongCount: 5,
        relatedQuestions: [1, 5, 8],
        lastReviewDate: '2025-01-28',
    },
];

// transformToQuizQuestion과 indexToOptionId는 quizUtils.ts에서 import됨

type TabType = 'review' | 'wrong' | 'weak' | 'stats';

export const MyQuizPage: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<TabType>('review');

    // Data State
    const [todayReviews, setTodayReviews] = useState<ReviewItemDto[]>([]);
    const [wrongReviews, setWrongReviews] = useState<ReviewItemDto[]>([]);
    const [wrongSortType, setWrongSortType] = useState<WrongAnswerSortType>('MOST_WRONG');
    const [loading, setLoading] = useState(false);

    // UI Action State
    const [selectedReviewItem, setSelectedReviewItem] = useState<ReviewItemDto | null>(null);
    const [isRetrying, setIsRetrying] = useState(false);

    // 퀴즈 재도전 상태
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [selectedAnswers, setSelectedAnswers] = useState<number[]>([]);
    const [shortAnswer, setShortAnswer] = useState<string | null>('');
    const [showResult, setShowResult] = useState(false);
    const [isCorrectAnswer, setIsCorrectAnswer] = useState<boolean | null>(null); // 백엔드 채점 결과

    // Timer Hook
    const timer = useTimer();

    // 데이터 패칭 함수 분리 (useCallback으로 메모이제이션)
    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            const [todayData, wrongData] = await Promise.all([
                getTodayReviews(),
                getWrongAnswers(wrongSortType)
            ]);
            setTodayReviews(todayData?.items || []);
            setWrongReviews(wrongData?.items || []);
        } catch (error) {
            console.error("Failed to fetch reviews", error);
            setTodayReviews([]);
            setWrongReviews([]);
        } finally {
            setLoading(false);
        }
    }, [wrongSortType]);

    useEffect(() => {
        fetchData();
    }, [wrongSortType]);


    const handleBack = () => {
        if (isRetrying) {
            setIsRetrying(false);
            setSelectedReviewItem(null);
            setSelectedAnswer(null);
            setSelectedAnswers([]);
            setShortAnswer('');
            setShowResult(false);
            setIsCorrectAnswer(null);
        } else {
            navigate(-1);
        }
    };

    const handleRetry = (item: ReviewItemDto) => {
        setSelectedReviewItem(item);
        setIsRetrying(true);
        setSelectedAnswer(null);
        setSelectedAnswers([]);
        setShortAnswer('');
        setShowResult(false);
        setIsCorrectAnswer(null);
        timer.start();
    };

    const handleToggleAnswer = (index: number) => {
        setSelectedAnswers(prev =>
            prev.includes(index)
                ? prev.filter(i => i !== index)
                : [...prev, index].sort((a, b) => a - b)
        );
    };

    const handleSubmitMultiple = async () => {
        if (!selectedReviewItem) return;

        // 정답 제출 여부 체크
        const isSingleType = selectedReviewItem.question.questionType === 'MULTIPLE_CHOICE';
        const hasAnswer = isSingleType ? selectedAnswer !== null : selectedAnswers.length > 0;

        if (!hasAnswer) return;

        // API 호출
        const responseTimeMs = timer.stop();

        // 정답 문자열 생성 (Index -> Option ID 변환)
        const getOptionId = (idx: number) => {
            const option = selectedReviewItem.question.options[idx];
            return option?.id || indexToOptionId(idx);
        };

        const answerString = isSingleType
            ? getOptionId(selectedAnswer as number)
            : selectedAnswers.sort((a, b) => a - b).map(getOptionId).join(',');

        try {
            const result = await submitReview({
                contentType: selectedReviewItem.contentType,
                contentId: selectedReviewItem.contentId,
                userAnswer: answerString,
                responseTimeMs
            });
            // 백엔드 채점 결과 사용
            setIsCorrectAnswer(result.isCorrect);
        } catch (e) {
            console.error("Review submission failed", e);
            setIsCorrectAnswer(false); // 실패 시 오답 처리
        }
        setShowResult(true);
    };

    const handleSubmitShort = async () => {
        if ((shortAnswer !== null && !shortAnswer.trim()) || !selectedReviewItem) return;

        const responseTimeMs = timer.stop();

        try {
            const result = await submitReview({
                contentType: selectedReviewItem.contentType,
                contentId: selectedReviewItem.contentId,
                userAnswer: shortAnswer,
                responseTimeMs
            });
            // 백엔드 채점 결과 사용
            setIsCorrectAnswer(result.isCorrect);
        } catch (e) {
            console.error("Review submission failed", e);
            setIsCorrectAnswer(false); // 실패 시 오답 처리
        }
        setShowResult(true);
    };

    const handleFinishRetry = () => {
        setIsRetrying(false);
        setSelectedReviewItem(null);
        setSelectedAnswer(null);
        setSelectedAnswers([]);
        setShortAnswer('');
        setShowResult(false);
        setIsCorrectAnswer(null);
        // 목록으로 돌아갈 때 데이터 다시 가져오기
        fetchData();
    };

    // 통계 계산 (Using wrongReviews for stats now)
    const totalWrongCount = wrongReviews.reduce((sum, w) => sum + w.lapses, 0);
    const avgWrongCount = wrongReviews.length ? (totalWrongCount / wrongReviews.length).toFixed(1) : "0";


    return (
        <div className="py-8">
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 미니멀 헤더 */}
                <div className="mb-6">
                    {/* 브레드크럼 */}
                    <nav className="flex items-center gap-1.5 text-sm mb-2">
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="text-text-tertiary hover:text-primary transition-colors"
                        >
                            대시보드
                        </button>
                        <ChevronRight size={14} className="text-text-tertiary" />
                        <button
                            onClick={isRetrying ? handleBack : undefined}
                            className={cn(
                                isRetrying
                                    ? 'text-text-tertiary hover:text-primary transition-colors'
                                    : 'text-text-primary font-medium'
                            )}
                        >
                            퀴즈 관리
                        </button>
                        {isRetrying && selectedReviewItem && (
                            <>
                                <ChevronRight size={14} className="text-text-tertiary" />
                                <span className="text-text-primary font-medium">다시 풀기</span>
                            </>
                        )}
                    </nav>

                    {/* 페이지 타이틀 + 뒤로가기 */}
                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleBack}
                            className="text-text-tertiary hover:text-text-primary transition-colors"
                        >
                            <ChevronLeft size={24} strokeWidth={1.5} />
                        </button>
                        <h1 className="text-2xl font-bold text-text-primary mb-0">
                            {isRetrying ? '문제 다시 풀기' : '퀴즈 관리'}
                        </h1>
                        {isRetrying && selectedReviewItem && (
                            <span className="px-3 py-1 bg-primary/10 text-primary rounded-full text-sm font-medium">
                                {selectedReviewItem.question.category}
                            </span>
                        )}
                    </div>
                </div>

                {isRetrying && selectedReviewItem ? (
                    // 재도전 화면
                    <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
                        <div className="p-8">
                            <AnimatePresence mode="wait">
                                <motion.div
                                    key="retry"
                                    initial={{ opacity: 0, x: 20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -20 }}
                                >
                                    {/* 틀린 횟수 표시 */}
                                    <div className="flex items-center gap-2 mb-4 p-3 bg-error/5 rounded-xl border border-error/20">
                                        <AlertTriangle className="text-error" size={18} />
                                        <span className="text-sm text-error font-medium">
                                            이 문제를 {selectedReviewItem.lapses}번 틀렸습니다
                                        </span>
                                    </div>

                                    {selectedReviewItem.question.questionType === 'MULTIPLE_CHOICE' ? (
                                        <QuizSingleChoice
                                            quiz={transformToQuizQuestion(selectedReviewItem)}
                                            selectedAnswer={selectedAnswer}
                                            showResult={showResult}
                                            onSelectAnswer={setSelectedAnswer}
                                            onSubmit={handleSubmitMultiple}
                                        />
                                    ) : selectedReviewItem.question.questionType === 'MULTIPLE_CHOICE_MULTIPLE' ? (
                                        <QuizMultipleChoice
                                            quiz={transformToQuizQuestion(selectedReviewItem)}
                                            selectedAnswers={selectedAnswers}
                                            showResult={showResult}
                                            onToggleAnswer={handleToggleAnswer}
                                            onSubmit={handleSubmitMultiple}
                                        />
                                    ) : (
                                        <QuizShortAnswer
                                            quiz={transformToQuizQuestion(selectedReviewItem)}
                                            userAnswer={shortAnswer}
                                            showResult={showResult}
                                            onChangeAnswer={setShortAnswer}
                                            onSubmit={handleSubmitShort}
                                        />
                                    )}

                                    {showResult && (
                                        <motion.div
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            className="mt-4 space-y-4"
                                        >
                                            {/* 정답/오답 결과 표시 - 백엔드 채점 결과 사용 */}
                                            <div className={cn(
                                                'p-4 rounded-xl border-2 flex items-center gap-3',
                                                isCorrectAnswer
                                                    ? 'bg-accent/10 border-accent/30'
                                                    : 'bg-error/10 border-error/30'
                                            )}>
                                                {isCorrectAnswer ? (
                                                    <>
                                                        <CheckCircle2 className="text-accent" size={24} />
                                                        <div>
                                                            <p className="font-bold text-accent">정답입니다!</p>
                                                            <p className="text-sm text-text-secondary">이제 이 개념을 확실히 이해하셨네요.</p>
                                                        </div>
                                                    </>
                                                ) : (
                                                    <>
                                                        <XCircle className="text-error" size={24} />
                                                        <div>
                                                            <p className="font-bold text-error">오답입니다</p>
                                                            <p className="text-sm text-text-secondary">해설을 다시 확인해보세요.</p>
                                                        </div>
                                                    </>
                                                )}
                                            </div>

                                            <button
                                                onClick={handleFinishRetry}
                                                className="w-full py-3 rounded-xl font-bold bg-secondary hover:bg-secondary-dark text-white transition-colors"
                                            >
                                                목록으로 돌아가기
                                            </button>
                                        </motion.div>
                                    )}
                                </motion.div>
                            </AnimatePresence>
                        </div>
                    </div>
                ) : (
                    /* 좌측 탭 + 우측 콘텐츠 통합 레이아웃 */
                    <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
                        <div className="flex">
                            {/* 좌측 탭 네비게이션 */}
                            <div className="w-60 flex-shrink-0 bg-gray-50/70 relative">
                                {[
                                    { id: 'review' as TabType, label: '오늘의 복습', icon: Clock, count: todayReviews.length },
                                    { id: 'wrong' as TabType, label: '틀린 문제', icon: XCircle, count: wrongReviews.length },
                                    { id: 'weak' as TabType, label: '취약 개념', icon: TrendingDown, count: MOCK_WEAK_CONCEPTS.length },
                                    { id: 'stats' as TabType, label: '통계', icon: BarChart3 },
                                ].map(tab => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setActiveTab(tab.id)}
                                        className={cn(
                                            'w-full flex items-center gap-2 px-4 py-4 text-sm font-medium transition-colors relative',
                                            activeTab === tab.id
                                                ? 'text-primary bg-white -mr-px z-10'
                                                : 'text-text-secondary hover:text-text-primary hover:bg-gray-100 border-b border-gray-100'
                                        )}
                                    >
                                        {/* 왼쪽 인디케이터 */}
                                        {activeTab === tab.id && (
                                            <motion.div
                                                layoutId="tab-indicator"
                                                className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
                                            />
                                        )}
                                        <tab.icon size={18} />
                                        <span>{tab.label}</span>
                                        {tab.count !== undefined && (
                                            <span className={cn(
                                                'ml-auto px-1.5 py-0.5 rounded-full text-xs',
                                                activeTab === tab.id
                                                    ? 'bg-primary/10 text-primary'
                                                    : 'bg-gray-200 text-text-tertiary'
                                            )}>
                                                {tab.count}
                                            </span>
                                        )}
                                    </button>
                                ))}
                                {/* 우측 border line */}
                                <div className="absolute top-0 right-0 bottom-0 w-px bg-gray-200" />
                            </div>

                            {/* 우측 콘텐츠 영역 */}
                            <div className="flex-1 p-8">
                                {/* 로딩 상태 표시 */}
                                {loading ? (
                                    <div className="flex flex-col items-center justify-center py-20">
                                        <div className="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin mb-4" />
                                        <p className="text-text-secondary font-medium">데이터를 불러오는 중...</p>
                                    </div>
                                ) : (
                                    <AnimatePresence mode="wait">
                                        {activeTab === 'review' && (
                                            <motion.div
                                                key="review"
                                                initial={{ opacity: 0, x: 10 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                exit={{ opacity: 0, x: -10 }}
                                            >
                                                <ReviewItemList
                                                    items={todayReviews}
                                                    onRetry={handleRetry}
                                                    type="review"
                                                />
                                            </motion.div>
                                        )}

                                        {activeTab === 'wrong' && (
                                            <motion.div
                                                key="wrong"
                                                initial={{ opacity: 0, x: 10 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                exit={{ opacity: 0, x: -10 }}
                                            >
                                                {/* Sorting Toggle */}
                                                <div className="mb-4 flex gap-2">
                                                    <button
                                                        onClick={() => setWrongSortType('MOST_WRONG')}
                                                        className={cn(
                                                            'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                                                            wrongSortType === 'MOST_WRONG'
                                                                ? 'bg-primary text-white'
                                                                : 'bg-gray-100 text-text-secondary hover:bg-gray-200'
                                                        )}
                                                    >
                                                        많이 틀린 순
                                                    </button>
                                                    <button
                                                        onClick={() => setWrongSortType('FSRS_RECOMMENDED')}
                                                        className={cn(
                                                            'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                                                            wrongSortType === 'FSRS_RECOMMENDED'
                                                                ? 'bg-primary text-white'
                                                                : 'bg-gray-100 text-text-secondary hover:bg-gray-200'
                                                        )}
                                                    >
                                                        복습 우선순위
                                                    </button>
                                                    <button
                                                        onClick={() => setWrongSortType('LATEST')}
                                                        className={cn(
                                                            'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                                                            wrongSortType === 'LATEST'
                                                                ? 'bg-primary text-white'
                                                                : 'bg-gray-100 text-text-secondary hover:bg-gray-200'
                                                        )}
                                                    >
                                                        최신순
                                                    </button>
                                                </div>
                                                <ReviewItemList
                                                    items={wrongReviews}
                                                    onRetry={handleRetry}
                                                    type="wrong"
                                                />
                                            </motion.div>
                                        )}

                                        {activeTab === 'weak' && (
                                            <motion.div
                                                key="weak"
                                                initial={{ opacity: 0, x: 10 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                exit={{ opacity: 0, x: -10 }}
                                            >
                                                <WeakConceptList concepts={MOCK_WEAK_CONCEPTS} />
                                            </motion.div>
                                        )}

                                        {activeTab === 'stats' && (
                                            <motion.div
                                                key="stats"
                                                initial={{ opacity: 0, x: 10 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                exit={{ opacity: 0, x: -10 }}
                                            >
                                                <StatsView
                                                    wrongReviews={wrongReviews}
                                                    totalWrong={wrongReviews.length}
                                                    totalWrongCount={totalWrongCount}
                                                    avgWrongCount={avgWrongCount}
                                                    weakConcepts={MOCK_WEAK_CONCEPTS}
                                                />
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                )}
                            </div>
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
};

// 문제 리스트 컴포넌트
interface ReviewItemListProps {
    items: ReviewItemDto[];
    onRetry: (item: ReviewItemDto) => void;
    type: 'review' | 'wrong';
}

const ReviewItemList: React.FC<ReviewItemListProps> = ({ items, onRetry, type }) => {
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 5;
    const totalPages = Math.ceil(items.length / itemsPerPage);
    const showPagination = items.length > itemsPerPage;

    const currentItems = items.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    // Backend already handles sorting based on selected sortType
    // No need to re-sort on frontend
    const displayItems = currentItems;

    const getDifficultyColor = (diff: number) => {
        if (diff < 3) return 'bg-accent/20 text-accent-dark';
        if (diff < 7) return 'bg-secondary/20 text-secondary-dark';
        return 'bg-error/20 text-error';
    };

    const getDifficultyLabel = (diff: number) => {
        if (diff < 3) return '쉬움';
        if (diff < 7) return '보통';
        return '어려움';
    };

    return (
        <div>
            {items.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 text-center">
                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                        <CheckCircle2 size={32} className="text-gray-400" />
                    </div>
                    <h3 className="text-lg font-bold text-text-primary mb-1">
                        {type === 'review' ? '오늘의 복습 끝!' : '틀린 문제가 없습니다'}
                    </h3>
                    <p className="text-text-tertiary">
                        {type === 'review'
                            ? '오늘은 더 이상 복습할 내용이 없습니다. 훌륭해요!'
                            : '완벽하게 이해하고 계시네요.'}
                    </p>
                </div>
            )}

            {/* 리스트 아이템 */}
            <div className="space-y-4">
                {displayItems.map((item) => (
                    <div
                        key={item.reviewItemId}
                        className="px-5 py-4 rounded-xl bg-white shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] hover:bg-gray-50 transition-all"
                    >
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                                {/* 메타 정보 */}
                                <div className="flex items-center gap-2 mb-3">
                                    <span className={cn(
                                        'px-2.5 py-1 rounded-full text-xs font-medium',
                                        getDifficultyColor(item.difficulty)
                                    )}>
                                        {getDifficultyLabel(item.difficulty)}
                                    </span>
                                    <span className="px-2.5 py-1 bg-gray-50 rounded-full text-xs font-medium text-text-secondary">
                                        {item.question.category || '일반'}
                                    </span>
                                    {type === 'wrong' && (
                                        <span className="px-2.5 py-1 bg-error/5 rounded-full text-xs font-medium text-error flex items-center gap-1">
                                            <XCircle size={12} />
                                            {item.lapses}회 오답
                                        </span>
                                    )}
                                    {type === 'review' && (
                                        <span className="px-2.5 py-1 bg-primary/5 rounded-full text-xs font-medium text-primary flex items-center gap-1">
                                            <Clock size={12} />
                                            오늘 복습
                                        </span>
                                    )}
                                </div>

                                {/* 문제 */}
                                <p className="text-text-primary font-medium line-clamp-2 leading-relaxed">
                                    {item.question.questionText}
                                </p>

                                {/* 마지막 날짜 */}
                                <div className="flex items-center gap-1 mt-3 text-xs text-text-tertiary">
                                    <Clock size={12} />
                                    <span>
                                        {type === 'review' ? '복습 예정일: ' : '마지막 오답: '}
                                        {new Date(item.nextReviewAt).toLocaleDateString()}
                                    </span>
                                </div>
                            </div>

                            {/* 다시 풀기 버튼 */}
                            <button
                                onClick={() => onRetry(item)}
                                className="flex-shrink-0 flex items-center gap-2 px-4 py-2.5 rounded-lg bg-primary/5 text-primary hover:bg-primary/10 transition-colors text-sm font-medium"
                            >
                                <RotateCcw size={14} />
                                <span>{type === 'review' ? '복습하기' : '다시 풀기'}</span>
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* 페이지네이션 */}
            {showPagination && (
                <nav className="flex items-center justify-center gap-2 mt-6 pt-5 border-t border-gray-50">
                    <button
                        type="button"
                        onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                        disabled={currentPage === 1}
                        className={cn(
                            'flex items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-colors',
                            currentPage === 1
                                ? 'text-gray-300 cursor-not-allowed'
                                : 'text-text-secondary hover:text-text-primary'
                        )}
                    >
                        <ChevronLeft size={18} />
                        <span className="hidden sm:inline">이전</span>
                    </button>

                    <div className="flex items-center gap-1">
                        {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                            <button
                                key={page}
                                type="button"
                                onClick={() => setCurrentPage(page)}
                                className={cn(
                                    'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                                    page === currentPage
                                        ? 'text-primary bg-primary/10'
                                        : 'text-text-secondary hover:text-text-primary'
                                )}
                            >
                                {page}
                            </button>
                        ))}
                    </div>

                    <button
                        type="button"
                        onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                        disabled={currentPage === totalPages}
                        className={cn(
                            'flex items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-colors',
                            currentPage === totalPages
                                ? 'text-gray-300 cursor-not-allowed'
                                : 'text-text-secondary hover:text-text-primary'
                        )}
                    >
                        <span className="hidden sm:inline">다음</span>
                        <ChevronRight size={18} />
                    </button>
                </nav>
            )}
        </div>
    );
};

// 취약 개념 리스트 컴포넌트
interface WeakConceptListProps {
    concepts: WeakConcept[];
}

const WeakConceptList: React.FC<WeakConceptListProps> = ({ concepts }) => {
    // 틀린 횟수로 정렬
    const sortedConcepts = [...concepts].sort((a, b) => b.wrongCount - a.wrongCount);

    return (
        <div className="space-y-4">
            {sortedConcepts.map((concept, index) => (
                <div
                    key={concept.id}
                    className="px-5 py-4 rounded-xl border border-gray-100 hover:border-gray-200 transition-all"
                >
                    <div className="flex items-start justify-between gap-4">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-3">
                                {/* 순위 뱃지 */}
                                <span className={cn(
                                    'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold',
                                    index === 0 ? 'bg-error/10 text-error' :
                                        index === 1 ? 'bg-warning/10 text-warning' :
                                            index === 2 ? 'bg-secondary/10 text-secondary' :
                                                'bg-gray-50 text-gray-400'
                                )}>
                                    {index + 1}
                                </span>
                                <span className="px-2.5 py-1 bg-gray-50 rounded-full text-xs font-medium text-text-secondary">
                                    {concept.category}
                                </span>
                            </div>

                            <h4 className="font-bold text-text-primary mb-2">
                                {concept.concept}
                            </h4>

                            <div className="flex items-center gap-4 text-xs text-text-tertiary">
                                <span className="flex items-center gap-1.5">
                                    <XCircle size={12} className="text-error/70" />
                                    관련 오답 {concept.wrongCount}회
                                </span>
                                <span className="flex items-center gap-1.5">
                                    <BookOpen size={12} />
                                    관련 문제 {concept.relatedQuestions.length}개
                                </span>
                                <span className="flex items-center gap-1.5">
                                    <Clock size={12} />
                                    {concept.lastReviewDate}
                                </span>
                            </div>
                        </div>

                        {/* 취약도 표시 바 */}
                        <div className="flex-shrink-0 w-28">
                            <div className="text-xs text-text-tertiary mb-1.5 text-right">취약도</div>
                            <div className="w-full bg-gray-100 rounded-full h-2">
                                <div
                                    className={cn(
                                        'h-2 rounded-full transition-all',
                                        concept.wrongCount >= 4 ? 'bg-error/80' :
                                            concept.wrongCount >= 3 ? 'bg-warning/80' :
                                                'bg-secondary/80'
                                    )}
                                    style={{ width: `${Math.min(concept.wrongCount * 20, 100)}%` }}
                                />
                            </div>
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
};

// 통계 뷰 컴포넌트
interface StatsViewProps {
    wrongReviews: ReviewItemDto[];
    totalWrong: number;
    totalWrongCount: number;
    avgWrongCount: string;
    weakConcepts: WeakConcept[];
}

// 기술 스택 숙련도 Mock 데이터 (공식 브랜드 컬러 적용)
const TECH_STACK_PROFICIENCY = [
    {
        name: 'React',
        level: 75,
        brandColor: '#61DBFB',
        quizCount: 24,
        correctRate: 78,
        logo: (
            <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                <path d="M12 10.11c1.03 0 1.87.84 1.87 1.89 0 1-.84 1.85-1.87 1.85S10.13 13 10.13 12c0-1.05.84-1.89 1.87-1.89M7.37 20c.63.38 2.01-.2 3.6-1.7-.52-.59-1.03-1.23-1.51-1.9a22.7 22.7 0 01-2.4-.36c-.51 2.14-.32 3.61.31 3.96m.71-5.74l-.29-.51c-.11.29-.22.58-.29.86.27.06.57.11.88.16l-.3-.51m6.54-.76l.81-1.5-.81-1.5c-.3-.53-.62-1-.91-1.47C13.17 9 12.6 9 12 9s-1.17 0-1.71.03c-.29.47-.61.94-.91 1.47L8.57 12l.81 1.5c.3.53.62 1 .91 1.47.54.03 1.11.03 1.71.03s1.17 0 1.71-.03c.29-.47.61-.94.91-1.47M12 6.78c-.19.22-.39.45-.59.72h1.18c-.2-.27-.4-.5-.59-.72m0 10.44c.19-.22.39-.45.59-.72h-1.18c.2.27.4.5.59.72M16.62 4c-.62-.38-2 .2-3.59 1.7.52.59 1.03 1.23 1.51 1.9.82.08 1.63.2 2.4.36.51-2.14.32-3.61-.32-3.96m-.7 5.74l.29.51c.11-.29.22-.58.29-.86-.27-.06-.57-.11-.88-.16l.3.51m1.45-7.05c1.47.84 1.63 3.05 1.01 5.63 2.54.75 4.37 1.99 4.37 3.68s-1.83 2.93-4.37 3.68c.62 2.58.46 4.79-1.01 5.63-1.46.84-3.45-.12-5.37-1.95-1.92 1.83-3.91 2.79-5.38 1.95-1.46-.84-1.62-3.05-1-5.63-2.54-.75-4.37-1.99-4.37-3.68s1.83-2.93 4.37-3.68c-.62-2.58-.46-4.79 1-5.63 1.47-.84 3.46.12 5.38 1.95 1.92-1.83 3.91-2.79 5.37-1.95M17.08 12c.34.75.64 1.5.89 2.26 2.1-.63 3.28-1.53 3.28-2.26s-1.18-1.63-3.28-2.26c-.25.76-.55 1.51-.89 2.26M6.92 12c-.34-.75-.64-1.5-.89-2.26-2.1.63-3.28 1.53-3.28 2.26s1.18 1.63 3.28 2.26c.25-.76.55-1.51.89-2.26m9 2.26l-.3.51c.31-.05.61-.1.88-.16-.07-.28-.18-.57-.29-.86l-.29.51m-9.46.86c.27.06.57.11.88.16l-.3-.51-.29.51c-.11.29-.22.58-.29.86m9.46-5.24l.3-.51c-.31.05-.61.1-.88.16.07.28.18.57.29.86l.29-.51m-9.46-.86c-.27-.06-.57-.11-.88-.16l.3.51.29-.51c.11-.29.22-.58.29-.86" />
            </svg>
        )
    },
    {
        name: 'TypeScript',
        level: 60,
        brandColor: '#3178C6',
        quizCount: 18,
        correctRate: 65,
        logo: (
            <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                <path d="M1.125 0C.502 0 0 .502 0 1.125v21.75C0 23.498.502 24 1.125 24h21.75c.623 0 1.125-.502 1.125-1.125V1.125C24 .502 23.498 0 22.875 0zm17.363 9.75c.612 0 1.154.037 1.627.111a6.38 6.38 0 0 1 1.306.34v2.458a3.95 3.95 0 0 0-.643-.361 5.093 5.093 0 0 0-.717-.26 5.453 5.453 0 0 0-1.426-.2c-.3 0-.573.028-.819.086a2.1 2.1 0 0 0-.623.242c-.17.104-.3.229-.393.374a.888.888 0 0 0-.14.49c0 .196.053.373.156.529.104.156.252.304.443.444s.423.276.696.41c.273.135.582.274.926.416.47.197.892.407 1.266.628.374.222.695.473.963.753.268.279.472.598.614.957.142.359.214.776.214 1.253 0 .657-.125 1.21-.373 1.656a3.033 3.033 0 0 1-1.012 1.085 4.38 4.38 0 0 1-1.487.596c-.566.12-1.163.18-1.79.18a9.916 9.916 0 0 1-1.84-.164 5.544 5.544 0 0 1-1.512-.493v-2.63a5.033 5.033 0 0 0 3.237 1.2c.333 0 .624-.03.872-.09.249-.06.456-.144.623-.25.166-.108.29-.234.373-.38a1.023 1.023 0 0 0-.074-1.089 2.12 2.12 0 0 0-.537-.5 5.597 5.597 0 0 0-.807-.444 27.72 27.72 0 0 0-1.007-.436c-.918-.383-1.602-.852-2.053-1.405-.45-.553-.676-1.222-.676-2.005 0-.614.123-1.141.369-1.582.246-.441.58-.804 1.004-1.089a4.494 4.494 0 0 1 1.47-.629 7.536 7.536 0 0 1 1.77-.201zm-15.113.188h9.563v2.166H9.506v9.646H6.789v-9.646H3.375z" />
            </svg>
        )
    },
    {
        name: 'JavaScript',
        level: 85,
        brandColor: '#F7DF1E',
        quizCount: 32,
        correctRate: 88,
        logo: (
            <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                <path d="M0 0h24v24H0V0zm22.034 18.276c-.175-1.095-.888-2.015-3.003-2.873-.736-.345-1.554-.585-1.797-1.14-.091-.33-.105-.51-.046-.705.15-.646.915-.84 1.515-.66.39.12.75.42.976.9 1.034-.676 1.034-.676 1.755-1.125-.27-.42-.404-.601-.586-.78-.63-.705-1.469-1.065-2.834-1.034l-.705.089c-.676.165-1.32.525-1.71 1.005-1.14 1.291-.811 3.541.569 4.471 1.365 1.02 3.361 1.244 3.616 2.205.24 1.17-.87 1.545-1.966 1.41-.811-.18-1.26-.586-1.755-1.336l-1.83 1.051c.21.48.45.689.81 1.109 1.74 1.756 6.09 1.666 6.871-1.004.029-.09.24-.705.074-1.65l.046.067zm-8.983-7.245h-2.248c0 1.938-.009 3.864-.009 5.805 0 1.232.063 2.363-.138 2.711-.33.689-1.18.601-1.566.48-.396-.196-.597-.466-.83-.855-.063-.105-.11-.196-.127-.196l-1.825 1.125c.305.63.75 1.172 1.324 1.517.855.51 2.004.675 3.207.405.783-.226 1.458-.691 1.811-1.411.51-.93.402-2.07.397-3.346.012-2.054 0-4.109 0-6.179l.004-.056z" />
            </svg>
        )
    },
    {
        name: 'Tailwind',
        level: 70,
        brandColor: '#06B6D4',
        quizCount: 12,
        correctRate: 72,
        logo: (
            <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                <path d="M12.001 4.8c-3.2 0-5.2 1.6-6 4.8 1.2-1.6 2.6-2.2 4.2-1.8.913.228 1.565.89 2.288 1.624C13.666 10.618 15.027 12 18.001 12c3.2 0 5.2-1.6 6-4.8-1.2 1.6-2.6 2.2-4.2 1.8-.913-.228-1.565-.89-2.288-1.624C16.337 6.182 14.976 4.8 12.001 4.8zm-6 7.2c-3.2 0-5.2 1.6-6 4.8 1.2-1.6 2.6-2.2 4.2-1.8.913.228 1.565.89 2.288 1.624 1.177 1.194 2.538 2.576 5.512 2.576 3.2 0 5.2-1.6 6-4.8-1.2 1.6-2.6 2.2-4.2 1.8-.913-.228-1.565-.89-2.288-1.624C10.337 13.382 8.976 12 6.001 12z" />
            </svg>
        )
    },
    {
        name: 'Node.js',
        level: 45,
        brandColor: '#339933',
        quizCount: 8,
        correctRate: 50,
        logo: (
            <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                <path d="M11.998 24c-.321 0-.641-.084-.922-.247L8.14 22.016c-.438-.245-.224-.332-.08-.383.548-.19.659-.233 1.243-.563.062-.034.142-.021.205.016l2.255 1.339c.082.045.198.045.275 0l8.795-5.076c.082-.047.134-.141.134-.238V6.921c0-.099-.053-.193-.137-.242l-8.791-5.072c-.081-.047-.189-.047-.271 0L3.075 6.68c-.085.049-.139.143-.139.242v10.19c0 .097.054.189.137.236l2.409 1.392c1.307.654 2.108-.116 2.108-.891V7.787c0-.142.114-.253.256-.253h1.115c.139 0 .255.112.255.253v10.064c0 1.745-.951 2.745-2.604 2.745-.509 0-.909 0-2.026-.551L2.28 18.675c-.57-.329-.922-.943-.922-1.604V6.881c0-.66.351-1.274.922-1.603L11.075.203c.559-.321 1.303-.321 1.858 0l8.794 5.075c.57.329.924.943.924 1.603v10.19c0 .66-.354 1.273-.924 1.604l-8.794 5.076c-.28.163-.6.247-.924.247h-.011zm2.722-7.022c-3.863 0-4.673-1.774-4.673-3.262 0-.141.113-.253.255-.253h1.137c.127 0 .232.092.251.215.171 1.158.679 1.739 2.989 1.739 1.839 0 2.621-.416 2.621-1.391 0-.562-.222-.979-3.078-1.26-2.386-.234-3.862-.763-3.862-2.671 0-1.76 1.484-2.806 3.972-2.806 2.795 0 4.178.97 4.352 3.055a.257.257 0 01-.064.189.258.258 0 01-.182.078h-1.144c-.12 0-.226-.085-.249-.201-.277-1.227-.944-1.62-2.712-1.62-1.997 0-2.231.696-2.231 1.217 0 .632.275.816 2.983 1.174 2.68.355 3.958.858 3.958 2.741 0 1.903-1.586 2.984-4.352 2.984l.029-.028z" />
            </svg>
        )
    },
];

const StatsView: React.FC<StatsViewProps> = ({ wrongReviews, totalWrong, totalWrongCount, avgWrongCount, weakConcepts }) => {
    // 카테고리별 오답 문제 수 (틀린 문제 기반)
    const categoryStats = wrongReviews.reduce((acc, wrong) => {
        const category = wrong.question.category || '기타';
        acc[category] = (acc[category] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    // 카테고리별 총 오답 횟수
    const categoryWrongCounts = wrongReviews.reduce((acc, wrong) => {
        const category = wrong.question.category || '기타';
        acc[category] = (acc[category] || 0) + wrong.lapses;
        return acc;
    }, {} as Record<string, number>);

    const maxCategoryCount = Math.max(...Object.values(categoryStats), 1);

    return (
        <div className="space-y-6">
            {/* 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                {[
                    { label: '틀린 문제 수', value: totalWrong, icon: XCircle, color: 'error' },
                    { label: '총 오답 횟수', value: totalWrongCount, icon: AlertTriangle, color: 'warning' },
                    { label: '평균 오답 횟수', value: avgWrongCount, icon: Target, color: 'secondary' },
                ].map((stat) => (
                    <div
                        key={stat.label}
                        className="rounded-xl border border-gray-100 px-5 py-4"
                    >
                        <div className="flex items-center gap-4">
                            <div className={cn(
                                'w-12 h-12 rounded-xl flex items-center justify-center',
                                stat.color === 'error' && 'bg-error/5',
                                stat.color === 'warning' && 'bg-warning/5',
                                stat.color === 'secondary' && 'bg-secondary/5',
                            )}>
                                <stat.icon className={cn(
                                    stat.color === 'error' && 'text-error/80',
                                    stat.color === 'warning' && 'text-warning/80',
                                    stat.color === 'secondary' && 'text-secondary/80',
                                )} size={22} />
                            </div>
                            <div>
                                <div className="text-2xl font-bold text-text-primary">{stat.value}</div>
                                <div className="text-sm text-text-tertiary">{stat.label}</div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* 카테고리별 오답 분포 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">카테고리별 오답 분포</h3>
                </div>
                <div className="p-5 space-y-5">
                    {Object.entries(categoryStats)
                        .sort(([, a], [, b]) => b - a)
                        .map(([category, count]) => {
                            const percentage = (count / maxCategoryCount) * 100;
                            return (
                                <div key={category}>
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-sm font-medium text-text-primary">{category}</span>
                                        <span className="text-sm text-text-tertiary">
                                            {count}문제 · 총 {categoryWrongCounts[category]}회 오답
                                        </span>
                                    </div>
                                    <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden">
                                        <motion.div
                                            initial={{ width: 0 }}
                                            animate={{ width: `${percentage}%` }}
                                            transition={{ duration: 0.8, ease: 'easeOut' }}
                                            className={cn(
                                                'h-3 rounded-full',
                                                percentage >= 70 ? 'bg-error' :
                                                    percentage >= 40 ? 'bg-warning' :
                                                        'bg-primary'
                                            )}
                                        />
                                    </div>
                                </div>
                            );
                        })}
                </div>
            </div>

            {/* 기술 스택 숙련도 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">나의 기술 스택 범위</h3>
                    <p className="text-xs text-text-tertiary mt-1">퀴즈 학습 데이터 기반 숙련도</p>
                </div>
                <div className="p-5">
                    <div className="space-y-5">
                        {TECH_STACK_PROFICIENCY.map((tech) => (
                            <div key={tech.name}>
                                <div className="flex items-center justify-between mb-2">
                                    <div className="flex items-center gap-3">
                                        {/* 공식 로고 */}
                                        <span
                                            className="flex items-center justify-center w-6 h-6 rounded-md"
                                            style={{ backgroundColor: `${tech.brandColor}15` }}
                                        >
                                            <span style={{ color: tech.brandColor }}>{tech.logo}</span>
                                        </span>
                                        <span className="text-sm font-medium text-text-primary">{tech.name}</span>
                                    </div>
                                    <div className="flex items-center gap-4 text-xs text-text-tertiary">
                                        <span>퀴즈 {tech.quizCount}개</span>
                                        <span>정답률 {tech.correctRate}%</span>
                                        <span className="font-semibold text-text-primary">{tech.level}%</span>
                                    </div>
                                </div>
                                <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                                    <motion.div
                                        initial={{ width: 0 }}
                                        animate={{ width: `${tech.level}%` }}
                                        transition={{ duration: 1, ease: 'easeOut', delay: 0.1 }}
                                        className="h-2.5 rounded-full"
                                        style={{ backgroundColor: tech.brandColor }}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* 원형 차트 스타일 요약 */}
                    <div className="mt-6 pt-5 border-t border-gray-50">
                        <div className="grid grid-cols-5 gap-3">
                            {TECH_STACK_PROFICIENCY.map((tech) => (
                                <div key={tech.name} className="text-center">
                                    <div className="relative w-16 h-16 mx-auto mb-2">
                                        {/* 배경 원 */}
                                        <svg className="w-full h-full -rotate-90" viewBox="0 0 64 64">
                                            <circle
                                                cx="32"
                                                cy="32"
                                                r="28"
                                                fill="none"
                                                stroke="#f3f4f6"
                                                strokeWidth="5"
                                            />
                                            <motion.circle
                                                cx="32"
                                                cy="32"
                                                r="28"
                                                fill="none"
                                                stroke={tech.brandColor}
                                                strokeWidth="5"
                                                strokeLinecap="round"
                                                strokeDasharray={`${2 * Math.PI * 28}`}
                                                initial={{ strokeDashoffset: 2 * Math.PI * 28 }}
                                                animate={{ strokeDashoffset: 2 * Math.PI * 28 * (1 - tech.level / 100) }}
                                                transition={{ duration: 1.2, ease: 'easeOut', delay: 0.2 }}
                                            />
                                        </svg>
                                        {/* 중앙 로고 */}
                                        <div className="absolute inset-0 flex items-center justify-center">
                                            <span style={{ color: tech.brandColor }}>{tech.logo}</span>
                                        </div>
                                    </div>
                                    <div className="flex flex-col items-center">
                                        <span className="text-xs font-medium text-text-secondary">{tech.name}</span>
                                        <span className="text-[10px] text-text-tertiary">{tech.level}%</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* 학습 제안 */}
            <div className="rounded-xl border border-gray-100 overflow-hidden">
                <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
                    <h3 className="font-semibold text-text-primary mb-0">학습 제안</h3>
                </div>
                <div className="p-5">
                    <div className="px-5 py-4 bg-primary/5 rounded-xl">
                        <div className="flex items-start gap-4">
                            <div className="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                                <Brain className="text-primary/80" size={20} />
                            </div>
                            <div className="flex-1">
                                <h4 className="font-semibold text-text-primary mb-1">
                                    {weakConcepts[0]?.concept} 집중 학습 권장
                                </h4>
                                <p className="text-sm text-text-secondary leading-relaxed">
                                    가장 많이 틀린 개념입니다. 관련 문제를 다시 풀어보고,
                                    해당 개념에 대한 추가 학습을 권장합니다.
                                </p>
                                <button className="mt-4 flex items-center gap-2 text-sm font-medium text-primary hover:text-primary-dark transition-colors">
                                    <Play size={14} />
                                    관련 문제 풀기
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MyQuizPage;
