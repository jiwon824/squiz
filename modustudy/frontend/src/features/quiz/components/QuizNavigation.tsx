/**
 * =============================================================================
 * QuizNavigation.tsx - 퀴즈 네비게이션 버튼 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 이전/다음 문제로 이동하는 네비게이션 버튼을 제공합니다.
 * 마지막 문제에서는 "다음" 버튼이 "완료" 버튼으로 변경됩니다.
 * 
 * =============================================================================
 */

import { ChevronLeft, ChevronRight, CheckCircle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { cn } from '@/shared/utils/cn';

// -----------------------------------------------------------------------------
// Props 인터페이스
// -----------------------------------------------------------------------------
interface QuizNavigationProps {
    /** 현재 문제 인덱스 (0-based) */
    currentIndex: number;
    /** 전체 문제 수 */
    totalQuestions: number;
    /** 이전 버튼 클릭 핸들러 */
    onPrevious: () => void;
    /** 다음 버튼 클릭 핸들러 */
    onNext: () => void;
    /** 완료 버튼 클릭 핸들러 */
    onComplete: () => void;
    /** 로딩 상태 */
    isLoading?: boolean;
    /** 다음 버튼 비활성화 여부 (현재 문제 답변 미완료 시) */
    isNextDisabled?: boolean;
    /** 완료 버튼 비활성화 여부 (현재 문제 답변 미완료 시) */
    isCompleteDisabled?: boolean;
    /** 추가 CSS 클래스 */
    className?: string;
}

// -----------------------------------------------------------------------------
// 컴포넌트
// -----------------------------------------------------------------------------
export const QuizNavigation: React.FC<QuizNavigationProps> = ({
    currentIndex,
    totalQuestions,
    onPrevious,
    onNext,
    onComplete,
    isLoading = false,
    isNextDisabled = false,
    isCompleteDisabled = false,
    className,
}) => {
    // 첫 번째 문제인지 확인
    const isFirstQuestion = currentIndex === 0;
    // 마지막 문제인지 확인
    const isLastQuestion = currentIndex === totalQuestions - 1;

    return (
        <div
            className={cn(
                'flex items-center justify-between gap-4',
                className
            )}
        >
            {/* 이전 버튼 */}
            <Button
                variant="google-outline"
                size="lg"
                onClick={onPrevious}
                disabled={isFirstQuestion || isLoading}
                leftIcon={<ChevronLeft size={20} />}
                className="min-w-[120px]"
            >
                이전
            </Button>

            {/* 다음/완료 버튼 */}
            {isLastQuestion ? (
                <Button
                    variant="primary"
                    size="lg"
                    onClick={onComplete}
                    disabled={isCompleteDisabled || isLoading}
                    isLoading={isLoading}
                    rightIcon={!isLoading && <CheckCircle size={20} />}
                    className={cn(
                        'min-w-[120px]',
                        isCompleteDisabled && !isLoading && 'opacity-50 cursor-not-allowed'
                    )}
                    style={{
                        background: isCompleteDisabled && !isLoading
                            ? 'var(--color-text-disabled, #9ca3af)'
                            : 'linear-gradient(135deg, var(--color-google-green) 0%, var(--color-secondary) 100%)',
                    }}
                >
                    완료
                </Button>
            ) : (
                <Button
                    variant="google-primary"
                    size="lg"
                    onClick={onNext}
                    disabled={isNextDisabled || isLoading}
                    isLoading={isLoading}
                    rightIcon={!isLoading && <ChevronRight size={20} />}
                    className={cn(
                        'min-w-[120px]',
                        isNextDisabled && !isLoading && 'opacity-50 cursor-not-allowed'
                    )}
                >
                    다음
                </Button>
            )}
        </div>
    );
};
