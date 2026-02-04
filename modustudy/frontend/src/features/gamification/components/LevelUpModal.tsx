/**
 * 레벨업 축하 모달
 * 사용자가 레벨업 했을 때 표시되는 축하 애니메이션 모달
 * SQUIZ 아일랜드 - 돌고래 마스코트 컨셉
 */

import React, { useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Sparkles, Trophy, ArrowRight, X } from 'lucide-react';
import confetti from 'canvas-confetti';

interface LevelUpModalProps {
    isOpen: boolean;
    onClose: () => void;
    previousLevel: number;
    newLevel: number;
    newLevelName: string;
}

// 레벨업 보상 메시지
const LEVEL_REWARDS: Record<number, string[]> = {
    2: ['학습의 즐거움을 알아가는 중!', '첫 걸음을 내딛었어요'],
    3: ['열정이 불타오르고 있어요!', '꾸준함의 힘을 보여주세요'],
    4: ['성실함의 결실을 맺고 있어요!', '이제 거의 마스터 수준'],
    5: ['진정한 학습 마스터!', '멈추지 않는 노력의 결과'],
    6: ['전설의 그랜드마스터!', '모든 도전을 정복했습니다'],
};

// 돌고래 자연스러운 수영 애니메이션
const dolphinSwim1 = {
    animate: {
        // 부드러운 8자 곡선 경로
        x: [-160, -100, 0, 100, 160, 100, 0, -100, -160],
        y: [-60, -100, -80, -100, -60, 100, 120, 100, -60],
        // 이동 방향에 따라 자연스럽게 회전
        rotate: [15, 5, -5, -15, -25, -15, 5, 15, 15],
        // 자연스러운 좌우 반전
        scaleX: [1, 1, 1, 1, -1, -1, -1, -1, 1],
        // 원근감을 위한 크기 변화
        scale: [1, 1.05, 1.1, 1.05, 1, 0.95, 0.9, 0.95, 1],
    },
    transition: {
        duration: 8,
        repeat: Infinity,
        ease: 'easeInOut' as const,
        times: [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1],
    },
};

const dolphinSwim2 = {
    animate: {
        // 반대 방향 8자 곡선
        x: [160, 100, 0, -100, -160, -100, 0, 100, 160],
        y: [80, 120, 100, 120, 80, -80, -100, -80, 80],
        rotate: [-15, -5, 5, 15, 25, 15, -5, -15, -15],
        scaleX: [-1, -1, -1, -1, 1, 1, 1, 1, -1],
        scale: [1, 0.95, 0.9, 0.95, 1, 1.05, 1.1, 1.05, 1],
    },
    transition: {
        duration: 8,
        repeat: Infinity,
        ease: 'easeInOut' as const,
        times: [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1],
    },
};

export const LevelUpModal: React.FC<LevelUpModalProps> = ({
    isOpen,
    onClose,
    previousLevel,
    newLevel,
    newLevelName,
}) => {
    const rewards = LEVEL_REWARDS[newLevel] || ['축하합니다!', '레벨업을 달성했어요'];

    // 모달 열릴 때 confetti 효과
    useEffect(() => {
        if (isOpen) {
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
                    {/* 배경 오버레이 - 바다 느낌 그라데이션 */}
                    <motion.div
                        className="absolute inset-0"
                        style={{
                            background: 'linear-gradient(180deg, rgba(0,80,120,0.85) 0%, rgba(0,60,100,0.9) 50%, rgba(0,40,80,0.95) 100%)',
                        }}
                        onClick={onClose}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    />

                    {/* 배경 거품 효과 */}
                    <div className="absolute inset-0 overflow-hidden pointer-events-none">
                        {[...Array(12)].map((_, i) => (
                            <motion.div
                                key={i}
                                className="absolute text-white/30"
                                style={{
                                    left: `${Math.random() * 100}%`,
                                    bottom: -30,
                                    fontSize: 16 + Math.random() * 20,
                                }}
                                animate={{
                                    y: -window.innerHeight - 100,
                                    opacity: [0, 0.5, 0.5, 0],
                                }}
                                transition={{
                                    duration: 5 + Math.random() * 3,
                                    delay: i * 0.4,
                                    repeat: Infinity,
                                    ease: 'linear',
                                }}
                            >
                                🫧
                            </motion.div>
                        ))}
                    </div>

                    {/* 메인 컨텐츠 - 카드 + 회전하는 돌고래 */}
                    <motion.div
                        className="relative flex items-center justify-center px-4"
                        initial={{ scale: 0.5, opacity: 0, y: 50 }}
                        animate={{ scale: 1, opacity: 1, y: 0 }}
                        exit={{ scale: 0.8, opacity: 0, y: 20 }}
                        transition={{ type: 'spring', damping: 20, stiffness: 300 }}
                    >
                        {/* 카드 주변을 헤엄치는 돌고래 1 */}
                        <motion.img
                            src="/images/dancing_dolphin.png"
                            alt="돌고래"
                            className="absolute w-16 h-16 sm:w-20 sm:h-20 object-contain z-20 hidden sm:block"
                            animate={dolphinSwim1.animate}
                            transition={dolphinSwim1.transition}
                        />

                        {/* 카드 주변을 헤엄치는 돌고래 2 */}
                        <motion.img
                            src="/images/dancing_dolphin.png"
                            alt="돌고래"
                            className="absolute w-16 h-16 sm:w-20 sm:h-20 object-contain z-20 hidden sm:block"
                            animate={dolphinSwim2.animate}
                            transition={dolphinSwim2.transition}
                        />

                        {/* 레벨업 카드 */}
                        <div className="relative w-full max-w-sm z-10">
                            {/* 닫기 버튼 */}
                            <button
                                onClick={onClose}
                                className="absolute -top-3 -right-3 w-8 h-8 bg-white rounded-full shadow-lg flex items-center justify-center z-10 hover:bg-gray-100 transition-colors"
                            >
                                <X size={16} className="text-gray-500" />
                            </button>

                            {/* 글래스모피즘 카드 */}
                            <div className="relative overflow-hidden rounded-3xl backdrop-blur-md bg-white/20 shadow-2xl">
                                {/* 배경 장식 - 반짝이는 별들 */}
                                <div className="absolute inset-0 overflow-hidden">
                                    {[...Array(15)].map((_, i) => (
                                        <motion.div
                                            key={i}
                                            className="absolute text-yellow-300"
                                            style={{
                                                left: `${Math.random() * 100}%`,
                                                top: `${Math.random() * 100}%`,
                                                fontSize: 10 + Math.random() * 8,
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
                                        >
                                            ✨
                                        </motion.div>
                                    ))}
                                </div>

                                {/* 콘텐츠 */}
                                <div className="relative p-6 sm:p-8 text-center">
                                    {/* 모바일 돌고래 (상단) */}
                                    <div className="flex justify-center gap-4 mb-4 sm:hidden">
                                        <motion.img
                                            src="/images/dancing_dolphin.png"
                                            alt="돌고래"
                                            className="w-14 h-14 object-contain"
                                            animate={{
                                                y: [0, -8, 0],
                                                rotate: [-5, 5, -5],
                                            }}
                                            transition={{
                                                duration: 1.5,
                                                repeat: Infinity,
                                                ease: 'easeInOut',
                                            }}
                                        />
                                        <motion.img
                                            src="/images/dancing_dolphin.png"
                                            alt="돌고래"
                                            className="w-14 h-14 object-contain"
                                            style={{ scaleX: -1 }}
                                            animate={{
                                                y: [0, -8, 0],
                                                rotate: [5, -5, 5],
                                            }}
                                            transition={{
                                                duration: 1.5,
                                                repeat: Infinity,
                                                ease: 'easeInOut',
                                            }}
                                        />
                                    </div>

                                    {/* 축하 메시지 */}
                                    <motion.div
                                        initial={{ opacity: 0, y: 10 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ delay: 0.3 }}
                                        className="mb-2"
                                    >
                                        <span className="text-2xl">🎉</span>
                                        <p className="text-white font-bold text-lg mt-1">축하합니다!</p>
                                    </motion.div>

                                    {/* 레벨업 텍스트 */}
                                    <motion.div
                                        initial={{ opacity: 0, y: 20 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ delay: 0.4 }}
                                    >
                                        <div className="flex items-center justify-center gap-2 mb-2">
                                            <Sparkles size={18} className="text-yellow-300" />
                                            <span className="text-white/80 text-xs font-medium tracking-wider uppercase">
                                                Level Up!
                                            </span>
                                            <Sparkles size={18} className="text-yellow-300" />
                                        </div>

                                        <h2 className="text-5xl font-black text-white mb-1" style={{ textShadow: '0 2px 10px rgba(0,0,0,0.3)' }}>
                                            Lv.{newLevel}
                                        </h2>
                                        <p className="text-xl font-bold text-cyan-200 mb-4">
                                            {newLevelName}
                                        </p>

                                        {/* 레벨 전환 표시 */}
                                        <div className="flex items-center justify-center gap-3 mb-4">
                                            <span className="px-3 py-1 bg-white/20 rounded-full text-white/80 text-sm">
                                                Lv.{previousLevel}
                                            </span>
                                            <ArrowRight size={18} className="text-white/60" />
                                            <span className="px-3 py-1 bg-cyan-400/40 rounded-full text-white font-bold text-sm">
                                                Lv.{newLevel}
                                            </span>
                                        </div>
                                    </motion.div>

                                    {/* 보상 메시지 */}
                                    <motion.div
                                        initial={{ opacity: 0, y: 20 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ delay: 0.6 }}
                                        className="bg-white/10 backdrop-blur-sm rounded-2xl p-3 mb-4"
                                    >
                                        <div className="flex items-center justify-center gap-2 mb-1">
                                            <Trophy size={14} className="text-yellow-300" />
                                            <span className="text-white/90 font-medium text-sm">{rewards[0]}</span>
                                        </div>
                                        <p className="text-white/70 text-xs">{rewards[1]}</p>
                                    </motion.div>

                                </div>
                            </div>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default LevelUpModal;
