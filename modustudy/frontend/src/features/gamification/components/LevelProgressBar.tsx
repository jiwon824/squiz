/**
 * 레벨 진행바 컴포넌트
 * 헤더나 프로필에서 현재 레벨과 경험치 진행도를 표시
 */

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Zap, ChevronDown, ChevronUp, Award, TrendingUp } from 'lucide-react';
import { cn, classBuilder } from '@/shared/utils/cn';
import type { UserStatsResponse } from '@/api/endpoints/gamificationApi';

interface LevelProgressBarProps {
    stats: UserStatsResponse | null;
    variant?: 'compact' | 'full'; // compact: 헤더용, full: 프로필용
    className?: string;
}

// 레벨별 색상 테마
const LEVEL_COLORS: Record<number, { bg: string; text: string; border: string; glow: string }> = {
    1: { bg: 'bg-emerald-500', text: 'text-emerald-600', border: 'border-emerald-200', glow: 'shadow-emerald-500/30' },
    2: { bg: 'bg-blue-500', text: 'text-blue-600', border: 'border-blue-200', glow: 'shadow-blue-500/30' },
    3: { bg: 'bg-purple-500', text: 'text-purple-600', border: 'border-purple-200', glow: 'shadow-purple-500/30' },
    4: { bg: 'bg-orange-500', text: 'text-orange-600', border: 'border-orange-200', glow: 'shadow-orange-500/30' },
    5: { bg: 'bg-rose-500', text: 'text-rose-600', border: 'border-rose-200', glow: 'shadow-rose-500/30' },
    6: { bg: 'bg-amber-500', text: 'text-amber-600', border: 'border-amber-200', glow: 'shadow-amber-500/30' },
};

// 레벨별 아이콘 이모지
const LEVEL_ICONS: Record<number, string> = {
    1: '🌱',
    2: '📖',
    3: '🔥',
    4: '⭐',
    5: '👑',
    6: '💎',
};

export const LevelProgressBar: React.FC<LevelProgressBarProps> = ({
    stats,
    variant = 'compact',
    className,
}) => {
    const [isExpanded, setIsExpanded] = useState(false);

    if (!stats) return null;

    const { level, levelName, levelProgress, nextLevel, totalActivityDays, currentStreak } = stats;
    const colors = LEVEL_COLORS[level] || LEVEL_COLORS[1];
    const levelIcon = LEVEL_ICONS[level] || '🌱';

    // 컴팩트 버전 (헤더용)
    if (variant === 'compact') {
        return (
            <div className={cn("relative", className)}>
                <button
                    onClick={() => setIsExpanded(!isExpanded)}
                    className={cn(
                        "flex items-center gap-2 px-3 py-1.5 rounded-full transition-all",
                        "bg-white/80 backdrop-blur-sm border",
                        colors.border,
                        "hover:shadow-md"
                    )}
                >
                    <span className="text-base">{levelIcon}</span>
                    <span className={cn("text-xs font-bold", colors.text)}>Lv.{level}</span>

                    {/* 미니 진행바 */}
                    <div className="w-16 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                        <motion.div
                            className={cn("h-full rounded-full", colors.bg)}
                            initial={{ width: 0 }}
                            animate={{ width: `${levelProgress.percentage}%` }}
                            transition={{ duration: 0.8, ease: "easeOut" }}
                        />
                    </div>

                    <span className="text-[10px] text-gray-500">{levelProgress.percentage}%</span>

                    {isExpanded ? (
                        <ChevronUp size={12} className="text-gray-400" />
                    ) : (
                        <ChevronDown size={12} className="text-gray-400" />
                    )}
                </button>

                {/* 확장 패널 */}
                {isExpanded && (
                    <motion.div
                        initial={{ opacity: 0, y: -10, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: -10, scale: 0.95 }}
                        className={cn(classBuilder.card('modal'), 'absolute top-full right-0 mt-2 w-72 border border-gray-100 p-4 z-50')}
                    >
                        <LevelDetailCard
                            level={level}
                            levelName={levelName}
                            levelIcon={levelIcon}
                            levelProgress={levelProgress}
                            nextLevel={nextLevel}
                            totalActivityDays={totalActivityDays}
                            currentStreak={currentStreak}
                            colors={colors}
                        />
                    </motion.div>
                )}
            </div>
        );
    }

    // 풀 버전 (프로필용)
    return (
        <div className={cn("bg-white rounded-2xl p-6", className)}>
            <LevelDetailCard
                level={level}
                levelName={levelName}
                levelIcon={levelIcon}
                levelProgress={levelProgress}
                nextLevel={nextLevel}
                totalActivityDays={totalActivityDays}
                currentStreak={currentStreak}
                colors={colors}
            />
        </div>
    );
};

// 상세 카드 내부 컴포넌트
interface LevelDetailCardProps {
    level: number;
    levelName: string;
    levelIcon: string;
    levelProgress: { current: number; required: number; percentage: number };
    nextLevel: { level: number; name: string };
    totalActivityDays: number;
    currentStreak: number;
    colors: { bg: string; text: string; border: string; glow: string };
}

const LevelDetailCard: React.FC<LevelDetailCardProps> = ({
    level,
    levelName,
    levelIcon,
    levelProgress,
    nextLevel,
    totalActivityDays,
    currentStreak,
    colors,
}) => {
    const isMaxLevel = level >= 6;

    return (
        <div className="space-y-4">
            {/* 현재 레벨 */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className={cn(
                        "w-12 h-12 rounded-xl flex items-center justify-center text-2xl",
                        "bg-gradient-to-br from-gray-50 to-gray-100",
                        "shadow-inner"
                    )}>
                        {levelIcon}
                    </div>
                    <div>
                        <p className="text-sm text-gray-500">현재 레벨</p>
                        <p className={cn("text-lg font-bold", colors.text)}>
                            Lv.{level} {levelName}
                        </p>
                    </div>
                </div>
                {!isMaxLevel && (
                    <div className="text-right">
                        <p className="text-xs text-gray-400">다음 레벨</p>
                        <p className="text-sm font-medium text-gray-600">
                            Lv.{nextLevel.level} {nextLevel.name}
                        </p>
                    </div>
                )}
            </div>

            {/* 경험치 진행바 */}
            <div className="space-y-2">
                <div className="flex items-center justify-between text-xs">
                    <span className="text-gray-500 flex items-center gap-1">
                        <Zap size={12} className={colors.text} />
                        경험치
                    </span>
                    <span className={cn("font-bold", colors.text)}>
                        {levelProgress.current} / {levelProgress.required}
                    </span>
                </div>
                <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                    <motion.div
                        className={cn(
                            "h-full rounded-full relative",
                            colors.bg,
                            "shadow-lg",
                            colors.glow
                        )}
                        initial={{ width: 0 }}
                        animate={{ width: `${levelProgress.percentage}%` }}
                        transition={{ duration: 1, ease: "easeOut" }}
                    >
                        {/* 반짝이는 효과 */}
                        <motion.div
                            className="absolute inset-0 bg-white/30"
                            animate={{
                                x: ['-100%', '100%'],
                            }}
                            transition={{
                                duration: 2,
                                repeat: Infinity,
                                repeatDelay: 3,
                            }}
                            style={{
                                background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent)',
                            }}
                        />
                    </motion.div>
                </div>
                {!isMaxLevel && (
                    <p className="text-xs text-gray-400 text-center">
                        다음 레벨까지 <span className="font-bold text-gray-600">{levelProgress.required - levelProgress.current}</span> 경험치 남음
                    </p>
                )}
            </div>

            {/* 통계 */}
            <div className="grid grid-cols-2 gap-3 pt-2">
                <div className="flex items-center gap-2 p-2 bg-gray-50 rounded-lg">
                    <Award size={16} className="text-amber-500" />
                    <div>
                        <p className="text-[10px] text-gray-400">총 활동</p>
                        <p className="text-sm font-bold text-gray-700">{totalActivityDays}일</p>
                    </div>
                </div>
                <div className="flex items-center gap-2 p-2 bg-gray-50 rounded-lg">
                    <TrendingUp size={16} className="text-green-500" />
                    <div>
                        <p className="text-[10px] text-gray-400">연속 활동</p>
                        <p className="text-sm font-bold text-gray-700">{currentStreak}일</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LevelProgressBar;
