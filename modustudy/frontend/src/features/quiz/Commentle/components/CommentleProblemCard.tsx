import React from 'react';
import { Problem } from '../hooks/useCommentleGame';
import { Tag, Zap, HelpCircle } from 'lucide-react';
import { cn, classBuilder } from '@/shared/utils/cn';

interface CommentleProblemCardProps {
    problem: Problem;
    attemptCount: number;
}

/* 코멘틀 퀴즈 문제 정보 카드 컴포넌트*/
export const CommentleProblemCard: React.FC<CommentleProblemCardProps> = ({ problem, attemptCount }) => {
    // 시도 횟수에 따라 힌트 공개 (5회, 10회, 20회)
    const getUnlockedHints = () => {
        if (attemptCount >= 20) return 3;
        if (attemptCount >= 10) return 2;
        if (attemptCount >= 5) return 1;
        return 0;
    };

    const unlockedHintCount = getUnlockedHints();

    return (
        <div className="space-y-5">
            {/* 상단 정보 - 가로 배치 */}
            <div className="flex flex-wrap items-center gap-4">
                {/* Category */}
                <div className="flex items-center gap-2.5 bg-slate-100 px-5 py-2.5 rounded-full">
                    <Tag size={20} className="text-primary" />
                    <span className="text-lg font-semibold text-slate-700">{problem.category}</span>
                </div>

                {/* Difficulty */}
                <div className="flex items-center gap-2.5 bg-slate-100 px-5 py-2.5 rounded-full">
                    <Zap size={20} className="text-amber-500" />
                    <span className="text-lg font-semibold text-slate-700 capitalize">{problem.difficulty}</span>
                </div>

                {/* Attempt */}
                <div className="flex items-center gap-2 bg-slate-100 px-5 py-2.5 rounded-full">
                    <span className="text-lg text-slate-500">시도</span>
                    <span className="text-xl font-bold text-primary">{attemptCount + 1}</span>
                </div>
            </div>

            {/* 힌트 섹션 */}
            <div className={cn(classBuilder.card('elevated'), 'p-6')}>
                <div className="flex items-center gap-3 mb-6">
                    <div className="bg-warning/10 p-2 rounded-xl text-amber-500">
                        <HelpCircle size={20} />
                    </div>
                    <span className="text-xl font-bold text-text-primary">힌트</span>
                    <span className="text-sm text-text-tertiary ml-auto">hover to reveal</span>
                </div>

                <div className="space-y-4">
                    {problem.hints.length > 0 ? (
                        problem.hints.map((hint, index) => {
                            const isUnlocked = index < unlockedHintCount;
                            const nextUnlockAttempt = index === 0 ? 5 : index === 1 ? 10 : 20;

                            return (
                                <div
                                    key={index}
                                    className={`flex items-start gap-4 p-4 rounded-xl transition-all ${
                                        isUnlocked
                                            ? 'bg-white border border-amber-200'
                                            : 'bg-background-secondary border border-border-light opacity-60'
                                    }`}
                                >
                                    <span className={`text-lg font-bold mt-0.5 ${
                                        isUnlocked ? 'text-amber-500' : 'text-text-tertiary'
                                    }`}>
                                        #{index + 1}
                                    </span>
                                    {isUnlocked ? (
                                        <span className="text-lg text-text-primary group cursor-help blur-sm hover:blur-none transition-all duration-300 flex-1">
                                            {hint}
                                        </span>
                                    ) : (
                                        <span className="text-base text-text-tertiary italic flex-1">
                                            🔒 {nextUnlockAttempt}회 시도 후 공개
                                        </span>
                                    )}
                                </div>
                            );
                        })
                    ) : (
                        <p className="text-lg text-text-tertiary italic">아직 공개된 힌트가 없습니다.</p>
                    )}
                </div>
            </div>
        </div>
    );
};
