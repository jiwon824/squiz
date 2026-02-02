/**
 * 레벨 배지 컴포넌트
 * 닉네임 옆에 표시되는 간단한 레벨 배지
 */

import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/shared/utils/cn';

interface LevelBadgeProps {
    level: number;
    levelName?: string;
    size?: 'sm' | 'md' | 'lg';
    variant?: 'solid' | 'text'; // solid: 배경 있음, text: 배경 없음
    showName?: boolean;
    className?: string;
}

// 레벨별 색상 테마
const LEVEL_COLORS: Record<number, { bg: string; text: string; gradient: string }> = {
    1: { bg: 'bg-emerald-100', text: 'text-emerald-700', gradient: 'from-emerald-400 to-emerald-600' },
    2: { bg: 'bg-blue-100', text: 'text-blue-700', gradient: 'from-blue-400 to-blue-600' },
    3: { bg: 'bg-purple-100', text: 'text-purple-700', gradient: 'from-purple-400 to-purple-600' },
    4: { bg: 'bg-orange-100', text: 'text-orange-700', gradient: 'from-orange-400 to-orange-600' },
    5: { bg: 'bg-rose-100', text: 'text-rose-700', gradient: 'from-rose-400 to-rose-600' },
    6: { bg: 'bg-amber-100', text: 'text-amber-700', gradient: 'from-amber-400 to-amber-600' },
};

// 레벨별 아이콘 이모지
const LEVEL_ICONS: Record<number, string> = {
    1: '🌱',
    2: '📚',
    3: '🔥',
    4: '⭐',
    5: '👑',
    6: '💎',
};

// 크기별 스타일
const SIZE_STYLES = {
    sm: {
        container: 'px-2 py-0.5 gap-1',
        icon: 'text-xs',
        text: 'text-xs',
    },
    md: {
        container: 'px-2.5 py-1 gap-1.5',
        icon: 'text-sm',
        text: 'text-sm',
    },
    lg: {
        container: 'px-3 py-1.5 gap-2',
        icon: 'text-base',
        text: 'text-base',
    },
};

export const LevelBadge: React.FC<LevelBadgeProps> = ({
    level,
    levelName,
    size = 'md',
    variant = 'solid',
    showName = false,
    className,
}) => {
    const colors = LEVEL_COLORS[level] || LEVEL_COLORS[1];
    const icon = LEVEL_ICONS[level] || '🌱';
    const sizeStyle = SIZE_STYLES[size];
    const isText = variant === 'text';

    return (
        <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            whileHover={{ scale: 1.05 }}
            className={cn(
                "inline-flex items-center font-semibold",
                isText ? "gap-1" : ["rounded-full", sizeStyle.container, colors.bg, "shadow-sm"],
                colors.text,
                className
            )}
        >
            <span className={sizeStyle.icon}>{icon}</span>
            <span className={cn("font-bold", sizeStyle.text)}>
                Lv.{level}
                {showName && levelName && (
                    <span className="font-medium ml-1">{levelName}</span>
                )}
            </span>
        </motion.div>
    );
};

export default LevelBadge;
