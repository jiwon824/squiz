// 오늘의 목표 카드 컴포넌트
// Zustand store를 통해 캘린더 등과 양방향 동기화

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Target, Plus, CheckCircle2, Circle, Edit3 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { PostIt, getPostItTheme } from '@/shared/components/layouts/PostIt';
import { useGoalsStore } from '../store/goalsStore';
import { GoalsEditModal } from './GoalsEditModal';

const theme = getPostItTheme('yellow');

export const TodayGoalsCard: React.FC = () => {
    const { goals, toggleGoal } = useGoalsStore();
    const [isModalOpen, setIsModalOpen] = useState(false);

    const completedCount = goals.filter(g => g.completed).length;
    const totalCount = goals.length;
    const progressPercent = totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

    return (
        <>
            <PostIt color="yellow" rotate={1.2} tapeRotate={-2}>
                {/* 헤더 */}
                <div className="flex items-center justify-between mb-4 mt-1">
                    <div className="flex items-center gap-2.5">
                        <Target size={20} style={{ color: theme.text }} />
                        <h3 className="font-bold text-lg leading-6 mb-0" style={{ color: theme.text }}>
                            오늘의 목표
                        </h3>
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-xs" style={{ color: theme.sub }}>
                            {completedCount}/{totalCount}
                        </span>
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="p-1.5 rounded-lg transition-colors hover:opacity-70"
                            style={{ color: theme.sub }}
                            title="목표 수정"
                        >
                            <Edit3 size={14} />
                        </button>
                    </div>
                </div>

                {/* 진행률 바 */}
                {totalCount > 0 && (
                    <div className="mb-4">
                        <div
                            className="w-full rounded-full h-1.5 overflow-hidden"
                            style={{ background: 'rgba(200,180,50,0.2)' }}
                        >
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${progressPercent}%` }}
                                transition={{ duration: 0.5, ease: 'easeOut' }}
                                className={cn(
                                    'h-1.5 rounded-full',
                                    progressPercent === 100 ? 'bg-accent' : 'bg-primary'
                                )}
                            />
                        </div>
                    </div>
                )}

                {/* 목표 리스트 */}
                {goals.length === 0 ? (
                    <div className="text-center py-4">
                        <p className="text-sm mb-3" style={{ color: theme.sub }}>
                            오늘의 목표를 설정해보세요
                        </p>
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className={cn(
                                'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg',
                                'bg-primary/10 text-primary font-medium text-sm',
                                'hover:bg-primary/20 transition-colors'
                            )}
                        >
                            <Plus size={14} />
                            추가
                        </button>
                    </div>
                ) : (
                    <ul className="space-y-3">
                        <AnimatePresence>
                            {goals.map((goal) => (
                                <motion.li
                                    key={goal.id}
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    exit={{ opacity: 0 }}
                                >
                                    <button
                                        onClick={() => toggleGoal(goal.id)}
                                        className="w-full flex items-start gap-2 text-sm text-left group"
                                        style={{ color: theme.sub }}
                                    >
                                        {/* 체크 아이콘 */}
                                        <span className="flex-shrink-0 mt-0.5">
                                            {goal.completed ? (
                                                <CheckCircle2 className="text-accent" size={16} />
                                            ) : (
                                                <Circle
                                                    size={16}
                                                    style={{ color: 'rgba(93,78,0,0.3)' }}
                                                    className="group-hover:text-primary/50 transition-colors"
                                                />
                                            )}
                                        </span>
                                        {/* 목표 텍스트 */}
                                        <span className={cn(
                                            'flex-1 transition-colors',
                                            goal.completed && 'line-through opacity-50'
                                        )}>
                                            {goal.text}
                                        </span>
                                    </button>
                                </motion.li>
                            ))}
                        </AnimatePresence>

                        {/* 목표 추가 버튼 */}
                        {goals.length < 5 && (
                            <li>
                                <button
                                    onClick={() => setIsModalOpen(true)}
                                    className="flex items-center gap-1.5 text-sm transition-colors hover:opacity-70"
                                    style={{ color: theme.sub }}
                                >
                                    <Plus size={14} />
                                    <span>목표 추가</span>
                                </button>
                            </li>
                        )}
                    </ul>
                )}
            </PostIt>

            {/* 목표 수정 모달 */}
            <GoalsEditModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
            />
        </>
    );
};

export default TodayGoalsCard;
