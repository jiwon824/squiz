// 미팅 기반 퀴즈 인라인 풀이 뷰
// 미팅 종료 후 AI가 자동 생성한 퀴즈를 탭 내에서 풀 수 있도록 함

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Loader2,
    CheckCircle2,
    XCircle,
    ChevronRight,
    Circle,
    Trophy,
    RotateCcw,
    Sparkles,
    Send,
} from 'lucide-react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import {
    studyQuizApi,
    type StudyQuizDetail,
    type StudyQuizSubmitResponse,
} from '@/api/endpoints/studyQuizApi';

interface QuizViewProps {
    /** 현재 선택된 스터디 ID */
    studyId: number;
    /** 현재 선택된 미팅(리포트) ID */
    meetingId: number;
    /** 추가 클래스명 */
    className?: string;
}

/** options JSON 문자열을 파싱 */
const parseOptions = (optionsStr: string | null): { id: string; text: string }[] => {
    if (!optionsStr) return [];
    try {
        const parsed = JSON.parse(optionsStr);
        if (Array.isArray(parsed)) {
            return parsed.map((opt, idx) => {
                if (typeof opt === 'string') {
                    return { id: String.fromCharCode(65 + idx), text: opt };
                }
                if (typeof opt === 'object' && opt !== null) {
                    return {
                        id: opt.id || String.fromCharCode(65 + idx),
                        text: opt.text || opt.label || String(opt.id || ''),
                    };
                }
                return { id: String.fromCharCode(65 + idx), text: String(opt) };
            });
        }
        return [];
    } catch {
        return [];
    }
};

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
    const [userAnswer, setUserAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [result, setResult] = useState<StudyQuizSubmitResponse | null>(null);
    const [correctCount, setCorrectCount] = useState(0);
    const [isComplete, setIsComplete] = useState(false);

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
                setQuiz(detail);
            } else {
                setQuiz(null);
            }
        } catch (err) {
            console.error('퀴즈 로드 실패:', err);
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

    // 답안 제출
    const handleSubmit = useCallback(async () => {
        if (!quiz || !userAnswer.trim()) return;

        const question = quiz.questions[currentIndex];
        const responseTimeMs = Date.now() - startTimeRef.current;

        setSubmitting(true);
        try {
            const res = await studyQuizApi.submitAnswer(
                studyId,
                quiz.id,
                question.id,
                userAnswer,
                responseTimeMs
            );
            setResult(res);
            setSubmitted(true);
            if (res.correct) {
                setCorrectCount(prev => prev + 1);
            }
        } catch (err) {
            console.error('답안 제출 실패:', err);
            // 제출 실패 시에도 로컬에서 정답 비교
            setResult({
                correct: userAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase(),
                correctAnswer: question.correctAnswer,
                explanation: question.explanation,
            });
            setSubmitted(true);
            if (userAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase()) {
                setCorrectCount(prev => prev + 1);
            }
        } finally {
            setSubmitting(false);
        }
    }, [quiz, currentIndex, userAnswer, studyId]);

    // 다음 문제
    const handleNext = useCallback(() => {
        if (!quiz) return;
        if (currentIndex + 1 >= quiz.questions.length) {
            setIsComplete(true);
        } else {
            setCurrentIndex(prev => prev + 1);
            setUserAnswer('');
            setSubmitted(false);
            setResult(null);
        }
    }, [quiz, currentIndex]);

    // 다시 풀기
    const handleRetry = useCallback(() => {
        setCurrentIndex(0);
        setUserAnswer('');
        setSubmitted(false);
        setResult(null);
        setCorrectCount(0);
        setIsComplete(false);
    }, []);

    // 로딩 상태
    if (loading) {
        return (
            <div className={cn('flex items-center justify-center h-48', className)}>
                <Loader2 size={24} className="animate-spin text-primary mr-3" />
                <span className="text-text-secondary text-sm">퀴즈 로딩 중...</span>
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
                            <Loader2
                                size={20}
                                className="absolute -bottom-1 -right-1 animate-spin text-accent"
                            />
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
    const options = parseOptions(currentQuestion.options);
    const progress = ((currentIndex + 1) / quiz.questions.length) * 100;

    return (
        <div className={cn('space-y-4', className)}>
            {/* 헤더: 퀴즈 제목 + 진행률 */}
            <div className="rounded-xl border border-border overflow-hidden">
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

                {/* 문제 영역 */}
                <div className="p-6">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={currentIndex}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            {/* 문제 번호 + 유형 */}
                            <div className="flex items-center gap-2 mb-4">
                                <span className={cn(
                                    'px-2.5 py-1 text-xs font-bold rounded-full',
                                    'bg-primary text-white'
                                )}>
                                    Q{currentIndex + 1}
                                </span>
                                <span className="text-xs text-text-tertiary">
                                    {currentQuestion.questionType === 'MULTIPLE_CHOICE' ? '객관식' : '단답형'}
                                </span>
                            </div>

                            {/* 문제 텍스트 */}
                            <p className="text-base font-medium text-text-primary mb-6 leading-relaxed">
                                {currentQuestion.questionText}
                            </p>

                            {/* 선택지 또는 입력 */}
                            {currentQuestion.questionType === 'MULTIPLE_CHOICE' && options.length > 0 ? (
                                <div className="space-y-2.5">
                                    {options.map(opt => {
                                        const isSelected = userAnswer === opt.id;
                                        const showCorrect = submitted && result && opt.id === result.correctAnswer;
                                        const showWrong = submitted && result && isSelected && !result.correct;

                                        return (
                                            <button
                                                key={opt.id}
                                                onClick={() => !submitted && setUserAnswer(opt.id)}
                                                disabled={submitted}
                                                className={cn(
                                                    'w-full flex items-center gap-3 px-4 py-3.5 rounded-xl text-left transition-all',
                                                    'border-2',
                                                    !submitted && conditionalClasses.state(
                                                        isSelected,
                                                        'border-primary bg-primary/5',
                                                        'border-border hover:border-primary/40 hover:bg-surface-hover'
                                                    ),
                                                    submitted && showCorrect && 'border-secondary bg-secondary/5',
                                                    submitted && showWrong && 'border-error bg-error/5',
                                                    submitted && !showCorrect && !showWrong && 'border-border opacity-50',
                                                    submitted && 'cursor-default'
                                                )}
                                            >
                                                {/* 아이콘 */}
                                                {submitted && showCorrect ? (
                                                    <CheckCircle2 size={18} className="text-secondary flex-shrink-0" />
                                                ) : submitted && showWrong ? (
                                                    <XCircle size={18} className="text-error flex-shrink-0" />
                                                ) : (
                                                    <Circle
                                                        size={18}
                                                        className={cn(
                                                            'flex-shrink-0',
                                                            isSelected ? 'text-primary' : 'text-text-tertiary'
                                                        )}
                                                    />
                                                )}
                                                <span className={cn(
                                                    'text-sm font-medium',
                                                    submitted && showCorrect && 'text-secondary-dark',
                                                    submitted && showWrong && 'text-error',
                                                    !submitted && isSelected && 'text-primary',
                                                    !submitted && !isSelected && 'text-text-secondary',
                                                    submitted && !showCorrect && !showWrong && 'text-text-tertiary'
                                                )}>
                                                    {/* AI가 이미 "A. " prefix를 포함한 경우 중복 방지 */}
                                                    {opt.text.match(/^[A-D]\.\s/) ? opt.text : `${opt.id}. ${opt.text}`}
                                                </span>
                                            </button>
                                        );
                                    })}
                                </div>
                            ) : (
                                <input
                                    type="text"
                                    value={userAnswer}
                                    onChange={e => !submitted && setUserAnswer(e.target.value)}
                                    onKeyDown={e => e.key === 'Enter' && !submitted && userAnswer.trim() && handleSubmit()}
                                    disabled={submitted}
                                    placeholder="답을 입력하세요..."
                                    className={cn(
                                        'w-full px-4 py-3 text-sm rounded-xl transition-all',
                                        'border-2 border-border bg-background',
                                        'focus:border-primary focus:outline-none',
                                        submitted && 'cursor-default opacity-80'
                                    )}
                                />
                            )}

                            {/* 피드백 영역 */}
                            <AnimatePresence>
                                {submitted && result && (
                                    <motion.div
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: 'auto' }}
                                        exit={{ opacity: 0, height: 0 }}
                                        className="mt-5"
                                    >
                                        <div className={cn(
                                            'rounded-xl p-4 border',
                                            result.correct
                                                ? 'bg-secondary/5 border-secondary/30'
                                                : 'bg-error/5 border-error/30'
                                        )}>
                                            <div className="flex items-center gap-2 mb-2">
                                                {result.correct ? (
                                                    <CheckCircle2 size={18} className="text-secondary" />
                                                ) : (
                                                    <XCircle size={18} className="text-error" />
                                                )}
                                                <span className={cn(
                                                    'text-sm font-semibold',
                                                    result.correct ? 'text-secondary-dark' : 'text-error'
                                                )}>
                                                    {result.correct ? '정답입니다!' : '오답입니다'}
                                                </span>
                                            </div>
                                            {!result.correct && (
                                                <p className="text-sm text-text-secondary mb-1">
                                                    정답: <span className="font-medium text-secondary">{result.correctAnswer}</span>
                                                </p>
                                            )}
                                            {result.explanation && (
                                                <p className="text-sm text-text-secondary mt-2">
                                                    {result.explanation}
                                                </p>
                                            )}
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            {/* 버튼 영역 */}
                            <div className="mt-6 flex justify-end">
                                {!submitted ? (
                                    <button
                                        onClick={handleSubmit}
                                        disabled={!userAnswer.trim() || submitting}
                                        className={cn(
                                            'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg transition-colors',
                                            'bg-primary text-white',
                                            'hover:bg-primary-dark',
                                            'disabled:opacity-50 disabled:cursor-not-allowed'
                                        )}
                                    >
                                        {submitting ? (
                                            <Loader2 size={15} className="animate-spin" />
                                        ) : (
                                            <Send size={15} />
                                        )}
                                        제출하기
                                    </button>
                                ) : (
                                    <button
                                        onClick={handleNext}
                                        className={cn(
                                            'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg transition-colors',
                                            'bg-primary text-white hover:bg-primary-dark'
                                        )}
                                    >
                                        {currentIndex + 1 >= quiz.questions.length ? '결과 보기' : '다음 문제'}
                                        <ChevronRight size={15} />
                                    </button>
                                )}
                            </div>
                        </motion.div>
                    </AnimatePresence>
                </div>
            </div>
        </div>
    );
};
