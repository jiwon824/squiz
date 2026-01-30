// 액션 아이템 뷰: 체크박스 + 진행률

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import type { MeetingReport } from './types';

interface ActionItemsViewProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 추가 클래스명 */
    className?: string;
}

export const ActionItemsView: React.FC<ActionItemsViewProps> = ({
    report,
    className,
}) => {
    const [completedItems, setCompletedItems] = useState<number[]>([]);

    const toggleComplete = (idx: number) => {
        setCompletedItems(prev =>
            prev.includes(idx)
                ? prev.filter(i => i !== idx)
                : [...prev, idx]
        );
    };

    const completionRate = report.actionItems.length > 0
        ? Math.round((completedItems.length / report.actionItems.length) * 100)
        : 0;

    return (
        <div className={cn('space-y-4', className)}>
            {/* 액션 아이템 리스트 */}
            <div className="rounded-xl border border-border overflow-hidden">
                <div className={cn(
                    'px-5 py-4 border-b border-border',
                    'bg-background/50 flex items-center justify-between'
                )}>
                    <h3 className="font-semibold text-text-primary mb-0">액션 아이템</h3>
                    <span className="text-sm text-text-tertiary">
                        {completedItems.length} / {report.actionItems.length} 완료
                    </span>
                </div>
                <div className="p-5 space-y-3">
                    {report.actionItems.map((item, idx) => {
                        const isCompleted = completedItems.includes(idx);
                        return (
                            <label
                                key={idx}
                                className={cn(
                                    'flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition-all',
                                    conditionalClasses.state(
                                        isCompleted,
                                        'bg-accent/5 border-accent/30',
                                        'border-border hover:border-border-lighter'
                                    )
                                )}
                            >
                                <input
                                    type="checkbox"
                                    checked={isCompleted}
                                    onChange={() => toggleComplete(idx)}
                                    className="mt-0.5 w-4 h-4 rounded border-border text-primary focus:ring-primary"
                                />
                                <span className={cn(
                                    'flex-1',
                                    conditionalClasses.state(
                                        isCompleted,
                                        'line-through text-text-tertiary',
                                        'text-text-secondary'
                                    )
                                )}>
                                    {item}
                                </span>
                            </label>
                        );
                    })}
                </div>
            </div>

            {/* 진행률 */}
            <div className="rounded-xl border border-border p-5">
                <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-text-primary">진행률</span>
                    <span className="text-sm text-text-tertiary">{completionRate}%</span>
                </div>
                <div className="w-full bg-background rounded-full h-2.5 overflow-hidden">
                    <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${completionRate}%` }}
                        transition={{ duration: 0.5 }}
                        className="h-2.5 bg-accent rounded-full"
                    />
                </div>
            </div>
        </div>
    );
};
