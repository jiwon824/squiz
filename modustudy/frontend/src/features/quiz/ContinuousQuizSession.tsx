/**
 * =============================================================================
 * ContinuousQuizSession.tsx - 연속 학습 모드 세션 컴포넌트
 * =============================================================================
 *
 * 목적 (PURPOSE):
 * 말해보카 스타일의 연속 학습 플로우를 구현하는 메인 컴포넌트입니다.
 *
 * 핵심 특징:
 * - Atomic Interaction: 매 제출마다 다음 문제를 직접 반환
 * - Forward-only: 뒤로가기 없는 전진 전용 플로우
 * - FSRS Timing: useRef로 문제 렌더링 시점부터 제출까지의 시간 측정
 * - Instant Feedback: 제출 즉시 정답/오답 피드백 표시
 *
 * URL: /continuous-quiz/:courseId/section/:sectionNumber
 *
 * =============================================================================
 */

import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { ArrowLeft, Loader2, AlertCircle, RefreshCw, CheckCircle, XCircle, Trophy } from 'lucide-react';

import { QuestionCard } from './components/QuestionCard';
import { ContinuousQuizNavigation } from './components/ContinuousQuizNavigation';
import { Button } from '@/shared/components/Button';
import { useUIStore } from '@/store/uiStore';

import {
    fetchNextQuestion,
    submitAnswer,
    ContinuousQuizQuestion,
} from '@/api/endpoints/continuousQuizApi';

import { useTimer } from './hooks/useTimer';
import type { QuizQuestion as QuizQuestionType, QuestionType } from './types/QuizQuestion.types';

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * 답변이 유효한지(null이나 비어있지 않은지) 확인하는 헬퍼 함수
 */
const isValidAnswer = (answer: string | string[] | null | undefined): answer is string | string[] => {
    if (answer === null || answer === undefined) return false;
    if (typeof answer === 'string') return answer.trim() !== '';
    if (Array.isArray(answer)) return answer.length > 0 && answer.some(a => a.trim() !== '');
    return false;
};

/**
 * API 문제 데이터를 UI 컴포넌트가 기대하는 형식으로 변환
 */
const mapApiQuestionToUiQuestion = (apiQuestion: ContinuousQuizQuestion): QuizQuestionType => {
    const typeMap: Record<string, QuestionType> = {
        'MULTIPLE_CHOICE': 'single-choice',
        'MULTIPLE_CHOICE_MULTIPLE': 'multiple-choice',
        'SHORT_ANSWER': 'short-answer',
    };

    return {
        id: String(apiQuestion.questionId),
        type: typeMap[apiQuestion.questionType] || 'single-choice',
        question: apiQuestion.questionText,
        options: apiQuestion.options?.map(opt => ({
            id: opt.id,
            text: opt.text,
        })),
        correctAnswer: '',
        difficulty: 'Medium',
        category: 'CS',
    };
};

// =============================================================================
// 피드백 모달 컴포넌트
// =============================================================================
interface FeedbackModalProps {
    isOpen: boolean;
    isCorrect: boolean;
    correctAnswer: string;
    explanation?: string;
    onContinue: () => void;
}

const FeedbackModal: React.FC<FeedbackModalProps> = ({
    isOpen,
    isCorrect,
    correctAnswer,
    explanation,
    onContinue,
}) => {
    if (!isOpen) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
        >
            <div
                className="w-full max-w-md rounded-2xl p-6 animate-in fade-in zoom-in duration-200"
                style={{
                    backgroundColor: 'var(--color-surface)',
                    boxShadow: 'var(--shadow-xl)',
                }}
            >
                {/* 정답/오답 아이콘 */}
                <div className="flex justify-center mb-4">
                    {isCorrect ? (
                        <div
                            className="w-16 h-16 rounded-full flex items-center justify-center"
                            style={{ backgroundColor: 'var(--color-success-light)' }}
                        >
                            <CheckCircle size={40} style={{ color: 'var(--color-success)' }} />
                        </div>
                    ) : (
                        <div
                            className="w-16 h-16 rounded-full flex items-center justify-center"
                            style={{ backgroundColor: 'var(--color-error-light)' }}
                        >
                            <XCircle size={40} style={{ color: 'var(--color-error)' }} />
                        </div>
                    )}
                </div>

                {/* 결과 텍스트 */}
                <h3
                    className="text-xl font-bold text-center mb-2"
                    style={{ color: isCorrect ? 'var(--color-success)' : 'var(--color-error)' }}
                >
                    {isCorrect ? '정답입니다!' : '오답입니다'}
                </h3>

                {/* 정답 표시 (오답일 경우) */}
                {!isCorrect && (
                    <div
                        className="text-center mb-4 p-3 rounded-lg"
                        style={{ backgroundColor: 'var(--color-background)' }}
                    >
                        <span
                            className="text-sm"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            정답:
                        </span>
                        <p
                            className="font-medium mt-1"
                            style={{ color: 'var(--color-text-primary)' }}
                        >
                            {correctAnswer}
                        </p>
                    </div>
                )}

                {/* 해설 (있을 경우) */}
                {explanation && (
                    <div
                        className="text-sm mb-4 p-3 rounded-lg"
                        style={{
                            backgroundColor: 'var(--color-info-light)',
                            color: 'var(--color-text-secondary)',
                        }}
                    >
                        {explanation}
                    </div>
                )}

                {/* 계속하기 버튼 */}
                <Button
                    variant="google-primary"
                    size="lg"
                    onClick={onContinue}
                    className="w-full"
                >
                    다음 문제
                </Button>
            </div>
        </div>
    );
};

// =============================================================================
// 세션 완료 화면 컴포넌트
// =============================================================================
interface SessionCompleteProps {
    summary: {
        totalQuestions: number;
        correctCount: number;
        incorrectCount: number;
        averageResponseTimeMs: number;
    };
    onReturnToCourse: () => void;
    onRetry: () => void;
}

const SessionComplete: React.FC<SessionCompleteProps> = ({
    summary,
    onReturnToCourse,
    onRetry,
}) => {
    const accuracy = Math.round((summary.correctCount / summary.totalQuestions) * 100);
    const avgTime = (summary.averageResponseTimeMs / 1000).toFixed(1);

    return (
        <div
            className="min-h-screen flex items-center justify-center p-4"
            style={{ backgroundColor: 'var(--color-background)' }}
        >
            <div
                className="w-full max-w-md rounded-2xl p-8 text-center"
                style={{
                    backgroundColor: 'var(--color-surface)',
                    boxShadow: 'var(--shadow-xl)',
                }}
            >
                {/* 트로피 아이콘 */}
                <div className="flex justify-center mb-6">
                    <div
                        className="w-20 h-20 rounded-full flex items-center justify-center"
                        style={{ backgroundColor: 'var(--color-warning-light)' }}
                    >
                        <Trophy size={48} style={{ color: 'var(--color-warning)' }} />
                    </div>
                </div>

                <h2
                    className="text-2xl font-bold mb-2"
                    style={{ color: 'var(--color-text-primary)' }}
                >
                    학습 완료!
                </h2>

                <p
                    className="text-sm mb-6"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    오늘의 연속 학습을 완료했습니다.
                </p>

                {/* 통계 */}
                <div
                    className="grid grid-cols-3 gap-4 mb-8 p-4 rounded-xl"
                    style={{ backgroundColor: 'var(--color-background)' }}
                >
                    <div>
                        <p
                            className="text-2xl font-bold"
                            style={{ color: 'var(--color-primary)' }}
                        >
                            {summary.totalQuestions}
                        </p>
                        <p
                            className="text-xs"
                            style={{ color: 'var(--color-text-tertiary)' }}
                        >
                            총 문제
                        </p>
                    </div>
                    <div>
                        <p
                            className="text-2xl font-bold"
                            style={{ color: 'var(--color-success)' }}
                        >
                            {accuracy}%
                        </p>
                        <p
                            className="text-xs"
                            style={{ color: 'var(--color-text-tertiary)' }}
                        >
                            정답률
                        </p>
                    </div>
                    <div>
                        <p
                            className="text-2xl font-bold"
                            style={{ color: 'var(--color-info)' }}
                        >
                            {avgTime}s
                        </p>
                        <p
                            className="text-xs"
                            style={{ color: 'var(--color-text-tertiary)' }}
                        >
                            평균 시간
                        </p>
                    </div>
                </div>

                {/* 상세 결과 */}
                <div
                    className="flex justify-center gap-8 mb-8"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    <div className="flex items-center gap-2">
                        <CheckCircle size={18} style={{ color: 'var(--color-success)' }} />
                        <span>{summary.correctCount} 정답</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <XCircle size={18} style={{ color: 'var(--color-error)' }} />
                        <span>{summary.incorrectCount} 오답</span>
                    </div>
                </div>

                {/* 버튼들 */}
                <div className="flex flex-col gap-3">
                    <Button
                        variant="google-primary"
                        size="lg"
                        onClick={onReturnToCourse}
                        className="w-full"
                    >
                        코스로 돌아가기
                    </Button>
                    <Button
                        variant="google-outline"
                        size="md"
                        onClick={onRetry}
                        leftIcon={<RefreshCw size={18} />}
                        className="w-full"
                    >
                        다시 학습하기
                    </Button>
                </div>
            </div>
        </div>
    );
};

// =============================================================================
// 메인 컴포넌트
// =============================================================================
export const ContinuousQuizSession = () => {
    const { courseId, sectionNumber } = useParams<{
        courseId: string;
        sectionNumber: string;
    }>();
    const navigate = useNavigate();
    const location = useLocation();
    const showToast = useUIStore((state) => state.showToast);

    // 문제 제한 설정 (없으면 무제한)
    const questionLimit = (location.state as { limit?: number })?.limit;

    // 현재 문제 상태
    const [currentQuestion, setCurrentQuestion] = useState<QuizQuestionType | null>(null);
    const [currentApiQuestion, setCurrentApiQuestion] = useState<ContinuousQuizQuestion | null>(null);
    const [currentAnswer, setCurrentAnswer] = useState<string | string[] | undefined>(undefined);

    // 진행 상태 (로컬 통계 추적)
    const [solvedCount, setSolvedCount] = useState(0);
    const [correctCount, setCorrectCount] = useState(0);
    const [totalResponseTimeMs, setTotalResponseTimeMs] = useState(0);

    // UI 상태
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // 피드백 모달 상태
    const [showFeedback, setShowFeedback] = useState(false);
    const [feedbackData, setFeedbackData] = useState<{
        isCorrect: boolean;
        correctAnswer: string;
        explanation?: string;
    } | null>(null);

    // 세션 완료 상태
    const [isSessionComplete, setIsSessionComplete] = useState(false);
    const [sessionSummary, setSessionSummary] = useState<{
        totalQuestions: number;
        correctCount: number;
        incorrectCount: number;
        averageResponseTimeMs: number;
    } | null>(null);

    // FSRS 응답 시간 측정 (useTimer - 탭 전환 감지 포함)
    const { start: startTimer, stop: stopTimer } = useTimer();

    // 다음 문제 데이터 (제출 응답에서 받아 "다음 문제" 클릭 시 적용)
    const [nextQuestionData, setNextQuestionData] = useState<ContinuousQuizQuestion | null>(null);

    // 브라우저 뒤로가기/새로고침 시 경고
    useEffect(() => {
        if (isSessionComplete) return;

        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            e.preventDefault();
            e.returnValue = '';
            return '';
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [isSessionComplete]);

    // 새 문제 렌더 시 타이머 시작
    useEffect(() => {
        if (currentQuestion && !showFeedback) {
            startTimer();
        }
    }, [currentQuestion, showFeedback, startTimer]);

    // 초기 문제 로드
    useEffect(() => {
        const loadFirstQuestion = async () => {
            if (!courseId || !sectionNumber) {
                setError('코스 ID 또는 섹션 번호가 없습니다.');
                setIsLoading(false);
                return;
            }

            setIsLoading(true);
            setError(null);

            try {
                const numericCourseId = parseInt(courseId, 10);
                const numericSectionNumber = parseInt(sectionNumber, 10);

                if (isNaN(numericCourseId) || isNaN(numericSectionNumber)) {
                    throw new Error('잘못된 코스 ID 또는 섹션 번호입니다.');
                }

                const response = await fetchNextQuestion(numericCourseId, numericSectionNumber);

                // 무한 루프 모드: 문제가 없으면 에러 처리 (완료 화면 아님)
                if (!response.question) {
                    throw new Error('해당 섹션에 문제가 없습니다.');
                }

                const uiQuestion = mapApiQuestionToUiQuestion(response.question);
                setCurrentQuestion(uiQuestion);
                setCurrentApiQuestion(response.question);

                console.log('[ContinuousQuizSession] 첫 번째 문제 로드 완료:', {
                    questionId: response.question.questionId,
                });

                setIsLoading(false);
            } catch (err) {
                console.error('[ContinuousQuizSession] 초기화 실패:', err);
                setError(err instanceof Error ? err.message : '퀴즈를 시작하는데 실패했습니다.');
                setIsLoading(false);
            }
        };

        loadFirstQuestion();
    }, [courseId, sectionNumber]);

    // 답안 변경 핸들러
    const handleAnswerChange = useCallback((answer: string | string[]) => {
        setCurrentAnswer(answer);
    }, []);

    // 피드백 확인 후 다음 문제로 전환 (사용자 클릭 필수)
    const handleNextQuestion = useCallback(() => {
        // 목표 문제 수 달성 시 세션 종료
        if (questionLimit && solvedCount >= questionLimit) {
            const avgResponseTime = solvedCount > 0 ? Math.round(totalResponseTimeMs / solvedCount) : 0;
            setSessionSummary({
                totalQuestions: solvedCount,
                correctCount: correctCount,
                incorrectCount: solvedCount - correctCount,
                averageResponseTimeMs: avgResponseTime,
            });
            setIsSessionComplete(true);
            return;
        }

        if (nextQuestionData) {
            const nextUiQuestion = mapApiQuestionToUiQuestion(nextQuestionData);
            setCurrentQuestion(nextUiQuestion);
            setCurrentApiQuestion(nextQuestionData);
            setNextQuestionData(null);
        }
        setShowFeedback(false);
        setFeedbackData(null);
        setCurrentAnswer(undefined);
    }, [nextQuestionData, questionLimit, solvedCount, totalResponseTimeMs, correctCount]);

    // 답안 제출 핸들러 - useTimer로 시간 측정, 백엔드 isCorrect 기반 피드백
    const handleSubmit = useCallback(async () => {
        if (!currentApiQuestion || !isValidAnswer(currentAnswer)) {
            showToast('답변을 선택해주세요.', 'info');
            return;
        }

        const responseTimeMs = stopTimer();
        setIsSubmitting(true);

        try {
            const response = await submitAnswer(
                currentApiQuestion.questionId,
                currentAnswer,
                responseTimeMs
            );

            // 백엔드 isCorrect를 그대로 사용하여 피드백 표시
            setFeedbackData({
                isCorrect: response.isCorrect,
                correctAnswer: response.correctAnswer,
                explanation: response.explanation,
            });
            setShowFeedback(true);

            // 로컬 통계 업데이트
            setSolvedCount(prev => prev + 1);
            if (response.isCorrect) {
                setCorrectCount(prev => prev + 1);
            }
            setTotalResponseTimeMs(prev => prev + responseTimeMs);

            // 다음 문제 데이터 저장 ("다음 문제" 클릭 시 적용)
            if (response.nextQuestion) {
                setNextQuestionData(response.nextQuestion);
            } else {
                console.error('[ContinuousQuizSession] 다음 문제가 없습니다');
                showToast('다음 문제를 불러오는데 실패했습니다.', 'error');
            }
        } catch (err) {
            console.error('[ContinuousQuizSession] 제출 실패:', err);
            showToast(err instanceof Error ? err.message : '제출에 실패했습니다.', 'error');
        } finally {
            setIsSubmitting(false);
        }
    }, [currentApiQuestion, currentAnswer, showToast, stopTimer]);

    // 코스로 돌아가기
    const handleReturnToCourse = useCallback(() => {
        navigate(`/quiz-practice/${courseId}`);
    }, [courseId, navigate]);

    // 다시 학습하기
    const handleRetry = useCallback(() => {
        window.location.reload();
    }, []);

    // 세션 종료 및 결과 표시
    const handleEndSession = useCallback(() => {
        if (solvedCount === 0) {
            // 문제를 풀지 않은 경우 바로 나가기
            navigate(`/quiz-practice/${courseId}`);
            return;
        }

        if (confirm('학습을 종료하고 결과를 확인하시겠습니까?')) {
            const avgResponseTime = solvedCount > 0 ? Math.round(totalResponseTimeMs / solvedCount) : 0;
            setSessionSummary({
                totalQuestions: solvedCount,
                correctCount: correctCount,
                incorrectCount: solvedCount - correctCount,
                averageResponseTimeMs: avgResponseTime,
            });
            setIsSessionComplete(true);
        }
    }, [solvedCount, correctCount, totalResponseTimeMs, courseId, navigate]);

    // 뒤로가기 버튼 (확인 없이 바로 나가기)
    const handleBackClick = () => {
        if (solvedCount > 0) {
            handleEndSession();
        } else {
            navigate(`/quiz-practice/${courseId}`);
        }
    };

    // 로딩 상태
    if (isLoading) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div className="text-center">
                    <Loader2
                        size={48}
                        className="animate-spin mx-auto mb-4"
                        style={{ color: 'var(--color-primary)' }}
                    />
                    <p style={{ color: 'var(--color-text-secondary)' }}>
                        학습을 준비하는 중...
                    </p>
                </div>
            </div>
        );
    }

    // 에러 상태
    if (error || (!currentQuestion && !isSessionComplete)) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div
                    className="text-center max-w-md mx-auto p-8 rounded-2xl"
                    style={{
                        backgroundColor: 'var(--color-surface)',
                        boxShadow: 'var(--shadow-lg)',
                    }}
                >
                    <AlertCircle
                        size={48}
                        className="mx-auto mb-4"
                        style={{ color: 'var(--color-error)' }}
                    />
                    <h2
                        className="text-xl font-bold mb-2"
                        style={{ color: 'var(--color-text-primary)' }}
                    >
                        문제를 불러올 수 없습니다
                    </h2>
                    <p
                        className="text-sm mb-6"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        {error || '문제 데이터를 찾을 수 없습니다.'}
                    </p>
                    <div className="flex gap-3 justify-center">
                        <Button
                            variant="google-primary"
                            size="md"
                            onClick={handleRetry}
                            leftIcon={<RefreshCw size={18} />}
                        >
                            다시 시도
                        </Button>
                        <Button
                            variant="google-ghost"
                            size="md"
                            onClick={() => navigate(`/quiz-practice/${courseId || ''}`)}
                        >
                            코스로 돌아가기
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    // 세션 완료 화면
    if (isSessionComplete && sessionSummary) {
        return (
            <SessionComplete
                summary={sessionSummary}
                onReturnToCourse={handleReturnToCourse}
                onRetry={handleRetry}
            />
        );
    }

    // 메인 퀴즈 화면
    return (
        <div
            className="min-h-screen"
            style={{ backgroundColor: 'var(--color-background)' }}
        >
            {/* 헤더 */}
            <header
                className="sticky top-0 z-10 backdrop-blur-md"
                style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    borderBottom: '1px solid var(--color-border)',
                }}
            >
                <div className="max-w-3xl mx-auto px-4 py-4">
                    <div className="flex items-center justify-between mb-3">
                        <Button
                            variant="google-ghost"
                            size="sm"
                            onClick={handleBackClick}
                            leftIcon={<ArrowLeft size={18} />}
                        >
                            나가기
                        </Button>

                        <span
                            className="text-sm font-medium px-3 py-1 rounded-full"
                            style={{
                                backgroundColor: 'var(--color-primary-light)',
                                color: 'var(--color-primary)',
                            }}
                        >
                            연속 학습
                        </span>
                    </div>

                    {/* 진행 상황 카운터 */}
                    <div
                        className="flex items-center justify-between text-sm"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        <span>
                            진행: <strong style={{ color: 'var(--color-primary)' }}>{solvedCount}</strong>
                            {questionLimit ? ` / ${questionLimit}` : ' 문제'}
                        </span>
                        <span>정답률: <strong style={{ color: 'var(--color-success)' }}>
                            {solvedCount > 0 ? Math.round((correctCount / solvedCount) * 100) : 0}%
                        </strong></span>
                    </div>
                </div>
            </header>

            {/* 메인 컨텐츠 */}
            <main className="max-w-3xl mx-auto px-4 py-8">
                {currentQuestion && (
                    <>
                        {/* 문제 카드 */}
                        <QuestionCard
                            question={currentQuestion}
                            questionNumber={solvedCount + 1}
                            currentAnswer={currentAnswer}
                            onAnswerChange={handleAnswerChange}
                            className="mb-8"
                        />

                        {/* 제출 버튼 */}
                        <ContinuousQuizNavigation
                            onSubmit={handleSubmit}
                            isLoading={isSubmitting}
                            isSubmitDisabled={!isValidAnswer(currentAnswer)}
                        />
                    </>
                )}
            </main>

            {/* 피드백 모달 */}
            {feedbackData && (
                <FeedbackModal
                    isOpen={showFeedback}
                    isCorrect={feedbackData.isCorrect}
                    correctAnswer={feedbackData.correctAnswer}
                    explanation={feedbackData.explanation}
                    onContinue={handleNextQuestion}
                />
            )}
        </div>
    );
};
