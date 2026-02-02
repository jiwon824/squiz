import React, { useState } from 'react';
import { X, AlertTriangle, ShieldCheck } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';

interface RecruitmentReportModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (reason: string) => void;
    targetTitle?: string;
}

export const RecruitmentReportModal: React.FC<RecruitmentReportModalProps> = ({ isOpen, onClose, onSubmit, targetTitle }) => {
    const [reason, setReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const reasons = [
        '부적절한 홍보 게시글',
        '욕설 및 비하 발언',
        '음란물 또는 불법 정보',
        '도배 및 스팸',
        '기타 사유'
    ];

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!reason) return;

        setIsSubmitting(true);
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 800));
        onSubmit(reason);
        setIsSubmitting(false);
        setReason('');
        onClose();
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fadeIn" onClick={onClose} />

            {/* Modal */}
            <div className="relative bg-white w-full max-w-md rounded-[32px] p-8 shadow-2xl animate-scaleIn overflow-hidden border border-border-light">
                {/* Header Decoration */}
                <div className="absolute top-0 left-0 w-full h-2 bg-error" />

                <div className="flex justify-between items-center mb-6">
                    <div className="flex items-center gap-3">
                        <div className="flex-shrink-0 p-2 bg-error/10 text-error rounded-xl">
                            <AlertTriangle size={20} />
                        </div>
                        <h3 className="text-xl font-bold text-text-primary tracking-tight leading-tight">게시글 신고</h3>
                    </div>
                    <Button
                        variant="google-ghost"
                        size="sm"
                        isCircle
                        onClick={onClose}
                        className="text-text-secondary hover:bg-background-secondary"
                    >
                        <X size={20} />
                    </Button>
                </div>

                <div className="mb-6">
                    <p className="text-sm text-text-secondary leading-relaxed mb-1">신고 대상:</p>
                    <p className="font-bold text-text-primary line-clamp-1 italic text-sm border-l-4 border-error/30 pl-3">
                        "{targetTitle}"
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="space-y-3">
                        <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">신고 사유 선택</label>
                        <div className="grid grid-cols-1 gap-2">
                            {reasons.map((r) => (
                                <button
                                    key={r}
                                    type="button"
                                    onClick={() => setReason(r)}
                                    className={cn(
                                        "flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-medium border transition-all text-left",
                                        reason === r
                                            ? "bg-error/5 border-error text-error shadow-sm"
                                            : "bg-white border-border-light text-text-secondary hover:border-error/30"
                                    )}
                                >
                                    <div className={cn(
                                        "w-4 h-4 rounded-full border-2 flex items-center justify-center transition-all",
                                        reason === r ? "border-error bg-error" : "border-border-light"
                                    )}>
                                        {reason === r && <div className="w-1.5 h-1.5 bg-white rounded-full" />}
                                    </div>
                                    {r}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="bg-background-secondary/50 p-4 rounded-2xl border border-border-light/50">
                        <div className="flex gap-3 text-[11px] text-text-tertiary leading-relaxed">
                            <ShieldCheck size={16} className="shrink-0" />
                            <p>허위 신고일 경우 서비스 이용이 제한될 수 있습니다. 운영진이 신속하게 내용을 검토한 후 조치하겠습니다.</p>
                        </div>
                    </div>

                    <div className="flex gap-3 pt-2">
                        <Button
                            type="button"
                            onClick={onClose}
                            variant="google-outline"
                            size="lg"
                            fullWidth
                            className="py-6"
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            disabled={!reason || isSubmitting}
                            isLoading={isSubmitting}
                            variant="danger"
                            size="lg"
                            fullWidth
                            className="py-6"
                        >
                            신고 확정
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
};
