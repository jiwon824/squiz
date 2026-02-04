import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence, useMotionValue, useTransform } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Brain, Video, TrendingUp, FileText, BarChart3, ArrowUpRight, Calendar, Clock, Users, ChevronRight, Search, CheckCircle2, Circle, BookOpen, Star, ListChecks, Hash } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';

import { mockStudies } from '../../study/mockData';
import StudyCardContentV2 from '../../study/components/StudyCardContentV2';
import '../styles/DashboardV2.css';

// 히어로 메시지 데이터
const HERO_MESSAGES = [
    {
        id: 1,
        title: '말만 하세요,',
        subtitle: '공부는 저희가 시켜드릴게요',
        description: '스터디 참여만으로 자동으로 복습·요약·평가가 완성됩니다',
        icon: Sparkles,
        backgroundImage: '/images/background7.png', // 배경 이미지 경로
    },
    {
        id: 2,
        title: '스터디가 끝나면',
        subtitle: 'AI가 자동으로 정리해드려요',
        description: 'AI 기반 대화 요약본과 핵심 키워드 퀴즈를 자동 생성',
        icon: Brain,
        spotlightGradient: true, // 중앙 스포트라이트 + 파란색 그라데이션
        backgroundImage: '/images/background8.png', // 배경 이미지 경로
    },
    {
        id: 3,
        title: '어디서든 함께하는',
        subtitle: '실시간 화상 스터디',
        description: '화면 공유와 음성 채팅이 결합된 강력한 학습 환경',
        icon: Video,
        backgroundImage: '/images/background9.png', // 배경 이미지 경로
    },
];

export const GuestDashboardV2: React.FC = () => {
    const navigate = useNavigate();
    const [currentIndex, setCurrentIndex] = useState(0);
    const [isLaunching, setIsLaunching] = useState(false);

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentIndex((prev) => (prev + 1) % HERO_MESSAGES.length);
        }, 5000);
        return () => clearInterval(timer);
    }, []);

    // CTA 버튼 클릭 핸들러
    const handleStartClick = () => {
        setIsLaunching(true);
        // 애니메이션 완료 후 페이지 이동
        setTimeout(() => {
            navigate('/login');
        }, 800);
    };

    const currentHero = HERO_MESSAGES[currentIndex];
    const Icon = currentHero.icon;

    // 스크롤 진행도 공통 헬퍼
    const useScrollProgress = (ref: React.RefObject<HTMLElement | null>) => {
        const progress = useMotionValue(0);
        useEffect(() => {
            const container = document.getElementById('guest-content-scroll');
            const target = ref.current;
            if (!container || !target) return;
            const update = () => {
                const cRect = container.getBoundingClientRect();
                const tRect = target.getBoundingClientRect();
                const relTop = tRect.top - cRect.top;
                const total = cRect.height + tRect.height;
                progress.set(Math.max(0, Math.min(1, (cRect.height - relTop) / total)));
            };
            container.addEventListener('scroll', update, { passive: true });
            update();
            return () => container.removeEventListener('scroll', update);
        }, [ref, progress]);
        return progress;
    };

    // 스터디 섹션 스크롤 플로우
    const studySectionRef = useRef<HTMLElement>(null);
    const studyProgress = useScrollProgress(studySectionRef);
    const studyPathLength = useTransform(studyProgress, [0.15, 0.7], [0, 1]);
    const studyOpacity = useTransform(studyProgress, [0.1, 0.2, 0.75, 0.9], [0, 1, 1, 0]);

    // 서비스 특징 섹션 스크롤 플로우
    const featureSectionRef = useRef<HTMLElement>(null);
    const featureProgress = useScrollProgress(featureSectionRef);
    const flowPathLength = useTransform(featureProgress, [0.15, 0.75], [0, 1]);
    const flowOpacity = useTransform(featureProgress, [0.1, 0.2, 0.8, 0.9], [0, 1, 1, 0]);

    return (
        <div className="min-h-screen bg-white">
            {/* 히어로 섹션 - Apple 스타일 */}
            <section
                className="relative min-h-[80vh] flex items-center justify-center py-12"
                style={
                    currentHero.backgroundImage ? {
                        backgroundImage: `url(${currentHero.backgroundImage})`,
                        backgroundSize: 'cover',
                        backgroundPosition: 'center',
                    } : currentHero.spotlightGradient ? {
                        // 베이스 배경 (레이어들이 위에 쌓임)
                        background: 'linear-gradient(180deg, #F8FAFF 0%, #EEF4FF 100%)',
                    } : undefined
                }
            >
                {/* 배경 오버레이 */}
                {currentHero.backgroundImage ? (
                    <div className="absolute inset-0 bg-gradient-to-b from-white/80 via-white/40 to-white" />
                ) : currentHero.spotlightGradient ? (
                    <>
                        {/* 다층 그라데이션 - 아주 연하고 투명한 효과 */}
                        <div
                            className="absolute inset-0 opacity-20"
                            style={{
                                background: 'radial-gradient(ellipse 100% 80% at 50% 40%, transparent 0%, rgba(66, 133, 244, 0.05) 60%, rgba(66, 133, 244, 0.12) 100%)',
                            }}
                        />
                        <div
                            className="absolute inset-0 opacity-15"
                            style={{
                                background: 'radial-gradient(circle at 20% 10%, rgba(66, 133, 244, 0.08) 0%, transparent 40%)',
                            }}
                        />
                        <div
                            className="absolute inset-0 opacity-15"
                            style={{
                                background: 'radial-gradient(circle at 80% 90%, rgba(26, 115, 232, 0.1) 0%, transparent 40%)',
                            }}
                        />
                        {/* 중앙 밝은 스포트라이트 */}
                        <div
                            className="absolute inset-0"
                            style={{
                                background: 'radial-gradient(ellipse 60% 50% at 50% 45%, rgba(255,255,255,0.85) 0%, rgba(255,255,255,0.3) 55%, transparent 80%)',
                            }}
                        />
                    </>
                ) : (
                    <div className="absolute inset-0 bg-gradient-to-b from-gray-50 to-white" />
                )}

                <div className="relative max-w-5xl mx-auto px-8 text-center">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={currentIndex}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -20 }}
                            transition={{ duration: 0.6 }}
                            className="space-y-6"
                        >
                            {/* 아이콘 */}
                            <motion.div
                                className="flex justify-center"
                                animate={{ scale: [1, 1.05, 1] }}
                                transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
                            >
                                <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center">
                                    <Icon className="text-primary" size={32} />
                                </div>
                            </motion.div>

                            {/* 메인 타이틀 */}
                            <div className="space-y-3">
                                <h1 className="text-6xl font-black text-gray-900 tracking-tight leading-tight mb-0">
                                    {currentHero.title}
                                </h1>
                                <h2 className="text-6xl font-black text-primary tracking-tight leading-tight mb-0">
                                    {currentHero.subtitle}
                                </h2>
                            </div>

                            {/* 설명 */}
                            <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed">
                                {currentHero.description}
                            </p>
                        </motion.div>
                    </AnimatePresence>

                    {/* 인디케이터 */}
                    <div className="flex justify-center gap-2 mt-12">
                        {HERO_MESSAGES.map((_, index) => (
                            <button
                                key={index}
                                onClick={() => setCurrentIndex(index)}
                                className={cn(
                                    'h-2 rounded-full transition-all duration-300',
                                    index === currentIndex 
                                        ? 'w-8 bg-primary' 
                                        : 'w-2 bg-gray-300 hover:bg-gray-400'
                                )}
                            />
                        ))}
                    </div>
                </div>
            </section>

            {/* 인기 스터디 섹션 */}
            <section ref={studySectionRef} className="py-40 bg-gray-50 relative overflow-hidden">
                {/* SVG 가로 나선형 플로우 - 왼쪽→오른쪽 웨이브 후 아래로 이어짐 */}
                <motion.svg
                    className="absolute inset-0 w-full h-full pointer-events-none"
                    viewBox="0 0 1000 1000"
                    preserveAspectRatio="none"
                    fill="none"
                    style={{ opacity: studyOpacity }}
                >
                    <defs>
                        <linearGradient id="studyFlowGrad" x1="0" y1="0" x2="1" y2="0">
                            <stop offset="0%" stopColor="var(--color-primary)" stopOpacity="0.06" />
                            <stop offset="50%" stopColor="var(--color-primary)" stopOpacity="0.2" />
                            <stop offset="100%" stopColor="var(--color-secondary)" stopOpacity="0.12" />
                        </linearGradient>
                        <marker id="studyArrow" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                            <path d="M 0 0 L 10 4 L 0 8" fill="none" stroke="var(--color-primary)" strokeWidth="1.5" opacity="0.35" />
                        </marker>
                        <filter id="studyGlow">
                            <feGaussianBlur stdDeviation="3" result="blur" />
                            <feMerge>
                                <feMergeNode in="blur" />
                                <feMergeNode in="SourceGraphic" />
                            </feMerge>
                        </filter>
                    </defs>

                    {/* 메인 경로 - 타이틀→좌측→우측 웨이브→하단 중앙(1000)으로 빠짐 */}
                    <motion.path
                        d="M 500,80 C 500,130 80,140 50,280 C 20,400 200,440 320,410 C 440,380 480,460 500,490 C 520,520 620,380 780,410 C 940,440 980,410 960,490 C 940,570 740,600 500,680 C 260,760 500,880 500,1000"
                        stroke="url(#studyFlowGrad)"
                        strokeWidth="2.5"
                        strokeLinecap="round"
                        filter="url(#studyGlow)"
                        style={{ pathLength: studyPathLength }}
                    />

                    {/* 카드 방향 분기선 */}
                    <motion.path
                        d="M 170,420 C 185,460 200,500 210,540"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#studyArrow)"
                        style={{ pathLength: studyPathLength }}
                    />
                    <motion.path
                        d="M 380,400 C 388,440 395,480 400,540"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#studyArrow)"
                        style={{ pathLength: studyPathLength }}
                    />
                    <motion.path
                        d="M 640,400 C 635,440 630,480 625,540"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#studyArrow)"
                        style={{ pathLength: studyPathLength }}
                    />
                    <motion.path
                        d="M 850,420 C 842,460 835,500 830,540"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#studyArrow)"
                        style={{ pathLength: studyPathLength }}
                    />

                    {/* 노드 */}
                    {[
                        { cx: 500, cy: 80 },
                        { cx: 50, cy: 280 },
                        { cx: 320, cy: 410 },
                        { cx: 500, cy: 490 },
                        { cx: 780, cy: 410 },
                        { cx: 960, cy: 490 },
                        { cx: 500, cy: 680 },
                        { cx: 500, cy: 1000 },
                    ].map((n, i) => (
                        <motion.circle
                            key={i} cx={n.cx} cy={n.cy} r="4"
                            fill="var(--color-primary)" opacity="0.2"
                            style={{ pathLength: studyPathLength }}
                        />
                    ))}

                    {/* 진행 방향 퍼짐 라인 */}
                    {[
                        // 시작 (500,80) → 좌하단으로 퍼짐
                        { d: 'M 500,80 C 400,110 260,160 150,230', opacity: 0.1 },
                        { d: 'M 500,80 C 420,130 300,210 220,290', opacity: 0.06 },
                        { d: 'M 500,80 C 370,90 200,120 100,170', opacity: 0.04 },

                        // 좌측 꼭지 (50,280) → 우측으로 퍼짐
                        { d: 'M 50,280 C 150,290 290,330 400,370', opacity: 0.1 },
                        { d: 'M 50,280 C 130,310 260,370 360,420', opacity: 0.06 },
                        { d: 'M 50,280 C 170,280 320,290 440,310', opacity: 0.04 },

                        // 중앙 (500,490) → 우측으로 퍼짐
                        { d: 'M 500,490 C 600,480 740,440 840,420', opacity: 0.1 },
                        { d: 'M 500,490 C 580,500 700,480 800,460', opacity: 0.06 },
                        { d: 'M 500,490 C 620,510 760,520 860,500', opacity: 0.04 },

                        // 우측 꼭지 (960,490) → 좌하단으로 퍼짐
                        { d: 'M 960,490 C 840,530 700,580 600,630', opacity: 0.1 },
                        { d: 'M 960,490 C 860,550 740,630 660,700', opacity: 0.06 },
                        { d: 'M 960,490 C 820,510 660,540 540,580', opacity: 0.04 },

                        // 하단 중앙 (500,680) → 아래로 퍼짐
                        { d: 'M 500,680 C 420,720 300,770 220,810', opacity: 0.08 },
                        { d: 'M 500,680 C 580,720 700,770 780,810', opacity: 0.08 },
                        { d: 'M 500,680 C 440,750 360,840 320,920', opacity: 0.04 },
                        { d: 'M 500,680 C 560,750 640,840 680,920', opacity: 0.04 },
                    ].map((line, i) => (
                        <motion.path
                            key={`study-spread-${i}`}
                            d={line.d}
                            stroke="var(--color-primary)"
                            strokeWidth="1" strokeOpacity={line.opacity}
                            strokeLinecap="round" fill="none"
                            style={{ pathLength: studyPathLength }}
                        />
                    ))}
                </motion.svg>

                <div className="relative z-10 max-w-7xl mx-auto px-8">
                    {/* 타이틀 */}
                    <motion.div
                        initial={{ opacity: 0, y: 30 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.7 }}
                        className="text-center mb-20"
                    >
                        <h3 className="text-5xl font-black text-gray-900 mb-2">
                            지금 뜨는 스터디
                        </h3>
                        <p className="text-xl text-gray-600">
                            다른 사람들이 참여하고 있는 인기 스터디를 확인해보세요
                        </p>
                    </motion.div>

                    {/* 스터디 카드 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-16">
                        {mockStudies.slice(0, 4).map((study, i) => (
                            <motion.div
                                key={study.id}
                                initial={{ opacity: 0, y: 40 }}
                                whileInView={{ opacity: 1, y: 0 }}
                                viewport={{ once: true }}
                                transition={{ duration: 0.5, delay: i * 0.12 }}
                            >
                                <StudyCardContentV2
                                    study={study}
                                    variant="card"
                                    onClick={(studyId) => navigate(`/study/${studyId}`)}
                                />
                            </motion.div>
                        ))}
                    </div>

                    {/* 전체 보기 버튼 */}
                    <div className="text-center">
                        <Button
                            variant="text"
                            size="xl"
                            onClick={() => navigate('/study')}
                            rightIcon={<TrendingUp size={24} />}
                            className="text-xl"
                        >
                            전체 보기
                        </Button>
                    </div>
                </div>
            </section>

            {/* 서비스 특징 섹션 */}
            <section ref={featureSectionRef} className="py-40 bg-white overflow-hidden relative">
                {/* SVG 플로우 라인 - 3개 기능을 S커브로 연결 */}
                <motion.svg
                    className="absolute inset-0 w-full h-full pointer-events-none"
                    viewBox="0 0 1000 1000"
                    preserveAspectRatio="none"
                    fill="none"
                    style={{ opacity: flowOpacity }}
                >
                    <defs>
                        <linearGradient id="featureFlowGrad" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="var(--color-primary)" stopOpacity="0.08" />
                            <stop offset="50%" stopColor="var(--color-primary)" stopOpacity="0.2" />
                            <stop offset="100%" stopColor="var(--color-secondary)" stopOpacity="0.12" />
                        </linearGradient>
                        <marker id="featureArrow" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                            <path d="M 0 0 L 10 4 L 0 8" fill="none" stroke="var(--color-primary)" strokeWidth="1.5" opacity="0.35" />
                        </marker>
                        <filter id="featureGlow">
                            <feGaussianBlur stdDeviation="4" result="blur" />
                            <feMerge>
                                <feMergeNode in="blur" />
                                <feMergeNode in="SourceGraphic" />
                            </feMerge>
                        </filter>
                    </defs>

                    {/* 메인 경로 - 상단(0)에서 시작 → S커브로 3개 기능 연결 → 하단(1000)으로 */}
                    <motion.path
                        d="M 500,0 C 500,50 160,80 150,190 C 140,300 340,340 500,350 C 660,360 860,400 850,510 C 840,620 500,640 500,660 C 500,680 160,710 150,810 C 140,910 420,960 500,1000"
                        stroke="url(#featureFlowGrad)"
                        strokeWidth="2.5"
                        strokeLinecap="round"
                        filter="url(#featureGlow)"
                        style={{ pathLength: flowPathLength }}
                    />

                    {/* 분기선 - 각 기능 노드에서 출발하는 화살표 */}
                    <motion.path
                        d="M 150,190 C 250,195 400,210 540,230"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#featureArrow)"
                        style={{ pathLength: flowPathLength }}
                    />
                    <motion.path
                        d="M 850,510 C 750,515 600,530 460,550"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#featureArrow)"
                        style={{ pathLength: flowPathLength }}
                    />
                    <motion.path
                        d="M 150,810 C 250,815 400,830 540,850"
                        stroke="var(--color-primary)" strokeWidth="1.5" strokeOpacity="0.15"
                        strokeLinecap="round" markerEnd="url(#featureArrow)"
                        style={{ pathLength: flowPathLength }}
                    />

                    {/* 노드 */}
                    {[
                        { cx: 500, cy: 0 },     // 시작점 (스터디 섹션과 연결)
                        { cx: 150, cy: 190 },   // Feature 1
                        { cx: 500, cy: 350 },   // 중간 교차점
                        { cx: 850, cy: 510 },   // Feature 2
                        { cx: 150, cy: 810 },   // Feature 3
                        { cx: 500, cy: 1000 },  // 종료점
                    ].map((node, i) => (
                        <motion.circle
                            key={i}
                            cx={node.cx} cy={node.cy} r="5"
                            fill="var(--color-primary)" opacity="0.25"
                            style={{ pathLength: flowPathLength }}
                        />
                    ))}

                    {/* 방사형 퍼짐 라인 */}
                    {[
                        // 시작 (500,0) → 좌하단(메인 방향) + 좌우로 부채꼴 퍼짐
                        { d: 'M 500,0 C 420,40 300,80 200,130', opacity: 0.12 },
                        { d: 'M 500,0 C 450,60 350,140 280,200', opacity: 0.07 },
                        { d: 'M 500,0 C 360,30 200,50 100,80', opacity: 0.05 },
                        { d: 'M 500,0 C 560,40 660,70 740,90', opacity: 0.05 },

                        // Feature 1 (150,190) → 우측 퍼짐
                        { d: 'M 150,190 C 270,200 410,230 520,270', opacity: 0.12 },
                        { d: 'M 150,190 C 250,220 380,270 480,320', opacity: 0.08 },
                        { d: 'M 150,190 C 290,190 440,200 560,220', opacity: 0.06 },

                        // 중간 (500,350) → 우측 퍼짐
                        { d: 'M 500,350 C 610,360 740,400 830,450', opacity: 0.12 },
                        { d: 'M 500,350 C 590,380 720,430 810,490', opacity: 0.08 },
                        { d: 'M 500,350 C 630,350 780,370 890,400', opacity: 0.06 },

                        // Feature 2 (850,510) → 좌하단 퍼짐
                        { d: 'M 850,510 C 740,550 590,600 490,650', opacity: 0.12 },
                        { d: 'M 850,510 C 760,560 630,630 550,700', opacity: 0.08 },
                        { d: 'M 850,510 C 720,530 560,560 430,600', opacity: 0.06 },

                        // Feature 3 (150,810) → 우하단 퍼짐
                        { d: 'M 150,810 C 270,850 400,900 490,950', opacity: 0.12 },
                        { d: 'M 150,810 C 250,860 370,920 460,970', opacity: 0.08 },
                        { d: 'M 150,810 C 290,830 440,860 560,890', opacity: 0.06 },

                        // 종료 (500,1000) → 아래로 퍼짐
                        { d: 'M 500,1000 C 420,1020 310,1030 220,1030', opacity: 0.08 },
                        { d: 'M 500,1000 C 580,1020 690,1030 780,1030', opacity: 0.08 },
                    ].map((line, i) => (
                        <motion.path
                            key={`spread-${i}`}
                            d={line.d}
                            stroke="var(--color-primary)"
                            strokeWidth="1"
                            strokeOpacity={line.opacity}
                            strokeLinecap="round"
                            fill="none"
                            style={{ pathLength: flowPathLength }}
                        />
                    ))}
                </motion.svg>

                <div className="relative z-10 max-w-6xl mx-auto px-8">
                    <div className="text-center mb-24">
                        <h3 className="text-6xl font-black text-gray-900 mb-4">
                            Effortless Learning
                        </h3>
                        <p className="text-xl text-gray-500">
                            스터디 참여만으로 모든 학습 관리가 자동화됩니다
                        </p>
                    </div>
                    <div className="space-y-32">
                        <FeatureWithMockup
                            title="자동 대화 요약"
                            description="AI 기술로 스터디 대화를 실시간으로 기록하고 핵심 내용을 자동으로 요약합니다. 더 이상 수동으로 노트를 작성할 필요가 없습니다."
                            align="left"
                            mockup={<STTMockup />}
                        />
                        <FeatureWithMockup
                            title="AI 퀴즈 생성"
                            description="학습한 내용을 바탕으로 AI가 복습용 퀴즈를 자동 생성해 학습 효과를 높입니다. 스터디 후 바로 복습할 수 있습니다."
                            align="right"
                            mockup={<QuizMockup />}
                        />
                        <FeatureWithMockup
                            title="학습 데이터 자산화"
                            description="모든 스터디 기록이 나만의 학습 데이터베이스로 축적되어 언제든 다시 확인 가능합니다. 여러분의 학습 여정을 기록합니다."
                            align="left"
                            mockup={<ArchiveMockup />}
                        />
                    </div>
                </div>
            </section>

            {/* 최종 CTA 섹션 */}
            <section className="relative py-56 overflow-hidden">
                {/* 배경 그라데이션 */}
                <div className="absolute inset-0 bg-gradient-to-br from-primary via-primary/90 to-secondary" />

                {/* 배경 패턴 */}
                <div className="absolute inset-0 opacity-10">
                    <div className="absolute top-20 left-20 w-72 h-72 bg-white rounded-full blur-3xl" />
                    <div className="absolute bottom-20 right-20 w-96 h-96 bg-white rounded-full blur-3xl" />
                </div>

                <div className="relative max-w-4xl mx-auto px-8 text-center">
                    <motion.div
                        initial={{ opacity: 0, y: 40 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.8 }}
                        className="space-y-8"
                    >
                        {/* 화살표 + 타이틀 */}
                        <div>
                            {/* 화살표 아이콘 - 가로 중앙 */}
                            <motion.div
                                animate={isLaunching
                                    ? { x: 150, y: -120, opacity: 0, scale: 0.5 }
                                    : { x: 0, y: 0, opacity: 1, scale: 1 }
                                }
                                transition={{
                                    duration: 0.7,
                                    ease: [0.4, 0, 0.2, 1]
                                }}
                                className="flex justify-center mb-6"
                            >
                                <ArrowUpRight
                                    className="text-white"
                                    size={48}
                                    strokeWidth={2.5}
                                />
                            </motion.div>

                            <h3 className="text-5xl md:text-6xl font-black text-white mb-4 leading-tight">
                                지금 바로
                            </h3>
                            <h3 className="text-5xl md:text-6xl font-black text-white/90 mb-0 leading-tight">
                                시작하세요
                            </h3>
                        </div>

                        {/* 설명 */}
                        <p className="text-xl text-white/80 max-w-xl mx-auto leading-relaxed">
                            복잡한 학습 관리는 이제 그만.
                            <br />
                            스터디에만 집중하세요.
                        </p>

                        {/* CTA 버튼 */}
                        <motion.div
                            initial={{ opacity: 0, y: 20 }}
                            whileInView={{ opacity: 1, y: 0 }}
                            viewport={{ once: true }}
                            transition={{ delay: 0.3, duration: 0.6 }}
                        >
                            <Button
                                variant="secondary"
                                size="xl"
                                onClick={handleStartClick}
                                disabled={isLaunching}
                                className={cn(
                                    "bg-white hover:bg-gray-50 text-primary font-bold px-10 py-4 shadow-2xl transition-all",
                                    !isLaunching && "hover:shadow-white/25 hover:scale-105"
                                )}
                            >
                                무료로 시작하기
                            </Button>
                        </motion.div>

                        {/* 부가 정보 */}
                        <p className="text-sm text-white/60">
                            가입 후 모든 기능을 무료로 이용하세요
                        </p>
                    </motion.div>
                </div>
            </section>
        </div>
    );
};

// 목업이 포함된 특징 섹션 컴포넌트
interface FeatureWithMockupProps {
    title: string;
    description: string;
    align: 'left' | 'right';
    mockup: React.ReactNode;
}

const FeatureWithMockup: React.FC<FeatureWithMockupProps> = ({ title, description, align, mockup }) => (
    <motion.div
        initial={{ opacity: 0, y: 60 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        className={cn(
            'grid grid-cols-1 lg:grid-cols-2 gap-12 items-center',
            align === 'right' && 'lg:[direction:rtl] lg:*:[direction:ltr]'
        )}
    >
        {/* 텍스트 영역 */}
        <div className={cn(
            'space-y-6',
            align === 'right' ? 'lg:text-right' : 'lg:text-left'
        )}>
            <h4 className="text-4xl lg:text-5xl font-black text-gray-900 leading-tight mb-0">
                {title}
            </h4>
            <p className="text-lg lg:text-xl text-gray-600 leading-relaxed">
                {description}
            </p>
        </div>
        {/* 목업 영역 */}
        <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.2 }}
        >
            {mockup}
        </motion.div>
    </motion.div>
);

// STT 미팅 리포트 목업 - 실제 STTReportPage 기반
const STTMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-2xl border border-gray-200/60 overflow-hidden max-w-xl mx-auto">
        {/* 윈도우 타이틀바 */}
        <div className="px-4 py-2.5 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
            <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-red-400" />
                <div className="w-3 h-3 rounded-full bg-yellow-400" />
                <div className="w-3 h-3 rounded-full bg-green-400" />
            </div>
            <div className="flex-1 text-center">
                <span className="text-xs text-gray-400 font-medium">미팅 리포트</span>
            </div>
        </div>

        <div className="flex" style={{ minHeight: 340 }}>
            {/* 좌측: 미팅 리스트 사이드바 */}
            <div className="w-[38%] border-r border-gray-100 bg-gray-50/70 flex flex-col">
                {/* 스터디 선택 */}
                <div className="px-3 pt-3 pb-2">
                    <div className="flex items-center gap-2 px-2.5 py-2 bg-white rounded-lg border border-gray-200 text-xs text-gray-700">
                        <BookOpen size={13} className="text-primary" />
                        <span className="font-medium">React 스터디</span>
                    </div>
                </div>

                {/* 미팅 목록 */}
                {[
                    { title: '주간 회의 #12', date: '01-25', active: true },
                    { title: 'Hooks 심화 학습', date: '01-22', active: false },
                    { title: '코드리뷰 세션', date: '01-18', active: false },
                ].map((item, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, x: -10 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: i * 0.08 }}
                        className={cn(
                            'px-3 py-3 cursor-pointer border-b border-gray-100/80',
                            item.active
                                ? 'bg-white border-l-[3px] border-l-primary'
                                : 'hover:bg-gray-100/60'
                        )}
                    >
                        <div className="font-semibold text-xs text-gray-900 mb-0.5">{item.title}</div>
                        <div className="flex items-center gap-1 text-[10px] text-gray-400">
                            <Calendar size={10} />
                            <span>{item.date}</span>
                        </div>
                    </motion.div>
                ))}

                {/* 탭 네비게이션 */}
                <div className="mt-auto border-t border-gray-200 bg-white">
                    {[
                        { label: '요약', active: true },
                        { label: '전체 기록', active: false },
                        { label: '통계', active: false },
                    ].map((tab, i) => (
                        <div
                            key={i}
                            className={cn(
                                'px-3 py-2 text-[11px] cursor-pointer border-b border-gray-50',
                                tab.active
                                    ? 'text-primary font-bold bg-primary/5 border-l-[3px] border-l-primary'
                                    : 'text-gray-500 hover:bg-gray-50'
                            )}
                        >
                            {tab.label}
                        </div>
                    ))}
                </div>
            </div>

            {/* 우측: 리포트 상세 */}
            <div className="flex-1 p-4 overflow-hidden">
                <motion.div
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true }}
                    className="space-y-3"
                >
                    {/* 제목 + 메타데이터 */}
                    <div>
                        <h5 className="text-base font-bold text-gray-900 mb-1.5">주간 회의 #12 - React Hooks</h5>
                        <div className="flex items-center gap-3 text-[11px] text-gray-400">
                            <span className="flex items-center gap-1"><Calendar size={11} /> 2025-01-25</span>
                            <span className="flex items-center gap-1"><Clock size={11} /> 1시간 30분</span>
                            <span className="flex items-center gap-1"><Users size={11} /> 5명</span>
                        </div>
                    </div>

                    {/* 핵심 키워드 */}
                    <div className="flex flex-wrap gap-1.5">
                        {['#useEffect', '#의존성배열', '#클린업', '#커스텀훅', '#메모이제이션'].map((tag, i) => (
                            <motion.span
                                key={i}
                                initial={{ opacity: 0, scale: 0.8 }}
                                whileInView={{ opacity: 1, scale: 1 }}
                                viewport={{ once: true }}
                                transition={{ delay: 0.1 + i * 0.04 }}
                                className="px-2 py-1 bg-primary/8 text-primary text-[10px] font-semibold rounded-full"
                            >
                                {tag}
                            </motion.span>
                        ))}
                    </div>

                    {/* AI 분석 결과 */}
                    <div className="bg-gray-50 rounded-xl p-3 border border-gray-100">
                        <div className="flex items-center gap-1.5 mb-1.5">
                            <Sparkles size={12} className="text-primary" />
                            <span className="text-[11px] font-bold text-primary">AI 분석 결과</span>
                        </div>
                        <p className="text-xs text-gray-600 leading-relaxed">
                            useEffect 의존성 배열의 올바른 사용법과 클린업 함수의 실행 타이밍에 대해 심도 있게 논의했습니다. 커스텀 훅을 활용한 로직 재사용 패턴도 공유되었습니다.
                        </p>
                    </div>

                    {/* 주요 내용 */}
                    <div>
                        <div className="flex items-center gap-1.5 mb-2">
                            <span className="text-xs font-bold text-gray-700">주요 내용</span>
                            <span className="px-1.5 py-0.5 bg-primary/10 text-primary text-[9px] font-semibold rounded">AI 분석</span>
                        </div>
                        <div className="space-y-1.5">
                            {[
                                'useEffect 의존성 배열에 객체/배열 넣을 때 주의사항',
                                '클린업 함수는 언마운트 + 다음 effect 전에 실행',
                                'useMemo/useCallback으로 불필요한 리렌더링 방지',
                            ].map((item, i) => (
                                <motion.div
                                    key={i}
                                    initial={{ opacity: 0, x: -8 }}
                                    whileInView={{ opacity: 1, x: 0 }}
                                    viewport={{ once: true }}
                                    transition={{ delay: 0.2 + i * 0.06 }}
                                    className="flex items-start gap-2 text-xs text-gray-600"
                                >
                                    <ChevronRight size={12} className="text-primary mt-0.5 flex-shrink-0" />
                                    <span>{item}</span>
                                </motion.div>
                            ))}
                        </div>
                    </div>

                    {/* 액션 아이템 */}
                    <div className="bg-accent/5 rounded-lg p-2.5 border border-accent/10">
                        <div className="flex items-center gap-1.5 mb-1.5">
                            <ListChecks size={12} className="text-accent" />
                            <span className="text-[11px] font-bold text-accent">액션 아이템</span>
                            <span className="ml-auto text-[10px] text-gray-400">1 / 3 완료</span>
                        </div>
                        <div className="space-y-1">
                            {[
                                { text: 'useEffect 패턴 정리 문서 작성', done: true },
                                { text: '커스텀 훅 예제 코드 공유', done: false },
                            ].map((item, i) => (
                                <div key={i} className="flex items-center gap-2 text-[11px]">
                                    <div className={cn(
                                        'w-3.5 h-3.5 rounded border flex items-center justify-center flex-shrink-0',
                                        item.done
                                            ? 'bg-accent border-accent text-white'
                                            : 'border-gray-300'
                                    )}>
                                        {item.done && <span className="text-[8px]">✓</span>}
                                    </div>
                                    <span className={cn(item.done ? 'text-gray-400 line-through' : 'text-gray-600')}>
                                        {item.text}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                </motion.div>
            </div>
        </div>
    </div>
);

// AI 복습 퀴즈 목업 - 실제 QuizSession/QuestionCard 기반
const QuizMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-2xl border border-gray-200/60 overflow-hidden max-w-xl mx-auto">
        {/* 윈도우 타이틀바 */}
        <div className="px-4 py-2.5 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
            <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-red-400" />
                <div className="w-3 h-3 rounded-full bg-yellow-400" />
                <div className="w-3 h-3 rounded-full bg-green-400" />
            </div>
            <div className="flex-1 text-center">
                <span className="text-xs text-gray-400 font-medium">AI 복습 퀴즈</span>
            </div>
        </div>

        {/* 상단: 진행률 바 */}
        <div className="px-5 pt-4 pb-2">
            <div className="flex items-center justify-between mb-1.5">
                <span className="text-[11px] text-gray-500 font-medium">문제 진행</span>
                <span className="text-[11px] font-bold text-primary">3 / 10</span>
            </div>
            <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <motion.div
                    className="h-full rounded-full"
                    style={{ background: 'linear-gradient(90deg, var(--color-primary), var(--color-secondary))' }}
                    initial={{ width: 0 }}
                    whileInView={{ width: '30%' }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.8, delay: 0.2 }}
                />
            </div>
            <div className="text-right mt-1">
                <span className="text-[10px] text-gray-400">30% 완료</span>
            </div>
        </div>

        {/* 퀴즈 카드 */}
        <div className="px-5 pb-5">
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="bg-gray-50/50 rounded-xl p-4 border border-gray-100"
            >
                {/* 배지 행 */}
                <div className="flex items-center gap-2 mb-3">
                    <span className="px-2 py-0.5 bg-primary text-white text-[10px] font-bold rounded-md">Q3</span>
                    <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-[10px] font-medium rounded-md">단일선택</span>
                    <span className="px-2 py-0.5 bg-yellow-50 text-yellow-600 text-[10px] font-medium rounded-md border border-yellow-200">Medium</span>
                    <span className="px-2 py-0.5 bg-secondary/10 text-secondary text-[10px] font-medium rounded-md">React</span>
                </div>

                {/* 문제 텍스트 */}
                <p className="text-sm font-semibold text-gray-900 leading-relaxed mb-4">
                    useEffect의 클린업 함수는 언제 실행되나요?
                </p>

                {/* 선택지 */}
                <div className="space-y-2.5">
                    {[
                        { id: 'A', text: '컴포넌트가 처음 마운트될 때', selected: false },
                        { id: 'B', text: '언마운트 시 또는 다음 effect 실행 전', selected: true, correct: true },
                        { id: 'C', text: '렌더링이 완료된 직후', selected: false },
                        { id: 'D', text: 'setState가 호출될 때', selected: false },
                    ].map((option, i) => (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, x: -8 }}
                            whileInView={{ opacity: 1, x: 0 }}
                            viewport={{ once: true }}
                            transition={{ delay: 0.1 + i * 0.06 }}
                            className={cn(
                                'flex items-center gap-3 px-3.5 py-3 rounded-xl border-2 cursor-pointer transition-all',
                                option.correct
                                    ? 'border-primary bg-primary/5'
                                    : 'border-gray-100 bg-white hover:border-gray-200'
                            )}
                        >
                            {option.correct ? (
                                <CheckCircle2 size={18} className="text-primary flex-shrink-0" />
                            ) : (
                                <Circle size={18} className="text-gray-300 flex-shrink-0" />
                            )}
                            <span className={cn(
                                'text-xs',
                                option.correct ? 'text-primary font-semibold' : 'text-gray-700'
                            )}>
                                {option.text}
                            </span>
                        </motion.div>
                    ))}
                </div>
            </motion.div>

            {/* 네비게이션 버튼 */}
            <div className="flex items-center justify-between mt-4">
                <div className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-gray-200 text-xs text-gray-500 cursor-pointer hover:bg-gray-50">
                    <span>이전</span>
                </div>
                <div
                    className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs text-white font-semibold cursor-pointer"
                    style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-secondary))' }}
                >
                    <span>다음</span>
                    <ChevronRight size={14} />
                </div>
            </div>
        </div>
    </div>
);

// 학습 아카이브 목업 - 실제 LearningArchivePage 기반
const ArchiveMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-2xl border border-gray-200/60 overflow-hidden max-w-xl mx-auto">
        {/* 윈도우 타이틀바 */}
        <div className="px-4 py-2.5 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
            <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-red-400" />
                <div className="w-3 h-3 rounded-full bg-yellow-400" />
                <div className="w-3 h-3 rounded-full bg-green-400" />
            </div>
            <div className="flex-1 text-center">
                <span className="text-xs text-gray-400 font-medium">학습 보관함</span>
            </div>
        </div>

        <div className="flex" style={{ minHeight: 360 }}>
            {/* 좌측: 탭 네비게이션 */}
            <div className="w-[30%] border-r border-gray-100 bg-gray-50/70 flex flex-col py-2">
                {[
                    { icon: FileText, label: '전체 보관함', count: 24, active: true },
                    { icon: Clock, label: '최근 학습', count: 5, active: false },
                    { icon: Star, label: '즐겨찾기', count: 8, active: false },
                    { icon: BarChart3, label: '학습 통계', count: null, active: false },
                ].map((tab, i) => {
                    const TabIcon = tab.icon;
                    return (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, x: -8 }}
                            whileInView={{ opacity: 1, x: 0 }}
                            viewport={{ once: true }}
                            transition={{ delay: i * 0.05 }}
                            className={cn(
                                'flex items-center gap-2 px-3 py-2.5 mx-1.5 rounded-lg cursor-pointer text-[11px] mb-0.5',
                                tab.active
                                    ? 'bg-white text-primary font-bold shadow-sm border border-gray-100'
                                    : 'text-gray-500 hover:bg-gray-100/60'
                            )}
                        >
                            <TabIcon size={13} />
                            <span className="flex-1 truncate">{tab.label}</span>
                            {tab.count !== null && (
                                <span className={cn(
                                    'px-1.5 py-0.5 rounded-full text-[9px] font-semibold',
                                    tab.active ? 'bg-primary/10 text-primary' : 'bg-gray-200 text-gray-500'
                                )}>
                                    {tab.count}
                                </span>
                            )}
                        </motion.div>
                    );
                })}
            </div>

            {/* 우측: 콘텐츠 */}
            <div className="flex-1 p-4 overflow-hidden">
                <motion.div
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true }}
                    className="space-y-3"
                >
                    {/* 통계 요약 카드 */}
                    <div className="grid grid-cols-3 gap-2">
                        {[
                            { label: '총 학습', value: '24', icon: FileText, color: 'primary' },
                            { label: '퀴즈 풀이', value: '156', icon: Brain, color: 'secondary' },
                            { label: '평균 정답률', value: '87%', icon: BarChart3, color: 'accent' },
                        ].map((stat, i) => {
                            const StatIcon = stat.icon;
                            return (
                                <motion.div
                                    key={i}
                                    initial={{ opacity: 0, y: 8 }}
                                    whileInView={{ opacity: 1, y: 0 }}
                                    viewport={{ once: true }}
                                    transition={{ delay: i * 0.08 }}
                                    className="bg-gray-50 rounded-lg p-2.5 border border-gray-100 text-center"
                                >
                                    <StatIcon size={14} className={`text-${stat.color} mx-auto mb-1`} />
                                    <div className="text-base font-black text-gray-900">{stat.value}</div>
                                    <div className="text-[9px] text-gray-500">{stat.label}</div>
                                </motion.div>
                            );
                        })}
                    </div>

                    {/* 검색 + 태그 필터 */}
                    <div>
                        <div className="relative mb-2">
                            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" size={13} />
                            <div className="w-full pl-8 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-[11px] text-gray-400">
                                제목, 스터디명 검색...
                            </div>
                        </div>
                        <div className="flex flex-wrap gap-1.5">
                            {['전체', 'React', 'TypeScript', 'Network', 'OS'].map((tag, i) => (
                                <span
                                    key={tag}
                                    className={cn(
                                        'px-2 py-1 rounded-full text-[10px] font-medium',
                                        i === 0
                                            ? 'bg-primary text-white'
                                            : 'bg-gray-100 text-gray-500'
                                    )}
                                >
                                    {tag}
                                </span>
                            ))}
                        </div>
                    </div>

                    {/* 아카이브 아이템 */}
                    <div className="space-y-2">
                        {[
                            { title: 'React Hooks 심화 정리', study: 'React 스터디', date: '01-25', tags: ['React', 'Hooks'], keyPoints: 5, quizzes: 10, score: 90 },
                            { title: 'TCP/IP 3-Way Handshake', study: 'CS 기초 스터디', date: '01-24', tags: ['Network', 'TCP'], keyPoints: 7, quizzes: 8, score: 75 },
                            { title: 'TypeScript 제네릭 패턴', study: 'TypeScript 스터디', date: '01-22', tags: ['TypeScript'], keyPoints: 4, quizzes: 6, score: 83 },
                        ].map((archive, i) => (
                            <motion.div
                                key={i}
                                initial={{ opacity: 0, x: -8 }}
                                whileInView={{ opacity: 1, x: 0 }}
                                viewport={{ once: true }}
                                transition={{ delay: 0.15 + i * 0.08 }}
                                className="bg-gray-50/80 rounded-xl p-3 border border-gray-100 hover:shadow-md hover:bg-white transition-all cursor-pointer"
                            >
                                <div className="flex items-start justify-between mb-1">
                                    <h5 className="font-bold text-xs text-gray-900 mb-0">{archive.title}</h5>
                                    <span className="flex items-center gap-1 text-[10px] text-gray-400 flex-shrink-0">
                                        <Calendar size={10} />
                                        {archive.date}
                                    </span>
                                </div>
                                <p className="text-[10px] text-gray-500 mb-2">{archive.study}</p>
                                <div className="flex items-center justify-between">
                                    <div className="flex gap-1">
                                        {archive.tags.map((tag) => (
                                            <span
                                                key={tag}
                                                className="px-1.5 py-0.5 bg-primary/10 text-primary text-[9px] font-medium rounded-full"
                                            >
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                    <div className="flex items-center gap-2 text-[10px] text-gray-400">
                                        <span className="flex items-center gap-0.5">
                                            <Brain size={10} className="text-secondary" />
                                            {archive.quizzes}문제
                                        </span>
                                        <span className="flex items-center gap-0.5">
                                            <Hash size={10} className="text-accent" />
                                            {archive.keyPoints}
                                        </span>
                                        <span className={cn(
                                            'font-semibold',
                                            archive.score >= 85 ? 'text-green-500' : 'text-yellow-500'
                                        )}>
                                            {archive.score}%
                                        </span>
                                    </div>
                                </div>
                            </motion.div>
                        ))}
                    </div>
                </motion.div>
            </div>
        </div>
    </div>
);
