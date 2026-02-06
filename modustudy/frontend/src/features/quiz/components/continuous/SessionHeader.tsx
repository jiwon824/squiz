/**
 * =============================================================================
 * SessionHeader.tsx - 연속 학습 세션 헤더
 * =============================================================================
 *
 * 연속 학습 세션의 상단 헤더 컴포넌트입니다.
 *
 * 주요 기능:
 * - Breadcrumb 네비게이션 (퀴즈 > 연속 학습)
 * - 나가기 버튼
 * - "연속 학습" 모드 배지
 * - 실시간 진행 상황 카운터 (진행 문제 수, 정답률)
 *
 * =============================================================================
 */

import { ArrowLeft } from 'lucide-react';
import { Breadcrumb } from '@/shared/components/layouts/Breadcrumb';
import { Button } from '@/shared/components/Button';

// =============================================================================
// 타입 정의
// =============================================================================

export interface SessionHeaderProps {
    /** 푼 문제 수 */
    solvedCount: number;
    /** 정답 수 */
    correctCount: number;
    /** 문제 제한 (없으면 무제한 모드) */
    questionLimit?: number;
    /** 뒤로가기/나가기 핸들러 */
    onBackClick: () => void;
}

// =============================================================================
// 컴포넌트
// =============================================================================

export const SessionHeader: React.FC<SessionHeaderProps> = ({
    solvedCount,
    correctCount,
    questionLimit,
    onBackClick,
}) => {
    // ─────────────────────────────────────────────────────────────────────────
    // 실시간 정답률 계산 (0으로 나누기 방지)
    // ─────────────────────────────────────────────────────────────────────────
    const currentAccuracy = solvedCount > 0
        ? Math.round((correctCount / solvedCount) * 100)
        : 0;

    return (
        <header
            className="sticky top-0 z-10 backdrop-blur-md"
            style={{
                backgroundColor: 'rgba(255, 255, 255, 0.9)',
                borderBottom: '1px solid var(--color-border)',
            }}
        >
            <div className="max-w-3xl mx-auto px-4 py-4">
                {/* Breadcrumb 네비게이션 */}
                <Breadcrumb
                    items={[
                        { label: '퀴즈', path: '/quiz' },
                        { label: '연속 학습' },
                    ]}
                    className="mb-2"
                />

                {/* 나가기 버튼 + 모드 배지 */}
                <div className="flex items-center justify-between mb-3">
                    <Button
                        variant="google-ghost"
                        size="sm"
                        onClick={onBackClick}
                        leftIcon={<ArrowLeft size={18} />}
                    >
                        나가기
                    </Button>

                    {/* 연속 학습 모드 배지 */}
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

                {/* ─────────────────────────────────────────────────────────────
                    진행 상황 카운터
                    - 진행: N / M 문제 (제한 있을 때) 또는 N 문제 (무제한)
                    - 정답률: X% (실시간 계산)
                ───────────────────────────────────────────────────────────── */}
                <div
                    className="flex items-center justify-between text-sm"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    {/* 진행 문제 수 */}
                    <span>
                        진행: <strong style={{ color: 'var(--color-primary)' }}>{solvedCount}</strong>
                        {questionLimit ? ` / ${questionLimit}` : ' 문제'}
                    </span>

                    {/* 실시간 정답률 */}
                    <span>
                        정답률: <strong style={{ color: 'var(--color-success)' }}>
                            {currentAccuracy}%
                        </strong>
                    </span>
                </div>
            </div>
        </header>
    );
};
