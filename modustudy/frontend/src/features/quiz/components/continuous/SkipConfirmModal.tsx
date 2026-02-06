/**
 * =============================================================================
 * SkipConfirmModal.tsx - 건너뛰기 확인 모달
 * =============================================================================
 *
 * 문제 건너뛰기 전 사용자에게 확인을 요청하는 모달 컴포넌트입니다.
 *
 * 주요 기능:
 * - 오답 처리 경고 표시 (건너뛰면 오답으로 처리됨)
 * - 타이머 일시정지 안내 표시
 * - 취소/확인 액션 제공
 *
 * FSRS 관련:
 * - 이 모달이 열릴 때 부모에서 pauseTimer()가 호출됨
 * - 취소 시 부모에서 resumeTimer()가 호출됨
 * - 확인 시 부모에서 stopTimer()로 경과 시간 측정 후 오답 제출
 *
 * =============================================================================
 */

import { AlertCircle } from 'lucide-react';
import { Button } from '@/shared/components/Button';

// =============================================================================
// 타입 정의
// =============================================================================

export interface SkipConfirmModalProps {
    /** 모달 표시 여부 */
    isOpen: boolean;
    /** 취소 핸들러 (타이머 재개) */
    onCancel: () => void;
    /** 확인 핸들러 (오답 처리 제출) */
    onConfirm: () => void;
}

// =============================================================================
// 컴포넌트
// =============================================================================

export const SkipConfirmModal: React.FC<SkipConfirmModalProps> = ({
    isOpen,
    onCancel,
    onConfirm,
}) => {
    // 모달이 닫혀있으면 렌더링하지 않음
    if (!isOpen) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-in fade-in duration-200"
            style={{
                backgroundColor: 'rgba(0, 0, 0, 0.5)',
                backdropFilter: 'blur(16px)',
                WebkitBackdropFilter: 'blur(16px)',
            }}
        >
            <div
                className="w-full max-w-sm rounded-2xl p-6"
                style={{
                    backgroundColor: 'var(--color-surface)',
                    boxShadow: 'var(--shadow-xl)',
                }}
            >
                {/* ─────────────────────────────────────────────────────────────
                    경고 아이콘 및 메시지
                ───────────────────────────────────────────────────────────── */}
                <div className="text-center mb-6">
                    {/* 경고 아이콘 */}
                    <div
                        className="w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4"
                        style={{ backgroundColor: 'var(--color-warning-light)' }}
                    >
                        <AlertCircle size={24} style={{ color: 'var(--color-warning-dark)' }} />
                    </div>

                    {/* 제목 */}
                    <h3 className="text-lg font-bold mb-2 text-text-primary">
                        문제를 건너뛰시겠습니까?
                    </h3>

                    {/* 경고 문구: 오답 처리 + 타이머 일시정지 안내 */}
                    <p className="text-sm text-text-secondary leading-relaxed">
                        건너뛰기를 할 시 <strong className="text-error">오답 처리</strong>됩니다.<br />
                        <span className="text-xs text-text-tertiary">(타이머가 일시 정지되었습니다)</span>
                    </p>
                </div>

                {/* ─────────────────────────────────────────────────────────────
                    액션 버튼
                ───────────────────────────────────────────────────────────── */}
                <div className="flex gap-3">
                    {/* 취소 버튼: 타이머 재개 */}
                    <Button
                        variant="google-ghost"
                        size="md"
                        onClick={onCancel}
                        className="flex-1"
                    >
                        취소
                    </Button>

                    {/* 확인 버튼: 오답 처리 제출 */}
                    <Button
                        variant="google-primary"
                        size="md"
                        onClick={onConfirm}
                        className="flex-1 bg-error hover:bg-error-dark border-transparent text-white"
                    >
                        확인
                    </Button>
                </div>
            </div>
        </div>
    );
};
