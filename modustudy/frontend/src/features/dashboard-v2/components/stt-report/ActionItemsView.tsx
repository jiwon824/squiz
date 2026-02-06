// 액션 아이템 뷰: 체크박스 + 진행률 + 스터디장 수정 기능

import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Pencil, Trash2, Plus, Check, X } from 'lucide-react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import type { MeetingReport } from './types';

interface ActionItemsViewProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 스터디장 여부 */
    isLeader?: boolean;
    /** 추가 클래스명 */
    className?: string;
}

export const ActionItemsView: React.FC<ActionItemsViewProps> = ({
    report,
    isLeader = false,
    className,
}) => {
    const [completedItems, setCompletedItems] = useState<number[]>([]);
    // 스터디장 전용: 로컬 액션 아이템 상태
    const [localItems, setLocalItems] = useState<string[]>(report.actionItems);
    // 수정 중인 아이템 인덱스
    const [editingIdx, setEditingIdx] = useState<number | null>(null);
    // 수정 중인 텍스트
    const [editText, setEditText] = useState('');
    // 새 아이템 추가 모드
    const [isAdding, setIsAdding] = useState(false);
    const [newItemText, setNewItemText] = useState('');

    // report가 변경되면 로컬 상태 동기화
    useEffect(() => {
        setLocalItems(report.actionItems);
        setCompletedItems([]);
    }, [report.id, report.actionItems]);

    const toggleComplete = (idx: number) => {
        setCompletedItems(prev =>
            prev.includes(idx)
                ? prev.filter(i => i !== idx)
                : [...prev, idx]
        );
    };

    // 수정 시작
    const startEdit = (idx: number) => {
        setEditingIdx(idx);
        setEditText(localItems[idx]);
    };

    // 수정 저장
    const saveEdit = () => {
        if (editingIdx === null || !editText.trim()) return;
        const newItems = [...localItems];
        newItems[editingIdx] = editText.trim();
        setLocalItems(newItems);
        setEditingIdx(null);
        setEditText('');
    };

    // 수정 취소
    const cancelEdit = () => {
        setEditingIdx(null);
        setEditText('');
    };

    // 아이템 삭제
    const deleteItem = (idx: number) => {
        const newItems = localItems.filter((_, i) => i !== idx);
        setLocalItems(newItems);
        // 완료 상태도 업데이트
        setCompletedItems(prev => prev.filter(i => i !== idx).map(i => i > idx ? i - 1 : i));
    };

    // 새 아이템 추가
    const addNewItem = () => {
        if (!newItemText.trim()) return;
        setLocalItems([...localItems, newItemText.trim()]);
        setNewItemText('');
        setIsAdding(false);
    };

    const completionRate = localItems.length > 0
        ? Math.round((completedItems.length / localItems.length) * 100)
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
                    <div className="flex items-center gap-3">
                        <span className="text-sm text-text-tertiary">
                            {completedItems.length} / {localItems.length} 완료
                        </span>
                        {/* 스터디장: 추가 버튼 */}
                        {isLeader && !isAdding && (
                            <button
                                onClick={() => setIsAdding(true)}
                                className={cn(
                                    'flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium rounded-lg',
                                    'bg-primary/10 text-primary hover:bg-primary/20 transition-colors'
                                )}
                            >
                                <Plus size={14} />
                                추가
                            </button>
                        )}
                    </div>
                </div>
                <div className="p-5 space-y-3">
                    {/* 새 아이템 추가 입력 */}
                    {isLeader && isAdding && (
                        <div className="flex items-center gap-2 p-4 rounded-xl border border-primary/30 bg-primary/5">
                            <input
                                type="text"
                                value={newItemText}
                                onChange={(e) => setNewItemText(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') addNewItem();
                                    if (e.key === 'Escape') { setIsAdding(false); setNewItemText(''); }
                                }}
                                placeholder="새 액션 아이템 입력..."
                                className={cn(
                                    'flex-1 px-3 py-2 text-sm rounded-lg',
                                    'border border-border focus:border-primary focus:outline-none'
                                )}
                                autoFocus
                            />
                            <button
                                onClick={addNewItem}
                                disabled={!newItemText.trim()}
                                className={cn(
                                    'p-2 rounded-lg transition-colors',
                                    'bg-primary text-white hover:bg-primary/90',
                                    'disabled:opacity-50 disabled:cursor-not-allowed'
                                )}
                            >
                                <Check size={16} />
                            </button>
                            <button
                                onClick={() => { setIsAdding(false); setNewItemText(''); }}
                                className="p-2 rounded-lg text-text-tertiary hover:bg-surface-hover transition-colors"
                            >
                                <X size={16} />
                            </button>
                        </div>
                    )}

                    {localItems.length === 0 ? (
                        <div className="text-center py-8 text-text-tertiary text-sm">
                            등록된 액션 아이템이 없습니다
                        </div>
                    ) : (
                        localItems.map((item, idx) => {
                            const isCompleted = completedItems.includes(idx);
                            const isEditing = editingIdx === idx;

                            return (
                                <div
                                    key={idx}
                                    className={cn(
                                        'group flex items-start gap-3 p-4 rounded-xl border transition-all',
                                        isEditing
                                            ? 'border-primary/50 bg-primary/5'
                                            : conditionalClasses.state(
                                                isCompleted,
                                                'bg-accent/5 border-accent/30',
                                                'border-border hover:border-border-lighter'
                                            )
                                    )}
                                >
                                    {/* 체크박스 */}
                                    <input
                                        type="checkbox"
                                        checked={isCompleted}
                                        onChange={() => toggleComplete(idx)}
                                        disabled={isEditing}
                                        className="mt-0.5 w-4 h-4 rounded border-border text-primary focus:ring-primary cursor-pointer"
                                    />

                                    {/* 수정 모드 */}
                                    {isEditing ? (
                                        <div className="flex-1 flex items-center gap-2">
                                            <input
                                                type="text"
                                                value={editText}
                                                onChange={(e) => setEditText(e.target.value)}
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') saveEdit();
                                                    if (e.key === 'Escape') cancelEdit();
                                                }}
                                                className={cn(
                                                    'flex-1 px-3 py-1.5 text-sm rounded-lg',
                                                    'border border-border focus:border-primary focus:outline-none'
                                                )}
                                                autoFocus
                                            />
                                            <button
                                                onClick={saveEdit}
                                                disabled={!editText.trim()}
                                                className={cn(
                                                    'p-1.5 rounded-lg transition-colors',
                                                    'bg-primary text-white hover:bg-primary/90',
                                                    'disabled:opacity-50'
                                                )}
                                            >
                                                <Check size={14} />
                                            </button>
                                            <button
                                                onClick={cancelEdit}
                                                className="p-1.5 rounded-lg text-text-tertiary hover:bg-surface-hover transition-colors"
                                            >
                                                <X size={14} />
                                            </button>
                                        </div>
                                    ) : (
                                        <>
                                            {/* 텍스트 */}
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

                                            {/* 스터디장: 수정/삭제 버튼 */}
                                            {isLeader && (
                                                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                    <button
                                                        onClick={() => startEdit(idx)}
                                                        className="p-1.5 rounded-lg text-text-tertiary hover:text-primary hover:bg-primary/10 transition-colors"
                                                        title="수정"
                                                    >
                                                        <Pencil size={14} />
                                                    </button>
                                                    <button
                                                        onClick={() => deleteItem(idx)}
                                                        className="p-1.5 rounded-lg text-text-tertiary hover:text-red-500 hover:bg-red-50 transition-colors"
                                                        title="삭제"
                                                    >
                                                        <Trash2 size={14} />
                                                    </button>
                                                </div>
                                            )}
                                        </>
                                    )}
                                </div>
                            );
                        })
                    )}
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
