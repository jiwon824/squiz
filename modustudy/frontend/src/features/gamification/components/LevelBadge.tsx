/**
 * 레벨 배지 컴포넌트
 * 닉네임 옆에 표시되는 간단한 레벨 배지
 * 호버 시 전체 레벨 정보 팝오버 표시
 */

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/shared/utils/cn';

interface LevelBadgeProps {
    level: number;
    levelName?: string;
    size?: 'sm' | 'md' | 'lg';
    variant?: 'solid' | 'text'; // solid: 배경 있음, text: 배경 없음
    showName?: boolean;
    popoverAlign?: 'default' | 'left'; // left: 배지 왼쪽으로 팝오버 표시
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
    2: '📖',
    3: '🔥',
    4: '⭐',
    5: '👑',
    6: '💎',
};

// 레벨별 상세 정보
const LEVEL_INFO: Array<{ level: number; name: string; icon: string; description: string }> = [
    { level: 1, name: '새싹', icon: '🌱', description: '학습의 첫 걸음을 시작했어요' },
    { level: 2, name: '학습자', icon: '📖', description: '학습의 즐거움을 알아가는 중' },
    { level: 3, name: '열정가', icon: '🔥', description: '열정이 불타오르고 있어요' },
    { level: 4, name: '성실러', icon: '⭐', description: '성실함의 결실을 맺고 있어요' },
    { level: 5, name: '마스터', icon: '👑', description: '진정한 학습 마스터' },
    { level: 6, name: '그랜드마스터', icon: '💎', description: '전설의 그랜드마스터' },
];

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
    popoverAlign = 'default',
    className,
}) => {
    const [isHovered, setIsHovered] = useState(false);
    const [popoverPosition, setPopoverPosition] = useState<'bottom' | 'top'>('bottom');
    const [fixedStyle, setFixedStyle] = useState<React.CSSProperties>({});
    const badgeRef = useRef<HTMLDivElement>(null);

    const colors = LEVEL_COLORS[level] || LEVEL_COLORS[1];
    const icon = LEVEL_ICONS[level] || '🌱';
    const sizeStyle = SIZE_STYLES[size];
    const isText = variant === 'text';

    // 팝오버 위치 계산
    useEffect(() => {
        if (isHovered && badgeRef.current) {
            const rect = badgeRef.current.getBoundingClientRect();

            if (popoverAlign === 'left') {
                // 드롭다운 패널(가장 가까운 absolute 부모)의 왼쪽 바깥에 배치
                const dropdown = badgeRef.current.closest('.absolute') as HTMLElement | null;
                if (dropdown) {
                    const dropdownRect = dropdown.getBoundingClientRect();
                    setFixedStyle({
                        position: 'fixed',
                        top: dropdownRect.top,
                        left: dropdownRect.left - 264, // w-64 = 256px + 8px 간격
                    });
                }
            } else {
                const spaceBelow = window.innerHeight - rect.bottom;
                setPopoverPosition(spaceBelow < 320 ? 'top' : 'bottom');
            }
        }
    }, [isHovered, popoverAlign]);

    return (
        <div
            className="relative inline-flex"
            ref={badgeRef}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                whileHover={{ scale: 1.05 }}
                className={cn(
                    "inline-flex items-center font-semibold cursor-pointer",
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

            {/* 레벨 정보 팝오버 */}
            <AnimatePresence>
                {isHovered && (
                    <motion.div
                        initial={{ opacity: 0, y: -4 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -4 }}
                        transition={{ duration: 0.15 }}
                        className={cn(
                            "z-50 w-64 bg-white rounded-2xl shadow-[0_8px_30px_rgba(0,0,0,0.12)] p-4",
                            popoverAlign === 'left' ? 'fixed' : [
                                'absolute left-0',
                                popoverPosition === 'bottom' ? 'top-full mt-2' : 'bottom-full mb-2'
                            ]
                        )}
                        style={popoverAlign === 'left' ? fixedStyle : undefined}
                    >
                        <p className="text-xs font-semibold text-gray-400 mb-3">레벨 등급표</p>
                        <div className="space-y-1.5">
                            {LEVEL_INFO.map((info) => {
                                const isCurrent = info.level === level;
                                const itemColors = LEVEL_COLORS[info.level];
                                return (
                                    <div
                                        key={info.level}
                                        className={cn(
                                            "flex items-center gap-3 px-3 py-2 rounded-xl transition-colors",
                                            isCurrent
                                                ? [itemColors.bg, "ring-1 ring-inset", itemColors.text.replace('text-', 'ring-')]
                                                : "hover:bg-gray-50"
                                        )}
                                    >
                                        <span className="text-base w-6 text-center shrink-0">{info.icon}</span>
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-1.5">
                                                <span className={cn(
                                                    "text-sm font-bold",
                                                    isCurrent ? itemColors.text : "text-gray-700"
                                                )}>
                                                    Lv.{info.level}
                                                </span>
                                                <span className={cn(
                                                    "text-sm font-medium",
                                                    isCurrent ? itemColors.text : "text-gray-500"
                                                )}>
                                                    {info.name}
                                                </span>
                                                {isCurrent && (
                                                    <span className={cn(
                                                        "text-[10px] font-bold px-1.5 py-0.5 rounded-full",
                                                        itemColors.bg, itemColors.text
                                                    )}>
                                                        현재
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-[11px] text-gray-400 truncate">{info.description}</p>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default LevelBadge;
