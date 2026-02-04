import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Brain, Video, TrendingUp, MessageSquare, FileText, BarChart3, ArrowUpRight } from 'lucide-react';
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

    return (
        <div className="min-h-screen bg-white">
            {/* 히어로 섹션 - Apple 스타일 */}
            <section
                className="relative min-h-[60vh] flex items-center justify-center py-12"
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
            <section className="py-24 bg-gray-50">
                <div className="max-w-7xl mx-auto px-8">
                    <div className="text-center mb-16">
                        <h3 className="text-5xl font-black text-gray-900 mb-0">
                            지금 뜨는 스터디
                        </h3>
                        <p className="text-xl text-gray-600">
                            다른 사람들이 참여하고 있는 인기 스터디를 확인해보세요
                        </p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
                        {mockStudies.slice(0, 4).map((study) => (
                            <StudyCardContentV2
                                key={study.id}
                                study={study}
                                variant="card"
                                onClick={(studyId) => navigate(`/study/${studyId}`)}
                            />
                        ))}
                    </div>

                    <div className="text-center">
                        <Button
                            variant="text"
                            size="lg"
                            onClick={() => navigate('/study')}
                            rightIcon={<TrendingUp size={18} />}
                        >
                            전체 보기
                        </Button>
                    </div>
                </div>
            </section>

            {/* 서비스 특징 섹션 */}
            <section className="py-32 bg-white overflow-hidden">
                <div className="max-w-6xl mx-auto px-8">
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
            <section className="relative py-40 overflow-hidden">
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

// STT 미팅 리포트 목업 - 실제 위젯 스타일
const STTMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden max-w-lg mx-auto">
        {/* 헤더 */}
        <div className="px-6 pt-6 pb-4 flex items-center gap-3 border-b border-gray-50">
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                <FileText className="text-primary" size={20} />
            </div>
            <div>
                <h4 className="text-lg font-bold text-gray-900 mb-0">미팅 리포트</h4>
                <p className="text-xs text-gray-500">최근 스터디 요약</p>
            </div>
        </div>

        <div className="flex">
            {/* 좌측: 미팅 리스트 */}
            <div className="w-1/3 border-r border-gray-100 bg-gray-50/50">
                {[
                    { study: 'React 스터디', meeting: '주간 회의', active: true },
                    { study: 'TypeScript', meeting: '제네릭 학습', active: false },
                ].map((item, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, x: -10 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: i * 0.1 }}
                        className={cn(
                            'p-4 border-b border-gray-100 cursor-pointer',
                            item.active ? 'bg-primary/5 border-l-4 border-l-primary' : 'hover:bg-gray-100'
                        )}
                    >
                        <div className="font-bold text-sm text-gray-900">{item.study}</div>
                        <div className="text-xs text-gray-500 truncate">{item.meeting}</div>
                    </motion.div>
                ))}
            </div>

            {/* 우측: 리포트 상세 */}
            <div className="flex-1 p-5">
                <motion.div
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true }}
                >
                    <h5 className="text-lg font-bold text-gray-900 mb-1">주간 회의 - React Hooks</h5>
                    <div className="flex items-center gap-3 text-xs text-gray-500 mb-4">
                        <span>2024-01-25</span>
                        <span>5명 참여</span>
                    </div>

                    {/* 요약 */}
                    <div className="bg-gray-50 rounded-xl p-4 mb-4">
                        <div className="font-bold text-sm text-gray-700 mb-1">📝 요약</div>
                        <p className="text-sm text-gray-600">
                            useEffect 의존성 배열 사용법과 클린업 함수 동작 원리에 대해 논의
                        </p>
                    </div>

                    {/* 키워드 */}
                    <div className="flex flex-wrap gap-2">
                        {['React Hooks', 'useEffect', '클린업'].map((tag, i) => (
                            <motion.span
                                key={i}
                                initial={{ opacity: 0, scale: 0.8 }}
                                whileInView={{ opacity: 1, scale: 1 }}
                                viewport={{ once: true }}
                                transition={{ delay: 0.2 + i * 0.05 }}
                                className="px-3 py-1 bg-primary/10 text-primary text-xs font-medium rounded-full"
                            >
                                {tag}
                            </motion.span>
                        ))}
                    </div>
                </motion.div>
            </div>
        </div>
    </div>
);

// AI 복습 퀴즈 목업 - 실제 위젯 스타일
const QuizMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden max-w-lg mx-auto">
        {/* 헤더 */}
        <div className="px-6 pt-6 pb-4 flex items-center justify-between border-b border-gray-50">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-secondary/10 flex items-center justify-center">
                    <Brain className="text-secondary" size={20} />
                </div>
                <div>
                    <h4 className="text-lg font-bold text-gray-900 mb-0">AI 복습 퀴즈</h4>
                    <p className="text-xs text-gray-500">스터디 내용 기반 자동 생성</p>
                </div>
            </div>
            <div className="text-sm font-bold text-gray-900">2 / 5</div>
        </div>

        {/* 퀴즈 콘텐츠 */}
        <div className="p-6">
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
            >
                {/* 문제 */}
                <div className="mb-6">
                    <span className="inline-block px-2 py-1 bg-secondary/10 text-secondary text-xs font-medium rounded-lg mb-3">
                        객관식
                    </span>
                    <p className="text-lg font-medium text-gray-900">
                        useEffect의 클린업 함수는 언제 실행되나요?
                    </p>
                </div>

                {/* 선택지 */}
                <div className="space-y-3 mb-6">
                    {[
                        { text: '컴포넌트 마운트 시', selected: false },
                        { text: '언마운트 시 또는 다음 effect 실행 전', selected: true, correct: true },
                        { text: '렌더링 직후', selected: false },
                        { text: '상태 변경 시', selected: false },
                    ].map((option, i) => (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, x: -10 }}
                            whileInView={{ opacity: 1, x: 0 }}
                            viewport={{ once: true }}
                            transition={{ delay: i * 0.08 }}
                            className={cn(
                                'p-4 rounded-xl border-2 transition-all cursor-pointer',
                                option.correct
                                    ? 'border-accent bg-accent/5'
                                    : 'border-gray-100 hover:border-gray-200'
                            )}
                        >
                            <div className="flex items-center gap-3">
                                <div className={cn(
                                    'w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs font-bold',
                                    option.correct
                                        ? 'border-accent bg-accent text-white'
                                        : 'border-gray-300 text-gray-400'
                                )}>
                                    {option.correct ? '✓' : String.fromCharCode(65 + i)}
                                </div>
                                <span className={cn(
                                    'text-sm',
                                    option.correct ? 'text-accent font-medium' : 'text-gray-700'
                                )}>
                                    {option.text}
                                </span>
                            </div>
                        </motion.div>
                    ))}
                </div>

                {/* 진행률 */}
                <div className="flex items-center gap-3">
                    <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                        <motion.div
                            className="h-full bg-secondary rounded-full"
                            initial={{ width: 0 }}
                            whileInView={{ width: '40%' }}
                            viewport={{ once: true }}
                            transition={{ duration: 0.8, delay: 0.3 }}
                        />
                    </div>
                    <span className="text-xs text-gray-500 font-medium">40%</span>
                </div>
            </motion.div>
        </div>
    </div>
);

// 학습 아카이브 목업 - 실제 위젯 스타일
const ArchiveMockup: React.FC = () => (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden max-w-lg mx-auto">
        {/* 헤더 */}
        <div className="px-6 pt-6 pb-4 flex items-center justify-between border-b border-gray-50">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-accent/10 flex items-center justify-center">
                    <BarChart3 className="text-accent" size={20} />
                </div>
                <div>
                    <h4 className="text-lg font-bold text-gray-900 mb-0">학습 보관함</h4>
                    <p className="text-xs text-gray-500">과거 스터디 기록</p>
                </div>
            </div>
            <div className="flex items-center gap-1">
                <div className="p-2 rounded-lg bg-primary/10 text-primary">
                    <BarChart3 size={16} />
                </div>
                <div className="p-2 rounded-lg text-gray-400">
                    <FileText size={16} />
                </div>
            </div>
        </div>

        <div className="p-6">
            {/* 검색창 */}
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="relative mb-4"
            >
                <MessageSquare className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <div className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-400">
                    제목, 내용 검색...
                </div>
            </motion.div>

            {/* 태그 필터 */}
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.1 }}
                className="flex flex-wrap gap-2 mb-5"
            >
                {['전체', 'React', 'TypeScript', 'Zustand'].map((tag, i) => (
                    <span
                        key={tag}
                        className={cn(
                            'px-3 py-1.5 rounded-full text-xs font-medium',
                            i === 0
                                ? 'bg-primary text-white'
                                : 'bg-gray-100 text-gray-600'
                        )}
                    >
                        {tag}
                    </span>
                ))}
            </motion.div>

            {/* 아카이브 아이템 */}
            <div className="space-y-3">
                {[
                    { title: 'React Hooks 정리', study: 'React 스터디', date: '01-25', tags: ['React', 'Hooks'], keyPoints: 5 },
                    { title: 'TypeScript 제네릭', study: 'TypeScript 스터디', date: '01-24', tags: ['TypeScript'], keyPoints: 7 },
                ].map((archive, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, x: -10 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: 0.2 + i * 0.1 }}
                        className="bg-gray-50 rounded-xl p-4 border border-gray-100 hover:shadow-md hover:bg-white transition-all cursor-pointer"
                    >
                        <div className="flex items-start justify-between mb-2">
                            <h5 className="font-bold text-gray-900 mb-0">{archive.title}</h5>
                            <span className="text-xs text-gray-500">{archive.date}</span>
                        </div>
                        <p className="text-xs text-gray-500 mb-3">{archive.study}</p>
                        <div className="flex items-center justify-between">
                            <div className="flex gap-1.5">
                                {archive.tags.map((tag) => (
                                    <span
                                        key={tag}
                                        className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full"
                                    >
                                        {tag}
                                    </span>
                                ))}
                            </div>
                            <span className="text-xs text-gray-500">키포인트 {archive.keyPoints}</span>
                        </div>
                    </motion.div>
                ))}
            </div>
        </div>
    </div>
);
