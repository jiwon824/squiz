// AI 요약 편집 컴포넌트

import React, { useState } from 'react';
import { Pencil, Check, X } from 'lucide-react';
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
                <div className="group relative">
                    <p className="text-text-secondary text-sm leading-relaxed pr-8">
                        {summary}
                    </p>
                    <button
                        onClick={() => {
                            setEditText(summary);
                            setIsEditing(true);
                        }}
                        className={cn(
                            'absolute top-0 right-0 p-1.5 rounded-google',
                            'text-text-tertiary hover:text-primary hover:bg-primary/10',
                            'opacity-0 group-hover:opacity-100 transition-all'
                        )}
                        aria-label="요약 수정"
                    >
                        <Pencil size={14} />
                    </button>
                </div>
            )}
        </div>
    );
};
