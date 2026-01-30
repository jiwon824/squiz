import React, { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Brain, ChevronLeft, ChevronRight, Clock, CheckCircle2, Circle, Play } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import {
    MultipleChoiceQuiz,
    ShortAnswerQuiz,
    QuizProgress,
    QuizQuestion,
    WidgetHeader,
    WidgetContainer
} from '@/shared/components';

// 퀴즈 세트 타입
interface QuizSet {
    id: number;
    title: string;
    meetingTitle: string;
    createdAt: string;
    questionCount: number;
    completedCount: number;
    questions: QuizQuestion[];
}

// Mock 퀴즈 세트 데이터
const MOCK_QUIZ_SETS: QuizSet[] = [
    {
        id: 1,
        title: 'React Hooks 복습',
        meetingTitle: '프론트엔드 스터디 3회차',
        createdAt: '2025-01-28',
        questionCount: 4,
        completedCount: 0,
        questions: [
            {
                id: 1,
                type: 'multiple',
                question: 'useEffect의 클린업 함수는 언제 실행되나요?',
                options: ['컴포넌트 마운트 시', '언마운트 시 또는 다음 effect 실행 전', '렌더링 직후', '상태 변경 시'],
                correctAnswer: 1,
                explanation: '클린업 함수는 컴포넌트 언마운트 시 또는 다음 effect가 실행되기 전에 호출됩니다.',
                difficulty: 'medium',
                category: 'React',
            },
            {
                id: 2,
                type: 'short',
                question: 'React에서 컴포넌트의 상태를 관리하기 위해 사용하는 훅의 이름은?',
                correctAnswer: 'useState',
                explanation: 'useState는 함수형 컴포넌트에서 상태를 관리하기 위한 기본 훅입니다.',
                difficulty: 'easy',
                category: 'React',
            },
            {
                id: 3,
                type: 'multiple',
                question: 'useMemo의 주요 목적은?',
                options: ['상태 관리', '사이드 이펙트 처리', '값의 메모이제이션', 'DOM 접근'],
                correctAnswer: 2,
                explanation: 'useMemo는 계산 비용이 큰 값을 메모이제이션하여 불필요한 재계산을 방지합니다.',
                difficulty: 'medium',
                category: 'React',
            },
            {
                id: 4,
                type: 'short',
                question: 'useRef로 생성한 객체의 값을 접근할 때 사용하는 속성명은?',
                correctAnswer: 'current',
                explanation: 'useRef는 .current 속성을 통해 변경 가능한 값을 저장합니다.',
                difficulty: 'easy',
                category: 'React',
            },
        ],
    },
    {
        id: 2,
        title: 'TypeScript 기초',
        meetingTitle: '프론트엔드 스터디 2회차',
        createdAt: '2025-01-25',
        questionCount: 3,
        completedCount: 3,
        questions: [
            {
                id: 1,
                type: 'multiple',
                question: 'TypeScript의 Partial<T> 유틸리티 타입의 역할은?',
                options: ['모든 속성을 필수로 만듦', '모든 속성을 선택적으로 만듦', '특정 속성만 선택', '속성 제거'],
                correctAnswer: 1,
                explanation: 'Partial<T>는 타입 T의 모든 속성을 선택적(optional)으로 만드는 유틸리티 타입입니다.',
                difficulty: 'easy',
                category: 'TypeScript',
            },
            {
                id: 2,
                type: 'short',
                question: 'TypeScript에서 타입을 정의할 때 사용하는 키워드 두 가지는? (하나만 입력)',
                correctAnswer: 'type',
                explanation: 'type과 interface 키워드를 사용하여 타입을 정의할 수 있습니다.',
                difficulty: 'easy',
                category: 'TypeScript',
            },
            {
                id: 3,
                type: 'multiple',
                question: 'never 타입은 언제 사용되나요?',
                options: ['null 값을 표현할 때', '절대 반환하지 않는 함수', '빈 배열을 표현할 때', 'undefined 값을 표현할 때'],
                correctAnswer: 1,
                explanation: 'never 타입은 절대 발생하지 않는 값의 타입으로, 항상 예외를 던지거나 무한 루프를 도는 함수에 사용됩니다.',
                difficulty: 'hard',
                category: 'TypeScript',
            },
        ],
    },
    {
        id: 3,
        title: 'JavaScript 비동기',
        meetingTitle: '프론트엔드 스터디 1회차',
        createdAt: '2025-01-22',
        questionCount: 2,
        completedCount: 1,
        questions: [
            {
                id: 1,
                type: 'short',
                question: 'JavaScript에서 비동기 함수를 정의할 때 사용하는 키워드는?',
                correctAnswer: 'async',
                explanation: 'async 키워드를 함수 앞에 붙이면 해당 함수는 항상 Promise를 반환하는 비동기 함수가 됩니다.',
                difficulty: 'easy',
                category: 'JavaScript',
            },
            {
                id: 2,
                type: 'multiple',
                question: 'Promise.all()의 동작 방식은?',
                options: [
                    '순차적으로 실행',
                    '병렬로 실행하고 모두 완료 시 결과 반환',
                    '가장 빠른 것만 반환',
                    '실패한 것만 반환'
                ],
                correctAnswer: 1,
                explanation: 'Promise.all()은 여러 Promise를 병렬로 실행하고, 모든 Promise가 완료되면 결과 배열을 반환합니다.',
                difficulty: 'medium',
                category: 'JavaScript',
            },
        ],
    },
];

type ViewMode = 'list' | 'quiz';

export const MyQuizWidget: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [selectedQuizSet, setSelectedQuizSet] = useState<QuizSet | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [shortAnswer, setShortAnswer] = useState('');
    const [showResult, setShowResult] = useState(false);
    const [score, setScore] = useState({ correct: 0, total: 0 });

    const handleSelectQuizSet = (quizSet: QuizSet) => {
        setSelectedQuizSet(quizSet);
        setViewMode('quiz');
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setShortAnswer('');
        setShowResult(false);
        setScore({ correct: 0, total: 0 });
    };

    const handleBackToList = () => {
        setViewMode('list');
        setSelectedQuizSet(null);
    };

    const currentQuiz = selectedQuizSet?.questions[currentIndex];
    const isLastQuestion = selectedQuizSet ? currentIndex >= selectedQuizSet.questions.length - 1 : false;

    const handleSubmitMultiple = () => {
        if (selectedAnswer === null || !currentQuiz) return;

        const isCorrect = selectedAnswer === currentQuiz.correctAnswer;
        setScore((prev) => ({
            correct: prev.correct + (isCorrect ? 1 : 0),
            total: prev.total + 1,
        }));
        setShowResult(true);
    };

    const handleSubmitShort = () => {
        if (!shortAnswer.trim() || !currentQuiz) return;

        const correctAnswer = String(currentQuiz.correctAnswer).toLowerCase();
        const isCorrect = shortAnswer.trim().toLowerCase() === correctAnswer;
        setScore((prev) => ({
            correct: prev.correct + (isCorrect ? 1 : 0),
            total: prev.total + 1,
        }));
        setShowResult(true);
    };

    const handleNext = () => {
        if (!isLastQuestion) {
            setCurrentIndex((prev) => prev + 1);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
        }
    };

    return (
        <WidgetContainer>
            {/* 헤더 - 공통 컴포넌트 사용 */}
            <WidgetHeader
                icon={Brain}
                iconColor="secondary"
                title={viewMode === 'list' ? '내 복습 퀴즈' : selectedQuizSet?.title || ''}
                subtitle={viewMode === 'list' ? '스터디 내용 기반 자동 생성' : selectedQuizSet?.meetingTitle}
                showBackButton={viewMode === 'quiz'}
                onBack={handleBackToList}
                maximizePath="/quiz/my-quiz"
                rightActions={
                    viewMode === 'quiz' && score.total > 0 ? (
                        <div className="text-sm font-bold text-text-primary">
                            {score.correct} / {score.total}
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
                            <QuizSetList
                                quizSets={MOCK_QUIZ_SETS}
                                onSelect={handleSelectQuizSet}
                            />
                        </motion.div>
                    ) : currentQuiz ? (
                        <motion.div
                            key={`quiz-${currentIndex}`}
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.3 }}
                        >
                            {currentQuiz.type === 'multiple' ? (
                                <MultipleChoiceQuiz
                                    quiz={currentQuiz}
                                    questionNumber={currentIndex + 1}
                                    selectedAnswer={selectedAnswer}
                                    showResult={showResult}
                                    onSelectAnswer={setSelectedAnswer}
                                    onSubmit={handleSubmitMultiple}
                                    onNext={handleNext}
                                    isLastQuestion={isLastQuestion}
                                />
                            ) : (
                                <ShortAnswerQuiz
                                    quiz={currentQuiz}
                                    questionNumber={currentIndex + 1}
                                    userAnswer={shortAnswer}
                                    showResult={showResult}
                                    onChangeAnswer={setShortAnswer}
                                    onSubmit={handleSubmitShort}
                                    onNext={handleNext}
                                    isLastQuestion={isLastQuestion}
                                />
                            )}

                            <QuizProgress
                                current={currentIndex + 1}
                                total={selectedQuizSet?.questions.length || 0}
                                className="mt-6"
                            />
                        </motion.div>
                    ) : null}
                </AnimatePresence>
            </div>
        </WidgetContainer>
    );
};

// 페이지당 아이템 수
const ITEMS_PER_PAGE = 10;

// 퀴즈 세트 리스트 컴포넌트
interface QuizSetListProps {
    quizSets: QuizSet[];
    onSelect: (quizSet: QuizSet) => void;
}

const QuizSetList: React.FC<QuizSetListProps> = ({ quizSets, onSelect }) => {
    const [currentPage, setCurrentPage] = useState(1);

    const totalPages = Math.ceil(quizSets.length / ITEMS_PER_PAGE);
    const showPagination = quizSets.length > ITEMS_PER_PAGE;

    // 현재 페이지의 아이템들
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const currentItems = quizSets.slice(startIndex, startIndex + ITEMS_PER_PAGE);

    if (quizSets.length === 0) {
        return (
            <div className="text-center py-12">
                <Brain className="mx-auto text-gray-300 mb-4" size={48} />
                <p className="text-text-secondary">아직 생성된 퀴즈가 없습니다</p>
                <p className="text-sm text-text-tertiary mt-1">
                    스터디 미팅 후 AI가 자동으로 퀴즈를 생성합니다
                </p>
            </div>
        );
    }

    return (
        <div className="flex flex-col">
            {/* 스크롤 가능한 리스트 영역 */}
            <div className="max-h-[400px] overflow-y-auto space-y-3 pr-2 quiz-list-scroll">
                {currentItems.map((quizSet, index) => {
                    const isCompleted = quizSet.completedCount === quizSet.questionCount;
                    const progress = (quizSet.completedCount / quizSet.questionCount) * 100;

                    return (
                        <button
                            key={quizSet.id}
                            onClick={() => onSelect(quizSet)}
                            className={cn(
                                'w-full p-4 rounded-xl border text-left transition-colors',
                                'hover:border-primary/30 hover:bg-gray-50/50',
                                isCompleted ? 'border-accent/30 bg-accent/5' : 'border-gray-100'
                            )}
                        >
                            <div className="flex items-start justify-between gap-3">
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-1">
                                        {isCompleted ? (
                                            <CheckCircle2 className="text-accent flex-shrink-0" size={18} />
                                        ) : (
                                            <Circle className="text-gray-300 flex-shrink-0" size={18} />
                                        )}
                                        <h4 className="font-bold text-text-primary truncate mb-0">
                                            {quizSet.title}
                                        </h4>
                                    </div>
                                    <p className="text-sm text-text-secondary ml-6 truncate">
                                        {quizSet.meetingTitle}
                                    </p>
                                    <div className="flex items-center gap-3 mt-2 ml-6">
                                        <span className="text-xs text-text-tertiary flex items-center gap-1">
                                            <Clock size={12} />
                                            {quizSet.createdAt}
                                        </span>
                                        <span className="text-xs text-text-tertiary">
                                            {quizSet.questionCount}문제
                                        </span>
                                    </div>
                                    {/* 진행률 바 */}
                                    {!isCompleted && quizSet.completedCount > 0 && (
                                        <div className="mt-2 ml-6">
                                            <div className="w-full bg-gray-200 rounded-full h-1.5">
                                                <div
                                                    className="bg-primary rounded-full h-1.5 transition-all"
                                                    style={{ width: `${progress}%` }}
                                                />
                                            </div>
                                        </div>
                                    )}
                                </div>
                                <div className={cn(
                                    'flex-shrink-0 p-2 rounded-lg',
                                    isCompleted ? 'bg-accent/10' : 'bg-primary/10'
                                )}>
                                    <Play
                                        size={16}
                                        className={isCompleted ? 'text-accent' : 'text-primary'}
                                    />
                                </div>
                            </div>
                        </button>
                    );
                })}
            </div>

            {/* 페이지네이션 */}
            {showPagination && (
                <nav className="flex items-center justify-center gap-1 mt-4 pt-4 border-t border-gray-100">
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
                                aria-current={page === currentPage ? 'page' : undefined}
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
