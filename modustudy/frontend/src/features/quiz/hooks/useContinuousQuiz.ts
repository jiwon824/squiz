/**
 * =============================================================================
 * useContinuousQuiz.ts - 연속 학습 세션 핵심 로직 훅
 * =============================================================================
 *
 * 연속 학습 모드의 모든 비즈니스 로직을 캡슐화합니다.
 *
 * FSRS 타이머 로직:
 * - startTimer: 새 문제 렌더 시 (currentQuestion && !showFeedback && !showSkipConfirm)
 * - stopTimer: 답안 제출 시 (handleSubmit, handleSkipConfirm) → responseTimeMs 반환
 * - pauseTimer: 건너뛰기 클릭 시 (handleSkipClick)
 * - resumeTimer: 건너뛰기 취소 시 (handleSkipCancel)
 *
 * =============================================================================
 */

import { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useUIStore } from '@/store/uiStore';
import { useTimer } from './useTimer';
import {
    fetchNextQuestion,
    submitAnswer,
    ContinuousQuizQuestion,
} from '@/api/endpoints/continuousQuizApi';
import {
    QuestionState,
    ProgressState,
    ModalState,
    SessionState,
    UiState,
    SessionSummary,
    FeedbackData,
    isValidAnswer,
    mapApiQuestionToUiQuestion,
} from '../types/ContinuousQuiz.types';

// =============================================================================
// 훅 반환 타입
// =============================================================================

export interface UseContinuousQuizReturn {
    // 상태 그룹
    question: QuestionState;
    progress: ProgressState;
    modal: ModalState;
    session: SessionState;
    ui: UiState;

    // 액션
    handleAnswerChange: (answer: string | string[]) => void;
    handleSubmit: () => Promise<void>;
    handleSkipClick: () => void;
    handleSkipCancel: () => void;
    handleSkipConfirm: () => Promise<void>;
    handleNextQuestion: () => void;
    handleReturnToCourse: () => void;
    handleRetry: () => void;
    handleBackClick: () => void;

    // 유틸리티
    isAnswerValid: boolean;
    questionLimit: number | undefined;
}

// =============================================================================
// 훅 구현
// =============================================================================

export function useContinuousQuiz(): UseContinuousQuizReturn {
    const { courseId, sectionNumber } = useParams<{
        courseId: string;
        sectionNumber: string;
    }>();
    const navigate = useNavigate();
    const location = useLocation();
    const showToast = useUIStore((state) => state.showToast);

    // 문제 제한 설정 (없으면 무제한)
    const questionLimit = (location.state as { limit?: number })?.limit;

    // ─────────────────────────────────────────────────────────────────────────
    // FSRS 응답 시간 측정 (useTimer - 탭 전환 감지 포함)
    // ─────────────────────────────────────────────────────────────────────────
    const { start: startTimer, stop: stopTimer, pause: pauseTimer, resume: resumeTimer } = useTimer();

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 그룹 1: 문제 상태
    // ─────────────────────────────────────────────────────────────────────────
    const [question, setQuestion] = useState<QuestionState>({
        current: null,
        currentApi: null,
        answer: undefined,
        next: null,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 그룹 2: 진행 통계 (FSRS 응답 시간 누적 포함)
    // ─────────────────────────────────────────────────────────────────────────
    const [progress, setProgress] = useState<ProgressState>({
        solvedCount: 0,
        correctCount: 0,
        totalResponseTimeMs: 0,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 그룹 3: 모달 상태
    // ─────────────────────────────────────────────────────────────────────────
    const [modal, setModal] = useState<ModalState>({
        showFeedback: false,
        showSkipConfirm: false,
        feedbackData: null,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 그룹 4: 세션 상태
    // ─────────────────────────────────────────────────────────────────────────
    const [session, setSession] = useState<SessionState>({
        isComplete: false,
        summary: null,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 그룹 5: UI 상태
    // ─────────────────────────────────────────────────────────────────────────
    const [ui, setUi] = useState<UiState>({
        isLoading: true,
        isSubmitting: false,
        error: null,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // Effect 1: 브라우저 뒤로가기/새로고침 시 경고
    // ─────────────────────────────────────────────────────────────────────────
    useEffect(() => {
        if (session.isComplete) return;

        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            e.preventDefault();
            e.returnValue = '';
            return '';
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [session.isComplete]);

    // ─────────────────────────────────────────────────────────────────────────
    // Effect 2: 새 문제 렌더 시 FSRS 타이머 시작
    // 조건: 문제가 있고, 피드백/건너뛰기 모달이 닫혀있을 때
    // ─────────────────────────────────────────────────────────────────────────
    useEffect(() => {
        if (question.current && !modal.showFeedback && !modal.showSkipConfirm) {
            startTimer();
        }
    }, [question.current, modal.showFeedback, modal.showSkipConfirm, startTimer]);

    // ─────────────────────────────────────────────────────────────────────────
    // Effect 3: 초기 문제 로드
    // ─────────────────────────────────────────────────────────────────────────
    useEffect(() => {
        const loadFirstQuestion = async () => {
            if (!courseId || !sectionNumber) {
                setUi(prev => ({ ...prev, error: '코스 ID 또는 섹션 번호가 없습니다.', isLoading: false }));
                return;
            }

            setUi(prev => ({ ...prev, isLoading: true, error: null }));

            try {
                const numericCourseId = parseInt(courseId, 10);
                const numericSectionNumber = parseInt(sectionNumber, 10);

                if (isNaN(numericCourseId) || isNaN(numericSectionNumber)) {
                    throw new Error('잘못된 코스 ID 또는 섹션 번호입니다.');
                }

                const response = await fetchNextQuestion(numericCourseId, numericSectionNumber);

                if (!response.question) {
                    throw new Error('해당 섹션에 문제가 없습니다.');
                }

                const uiQuestion = mapApiQuestionToUiQuestion(response.question);
                setQuestion(prev => ({
                    ...prev,
                    current: uiQuestion,
                    currentApi: response.question,
                }));

                console.log('[useContinuousQuiz] 첫 번째 문제 로드 완료:', {
                    questionId: response.question.questionId,
                });

                setUi(prev => ({ ...prev, isLoading: false }));
            } catch (err) {
                console.error('[useContinuousQuiz] 초기화 실패:', err);
                setUi(prev => ({
                    ...prev,
                    error: err instanceof Error ? err.message : '퀴즈를 시작하는데 실패했습니다.',
                    isLoading: false,
                }));
            }
        };

        loadFirstQuestion();
    }, [courseId, sectionNumber]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 답안 변경
    // ─────────────────────────────────────────────────────────────────────────
    const handleAnswerChange = useCallback((answer: string | string[]) => {
        setQuestion(prev => ({ ...prev, answer }));
    }, []);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 답안 제출 (FSRS stopTimer로 시간 측정)
    // ─────────────────────────────────────────────────────────────────────────
    const handleSubmit = useCallback(async () => {
        // 피드백 모달이 열려있을 때는 제출 방지 (Enter 연타 버그 수정)
        if (modal.showFeedback) return;
        if (!question.currentApi) return;

        // 답안 검증
        if (!isValidAnswer(question.answer)) {
            showToast('답변을 선택해주세요.', 'info');
            return;
        }

        // FSRS: 타이머 정지 및 응답 시간 획득
        const responseTimeMs = stopTimer();
        setUi(prev => ({ ...prev, isSubmitting: true }));

        try {
            const response = await submitAnswer(
                question.currentApi.questionId,
                question.answer,
                responseTimeMs
            );

            // 피드백 데이터 설정
            setModal(prev => ({
                ...prev,
                showFeedback: true,
                feedbackData: {
                    isCorrect: response.isCorrect,
                    correctAnswer: response.correctAnswer,
                    explanation: response.explanation,
                },
            }));

            // 진행 통계 업데이트 (FSRS 응답 시간 누적)
            setProgress(prev => ({
                solvedCount: prev.solvedCount + 1,
                correctCount: response.isCorrect ? prev.correctCount + 1 : prev.correctCount,
                totalResponseTimeMs: prev.totalResponseTimeMs + responseTimeMs,
            }));

            // 다음 문제 저장
            if (response.nextQuestion) {
                setQuestion(prev => ({ ...prev, next: response.nextQuestion }));
            } else {
                console.error('[useContinuousQuiz] 다음 문제가 없습니다');
                showToast('다음 문제를 불러오는데 실패했습니다.', 'error');
            }
        } catch (err) {
            console.error('[useContinuousQuiz] 제출 실패:', err);
            showToast(err instanceof Error ? err.message : '제출에 실패했습니다.', 'error');
        } finally {
            setUi(prev => ({ ...prev, isSubmitting: false }));
        }
    }, [modal.showFeedback, question.currentApi, question.answer, showToast, stopTimer]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 건너뛰기 클릭 (FSRS pauseTimer)
    // ─────────────────────────────────────────────────────────────────────────
    const handleSkipClick = useCallback(() => {
        pauseTimer();
        setModal(prev => ({ ...prev, showSkipConfirm: true }));
    }, [pauseTimer]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 건너뛰기 취소 (FSRS resumeTimer)
    // ─────────────────────────────────────────────────────────────────────────
    const handleSkipCancel = useCallback(() => {
        setModal(prev => ({ ...prev, showSkipConfirm: false }));
        resumeTimer();
    }, [resumeTimer]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 건너뛰기 확인 - 오답 처리 (FSRS stopTimer)
    // ─────────────────────────────────────────────────────────────────────────
    const handleSkipConfirm = useCallback(async () => {
        setModal(prev => ({ ...prev, showSkipConfirm: false }));

        if (!question.currentApi) return;

        // FSRS: 일시정지 상태에서도 누적 시간 반환
        const responseTimeMs = stopTimer();
        setUi(prev => ({ ...prev, isSubmitting: true }));

        try {
            // 빈 문자열 제출 → 오답 처리
            const response = await submitAnswer(
                question.currentApi.questionId,
                "",
                responseTimeMs
            );

            setModal(prev => ({
                ...prev,
                showFeedback: true,
                feedbackData: {
                    isCorrect: false, // 건너뛰기는 항상 오답
                    correctAnswer: response.correctAnswer,
                    explanation: response.explanation,
                },
            }));

            // 진행 통계 업데이트 (오답이므로 correctCount 미증가)
            setProgress(prev => ({
                solvedCount: prev.solvedCount + 1,
                correctCount: prev.correctCount,
                totalResponseTimeMs: prev.totalResponseTimeMs + responseTimeMs,
            }));

            if (response.nextQuestion) {
                setQuestion(prev => ({ ...prev, next: response.nextQuestion }));
            } else {
                showToast('다음 문제를 불러오는데 실패했습니다.', 'error');
            }
        } catch (err) {
            console.error('[useContinuousQuiz] 건너뛰기 제출 실패:', err);
            showToast('제출에 실패했습니다.', 'error');
        } finally {
            setUi(prev => ({ ...prev, isSubmitting: false }));
        }
    }, [question.currentApi, stopTimer, showToast]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 다음 문제로 전환 (피드백 확인 후)
    // ─────────────────────────────────────────────────────────────────────────
    const handleNextQuestion = useCallback(() => {
        // 목표 문제 수 달성 시 세션 종료
        if (questionLimit && progress.solvedCount >= questionLimit) {
            const avgResponseTime = progress.solvedCount > 0
                ? Math.round(progress.totalResponseTimeMs / progress.solvedCount)
                : 0;

            setSession({
                isComplete: true,
                summary: {
                    totalQuestions: progress.solvedCount,
                    correctCount: progress.correctCount,
                    incorrectCount: progress.solvedCount - progress.correctCount,
                    averageResponseTimeMs: avgResponseTime,
                },
            });
            return;
        }

        // 다음 문제로 전환
        if (question.next) {
            const nextUiQuestion = mapApiQuestionToUiQuestion(question.next);
            setQuestion({
                current: nextUiQuestion,
                currentApi: question.next,
                answer: undefined,
                next: null,
            });
        }

        // 모달 상태 초기화
        setModal({
            showFeedback: false,
            showSkipConfirm: false,
            feedbackData: null,
        });
    }, [questionLimit, progress, question.next]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 코스로 돌아가기
    // ─────────────────────────────────────────────────────────────────────────
    const handleReturnToCourse = useCallback(() => {
        navigate(`/quiz-practice/${courseId}`);
    }, [courseId, navigate]);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 다시 학습하기
    // ─────────────────────────────────────────────────────────────────────────
    const handleRetry = useCallback(() => {
        window.location.reload();
    }, []);

    // ─────────────────────────────────────────────────────────────────────────
    // 핸들러: 뒤로가기/나가기 (세션 종료)
    // ─────────────────────────────────────────────────────────────────────────
    const handleBackClick = useCallback(() => {
        if (progress.solvedCount === 0) {
            navigate(`/quiz-practice/${courseId}`);
            return;
        }

        if (confirm('학습을 종료하고 결과를 확인하시겠습니까?')) {
            const avgResponseTime = progress.solvedCount > 0
                ? Math.round(progress.totalResponseTimeMs / progress.solvedCount)
                : 0;

            setSession({
                isComplete: true,
                summary: {
                    totalQuestions: progress.solvedCount,
                    correctCount: progress.correctCount,
                    incorrectCount: progress.solvedCount - progress.correctCount,
                    averageResponseTimeMs: avgResponseTime,
                },
            });
        }
    }, [progress, courseId, navigate]);

    // ─────────────────────────────────────────────────────────────────────────
    // 반환
    // ─────────────────────────────────────────────────────────────────────────
    return {
        // 상태 그룹
        question,
        progress,
        modal,
        session,
        ui,

        // 액션
        handleAnswerChange,
        handleSubmit,
        handleSkipClick,
        handleSkipCancel,
        handleSkipConfirm,
        handleNextQuestion,
        handleReturnToCourse,
        handleRetry,
        handleBackClick,

        // 유틸리티
        isAnswerValid: isValidAnswer(question.answer),
        questionLimit,
    };
}
