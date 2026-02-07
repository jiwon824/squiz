// 미팅 기반 퀴즈 인라인 풀이 뷰
// shared QuizForm 컴포넌트를 활용하여 퀴즈 UI를 렌더링

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    XCircle,
    Trophy,
    RotateCcw,
    Sparkles,
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { QuizSingleChoice, QuizShortAnswer } from '@/shared/components';
import { FeedbackModal } from '@/features/quiz/components/continuous/FeedbackModal';
import {
    studyQuizApi,
    type StudyQuizDetail,
} from '@/api/endpoints/studyQuizApi';
import { transformStudyQuizQuestion, parseOptions, indexToOptionId } from '@/shared/utils/quizUtils';

interface QuizViewProps {
    /** 현재 선택된 스터디 ID */
    studyId: number;
    /** 현재 선택된 미팅(리포트) ID */
    meetingId: number;
    /** 추가 클래스명 */
    className?: string;
}

export const QuizView: React.FC<QuizViewProps> = ({
    studyId,
    meetingId,
    className,
}) => {
    // 퀴즈 데이터
    const [quiz, setQuiz] = useState<StudyQuizDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // 풀이 상태
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [shortAnswer, setShortAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [showResult, setShowResult] = useState(false);
    const [correctCount, setCorrectCount] = useState(0);
    const [isComplete, setIsComplete] = useState(false);

    // 피드백 모달 상태
    const [showFeedback, setShowFeedback] = useState(false);
    const [feedbackData, setFeedbackData] = useState<{
        isCorrect: boolean;
        correctAnswer: string;
        explanation?: string;
    } | null>(null);

    // 응답 시간 측정
    const startTimeRef = useRef<number>(Date.now());

    // 퀴즈 로드
    const loadQuiz = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const quizzes = await studyQuizApi.getStudyQuizzes(studyId);
            // 해당 미팅에서 생성된 퀴즈 찾기
            const meetingQuiz = quizzes.find(
                q => q.sourceType === 'MEETING' && q.sourceId === meetingId && q.status === 'ACTIVE'
            );
            if (meetingQuiz) {
                const detail = await studyQuizApi.getQuizDetail(studyId, meetingQuiz.id);
                // 객관식 문제만 필터링 (주관식 제외)
                const filteredDetail = {
                    ...detail,
                    questions: detail.questions.filter(q => q.questionType === 'MULTIPLE_CHOICE'),
                };
                setQuiz(filteredDetail.questions.length > 0 ? filteredDetail : null);
            } else {
                setQuiz(null);
            }
        } catch (err) {
            setError('퀴즈를 불러오는 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    }, [studyId, meetingId]);

    useEffect(() => {
        loadQuiz();
    }, [loadQuiz]);

    // 문제 전환 시 타이머 리셋
    useEffect(() => {
        startTimeRef.current = Date.now();
    }, [currentIndex]);

    // 객관식 답안 제출
    const handleSubmitSingle = useCallback(async () => {
        if (!quiz || selectedAnswer === null || submitting) return;

        const question = quiz.questions[currentIndex];
        const options = parseOptions(question.options);
        const optionId = options[selectedAnswer]?.id || indexToOptionId(selectedAnswer);
        const responseTimeMs = Date.now() - startTimeRef.current;

        let isCorrect = false;
        setSubmitting(true);
        try {
            const res = await studyQuizApi.submitAnswer(
                studyId, quiz.id, question.id,
                { userAnswer: optionId, responseTimeMs }
            );
            isCorrect = res.isCorrect;
        } catch (err) {
            console.error('답안 제출 실패:', err);
            isCorrect = optionId.toUpperCase() === question.correctAnswer.trim().toUpperCase();
        } finally {
            setSubmitting(false);
        }

        if (isCorrect) setCorrectCount(prev => prev + 1);
        setShowResult(true);

        // 정답 텍스트 결정 (모달 표시용)
        const correctOption = options.find(o => o.id === question.correctAnswer);
        const correctAnswerText = correctOption?.text || question.correctAnswer;

        setFeedbackData({
            isCorrect,
            correctAnswer: correctAnswerText,
            explanation: question.explanation || undefined,
        });
        setShowFeedback(true);
    }, [quiz, currentIndex, selectedAnswer, submitting, studyId]);

    // 주관식 답안 제출
    const handleSubmitShort = useCallback(async () => {
        if (!quiz || !shortAnswer.trim() || submitting) return;

        const question = quiz.questions[currentIndex];
        const responseTimeMs = Date.now() - startTimeRef.current;

        let isCorrect = false;
        setSubmitting(true);
        try {
            const res = await studyQuizApi.submitAnswer(
                studyId, quiz.id, question.id,
                { userAnswer: shortAnswer.trim(), responseTimeMs }
            );
            isCorrect = res.isCorrect;
        } catch (err) {
            // 제출 실패 시에도 로컬에서 정답 비교
            isCorrect = shortAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
        } finally {
            setSubmitting(false);
        }

        if (isCorrect) setCorrectCount(prev => prev + 1);
        setShowResult(true);

        setFeedbackData({
            isCorrect,
            correctAnswer: question.correctAnswer,
            explanation: question.explanation || undefined,
        });
        setShowFeedback(true);
    }, [quiz, currentIndex, shortAnswer, submitting, studyId]);

    // 피드백 모달에서 다음 문제로 이동
    const handleFeedbackContinue = useCallback(() => {
        setShowFeedback(false);
        setFeedbackData(null);
        if (!quiz) return;
        if (currentIndex + 1 >= quiz.questions.length) {
            setIsComplete(true);
        } else {
            setCurrentIndex(prev => prev + 1);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
        }
    }, [quiz, currentIndex]);

    // 다시 풀기
    const handleRetry = useCallback(() => {
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setShortAnswer('');
        setShowResult(false);
        setCorrectCount(0);
        setIsComplete(false);
        setShowFeedback(false);
        setFeedbackData(null);
    }, []);

    // 로딩 상태
    if (loading) {
        return (
            <div className={cn('flex items-center justify-center h-48', className)}>
                <Spinner variant="center" size="md" label="퀴즈 로딩 중..." />
            </div>
        );
    }

    // 에러 상태
    if (error) {
        return (
            <div className={cn('flex flex-col items-center justify-center h-48 text-text-tertiary', className)}>
                <XCircle size={32} className="mb-3 opacity-50" />
                <span className="text-sm">{error}</span>
                <button
                    onClick={loadQuiz}
                    className="mt-3 text-sm text-primary hover:text-primary-dark transition-colors"
                >
                    다시 시도
                </button>
            </div>
        );
    }

    // 퀴즈 없음 (생성 중)
    if (!quiz) {
        return (
            <div className={cn('space-y-4', className)}>
                <div className="rounded-xl border border-border overflow-hidden">
                    <div className="flex flex-col items-center justify-center py-16 px-8">
                        <div className="relative mb-6">
                            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
                                <Sparkles size={28} className="text-primary" />
                            </div>
                            <div className="absolute -bottom-1 -right-1">
                                <Spinner size="sm" />
                            </div>
                        </div>
                        <h3 className="text-lg font-semibold text-text-primary mb-2">
                            퀴즈 생성 중
                        </h3>
                        <p className="text-sm text-text-secondary text-center max-w-sm">
                            AI가 미팅 내용을 분석하여 퀴즈를 생성하고 있습니다.
                            <br />
                            잠시 후 다시 확인해주세요.
                        </p>
                        <button
                            onClick={loadQuiz}
                            className={cn(
                                'mt-6 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg',
                                'text-primary border border-primary/30',
                                'hover:bg-primary/5 transition-colors'
                            )}
                        >
                            <RotateCcw size={14} />
                            새로고침
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // 완료 화면
    if (isComplete) {
        const totalQuestions = quiz.questions.length;
        const scorePercent = Math.round((correctCount / totalQuestions) * 100);

        return (
            <div className={cn('space-y-4', className)}>
                <div className="rounded-xl border border-border overflow-hidden">
                    <div className="flex flex-col items-center justify-center py-12 px-8">
                        <div className="w-20 h-20 rounded-full bg-accent/10 flex items-center justify-center mb-6">
                            <Trophy size={36} className="text-accent" />
                        </div>
                        <h3 className="text-xl font-bold text-text-primary mb-2">
                            퀴즈 완료!
                        </h3>
                        <p className="text-sm text-text-secondary mb-6">
                            {quiz.title}
                        </p>

                        {/* 결과 카드 */}
                        <div className="grid grid-cols-3 gap-4 w-full max-w-sm mb-8">
                            <div className="text-center p-4 rounded-xl bg-background border border-border">
                                <div className="text-2xl font-bold text-text-primary">{totalQuestions}</div>
                                <div className="text-xs text-text-tertiary mt-1">총 문제</div>
                            </div>
                            <div className="text-center p-4 rounded-xl bg-background border border-border">
                                <div className="text-2xl font-bold text-secondary">{correctCount}</div>
                                <div className="text-xs text-text-tertiary mt-1">정답</div>
                            </div>
                            <div className="text-center p-4 rounded-xl bg-background border border-border">
                                <div className="text-2xl font-bold text-primary">{scorePercent}%</div>
                                <div className="text-xs text-text-tertiary mt-1">정답률</div>
                            </div>
                        </div>

                        {/* 프로그레스 바 */}
                        <div className="w-full max-w-sm mb-6">
                            <div className="w-full bg-background rounded-full h-3 overflow-hidden">
                                <motion.div
                                    initial={{ width: 0 }}
                                    animate={{ width: `${scorePercent}%` }}
                                    transition={{ duration: 0.8, ease: 'easeOut' }}
                                    className={cn(
                                        'h-3 rounded-full',
                                        scorePercent >= 80 ? 'bg-secondary' :
                                            scorePercent >= 50 ? 'bg-accent' : 'bg-error'
                                    )}
                                />
                            </div>
                        </div>

                        <button
                            onClick={handleRetry}
                            className={cn(
                                'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg',
                                'bg-primary text-white hover:bg-primary-dark transition-colors'
                            )}
                        >
                            <RotateCcw size={15} />
                            다시 풀기
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // 문제 풀이 화면
    const currentQuestion = quiz.questions[currentIndex];
    const currentQuiz = transformStudyQuizQuestion(currentQuestion);
    const progress = ((currentIndex + 1) / quiz.questions.length) * 100;

    return (
        <div className={cn('space-y-4', className)}>
            <div className="rounded-xl border border-border overflow-hidden">
                {/* 헤더: 퀴즈 제목 + 진행률 */}
                <div className={cn(
                    'px-5 py-4 border-b border-border',
                    'bg-background/50 flex items-center justify-between'
                )}>
                    <div className="flex items-baseline gap-2">
                        <h3 className="font-semibold text-text-primary mb-0">{quiz.title}</h3>
                        <span className={cn(
                            'inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-semibold rounded-pill',
                            'bg-primary/10 text-[var(--color-primary)]'
                        )}>
                            <Sparkles size={10} />
                            AI 생성
                        </span>
                    </div>
                    <span className="text-sm text-text-tertiary">
                        {currentIndex + 1} / {quiz.questions.length}
                    </span>
                </div>

                {/* 진행률 바 */}
                <div className="w-full bg-background h-1">
                    <motion.div
                        className="h-1 bg-primary"
                        initial={{ width: 0 }}
                        animate={{ width: `${progress}%` }}
                        transition={{ duration: 0.3 }}
                    />
                </div>

                {/* 문제 영역 - shared QuizForm 컴포넌트 활용 (해설은 모달에서 표시) */}
                <div className="p-6">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={currentIndex}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            {currentQuestion.questionType === 'MULTIPLE_CHOICE' ? (
                                <QuizSingleChoice
                                    quiz={{ ...currentQuiz, explanation: '' }}
                                    questionNumber={currentIndex + 1}
                                    selectedAnswer={selectedAnswer}
                                    showResult={showResult}
                                    onSelectAnswer={setSelectedAnswer}
                                    onSubmit={handleSubmitSingle}
                                />
                            ) : (
                                <QuizShortAnswer
                                    quiz={{ ...currentQuiz, explanation: '' }}
                                    questionNumber={currentIndex + 1}
                                    userAnswer={shortAnswer}
                                    showResult={showResult}
                                    onChangeAnswer={(val) => setShortAnswer(val || '')}
                                    onSubmit={handleSubmitShort}
                                />
                            )}
                        </motion.div>
                    </AnimatePresence>
                </div>
            </div>

            {/* 피드백 모달 */}
            {feedbackData && (
                <FeedbackModal
                    isOpen={showFeedback}
                    isCorrect={feedbackData.isCorrect}
                    correctAnswer={feedbackData.correctAnswer}
                    explanation={feedbackData.explanation}
                    onContinue={handleFeedbackContinue}
                />
            )}
        </div>
    );
};
