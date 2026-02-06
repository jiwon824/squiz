import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button, Modal } from '@/shared/components';

interface StudyReportModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (reason: string) => Promise<void> | void;
    targetTitle?: string;
}

export const StudyReportModal: React.FC<StudyReportModalProps> = ({ isOpen, onClose, onSubmit, targetTitle }) => {
    const [reason, setReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const reasons = [
        '부적절한 스터디 홍보',
        '허위 정보 포함',
        '욕설 및 비하 발언',
        '음란물 또는 불법 정보',
        '도배 및 스팸',
        '기타 사유'
    ];

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!reason) return;

        setIsSubmitting(true);
        try {
            await onSubmit(reason);
            setReason('');
            onClose();
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="스터디 신고" maxWidth="md">
            <div className="space-y-8">
                <div>
                    <p className="text-xs font-black text-text-tertiary uppercase tracking-widest mb-2">신고 대상</p>
                    <p className="font-bold text-text-primary border-l-4 border-error/40 pl-4 py-1 bg-error/5 rounded-r-xl">
                        {targetTitle}
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-8">
                    <div className="space-y-4">
                        <label className="text-xs font-black text-text-tertiary uppercase tracking-widest block">신고 사유 선택 (필수)</label>
                        <div className="grid grid-cols-1 gap-2.5">
                            {reasons.map((r) => (
                                <button
                                    key={r}
                                    type="button"
                                    onClick={() => setReason(r)}
                                    className={cn(
                                        "flex items-center gap-4 px-5 py-4 rounded-[20px] text-sm font-bold border transition-all text-left",
                                        reason === r
                                            ? "bg-error/5 border-error text-error shadow-sm"
                                            : "bg-white border-border-light text-text-secondary hover:border-error/20"
                                    )}
                                >
                                    <div className={cn(
                                        "w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all shrink-0",
                                        reason === r ? "border-error bg-error" : "border-border-light"
                                    )}>
                                        {reason === r && <div className="w-2 h-2 bg-white rounded-full" />}
                                    </div>
                                    {r}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="p-5 rounded-2xl bg-background-secondary/50 border border-border-light/40 flex gap-4">
                        <ShieldCheck size={20} className="text-text-tertiary shrink-0 mt-0.5" />
                        <p className="text-xs font-medium text-text-tertiary leading-relaxed">
                            허위 신고일 경우 서비스 이용이 제한될 수 있습니다. 운영진이 신속하게 내용을 검토한 후 조치하겠습니다.
                        </p>
                    </div>

                    <div className="flex gap-3 pt-2">
                        <Button
                            type="button"
                            onClick={onClose}
                            variant="google-outline"
                            fullWidth
                            className="h-14 font-black"
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            disabled={!reason || isSubmitting}
                            isLoading={isSubmitting}
                            variant="danger"
                            fullWidth
                            className="h-14 font-black shadow-lg shadow-error/20"
                        >
                            신고하기
                        </Button>
                    </div>
                </form>
            </div>
        </Modal>
    );
};
