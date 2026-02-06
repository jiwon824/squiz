/**
 * =============================================================================
 * FeedbackModal.tsx - 퀴즈 답변 피드백 모달
 * =============================================================================
 *
 * 정답/오답 즉시 피드백을 표시하는 모달 컴포넌트입니다.
 *
 * 주요 기능:
 * - 정답/오답 시각적 구분 (아이콘, 색상)
 * - 오답 시 정답 표시
 * - 해설 표시 (선택)
 * - Enter 키로 다음 문제 진행 (100ms 지연으로 제출 Enter 전파 방지)
 *
 * =============================================================================
 */

import { useEffect } from 'react';
import { CheckCircle, XCircle } from 'lucide-react';
import { Button } from '@/shared/components/Button';

// =============================================================================
// 타입 정의
// =============================================================================

export interface FeedbackModalProps {
    /** 모달 표시 여부 */
    isOpen: boolean;
    /** 정답 여부 */
    isCorrect: boolean;
    /** 정답 텍스트 */
    correctAnswer: string;
    /** 해설 (선택) */
    explanation?: string;
    /** 다음 문제로 이동 핸들러 */
    onContinue: () => void;
}

// =============================================================================
// 컴포넌트
// =============================================================================

export const FeedbackModal: React.FC<FeedbackModalProps> = ({
    isOpen,
    isCorrect,
    correctAnswer,
    explanation,
    onContinue,
}) => {
    // ─────────────────────────────────────────────────────────────────────────
    // Enter 키로 다음 문제로 넘어가기
    // 제출 시 Enter 키 이벤트가 모달에 전파되지 않도록 지연 등록
    // ─────────────────────────────────────────────────────────────────────────
    useEffect(() => {
        if (!isOpen) return;

        const handleKeyDown = (e: KeyboardEvent) => {
            // IME 조합 중일 때는 무시 (한글 입력 등)
            if ((e as any).isComposing) return;
            if (e.key !== 'Enter') return;

            e.preventDefault();
            e.stopPropagation();
            onContinue();
        };

        // 100ms 지연으로 제출 Enter 키가 모달에 전파되는 것을 방지
        const timeoutId = setTimeout(() => {
            window.addEventListener('keydown', handleKeyDown);
        }, 100);

        return () => {
            clearTimeout(timeoutId);
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, onContinue]);

    // 모달이 닫혀있으면 렌더링하지 않음
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
