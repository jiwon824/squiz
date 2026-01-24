/**
 * =============================================================================
 * QuizSession.tsx - 퀴즈 세션 메인 페이지 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 퀴즈 세션의 전체 플로우를 관리하는 메인 페이지 컴포넌트입니다.
 * - 새 세션 시작 또는 이전 세션 복원
 * - 문제 셔플 및 순서 유지
 * - 답안 추적 및 저장
 * - 네비게이션 가드 (뒤로가기 시 확인)
 * - Mock API를 통한 답안 동기화
 * 
 * URL: /quiz-practice/:courseId/section/:sectionId/session
 * 
 * =============================================================================
 */

import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

// 컴포넌트 임포트
import { QuestionCard } from './components/QuestionCard';
import { QuizProgressBar } from './components/QuizProgressBar';
import { QuizNavigation } from './components/QuizNavigation';
import { QuizExitModal } from './components/QuizExitModal';
import { Button } from '@/shared/components/Button';

// 타입 및 서비스 임포트
import { QuizSession as QuizSessionType } from './types/QuizQuestion.types';
import { getQuestionById } from './data/quizQuestionData';
import {
    resumeOrCreateSession,
    updateAnswer,
    updateCurrentIndex,
    saveSession,
    clearSession,
    syncAnswerToServer,
    completeSession,
} from './services/quizSessionService';

// =============================================================================
// 컴포넌트
// =============================================================================

export const QuizSession = () => {
    const { courseId, sectionId } = useParams<{ courseId: string; sectionId: string }>();
    const navigate = useNavigate();

    // 상태 관리
    const [session, setSession] = useState<QuizSessionType | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isExitModalOpen, setIsExitModalOpen] = useState(false);
    const [isResumed, setIsResumed] = useState(false);
    const [hasChanges, setHasChanges] = useState(false);
    const [isInitialized, setIsInitialized] = useState(false);

    // 브라우저 뒤로가기/새로고침 시 경고 (beforeunload)
    useEffect(() => {
        if (!hasChanges) return;

        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            e.preventDefault();
            // 표준에 따라 returnValue 설정 (크롬 등에서 필요)
            e.returnValue = '';
            return '';
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [hasChanges]);

    // 세션 초기화
    useEffect(() => {
        if (!sectionId) {
            console.error('[QuizSession] sectionId가 없습니다.');
            setIsInitialized(true);
            return;
        }

        try {
            const { session: loadedSession, isResumed: resumed } = resumeOrCreateSession(sectionId);
            setSession(loadedSession);
            setIsResumed(resumed);

            if (resumed) {
                console.log('[QuizSession] 이전 세션 복원됨');
            }
        } catch (error) {
            console.error('[QuizSession] 세션 초기화 실패:', error);
        } finally {
            setIsInitialized(true);
        }
    }, [sectionId]);

    // 현재 문제 가져오기
    const currentQuestion = session
        ? getQuestionById(session.questionOrder[session.currentIndex])
        : null;

    // 현재 답안 가져오기
    const currentAnswer = session && currentQuestion
        ? session.answers[currentQuestion.id]?.answer
        : undefined;

    // 답변한 문제 수
    const answeredCount = session ? Object.keys(session.answers).length : 0;

    // 답안 변경 핸들러
    const handleAnswerChange = useCallback((answer: string | string[]) => {
        if (!session || !currentQuestion) return;

        const updatedSession = updateAnswer(session, currentQuestion.id, answer);
        setSession(updatedSession);
        setHasChanges(true);

        // Mock API 동기화
        syncAnswerToServer(session.sessionId, {
            questionId: currentQuestion.id,
            answer,
            timestamp: Date.now(),
        });
    }, [session, currentQuestion]);

    // 이전 문제로 이동
    const handlePrevious = useCallback(async () => {
        if (!session || session.currentIndex === 0) return;

        setIsLoading(true);
        try {
            const newIndex = session.currentIndex - 1;
            const updatedSession = updateCurrentIndex(session, newIndex);
            setSession(updatedSession);
        } finally {
            setIsLoading(false);
        }
    }, [session]);

    // 다음 문제로 이동
    const handleNext = useCallback(async () => {
        if (!session || session.currentIndex >= session.questionOrder.length - 1) return;

        setIsLoading(true);
        try {
            const newIndex = session.currentIndex + 1;
            const updatedSession = updateCurrentIndex(session, newIndex);
            setSession(updatedSession);
        } finally {
            setIsLoading(false);
        }
    }, [session]);

    // 퀴즈 완료
    const handleComplete = useCallback(async () => {
        if (!session) return;

        setIsLoading(true);
        try {
            const result = await completeSession(session);
            console.log('[QuizSession] 퀴즈 완료:', result);

            // 완료 후 코스 상세 페이지로 이동
            setHasChanges(false);
            navigate(`/quiz-practice/${courseId}`, { replace: true });
        } finally {
            setIsLoading(false);
        }
    }, [session, courseId, navigate]);

    // 임시 저장 후 나가기
    const handleSaveAndExit = useCallback(() => {
        if (session) {
            saveSession(session);
        }
        setHasChanges(false);
        setIsExitModalOpen(false);
        navigate(`/quiz-practice/${courseId}`);
    }, [session, courseId, navigate]);

    // 저장하지 않고 나가기
    const handleAbandon = useCallback(() => {
        if (sectionId) {
            clearSession(sectionId);
        }
        setHasChanges(false);
        setIsExitModalOpen(false);
        navigate(`/quiz-practice/${courseId}`);
    }, [sectionId, courseId, navigate]);

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

    // 초기화 중 로딩 표시
    if (!isInitialized) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <div className="text-center">
                    <div
                        className="w-12 h-12 border-4 border-t-transparent rounded-full animate-spin mx-auto mb-4"
                        style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }}
                    />
                    <p style={{ color: 'var(--color-text-secondary)' }}>
                        퀴즈를 불러오는 중...
                    </p>
                </div>
            </div>
        );
    }

    // 세션 또는 문제가 없는 경우 에러 표시
    if (!session || !currentQuestion) {
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
                    <div
                        className="w-16 h-16 mx-auto mb-4 flex items-center justify-center rounded-full"
                        style={{ backgroundColor: 'var(--color-error-light)' }}
                    >
                        <span style={{ fontSize: '2rem' }}>⚠️</span>
                    </div>
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
                        문제 데이터를 찾을 수 없거나 세션 초기화에 실패했습니다.
                    </p>
                    <Button
                        variant="google-primary"
                        size="md"
                        onClick={() => navigate(`/quiz-practice/${courseId || ''}`)}
                    >
                        코스 목록으로 돌아가기
                    </Button>
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

                    <QuizProgressBar
                        current={session.currentIndex + 1}
                        total={session.questionOrder.length}
                    />
                </div>
            </header>

            {/* 메인 컨텐츠 */}
            <main className="max-w-3xl mx-auto px-4 py-8">
                {/* 문제 카드 */}
                <QuestionCard
                    question={currentQuestion}
                    questionNumber={session.currentIndex + 1}
                    currentAnswer={currentAnswer}
                    onAnswerChange={handleAnswerChange}
                    className="mb-8"
                />

                {/* 네비게이션 */}
                <QuizNavigation
                    currentIndex={session.currentIndex}
                    totalQuestions={session.questionOrder.length}
                    onPrevious={handlePrevious}
                    onNext={handleNext}
                    onComplete={handleComplete}
                    isLoading={isLoading}
                />
            </main>

            {/* 종료 확인 모달 */}
            <QuizExitModal
                isOpen={isExitModalOpen}
                onClose={handleCloseModal}
                onSaveAndExit={handleSaveAndExit}
                onAbandon={handleAbandon}
                answeredCount={answeredCount}
                totalCount={session.questionOrder.length}
            />
        </div>
    );
};
