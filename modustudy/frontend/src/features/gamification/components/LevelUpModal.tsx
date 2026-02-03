/**
 * 레벨업 축하 모달
 * 사용자가 레벨업 했을 때 표시되는 축하 애니메이션 모달
 */

import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Sparkles, Trophy, Star, ArrowRight, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';
import confetti from 'canvas-confetti';

interface LevelUpModalProps {
    isOpen: boolean;
    onClose: () => void;
    previousLevel: number;
    newLevel: number;
    newLevelName: string;
}

// 레벨별 테마 색상
const LEVEL_THEMES: Record<number, { gradient: string; accent: string; emoji: string }> = {
    1: { gradient: 'from-emerald-400 to-emerald-600', accent: 'bg-emerald-500', emoji: '🌱' },
    2: { gradient: 'from-blue-400 to-blue-600', accent: 'bg-blue-500', emoji: '📚' },
    3: { gradient: 'from-purple-400 to-purple-600', accent: 'bg-purple-500', emoji: '🔥' },
    4: { gradient: 'from-orange-400 to-orange-600', accent: 'bg-orange-500', emoji: '⭐' },
    5: { gradient: 'from-rose-400 to-rose-600', accent: 'bg-rose-500', emoji: '👑' },
    6: { gradient: 'from-amber-400 to-amber-600', accent: 'bg-amber-500', emoji: '💎' },
};

// 레벨업 보상 메시지
const LEVEL_REWARDS: Record<number, string[]> = {
    2: ['학습의 즐거움을 알아가는 중!', '첫 걸음을 내딛었어요'],
    3: ['열정이 불타오르고 있어요!', '꾸준함의 힘을 보여주세요'],
    4: ['성실함의 결실을 맺고 있어요!', '이제 거의 마스터 수준'],
    5: ['진정한 학습 마스터!', '멈추지 않는 노력의 결과'],
    6: ['전설의 그랜드마스터!', '모든 도전을 정복했습니다'],
};

export const LevelUpModal: React.FC<LevelUpModalProps> = ({
    isOpen,
    onClose,
    previousLevel,
    newLevel,
    newLevelName,
}) => {
    const [showContent, setShowContent] = useState(false);
    const theme = LEVEL_THEMES[newLevel] || LEVEL_THEMES[1];
    const rewards = LEVEL_REWARDS[newLevel] || ['축하합니다!', '레벨업을 달성했어요'];

    // 모달 열릴 때 confetti 효과
    useEffect(() => {
        if (isOpen) {
            // 잠시 후 콘텐츠 표시
            const timer = setTimeout(() => setShowContent(true), 300);

            // Confetti 효과
            const duration = 3000;
            const end = Date.now() + duration;

            const frame = () => {
                confetti({
                    particleCount: 3,
                    angle: 60,
                    spread: 55,
                    origin: { x: 0 },
                    colors: ['#FFD700', '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4'],
                });
                confetti({
                    particleCount: 3,
                    angle: 120,
                    spread: 55,
                    origin: { x: 1 },
                    colors: ['#FFD700', '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4'],
                });

                if (Date.now() < end) {
                    requestAnimationFrame(frame);
                }
            };
            frame();

            return () => clearTimeout(timer);
        } else {
            setShowContent(false);
        }
    }, [isOpen]);

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="fixed inset-0 z-[100] flex items-center justify-center"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                >
                    {/* 배경 오버레이 */}
                    <motion.div
                        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                        onClick={onClose}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    />

                    {/* 모달 카드 */}
                    <motion.div
                        className="relative w-full max-w-md mx-4"
                        initial={{ scale: 0.5, opacity: 0, y: 50 }}
                        animate={{ scale: 1, opacity: 1, y: 0 }}
                        exit={{ scale: 0.8, opacity: 0, y: 20 }}
                        transition={{ type: 'spring', damping: 20, stiffness: 300 }}
                    >
                        {/* 닫기 버튼 */}
                        <button
                            onClick={onClose}
                            className="absolute -top-3 -right-3 w-8 h-8 bg-white rounded-full shadow-lg flex items-center justify-center z-10 hover:bg-gray-100 transition-colors"
                        >
                            <X size={16} className="text-gray-500" />
                        </button>

                        {/* 그라데이션 배경 카드 */}
                        <div className={cn(
                            "relative overflow-hidden rounded-3xl",
                            "bg-gradient-to-br",
                            theme.gradient,
                            "shadow-2xl"
                        )}>
                            {/* 배경 장식 */}
                            <div className="absolute inset-0 overflow-hidden">
                                {/* 반짝이는 별들 */}
                                {[...Array(20)].map((_, i) => (
                                    <motion.div
                                        key={i}
                                        className="absolute w-1 h-1 bg-white rounded-full"
                                        style={{
                                            left: `${Math.random() * 100}%`,
                                            top: `${Math.random() * 100}%`,
                                        }}
                                        animate={{
                                            opacity: [0, 1, 0],
                                            scale: [0, 1, 0],
                                        }}
                                        transition={{
                                            duration: 2,
                                            repeat: Infinity,
                                            delay: Math.random() * 2,
                                        }}
                                    />
                                ))}
                            </div>

                            {/* 콘텐츠 */}
                            <div className="relative p-8 text-center">
                                {/* 아이콘 배지 */}
                                <AnimatePresence>
                                    {showContent && (
                                        <motion.div
                                            initial={{ scale: 0, rotate: -180 }}
                                            animate={{ scale: 1, rotate: 0 }}
                                            transition={{ type: 'spring', damping: 10, stiffness: 200, delay: 0.2 }}
                                            className="relative mx-auto mb-6"
                                        >
                                            {/* 빛나는 링 */}
                                            <motion.div
                                                className="absolute inset-0 rounded-full border-4 border-white/30"
                                                animate={{
                                                    scale: [1, 1.3, 1],
                                                    opacity: [0.5, 0, 0.5],
                                                }}
                                                transition={{ duration: 2, repeat: Infinity }}
                                                style={{ width: 120, height: 120, left: -10, top: -10 }}
                                            />

                                            {/* 레벨 아이콘 */}
                                            <div className="w-24 h-24 mx-auto bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center text-5xl shadow-lg">
                                                {theme.emoji}
                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>

                                {/* 레벨업 텍스트 */}
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: 0.4 }}
                                >
                                    <div className="flex items-center justify-center gap-2 mb-2">
                                        <Sparkles size={20} className="text-yellow-300" />
                                        <span className="text-white/80 text-sm font-medium tracking-wider uppercase">
                                            Level Up!
                                        </span>
                                        <Sparkles size={20} className="text-yellow-300" />
                                    </div>

                                    <h2 className="text-4xl font-black text-white mb-1">
                                        Lv.{newLevel}
                                    </h2>
                                    <p className="text-xl font-bold text-white/90 mb-4">
                                        {newLevelName}
                                    </p>

                                    {/* 레벨 전환 표시 */}
                                    <div className="flex items-center justify-center gap-3 mb-6">
                                        <span className="px-3 py-1 bg-white/20 rounded-full text-white/80 text-sm">
                                            Lv.{previousLevel}
                                        </span>
                                        <ArrowRight size={20} className="text-white/60" />
                                        <span className="px-3 py-1 bg-white/30 rounded-full text-white font-bold text-sm">
                                            Lv.{newLevel}
                                        </span>
                                    </div>
                                </motion.div>

                                {/* 보상 메시지 */}
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: 0.6 }}
                                    className="bg-white/10 backdrop-blur-sm rounded-2xl p-4 mb-6"
                                >
                                    <div className="flex items-center justify-center gap-2 mb-2">
                                        <Trophy size={16} className="text-yellow-300" />
                                        <span className="text-white/90 font-medium">{rewards[0]}</span>
                                    </div>
                                    <p className="text-white/70 text-sm">{rewards[1]}</p>
                                </motion.div>

                                {/* 확인 버튼 */}
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: 0.8 }}
                                >
                                    <Button
                                        onClick={onClose}
                                        className="w-full bg-white text-gray-800 hover:bg-gray-100 font-bold py-3 rounded-xl shadow-lg"
                                    >
                                        <Star size={16} className="mr-2 text-yellow-500" />
                                        확인
                                    </Button>
                                </motion.div>
                            </div>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default LevelUpModal;
