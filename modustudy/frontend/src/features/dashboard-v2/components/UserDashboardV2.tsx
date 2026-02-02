import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useAuthStore } from '@/store/authStore';
import { STTReportWidget } from './STTReportWidget';
import { MyQuizWidget } from './MyQuizWidget';
import { LearningArchiveWidget } from './LearningArchiveWidget';
import { TodayGoalsCard } from './TodayGoalsCard';
import { MyCreatedStudiesWidget } from './MyCreatedStudiesWidget';
import { MyApplicationsWidget } from './MyApplicationsWidget';
import { WeakConceptWidget } from '@/features/quiz/components/WeakConceptWidget';

export const UserDashboardV2: React.FC = () => {
    const { user } = useAuthStore();

    return (
        <div className="flex-1 overflow-y-auto">
            <div className="max-w-7xl mx-auto p-8 space-y-8">
                {/* 환영 메시지 */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-gradient-to-r from-primary/10 to-secondary/10 rounded-2xl pt-8 px-8 pb-4"
                >
                    <div className="flex items-center gap-3 mb-3">
                        <Sparkles className="text-primary" size={32} />
                        <h1 className="text-3xl font-black text-text-primary mb-0">
                            안녕하세요 {user?.nickname || user?.name || ''}님! 👋
                        </h1>
                    </div>
                    <p className="text-text-secondary text-lg">오늘도 즐거운 학습 되세요!</p>
                </motion.div>

                {/* 내 스터디 위젯 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <MyCreatedStudiesWidget />
                    <MyApplicationsWidget />
                </div>

                {/* 메인 위젯 영역 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <STTReportWidget />
                    <MyQuizWidget />
                    <MyQuizWidget />
                </div>

                {/* 취약 개념 위젯 */}
                <div className="h-96">
                    <WeakConceptWidget />
                </div>

                {/* 학습 보관함 */}
                <LearningArchiveWidget />

                {/* 추가 정보 카드 */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* 오늘의 목표 - Zustand store로 캘린더와 동기화 */}
                    <TodayGoalsCard />
                    <InfoCard
                        title="다가오는 일정"
                        items={['React 스터디 (오늘 오후 7시)', 'TypeScript 스터디 (내일 오후 8시)']}
                        color="secondary"
                    />
                    <InfoCard
                        title="최근 성취"
                        items={['React Hooks 마스터', 'TypeScript 제네릭 완료', '3주 연속 출석']}
                        color="accent"
                    />
                </div>
            </div>
        </div>
    );
};

const TEXT_COLOR_STYLES = {
    primary: 'text-primary',
    secondary: 'text-secondary',
    accent: 'text-accent',
} as const;

const BG_COLOR_STYLES = {
    primary: 'bg-primary',
    secondary: 'bg-secondary',
    accent: 'bg-accent',
} as const;

// 정보 카드 컴포넌트
interface InfoCardProps {
    title: string;
    items: string[];
    color: 'primary' | 'secondary' | 'accent';
}

const InfoCard: React.FC<InfoCardProps> = ({ title, items, color }) => {
    return (
        <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100">
            <h3 className={cn('font-bold mb-0 flex items-center gap-2', TEXT_COLOR_STYLES[color])}>
                <span className="text-lg">{title}</span>
            </h3>
            <ul className="space-y-3">
                {items.map((item, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-sm text-text-secondary">
                        <span className={cn('mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0', BG_COLOR_STYLES[color])} />
                        <span>{item}</span>
                    </li>
                ))}
            </ul>
        </div>
    );
};
