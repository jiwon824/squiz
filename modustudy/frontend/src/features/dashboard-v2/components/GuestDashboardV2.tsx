import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Brain, Video, TrendingUp, MessageSquare, FileText, BarChart3 } from 'lucide-react';
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
    },
    {
        id: 2,
        title: '스터디가 끝나면',
        subtitle: 'AI가 자동으로 정리해드려요',
        description: 'STT 기반 대화 요약본과 핵심 키워드 퀴즈를 자동 생성',
        icon: Brain,
    },
    {
        id: 3,
        title: '어디서든 함께하는',
        subtitle: '실시간 화상 스터디',
        description: '화면 공유와 음성 채팅이 결합된 강력한 학습 환경',
        icon: Video,
    },
];

export const GuestDashboardV2: React.FC = () => {
    const navigate = useNavigate();
    const [currentIndex, setCurrentIndex] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentIndex((prev) => (prev + 1) % HERO_MESSAGES.length);
        }, 5000);
        return () => clearInterval(timer);
    }, []);

    const currentHero = HERO_MESSAGES[currentIndex];
    const Icon = currentHero.icon;

    return (
        <div className="min-h-screen bg-white">
            {/* 히어로 섹션 - Apple 스타일 */}
            <section className="relative min-h-[60vh] flex items-center justify-center py-12 bg-gradient-to-b from-gray-50 to-white">
                <div className="max-w-5xl mx-auto px-8 text-center">
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
                    <h3 className="text-6xl font-black text-center text-gray-900 mb-0">
                        Effortless Learning
                    </h3>
                    <div className="space-y-40">
                        <FeatureWithMockup
                            title="자동 대화 요약"
                            description="STT 기술로 스터디 대화를 실시간으로 기록하고 핵심 내용을 자동으로 요약합니다. 더 이상 수동으로 노트를 작성할 필요가 없습니다."
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
            <section className="py-32 bg-gray-900 text-white">
                <div className="max-w-4xl mx-auto px-8 text-center">
                    <h3 className="text-6xl font-black mb-0 leading-tight">
                        지금 바로<br />시작하세요
                    </h3>
                    <p className="text-xl text-gray-300 mb-12 leading-relaxed">
                        복잡한 학습 관리는 이제 그만.<br />스터디에만 집중하세요.
                    </p>
                    <Button
                        variant="secondary"
                        size="xl"
                        onClick={() => navigate('/login')}
                        className="bg-white hover:bg-gray-100 text-gray-900 shadow-xl hover:shadow-2xl"
                    >
                        시작하기
                    </Button>
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

// STT 대화 요약 목업
const STTMockup: React.FC = () => (
    <div className="mockup-browser max-w-md mx-auto">
        <div className="mockup-browser-header">
            <div className="mockup-browser-dot red" />
            <div className="mockup-browser-dot yellow" />
            <div className="mockup-browser-dot green" />
            <div className="mockup-browser-url">modustudy.com/meeting/summary</div>
        </div>
        <div className="p-6 space-y-4 bg-gray-50">
            {/* 대화 내용 */}
            <div className="space-y-3">
                {[
                    { name: '김민수', text: 'React의 useEffect는 사이드 이펙트 처리에 사용돼요', time: '14:23' },
                    { name: '이지현', text: '의존성 배열에 뭘 넣어야 하는지 헷갈려요', time: '14:25' },
                    { name: '김민수', text: '변경을 감지하고 싶은 값만 넣으면 됩니다', time: '14:26' },
                ].map((chat, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, x: -20 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: i * 0.15 }}
                        className="flex items-start gap-3"
                    >
                        <div className="mockup-avatar flex-shrink-0" style={{
                            background: i % 2 === 0
                                ? 'linear-gradient(135deg, #4285F4, #34A853)'
                                : 'linear-gradient(135deg, #EA4335, #FBBC04)'
                        }} />
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                                <span className="font-semibold text-sm text-gray-900">{chat.name}</span>
                                <span className="text-xs text-gray-400">{chat.time}</span>
                            </div>
                            <p className="text-sm text-gray-600 mt-0.5">{chat.text}</p>
                        </div>
                    </motion.div>
                ))}
            </div>
            {/* AI 요약 */}
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.5 }}
                className="mt-6 p-4 bg-blue-50 rounded-xl border border-blue-100"
            >
                <div className="flex items-center gap-2 mb-2">
                    <Sparkles className="text-blue-500" size={16} />
                    <span className="font-semibold text-sm text-blue-700">AI 요약</span>
                </div>
                <p className="text-sm text-blue-600">
                    React useEffect 훅의 의존성 배열 사용법에 대해 논의. 변경 감지가 필요한 값만 배열에 포함하면 됨.
                </p>
            </motion.div>
        </div>
    </div>
);

// AI 퀴즈 목업
const QuizMockup: React.FC = () => (
    <div className="mockup-browser max-w-md mx-auto">
        <div className="mockup-browser-header">
            <div className="mockup-browser-dot red" />
            <div className="mockup-browser-dot yellow" />
            <div className="mockup-browser-dot green" />
            <div className="mockup-browser-url">modustudy.com/quiz</div>
        </div>
        <div className="p-6 bg-gray-50">
            <div className="space-y-4">
                <div className="flex items-center gap-2 mb-4">
                    <Brain className="text-purple-500" size={20} />
                    <span className="font-bold text-gray-900">AI 생성 퀴즈</span>
                    <span className="ml-auto text-sm text-gray-500">Q1 / 5</span>
                </div>
                <motion.div
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true }}
                    className="p-4 bg-white rounded-xl shadow-sm"
                >
                    <p className="font-medium text-gray-900 mb-4">
                        React useEffect의 의존성 배열이 비어있을 때 어떻게 동작하나요?
                    </p>
                    <div className="space-y-2">
                        {[
                            '컴포넌트가 마운트될 때만 실행',
                            '모든 렌더링마다 실행',
                            '아무 때도 실행되지 않음',
                            '에러 발생',
                        ].map((option, i) => (
                            <motion.button
                                key={i}
                                initial={{ opacity: 0, x: -10 }}
                                whileInView={{ opacity: 1, x: 0 }}
                                viewport={{ once: true }}
                                transition={{ delay: i * 0.1 }}
                                className={cn(
                                    'w-full p-3 text-left rounded-lg border transition-all text-sm',
                                    i === 0
                                        ? 'border-green-500 bg-green-50 text-green-700'
                                        : 'border-gray-200 hover:border-gray-300 text-gray-700'
                                )}
                            >
                                <span className="font-medium mr-2">{String.fromCharCode(65 + i)}.</span>
                                {option}
                                {i === 0 && <span className="ml-2 text-green-600">✓</span>}
                            </motion.button>
                        ))}
                    </div>
                </motion.div>
                {/* 진행 바 */}
                <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                    <motion.div
                        className="h-full bg-gradient-to-r from-purple-500 to-blue-500"
                        initial={{ width: 0 }}
                        whileInView={{ width: '20%' }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.8, delay: 0.3 }}
                    />
                </div>
            </div>
        </div>
    </div>
);

// 학습 아카이브 목업
const ArchiveMockup: React.FC = () => (
    <div className="mockup-browser max-w-md mx-auto">
        <div className="mockup-browser-header">
            <div className="mockup-browser-dot red" />
            <div className="mockup-browser-dot yellow" />
            <div className="mockup-browser-dot green" />
            <div className="mockup-browser-url">modustudy.com/archive</div>
        </div>
        <div className="p-6 bg-gray-50">
            {/* 통계 카드 */}
            <div className="grid grid-cols-3 gap-3 mb-4">
                {[
                    { label: '총 학습 시간', value: '127h', icon: BarChart3, color: 'blue' },
                    { label: '완료한 퀴즈', value: '89개', icon: Brain, color: 'purple' },
                    { label: '참여 스터디', value: '12개', icon: MessageSquare, color: 'green' },
                ].map((stat, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, y: 10 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: i * 0.1 }}
                        className="p-3 bg-white rounded-xl shadow-sm text-center"
                    >
                        <stat.icon className={cn(
                            'mx-auto mb-1',
                            stat.color === 'blue' && 'text-blue-500',
                            stat.color === 'purple' && 'text-purple-500',
                            stat.color === 'green' && 'text-green-500',
                        )} size={18} />
                        <div className="text-lg font-bold text-gray-900">{stat.value}</div>
                        <div className="text-xs text-gray-500">{stat.label}</div>
                    </motion.div>
                ))}
            </div>
            {/* 최근 기록 */}
            <div className="space-y-3">
                <div className="flex items-center justify-between">
                    <span className="font-semibold text-sm text-gray-900">최근 학습 기록</span>
                    <FileText className="text-gray-400" size={16} />
                </div>
                {[
                    { title: 'React Hooks 심화', date: '오늘', score: 95 },
                    { title: 'TypeScript 기초', date: '어제', score: 88 },
                    { title: 'Next.js 라우팅', date: '2일 전', score: 92 },
                ].map((record, i) => (
                    <motion.div
                        key={i}
                        initial={{ opacity: 0, x: -10 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: 0.2 + i * 0.1 }}
                        className="flex items-center justify-between p-3 bg-white rounded-lg shadow-sm"
                    >
                        <div>
                            <div className="font-medium text-sm text-gray-900">{record.title}</div>
                            <div className="text-xs text-gray-500">{record.date}</div>
                        </div>
                        <div className={cn(
                            'px-2 py-1 rounded-full text-xs font-semibold',
                            record.score >= 90 ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'
                        )}>
                            {record.score}점
                        </div>
                    </motion.div>
                ))}
            </div>
        </div>
    </div>
);
