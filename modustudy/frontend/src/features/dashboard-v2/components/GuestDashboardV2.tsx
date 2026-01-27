import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Brain, Video, ArrowRight, TrendingUp } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';

import { mockStudies } from '../../study/mockData';
import StudyCardContentV2 from '../../study/components/StudyCardContentV2';

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
                                <h1 className="text-6xl font-black text-gray-900 tracking-tight leading-tight">
                                    {currentHero.title}
                                </h1>
                                <h2 className="text-6xl font-black text-primary tracking-tight leading-tight">
                                    {currentHero.subtitle}
                                </h2>
                            </div>

                            {/* 설명 */}
                            <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed">
                                {currentHero.description}
                            </p>

                            {/* CTA 버튼 */}
                            <div className="flex items-center justify-center gap-4 pt-8">
                                <Button
                                    variant="primary"
                                    size="lg"
                                    onClick={() => navigate('/login')}
                                    className="shadow-lg hover:shadow-xl"
                                >
                                    무료로 시작하기
                                </Button>
                                <Button
                                    variant="secondary"
                                    size="lg"
                                    onClick={() => navigate('/study')}
                                >
                                    스터디 둘러보기
                                </Button>
                            </div>
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
                        <h3 className="text-5xl font-black text-gray-900 mb-4">
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
            <section className="py-32 bg-white">
                <div className="max-w-5xl mx-auto px-8">
                    <h3 className="text-6xl font-black text-center text-gray-900 mb-24">
                        Effortless Learning
                    </h3>
                    <div className="space-y-32">
                        <FeatureSection
                            title="자동 대화 요약"
                            description="STT 기술로 스터디 대화를 실시간으로 기록하고 핵심 내용을 자동으로 요약합니다. 더 이상 수동으로 노트를 작성할 필요가 없습니다."
                            align="left"
                        />
                        <FeatureSection
                            title="AI 퀴즈 생성"
                            description="학습한 내용을 바탕으로 AI가 복습용 퀴즈를 자동 생성해 학습 효과를 높입니다. 스터디 후 바로 복습할 수 있습니다."
                            align="right"
                        />
                        <FeatureSection
                            title="학습 데이터 자산화"
                            description="모든 스터디 기록이 나만의 학습 데이터베이스로 축적되어 언제든 다시 확인 가능합니다. 여러분의 학습 여정을 기록합니다."
                            align="left"
                        />
                    </div>
                </div>
            </section>

            {/* 최종 CTA 섹션 */}
            <section className="py-32 bg-gray-900 text-white">
                <div className="max-w-4xl mx-auto px-8 text-center">
                    <h3 className="text-6xl font-black mb-6 leading-tight">
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

// 특징 섹션 컴포넌트
interface FeatureSectionProps {
    title: string;
    description: string;
    align: 'left' | 'right';
}

const FeatureSection: React.FC<FeatureSectionProps> = ({ title, description, align }) => (
    <motion.div
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8 }}
        className={cn(
            'flex flex-col gap-6',
            align === 'right' ? 'items-end text-right' : 'items-start text-left'
        )}
    >
        <h4 className="text-5xl font-black text-gray-900 max-w-xl leading-tight">
            {title}
        </h4>
        <p className="text-xl text-gray-600 max-w-2xl leading-relaxed">
            {description}
        </p>
    </motion.div>
);
