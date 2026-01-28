/**
 * =============================================================================
 * QuizSession.tsx - 퀴즈 세션 메인 페이지 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 퀴즈 세션의 전체 플로우를 관리하는 메인 페이지 컴포넌트입니다.
 * - 새 세션 시작 또는 이전 세션 재개 (API)
 * - 문제 셔플은 백엔드에서 처리
 * - 답안 실시간 저장 (API)
 * - 네비게이션 가드 (뒤로가기 시 확인)
 * - 섹션 제출 및 채점 (API)
 * 
 * URL: /quiz-practice/:courseId/section/:sectionNumber/session
 * 
 * =============================================================================
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, AlertCircle, RefreshCw } from 'lucide-react';

import { QuestionCard } from './components/QuestionCard';
import { QuizProgressBar } from './components/QuizProgressBar';
import { QuizNavigation } from './components/QuizNavigation';
import { QuizExitModal } from './components/QuizExitModal';
import { Button } from '@/shared/components/Button';
import { useUIStore } from '@/store/uiStore';

import {
    startOrResumeAttempt,
    saveAnswer as saveAnswerApi,
    submitAttempt,
    abandonAttempt,
    AttemptData,
    AttemptQuestion,
} from '@/api/endpoints/quizCourseApi';

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
const mapApiQuestionToUiQuestion = (apiQuestion: AttemptQuestion): QuizQuestionType => {
    // API 타입을 UI 타입으로 매핑
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
        correctAnswer: '', // 정답은 제출 후에만 표시
        difficulty: 'Medium',
        category: 'CS',
    };
};

// =============================================================================
// 컴포넌트
// =============================================================================

export const QuizSession = () => {
    const { courseId, sectionNumber } = useParams<{ courseId: string; sectionNumber: string }>();
    const navigate = useNavigate();
    const showToast = useUIStore((state) => state.showToast);

    // API 상태 관리
    const [attemptData, setAttemptData] = useState<AttemptData | null>(null);
    const [questions, setQuestions] = useState<QuizQuestionType[]>([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [answers, setAnswers] = useState<Record<string, string | string[]>>({});

    // UI 상태 관리
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isExitModalOpen, setIsExitModalOpen] = useState(false);
    const [isResumed, setIsResumed] = useState(false);
    const [hasChanges, setHasChanges] = useState(false);

    // 브라우저 뒤로가기/새로고침 시 경고 (beforeunload)
    useEffect(() => {
        if (!hasChanges) return;

        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            e.preventDefault();
            e.returnValue = '';
            return '';
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [hasChanges]);

    // 세션 초기화 (API 호출)
    useEffect(() => {
        const initializeSession = async () => {
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

                const data = await startOrResumeAttempt(numericCourseId, numericSectionNumber);
                setAttemptData(data);

                // 문제 변환 및 저장
                const uiQuestions = data.questions.map(mapApiQuestionToUiQuestion);
                setQuestions(uiQuestions);

                // 저장된 답안 복원
                const savedAnswers: Record<string, string | string[]> = {};
                data.questions.forEach(q => {
                    // 여기서 isValidAnswer가 true를 반환하면 q.savedAnswer는 string | string[]로 추론됩니다.
                    if (isValidAnswer(q.savedAnswer)) {
                        savedAnswers[String(q.questionId)] = q.savedAnswer;
                    }
                });
                setAnswers(savedAnswers);


                // 재개 여부 확인
                setIsResumed(data.answeredCount > 0);

                console.log('[QuizSession] 세션 시작/재개:', data.attemptId);
            } catch (err) {
                console.error('[QuizSession] 세션 초기화 실패:', err);
                setError(err instanceof Error ? err.message : '퀴즈를 시작하는데 실패했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        initializeSession();
    }, [courseId, sectionNumber]);

    // 현재 문제
    const currentQuestion = questions[currentIndex] || null;

    // 현재 답안
    const currentAnswer = currentQuestion ? answers[currentQuestion.id] : undefined;

    // 답변한 문제 수
    const answeredCount = useMemo(() => {
        return Object.values(answers).filter(isValidAnswer).length;
    }, [answers]);

    // 답안 변경 핸들러
    const handleAnswerChange = useCallback(async (answer: string | string[]) => {
        if (!attemptData || !currentQuestion || !courseId || !sectionNumber) return;

        // 로컬 상태 업데이트
        setAnswers(prev => ({
            ...prev,
            [currentQuestion.id]: answer,
        }));
        setHasChanges(true);

        // API로 답안 저장
        setIsSaving(true);
        try {
            await saveAnswerApi(
                parseInt(courseId, 10),
                parseInt(sectionNumber, 10),
                attemptData.attemptId,
                parseInt(currentQuestion.id, 10),
                answer
            );
            console.log('[QuizSession] 답안 저장 완료:', currentQuestion.id);
        } catch (err) {
            console.error('[QuizSession] 답안 저장 실패:', err);
            // 실패해도 로컬 상태는 유지 (다음 저장 시 재시도)
        } finally {
            setIsSaving(false);
        }
    }, [attemptData, currentQuestion, courseId, sectionNumber]);

    // 이전 문제로 이동
    const handlePrevious = useCallback(() => {
        if (currentIndex > 0) {
            setCurrentIndex(prev => prev - 1);
        }
    }, [currentIndex]);

    // 다음 문제로 이동
    const handleNext = useCallback(() => {
        // 1. 현재 문제의 답변 유효성 검사
        const currentAnswer = currentQuestion ? answers[currentQuestion.id] : undefined;

        if (!isValidAnswer(currentAnswer)) {
            showToast('답변을 선택해야 다음으로 넘어갈 수 있습니다.', 'info');
            return; // 유효하지 않으면 여기서 중단
        }

        // 2. 유효할 때만 다음 인덱스로 이동
        if (currentIndex < questions.length - 1) {
            setCurrentIndex(prev => prev + 1);
        }
    }, [currentIndex, questions.length, currentQuestion, answers, showToast]);

    // 퀴즈 완료 (제출)
    const handleComplete = useCallback(async () => {
        // 마지막 문제 답변 체크
        const currentAnswer = currentQuestion ? answers[currentQuestion.id] : undefined;

        if (!isValidAnswer(currentAnswer)) {
            showToast('마지막 문제의 답변을 선택해주세요.', 'info');
            return;
        }
        if (!attemptData || !courseId || !sectionNumber) return;

        setIsSubmitting(true);
        try {
            const result = await submitAttempt(
                parseInt(courseId, 10),
                parseInt(sectionNumber, 10),
                attemptData.attemptId
            );

            console.log('[QuizSession] 퀴즈 완료:', result);

            // TODO: 결과 페이지로 이동 또는 결과 모달 표시
            showToast(`점수: ${result.score}점 (정답: ${result.correctCount}/${result.totalQuestions}) ${result.isPassed ? '- 통과!' : '- 다시 도전해보세요!'}`, result.isPassed ? 'success' : 'info');

            setHasChanges(false);
            navigate(`/quiz-practice/${courseId}`, { replace: true });
        } catch (err) {
            console.error('[QuizSession] 제출 실패:', err);
            showToast(err instanceof Error ? err.message : '제출에 실패했습니다.', 'error');
        } finally {
            setIsSubmitting(false);
        }
    }, [attemptData, courseId, sectionNumber, navigate]);

    // 임시 저장 후 나가기 (현재 상태 유지, 다시 시작 시 재개)
    const handleSaveAndExit = useCallback(() => {
        setHasChanges(false);
        setIsExitModalOpen(false);
        navigate(`/quiz-practice/${courseId}`);
    }, [courseId, navigate]);

    // 시도 포기
    const handleAbandon = useCallback(async () => {
        if (!attemptData || !courseId || !sectionNumber) {
            setHasChanges(false);
            setIsExitModalOpen(false);
            navigate(`/quiz-practice/${courseId}`);
            return;
        }

        try {
            await abandonAttempt(
                parseInt(courseId, 10),
                parseInt(sectionNumber, 10),
                attemptData.attemptId
            );
            console.log('[QuizSession] 시도 포기 완료');
        } catch (err) {
            console.error('[QuizSession] 시도 포기 실패:', err);
        }

        setHasChanges(false);
        setIsExitModalOpen(false);
        navigate(`/quiz-practice/${courseId}`);
    }, [attemptData, courseId, sectionNumber, navigate]);

    // 모달 닫기 (계속 풀기)
    const handleCloseModal = useCallback(() => {
        setIsExitModalOpen(false);
    }, []);

    // 뒤로가기 버튼 클릭
    const handleBackClick = () => {
        if (hasChanges) {
            setIsExitModalOpen(true);
        } else {
            navigate(`/quiz-practice/${courseId}`);
        }
    };

    // 재시도 핸들러
    const handleRetry = () => {
        window.location.reload();
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
                        퀴즈를 불러오는 중...
                    </p>
                </div>
            </div>
        );
    }

    // 에러 상태
    if (error || !attemptData || questions.length === 0 || !currentQuestion) {
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
                        퀴즈를 불러올 수 없습니다
                    </h2>
                    <p
                        className="text-sm mb-6"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        {error || '문제 데이터를 찾을 수 없거나 세션 초기화에 실패했습니다.'}
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
                            코스로 돌아가기
                        </Button>

                        <div className="flex items-center gap-2">
                            {isSaving && (
                                <span
                                    className="text-xs flex items-center gap-1"
                                    style={{ color: 'var(--color-text-tertiary)' }}
                                >
                                    <Loader2 size={12} className="animate-spin" />
                                    저장 중...
                                </span>
                            )}

                            {isResumed && (
                                <span
                                    className="text-xs px-2 py-1 rounded-full"
                                    style={{
                                        backgroundColor: 'var(--color-info-light)',
                                        color: 'var(--color-info)',
                                    }}
                                >
                                    이전 진행 복원됨
                                </span>
                            )}
                        </div>
                    </div>

                    <QuizProgressBar
                        current={currentIndex + 1}
                        total={questions.length}
                    />
                </div>
            </header>

            {/* 메인 컨텐츠 */}
            <main className="max-w-3xl mx-auto px-4 py-8">
                {/* 문제 카드 */}
                <QuestionCard
                    question={currentQuestion}
                    questionNumber={currentIndex + 1}
                    currentAnswer={currentAnswer}
                    onAnswerChange={handleAnswerChange}
                    className="mb-8"
                />

                {/* 네비게이션 */}
                <QuizNavigation
                    currentIndex={currentIndex}
                    totalQuestions={questions.length}
                    onPrevious={handlePrevious}
                    onNext={handleNext}
                    onComplete={handleComplete}
                    isLoading={isSubmitting}
                    isNextDisabled={!isValidAnswer(currentAnswer)}
                    isCompleteDisabled={!isValidAnswer(currentAnswer)}
                />
            </main>

            {/* 종료 확인 모달 */}
            <QuizExitModal
                isOpen={isExitModalOpen}
                onClose={handleCloseModal}
                onSaveAndExit={handleSaveAndExit}
                onAbandon={handleAbandon}
                answeredCount={answeredCount}
                totalCount={questions.length}
            />
        </div>
    );
};
