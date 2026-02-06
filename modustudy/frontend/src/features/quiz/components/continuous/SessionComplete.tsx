/**
 * =============================================================================
 * SessionComplete.tsx - 세션 완료 결과 화면
 * =============================================================================
 *
 * 연속 학습 세션 완료 후 결과를 표시하는 전체 화면 컴포넌트입니다.
 *
 * 주요 기능:
 * - 학습 통계 표시 (총 문제, 정답률, 평균 응답 시간)
 * - 정답/오답 개수 상세 표시
 * - 다시 학습하기 / 코스로 돌아가기 액션
 *
 * FSRS 관련:
 * - averageResponseTimeMs는 부모에서 계산되어 전달됨
 * - 이 컴포넌트에서는 ms → s 변환만 수행 (소수점 1자리)
 *
 * =============================================================================
 */

import { CheckCircle, XCircle, Trophy, RefreshCw } from 'lucide-react';
import { Button } from '@/shared/components/Button';

// =============================================================================
// 타입 정의
// =============================================================================

/** 세션 통계 요약 데이터 */
export interface SessionSummary {
    /** 총 문제 수 */
    totalQuestions: number;
    /** 정답 수 */
    correctCount: number;
    /** 오답 수 */
    incorrectCount: number;
    /** 평균 응답 시간 (밀리초) - FSRS 측정값 */
    averageResponseTimeMs: number;
}

export interface SessionCompleteProps {
    /** 세션 통계 요약 */
    summary: SessionSummary;
    /** 코스로 돌아가기 핸들러 */
    onReturnToCourse: () => void;
    /** 다시 학습하기 핸들러 */
    onRetry: () => void;
}

// =============================================================================
// 컴포넌트
// =============================================================================

export const SessionComplete: React.FC<SessionCompleteProps> = ({
    summary,
    onReturnToCourse,
    onRetry,
}) => {
    // ─────────────────────────────────────────────────────────────────────────
    // 통계 계산
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 정답률 계산 (백분율, 반올림)
     * - 0으로 나누기 방지: totalQuestions가 0이면 0% 반환
     */
    const accuracy = summary.totalQuestions > 0
        ? Math.round((summary.correctCount / summary.totalQuestions) * 100)
        : 0;

    /**
     * 평균 응답 시간 변환 (ms → s, 소수점 1자리)
     * - FSRS에서 측정된 밀리초 값을 사용자 친화적인 초 단위로 변환
     */
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

                {/* ─────────────────────────────────────────────────────────────
                    통계 그리드: 총 문제 | 정답률 | 평균 시간
                ───────────────────────────────────────────────────────────── */}
                <div
                    className="grid grid-cols-3 gap-4 mb-8 p-4 rounded-xl"
                    style={{ backgroundColor: 'var(--color-background)' }}
                >
                    {/* 총 문제 */}
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

                    {/* 정답률 (%) */}
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

                    {/* 평균 응답 시간 (초) */}
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

                {/* ─────────────────────────────────────────────────────────────
                    상세 결과: 정답/오답 개수
                ───────────────────────────────────────────────────────────── */}
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

                {/* ─────────────────────────────────────────────────────────────
                    액션 버튼
                ───────────────────────────────────────────────────────────── */}
                <div className="flex flex-col gap-3">
                    <Button
                        variant="google-outline"
                        size="md"
                        onClick={onRetry}
                        leftIcon={<RefreshCw size={18} />}
                        className="w-full"
                    >
                        다시 학습하기
                    </Button>
                    <Button
                        variant="google-primary"
                        size="lg"
                        onClick={onReturnToCourse}
                        className="w-full"
                    >
                        코스로 돌아가기
                    </Button>
                </div>
            </div>
        </div>
    );
};
