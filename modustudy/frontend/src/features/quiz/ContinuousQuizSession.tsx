/**
 * =============================================================================
 * ContinuousQuizSession.tsx - 연속 학습 모드 세션 컴포넌트
 * =============================================================================
 *
 * 말해보카 스타일의 연속 학습 플로우를 구현하는 메인 컴포넌트입니다.
 *
 * 핵심 특징:
 * - Atomic Interaction: 매 제출마다 다음 문제를 직접 반환
 * - Forward-only: 뒤로가기 없는 전진 전용 플로우
 * - FSRS Timing: useContinuousQuiz 훅에서 문제 렌더링 시점부터 제출까지의 시간 측정
 * - Instant Feedback: 제출 즉시 정답/오답 피드백 표시
 *
 * URL: /continuous-quiz/:courseId/section/:sectionNumber
 *
 * =============================================================================
 */

import { Spinner } from '@/shared/components/Spinner';
import { QuestionCard } from './components/QuestionCard';
import { ContinuousQuizNavigation } from './components/ContinuousQuizNavigation';

import { useContinuousQuiz } from './hooks/useContinuousQuiz';
import { FeedbackModal } from './components/continuous/FeedbackModal';
import { SessionComplete } from './components/continuous/SessionComplete';
import { SessionHeader } from './components/continuous/SessionHeader';
import { SkipConfirmModal } from './components/continuous/SkipConfirmModal';
import { ErrorScreen } from './components/continuous/ErrorScreen';

// =============================================================================
// 컴포넌트
// =============================================================================

export const ContinuousQuizSession = () => {
    const {
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
        isAnswerValid,
        questionLimit,
    } = useContinuousQuiz();

    // ─────────────────────────────────────────────────────────────────────────
    // 로딩 상태
    // ─────────────────────────────────────────────────────────────────────────
    if (ui.isLoading) {
        return (
            <div
                className="min-h-screen flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-background)' }}
            >
                <Spinner variant="center" size="xl" label="학습을 준비하는 중..." />
            </div>
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 에러 상태
    // ─────────────────────────────────────────────────────────────────────────
    if (ui.error || (!question.current && !session.isComplete)) {
        return (
            <ErrorScreen
                error={ui.error}
                onRetry={handleRetry}
                onReturnToCourse={handleReturnToCourse}
            />
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 세션 완료 화면
    // ─────────────────────────────────────────────────────────────────────────
    if (session.isComplete && session.summary) {
        return (
            <SessionComplete
                summary={session.summary}
                onReturnToCourse={handleReturnToCourse}
                onRetry={handleRetry}
            />
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 메인 퀴즈 화면
    // ─────────────────────────────────────────────────────────────────────────
    return (
        <div
            className="min-h-screen"
            style={{ backgroundColor: 'var(--color-background)' }}
        >
            {/* 헤더 */}
            <SessionHeader
                solvedCount={progress.solvedCount}
                correctCount={progress.correctCount}
                questionLimit={questionLimit}
                onBackClick={handleBackClick}
            />

            {/* 메인 컨텐츠 */}
            <main className="max-w-3xl mx-auto px-4 py-8">
                {question.current && (
                    <>
                        {/* 문제 카드 */}
                        <QuestionCard
                            question={question.current}
                            questionNumber={progress.solvedCount + 1}
                            currentAnswer={question.answer}
                            onAnswerChange={handleAnswerChange}
                            onSubmit={modal.showFeedback ? undefined : handleSubmit}
                            className="mb-8"
                        />

                        {/* 제출/건너뛰기 버튼 */}
                        <ContinuousQuizNavigation
                            onSubmit={handleSubmit}
                            onSkip={handleSkipClick}
                            isLoading={ui.isSubmitting}
                            isSubmitDisabled={!isAnswerValid}
                        />
                    </>
                )}
            </main>

            {/* 피드백 모달 */}
            {modal.feedbackData && (
                <FeedbackModal
                    isOpen={modal.showFeedback}
                    isCorrect={modal.feedbackData.isCorrect}
                    correctAnswer={modal.feedbackData.correctAnswer}
                    explanation={modal.feedbackData.explanation}
                    onContinue={handleNextQuestion}
                />
            )}

            {/* 건너뛰기 확인 모달 */}
            <SkipConfirmModal
                isOpen={modal.showSkipConfirm}
                onCancel={handleSkipCancel}
                onConfirm={handleSkipConfirm}
            />
        </div>
    );
};
