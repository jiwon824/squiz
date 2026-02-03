/**
 * =============================================================================
 * ContinuousQuizNavigation.tsx - 연속 학습 모드 네비게이션 컴포넌트
 * =============================================================================
 *
 * 목적 (PURPOSE):
 * 연속 학습 모드(Sayvoca 스타일)를 위한 전진 전용 네비게이션입니다.
 * - "이전" 버튼 없음 (forward-only flow)
 * - "제출" 버튼만 제공
 *
 * =============================================================================
 */

import { Send } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { ButtonSpinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';

// -----------------------------------------------------------------------------
// Props 인터페이스
// -----------------------------------------------------------------------------
interface ContinuousQuizNavigationProps {
    /** 제출 버튼 클릭 핸들러 */
    onSubmit: () => void;
    /** 건너뛰기 버튼 클릭 핸들러 (옵션) */
    onSkip?: () => void;
    /** 로딩 상태 (제출 중) */
    isLoading?: boolean;
    /** 제출 버튼 비활성화 여부 (답변 미완료 시) */
    isSubmitDisabled?: boolean;
    /** 추가 CSS 클래스 */
    className?: string;
}

// -----------------------------------------------------------------------------
// 컴포넌트
// -----------------------------------------------------------------------------
export const ContinuousQuizNavigation: React.FC<ContinuousQuizNavigationProps> = ({
    onSubmit,
    onSkip,
    isLoading = false,
    isSubmitDisabled = false,
    className,
}) => {
    return (
        <div
            className={cn(
                'flex items-center justify-center gap-3',
                className
            )}
        >
            {onSkip && (
                <Button
                    variant="google-ghost"
                    size="lg"
                    onClick={onSkip}
                    disabled={isLoading}
                    className="min-w-[120px] px-6 text-text-secondary hover:bg-gray-100"
                >
                    건너뛰기
                </Button>
            )}

            <Button
                variant="google-primary"
                size="lg"
                onClick={onSubmit}
                disabled={isSubmitDisabled || isLoading}
                isLoading={isLoading}
                rightIcon={!isLoading && <Send size={20} />}
                className={cn(
                    'min-w-[200px] px-8',
                    isSubmitDisabled && !isLoading && 'opacity-50 cursor-not-allowed'
                )}
            >
                {isLoading ? (
                    <span className="flex items-center gap-2">
                        <ButtonSpinner />
                        채점 중...
                    </span>
                ) : (
                    '제출하기'
                )}
            </Button>
        </div>
    );
};
