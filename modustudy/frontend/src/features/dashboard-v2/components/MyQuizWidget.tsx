import React, { useState, useEffect, useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Brain, ChevronLeft, ChevronRight, Clock, CheckCircle2, Circle, Play, Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import {
    MultipleChoiceQuiz,
    ShortAnswerQuiz,
    QuizProgress,
    QuizQuestion,
    WidgetHeader,
    WidgetContainer
} from '@/shared/components';
import { studyApi } from '@/api/endpoints/studyApi';
import { studyQuizApi, StudyQuizListItem, StudyQuizDetail } from '@/api/endpoints/studyQuizApi';
import { useTimer } from '@/features/quiz/hooks/useTimer';

// 퀴즈 세트 타입 (UI용)
interface QuizSet {
    id: number;
    title: string;
    meetingTitle: string;
    createdAt: string;
    questionCount: number;
    completedCount: number;
    questions: QuizQuestion[];
}

type ViewMode = 'list' | 'quiz';

/**
 * 백엔드 응답을 UI QuizQuestion 형식으로 변환
 */
const transformToQuizQuestions = (detail: StudyQuizDetail): QuizQuestion[] => {
    return detail.questions.map((q, idx) => {
        const isMultiple = q.questionType === 'MULTIPLE_CHOICE';

        // options 파싱
        let options: string[] = [];
        if (q.options) {
            try {
                options = JSON.parse(q.options);
            } catch {
                options = [];
            }
        }

        // 정답 인덱스 찾기 (객관식인 경우)
        let correctAnswer: number | string = q.correctAnswer;
        if (isMultiple && options.length > 0) {
            const answerIdx = options.findIndex(opt =>
                opt.includes(q.correctAnswer) ||
                opt.startsWith(q.correctAnswer) ||
                opt === q.correctAnswer
            );
            correctAnswer = answerIdx >= 0 ? answerIdx : 0;
        }

        return {
            id: q.id,
            type: isMultiple ? 'multiple' : 'short',
            question: q.questionText,
            options: isMultiple ? options : undefined,
            correctAnswer,
            explanation: q.explanation || '',
            difficulty: 'medium',
            category: '',
        } as QuizQuestion;
    });
};

export const MyQuizWidget: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [selectedQuizSet, setSelectedQuizSet] = useState<QuizSet | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [shortAnswer, setShortAnswer] = useState('');
    const [showResult, setShowResult] = useState(false);
    const [score, setScore] = useState({ correct: 0, total: 0 });

    // 데이터 로딩 상태
    const [quizSets, setQuizSets] = useState<QuizSet[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [currentStudyId, setCurrentStudyId] = useState<number | null>(null);

    // 마운트 시 내 스터디 목록 조회 후 퀴즈 목록 로딩
    useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);

                // 내 스터디 목록 조회
                const studyResponse = await studyApi.getMyStudies(0, 20);
                if (studyResponse.content.length === 0) {
                    setQuizSets([]);
                    return;
                }

                // 모든 스터디의 퀴즈를 모아서 보여줌
                const allQuizSets: QuizSet[] = [];

                for (const study of studyResponse.content) {
                    try {
                        const quizzes = await studyQuizApi.getStudyQuizzes(study.id);

                        for (const quiz of quizzes) {
                            // 간략 정보로 QuizSet 생성 (questions는 선택 시 로드)
                            allQuizSets.push({
                                id: quiz.id,
                                title: quiz.title,
                                meetingTitle: study.name, // 스터디 이름을 meetingTitle로 사용
                                createdAt: quiz.createdAt.split('T')[0],
                                questionCount: quiz.questionCount,
                                completedCount: 0, // TODO: 사용자별 진행도 저장 시 업데이트
                                questions: [], // 선택 시 로드
                            });

                            // 첫 번째 스터디 ID 저장
                            if (!currentStudyId) {
                                setCurrentStudyId(study.id);
                            }
                        }
                    } catch (err) {
                        console.warn(`[MyQuizWidget] 스터디 ${study.id} 퀴즈 조회 실패:`, err);
                    }
                }

                // 최신순 정렬
                allQuizSets.sort((a, b) =>
                    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
                );

                setQuizSets(allQuizSets);
            } catch (err) {
                console.error('[MyQuizWidget] 데이터 로딩 실패:', err);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, []);

    // 퀴즈 세트 선택 시 상세 정보 로드
    const handleSelectQuizSet = useCallback(async (quizSet: QuizSet) => {
        // 이미 questions가 로드되어 있으면 바로 시작
        if (quizSet.questions.length > 0) {
            setSelectedQuizSet(quizSet);
            setViewMode('quiz');
            setCurrentIndex(0);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
            setScore({ correct: 0, total: 0 });
            return;
        }

        // questions가 비어있으면 상세 조회
        try {
            setIsLoading(true);

            // 해당 퀴즈의 스터디 ID 찾기
            const studyResponse = await studyApi.getMyStudies(0, 20);
            let studyId: number | null = null;

            for (const study of studyResponse.content) {
                try {
                    const detail = await studyQuizApi.getQuizDetail(study.id, quizSet.id);
                    if (detail) {
                        studyId = study.id;
                        const questions = transformToQuizQuestions(detail);

                        const loadedQuizSet: QuizSet = {
                            ...quizSet,
                            questions,
                        };

                        // 상태 업데이트
                        setQuizSets(prev => prev.map(q =>
                            q.id === quizSet.id ? loadedQuizSet : q
                        ));

                        setSelectedQuizSet(loadedQuizSet);
                        setViewMode('quiz');
                        setCurrentIndex(0);
                        setSelectedAnswer(null);
                        setShortAnswer('');
                        setShowResult(false);
                        setScore({ correct: 0, total: 0 });
                        break;
                    }
                } catch {
                    // 해당 스터디에 퀴즈가 없으면 다음 스터디 시도
                }
            }

            if (!studyId) {
                console.error('[MyQuizWidget] 퀴즈를 찾을 수 없음:', quizSet.id);
            }
        } catch (err) {
            console.error('[MyQuizWidget] 퀴즈 상세 로딩 실패:', err);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const handleBackToList = () => {
        setViewMode('list');
        setSelectedQuizSet(null);
    };

    const currentQuiz = selectedQuizSet?.questions[currentIndex];
    const isLastQuestion = selectedQuizSet ? currentIndex >= selectedQuizSet.questions.length - 1 : false;


    // 타이머 훅 사용
    const { start: startTimer, stop: stopTimer } = useTimer();

    // 퀴즈/문제 변경 시 타이머 시작
    useEffect(() => {
        if (viewMode === 'quiz' && currentQuiz) {
            startTimer();
        }
    }, [currentIndex, viewMode, currentQuiz, startTimer]);

    const handleSubmitMultiple = async () => {
        if (selectedAnswer === null || !currentQuiz || !selectedQuizSet || !currentStudyId) return;

        try {
            // 타이머 정지 및 경과 시간 획득
            const responseTimeMs = stopTimer();

            // API 호출
            const result = await studyQuizApi.submitAnswer(
                currentStudyId,
                Number(selectedQuizSet.id),
                currentQuiz.id,
                {
                    userAnswer: String(selectedAnswer),
                    responseTimeMs
                }
            );

            // 점수 업데이트 (백엔드 결과 사용)
            setScore((prev) => ({
                correct: prev.correct + (result.isCorrect ? 1 : 0),
                total: prev.total + 1,
            }));

            setShowResult(true);
        } catch (err) {
            console.error('[MyQuizWidget] 답안 제출 실패:', err);
            // 에러 시에도 일단 로컬 판정 결과는 보여주되, 에러 알림 필요할 수 있음
            setShowResult(true);
        }
    };

    const handleSubmitShort = async () => {
        if (!shortAnswer.trim() || !currentQuiz || !selectedQuizSet || !currentStudyId) return;

        try {
            // 타이머 정지 및 경과 시간 획득
            const responseTimeMs = stopTimer();

            // API 호출
            const result = await studyQuizApi.submitAnswer(
                currentStudyId,
                Number(selectedQuizSet.id),
                currentQuiz.id,
                {
                    userAnswer: shortAnswer.trim(),
                    responseTimeMs
                }
            );

            // 점수 업데이트 (백엔드 결과 사용)
            setScore((prev) => ({
                correct: prev.correct + (result.isCorrect ? 1 : 0),
                total: prev.total + 1,
            }));

            setShowResult(true);
        } catch (err) {
            console.error('[MyQuizWidget] 답안 제출 실패:', err);
            setShowResult(true);
        }
    };

    const handleNext = () => {
        if (!isLastQuestion) {
            setCurrentIndex((prev) => prev + 1);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
        }
    };

    // 로딩 중
    if (isLoading && viewMode === 'list') {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={Brain}
                    iconColor="secondary"
                    title="AI 복습 퀴즈"
                    subtitle="스터디 내용 기반 자동 생성"
                    maximizePath="/quiz/review"
                />
                <div className="flex items-center justify-center h-64 text-text-tertiary">
                    <Loader2 className="animate-spin mr-2" size={20} />
                    로딩 중...
                </div>
            </WidgetContainer>
        );
    }

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
                                quizSets={quizSets}
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
                {currentItems.map((quizSet) => {
                    const isCompleted = quizSet.completedCount === quizSet.questionCount && quizSet.questionCount > 0;
                    const progress = quizSet.questionCount > 0
                        ? (quizSet.completedCount / quizSet.questionCount) * 100
                        : 0;

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
