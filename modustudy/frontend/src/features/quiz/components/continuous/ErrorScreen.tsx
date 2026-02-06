/**
 * =============================================================================
 * ErrorScreen.tsx - 퀴즈 에러 화면
 * =============================================================================
 *
 * 퀴즈 로드 실패 시 표시되는 에러 화면 컴포넌트입니다.
 *
 * =============================================================================
 */

import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '@/shared/components/Button';

// =============================================================================
// 타입 정의
// =============================================================================

export interface ErrorScreenProps {
    /** 에러 메시지 (없으면 기본 메시지 표시) */
    error: string | null;
    /** 다시 시도 핸들러 */
    onRetry: () => void;
    /** 코스로 돌아가기 핸들러 */
    onReturnToCourse: () => void;
}

// =============================================================================
// 컴포넌트
// =============================================================================

export const ErrorScreen: React.FC<ErrorScreenProps> = ({
    error,
    onRetry,
    onReturnToCourse,
}) => {
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
                {/* 에러 아이콘 */}
                <AlertCircle
                    size={48}
                    className="mx-auto mb-4"
                    style={{ color: 'var(--color-error)' }}
                />

                {/* 에러 제목 */}
                <h2
                    className="text-xl font-bold mb-2"
                    style={{ color: 'var(--color-text-primary)' }}
                >
                    문제를 불러올 수 없습니다
                </h2>

                {/* 에러 메시지 */}
                <p
                    className="text-sm mb-6"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    {error || '문제 데이터를 찾을 수 없습니다.'}
                </p>

                {/* 액션 버튼 */}
                <div className="flex gap-3 justify-center">
                    <Button
                        variant="google-primary"
                        size="md"
                        onClick={onRetry}
                        leftIcon={<RefreshCw size={18} />}
                    >
                        다시 시도
                    </Button>
                    <Button
                        variant="google-ghost"
                        size="md"
                        onClick={onReturnToCourse}
                    >
                        코스로 돌아가기
                    </Button>
                </div>
            </div>
        </div>
    );
};
