// 오늘의 목표 수정 모달

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Plus, Trash2, GripVertical, Target } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useGoalsStore, Goal } from '../store/goalsStore';

interface GoalsEditModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const GoalsEditModal: React.FC<GoalsEditModalProps> = ({
    isOpen,
    onClose,
}) => {
    const { goals, addGoal, updateGoal, removeGoal } = useGoalsStore();
    const [newGoalText, setNewGoalText] = useState('');
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editingText, setEditingText] = useState('');
    const inputRef = useRef<HTMLInputElement>(null);

    // 모달 열릴 때 입력창 포커스
    useEffect(() => {
        if (isOpen && inputRef.current) {
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    }, [isOpen]);

    // ESC 키로 모달 닫기
    useEffect(() => {
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        if (isOpen) {
            document.addEventListener('keydown', handleEsc);
            return () => document.removeEventListener('keydown', handleEsc);
        }
    }, [isOpen, onClose]);

    const handleAddGoal = () => {
        if (newGoalText.trim() && goals.length < 5) {
            addGoal(newGoalText.trim());
            setNewGoalText('');
            inputRef.current?.focus();
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
            e.preventDefault();
            handleAddGoal();
        }
    };

    const handleStartEdit = (goal: Goal) => {
        setEditingId(goal.id);
        setEditingText(goal.text);
    };

    const handleSaveEdit = () => {
        if (editingId && editingText.trim()) {
            updateGoal(editingId, editingText.trim());
        }
        setEditingId(null);
        setEditingText('');
    };

    const handleEditKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
            e.preventDefault();
            handleSaveEdit();
        } else if (e.key === 'Escape') {
            setEditingId(null);
            setEditingText('');
        }
    };

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* 배경 오버레이 */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50"
                    />

                    {/* 모달 */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 20 }}
                        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                        className="fixed inset-0 flex items-center justify-center z-50 p-4"
                    >
                        <div
                            className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden"
                            onClick={(e) => e.stopPropagation()}
                        >
                            {/* 헤더 */}
                            <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                                        <Target className="text-primary" size={20} />
                                    </div>
                                    <div>
                                        <h2 className="text-lg font-bold text-text-primary">
                                            오늘의 목표
                                        </h2>
                                        <p className="text-xs text-text-tertiary">
                                            최대 5개까지 설정 가능
                                        </p>
                                    </div>
                                </div>
                                <button
                                    onClick={onClose}
                                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                                >
                                    <X size={20} className="text-text-tertiary" />
                                </button>
                            </div>

                            {/* 목표 입력 */}
                            <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/50">
                                <div className="flex gap-2">
                                    <input
                                        ref={inputRef}
                                        type="text"
                                        value={newGoalText}
                                        onChange={(e) => setNewGoalText(e.target.value)}
                                        onKeyDown={handleKeyDown}
                                        placeholder="새 목표를 입력하세요"
                                        disabled={goals.length >= 5}
                                        className={cn(
                                            'flex-1 px-4 py-2.5 rounded-xl border border-gray-200',
                                            'text-sm placeholder:text-text-tertiary',
                                            'focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary',
                                            'disabled:bg-gray-100 disabled:cursor-not-allowed'
                                        )}
                                    />
                                    <button
                                        onClick={handleAddGoal}
                                        disabled={!newGoalText.trim() || goals.length >= 5}
                                        className={cn(
                                            'px-4 py-2.5 rounded-xl font-medium text-sm',
                                            'bg-primary text-white',
                                            'hover:bg-primary-dark transition-colors',
                                            'disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed'
                                        )}
                                    >
                                        <Plus size={18} />
                                    </button>
                                </div>
                                {goals.length >= 5 && (
                                    <p className="text-xs text-warning mt-2">
                                        최대 5개까지만 설정할 수 있습니다
                                    </p>
                                )}
                            </div>

                            {/* 목표 리스트 */}
                            <div className="px-6 py-4 max-h-80 overflow-y-auto">
                                {goals.length === 0 ? (
                                    <div className="text-center py-8">
                                        <Target className="mx-auto text-gray-200 mb-3" size={40} />
                                        <p className="text-text-tertiary text-sm">
                                            아직 설정된 목표가 없습니다
                                        </p>
                                    </div>
                                ) : (
                                    <ul className="space-y-2">
                                        {goals.map((goal) => (
                                            <motion.li
                                                key={goal.id}
                                                layout
                                                initial={{ opacity: 0, x: -10 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                exit={{ opacity: 0, x: 10 }}
                                                className={cn(
                                                    'flex items-center gap-2 p-3 rounded-xl',
                                                    'bg-gray-50 hover:bg-gray-100 transition-colors group'
                                                )}
                                            >
                                                {/* 드래그 핸들 (추후 드래그 기능 추가 가능) */}
                                                <GripVertical
                                                    size={16}
                                                    className="text-gray-300 cursor-grab"
                                                />

                                                {/* 목표 텍스트 / 수정 입력 */}
                                                {editingId === goal.id ? (
                                                    <input
                                                        type="text"
                                                        value={editingText}
                                                        onChange={(e) => setEditingText(e.target.value)}
                                                        onKeyDown={handleEditKeyDown}
                                                        onBlur={handleSaveEdit}
                                                        autoFocus
                                                        className={cn(
                                                            'flex-1 px-2 py-1 rounded-lg border border-primary',
                                                            'text-sm focus:outline-none focus:ring-2 focus:ring-primary/20'
                                                        )}
                                                    />
                                                ) : (
                                                    <span
                                                        onClick={() => handleStartEdit(goal)}
                                                        className={cn(
                                                            'flex-1 text-sm cursor-pointer',
                                                            goal.completed
                                                                ? 'text-text-tertiary line-through'
                                                                : 'text-text-primary'
                                                        )}
                                                    >
                                                        {goal.text}
                                                    </span>
                                                )}

                                                {/* 삭제 버튼 */}
                                                <button
                                                    onClick={() => removeGoal(goal.id)}
                                                    className={cn(
                                                        'p-1.5 rounded-lg transition-colors',
                                                        'text-gray-300 hover:text-error hover:bg-error/10',
                                                        'opacity-0 group-hover:opacity-100'
                                                    )}
                                                >
                                                    <Trash2 size={14} />
                                                </button>
                                            </motion.li>
                                        ))}
                                    </ul>
                                )}
                            </div>

                            {/* 푸터 */}
                            <div className="px-6 py-4 border-t border-gray-100 bg-gray-50/50">
                                <button
                                    onClick={onClose}
                                    className={cn(
                                        'w-full py-2.5 rounded-xl font-medium text-sm',
                                        'bg-primary text-white',
                                        'hover:bg-primary-dark transition-colors'
                                    )}
                                >
                                    완료
                                </button>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
};

export default GoalsEditModal;
