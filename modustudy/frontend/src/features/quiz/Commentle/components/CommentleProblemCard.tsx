import React from 'react';
import { Problem } from '../hooks/useCommentleGame';
import { Tag, Zap, HelpCircle } from 'lucide-react';

interface CommentleProblemCardProps {
    problem: Problem;
    attemptCount: number;
}

/* 코멘틀 퀴즈 문제 정보 카드 컴포넌트*/
export const CommentleProblemCard: React.FC<CommentleProblemCardProps> = ({ problem, attemptCount }) => {
    return (
        <div className="bg-gradient-to-br from-surface to-white border border-border-light rounded-3xl p-8 shadow-sm mb-8 relative overflow-hidden">
            {/* 배경 장식 요소 */}
            <div className="absolute -top-12 -right-12 w-48 h-48 bg-primary/5 rounded-full blur-3xl pointer-events-none" />

            {/* 헤더 영역: 카테고리, 난이도, 시도 횟수 */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-8">
                <div className="flex flex-wrap gap-4">
                    {/* Category Chip */}
                    <div className="flex items-center gap-2.5 bg-primary/5 px-4 py-2 rounded-2xl border border-primary/10">
                        <Tag size={16} className="text-primary" />
                        <span className="text-xs font-bold text-text-tertiary uppercase tracking-wider">Category</span>
                        <span className="text-base font-bold text-primary">{problem.category}</span>
                    </div>

                    {/* Difficulty Chip */}
                    <div className="flex items-center gap-2.5 bg-accent/10 px-4 py-2 rounded-2xl border border-accent/20">
                        <Zap size={16} className="text-accent-dark" />
                        <span className="text-xs font-bold text-text-tertiary uppercase tracking-wider">Difficulty</span>
                        <span className="text-base font-bold text-accent-dark">{problem.difficulty}</span>
                    </div>
                </div>

                {/* Attempt Status */}
                <div className="flex items-center gap-3 bg-white/80 backdrop-blur-sm px-5 py-2.5 rounded-2xl border border-border-light shadow-sm">
                    <span className="text-sm font-medium text-text-secondary">현재 시도</span>
                    <span className="text-xl font-black text-error">{attemptCount + 1}</span>
                    <span className="text-sm font-medium text-text-secondary">번째</span>
                </div>
            </div>

            {/* Hints Section */}
            <div className="bg-white/60 rounded-2xl border border-white/40 p-6">
                <div className="flex items-center gap-3 mb-4">
                    <div className="bg-warning/20 p-2 rounded-lg text-warning-dark">
                        <HelpCircle size={20} />
                    </div>
                    <h3 className="font-bold text-text-primary">오늘의 힌트</h3>
                    <span className="ml-auto text-xs text-text-tertiary">마우스를 올리면 힌트가 보입니다</span>
                </div>

                <div className="flex flex-wrap gap-3">
                    {problem.hints.length > 0 ? (
                        problem.hints.map((hint, index) => (
                            <div
                                key={index}
                                className="hint-chip-fancy group cursor-help"
                                title="Hover to reveal"
                            >
                                <span className="text-xs font-bold text-warning-dark mr-2 opacity-50">#{index + 1}</span>
                                <span className="hint-text blur-md group-hover:blur-none transition-all duration-300 font-medium">
                                    {hint}
                                </span>
                            </div>
                        ))
                    ) : (
                        <div className="text-sm text-text-tertiary italic p-2 italic">아직 공개된 힌트가 없습니다.</div>
                    )}
                </div>
            </div>

        </div>
    );
};
