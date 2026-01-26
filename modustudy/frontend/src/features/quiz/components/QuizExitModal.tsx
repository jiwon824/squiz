/**
 * =============================================================================
 * QuizExitModal.tsx - 퀴즈 종료 확인 모달 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 사용자가 퀴즈를 나가려고 할 때 확인 모달을 표시합니다.
 * "임시 저장" 또는 "나가기" 옵션을 제공합니다.
 * 
 * =============================================================================
 */

import { Save, LogOut, AlertTriangle } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';

// -----------------------------------------------------------------------------
// Props 인터페이스
// -----------------------------------------------------------------------------
interface QuizExitModalProps {
    /** 모달 표시 여부 */
    isOpen: boolean;
    /** 모달 닫기 핸들러 */
    onClose: () => void;
    /** 임시 저장 후 나가기 핸들러 */
    onSaveAndExit: () => void;
    /** 저장 없이 나가기 핸들러 */
    onAbandon: () => void;
    /** 저장 로딩 상태 */
    isSaving?: boolean;
    /** 답변한 문제 수 */
    answeredCount: number;
    /** 전체 문제 수 */
    totalCount: number;
}

// -----------------------------------------------------------------------------
// 컴포넌트
// -----------------------------------------------------------------------------
export const QuizExitModal: React.FC<QuizExitModalProps> = ({
    isOpen,
    onClose,
    onSaveAndExit,
    onAbandon,
    isSaving = false,
    answeredCount,
    totalCount,
}) => {
    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title="퀴즈를 종료하시겠습니까?"
            maxWidth="md"
        >
            <div className="space-y-6">
                {/* 경고 아이콘 및 메시지 */}
                <div
                    className="flex items-start gap-4 p-4 rounded-xl"
                    style={{ backgroundColor: 'var(--color-warning-light)' }}
                >
                    <AlertTriangle
                        size={24}
                        style={{ color: 'var(--color-warning)', flexShrink: 0, marginTop: 2 }}
                    />
                    <div>
                        <p
                            className="font-medium mb-1"
                            style={{ color: 'var(--color-text-primary)' }}
                        >
                            진행 중인 퀴즈가 있습니다
                        </p>
                        <p
                            className="text-sm"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            현재 {totalCount}개 중 {answeredCount}개 문제에 답변하셨습니다.
                        </p>
                    </div>
                </div>

                {/* 옵션 설명 */}
                <div className="space-y-3">
                    <div
                        className="p-4 rounded-lg border"
                        style={{ borderColor: 'var(--color-border)' }}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <Save size={18} style={{ color: 'var(--color-primary)' }} />
                            <span
                                className="font-medium"
                                style={{ color: 'var(--color-text-primary)' }}
                            >
                                임시 저장
                            </span>
                        </div>
                        <p
                            className="text-sm"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            현재까지의 진행 상황을 저장하고 나중에 이어서 풀 수 있습니다.
                        </p>
                    </div>

                    <div
                        className="p-4 rounded-lg border"
                        style={{ borderColor: 'var(--color-border)' }}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <LogOut size={18} style={{ color: 'var(--color-error)' }} />
                            <span
                                className="font-medium"
                                style={{ color: 'var(--color-text-primary)' }}
                            >
                                저장하지 않고 나가기
                            </span>
                        </div>
                        <p
                            className="text-sm"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            모든 진행 상황이 삭제되며, 다음에 처음부터 다시 시작해야 합니다.
                        </p>
                    </div>
                </div>

                {/* 버튼 그룹 */}
                <div className="flex flex-col sm:flex-row gap-3 pt-2">
                    <Button
                        variant="google-outline"
                        size="lg"
                        onClick={onClose}
                        className="flex-1"
                        disabled={isSaving}
                    >
                        계속 풀기
                    </Button>
                    <Button
                        variant="google-primary"
                        size="lg"
                        onClick={onSaveAndExit}
                        isLoading={isSaving}
                        leftIcon={<Save size={18} />}
                        className="flex-1"
                    >
                        임시 저장
                    </Button>
                    <Button
                        variant="danger"
                        size="lg"
                        onClick={onAbandon}
                        leftIcon={<LogOut size={18} />}
                        className="flex-1"
                        disabled={isSaving}
                    >
                        나가기
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
