// AI 요약 편집 컴포넌트
// "AI 분석 결과(수정 가능)" 배지로 수정 가능성을 명시적으로 안내

import React, { useState } from 'react';
import { Pencil, Check, X, Sparkles } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface EditableSummaryProps {
    /** 현재 요약 텍스트 */
    summary: string;
    /** 저장 콜백 */
    onSave: (newSummary: string) => void;
    /** 추가 클래스명 */
    className?: string;
}

export const EditableSummary: React.FC<EditableSummaryProps> = ({
    summary,
    onSave,
    className,
}) => {
    const [isEditing, setIsEditing] = useState(false);
    const [editText, setEditText] = useState(summary);

    const handleSave = () => {
        onSave(editText);
        setIsEditing(false);
    };

    const handleCancel = () => {
        setEditText(summary);
        setIsEditing(false);
    };

    return (
        <div className={cn('space-y-3', className)}>
            {isEditing ? (
                <>
                    <textarea
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        rows={4}
                        className={cn(
                            'w-full p-3 text-sm text-text-primary leading-relaxed',
                            'bg-surface border border-border rounded-google',
                            'focus:ring-2 focus:ring-primary/30 focus:border-primary focus:outline-none',
                            'resize-none transition-colors'
                        )}
                    />
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleSave}
                            className={cn(
                                'inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-google',
                                'bg-primary text-white hover:bg-primary-dark transition-colors'
                            )}
                        >
                            <Check size={14} />
                            저장하기
                        </button>
                        <button
                            onClick={handleCancel}
                            className={cn(
                                'inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-google',
                                'text-text-secondary hover:bg-surface-hover transition-colors'
                            )}
                        >
                            <X size={14} />
                            취소
                        </button>
                    </div>
                </>
            ) : (
                <div className="group">
                    {/* AI 분석 안내 */}
                    <div className="flex items-center gap-2 mb-3">
                        <h3 className="text-base font-semibold text-[var(--color-primary)] mb-0 inline-flex items-center gap-1.5">
                            <Sparkles size={14} />
                            AI 분석 결과
                        </h3>
                        <span className="text-xs text-text-tertiary">
                            클릭하여 수정할 수 있습니다
                        </span>
                    </div>

                    {/* 요약 텍스트 + 편집 버튼 */}
                    <div
                        onClick={() => {
                            setEditText(summary);
                            setIsEditing(true);
                        }}
                        className={cn(
                            'relative p-3 rounded-google cursor-pointer min-h-[80px]',
                            'border border-transparent',
                            'hover:border-border hover:bg-surface-hover',
                            'transition-all'
                        )}
                    >
                        <p className="text-text-secondary text-sm leading-relaxed pr-8">
                            {summary}
                        </p>
                        <div className={cn(
                            'absolute top-3 right-3 p-1 rounded-google',
                            'text-text-tertiary group-hover:text-primary',
                            'transition-colors'
                        )}>
                            <Pencil size={14} />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
