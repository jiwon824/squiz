import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles, TrendingUp, Calendar } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useAuthStore } from '@/store/authStore';
import { STTReportWidget } from './STTReportWidget';
import { AIQuizWidget } from './AIQuizWidget';
import { LearningArchiveWidget } from './LearningArchiveWidget';

export const UserDashboardV2: React.FC = () => {
    const { user } = useAuthStore();

    return (
        <div className="flex-1 overflow-y-auto">
            <div className="max-w-7xl mx-auto p-8 space-y-8">
                {/* 환영 메시지 */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-gradient-to-r from-primary/10 to-secondary/10 rounded-2xl p-8"
                >
                    <div className="flex items-center gap-3 mb-3">
                        <Sparkles className="text-primary" size={32} />
                        <h1 className="text-3xl font-black text-text-primary mb-0">
                            안녕하세요 {user?.nickname || user?.name || ''}님! 👋
                        </h1>
                    </div>
                    <p className="text-text-secondary text-lg">오늘도 즐거운 학습 되세요!</p>
                </motion.div>

                {/* 통계 카드 */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <StatCard
                        icon={<Calendar className="text-primary" size={24} />}
                        title="이번 주 스터디"
                        value="3회"
                        change="+1"
                        color="primary"
                    />
                    <StatCard
                        icon={<TrendingUp className="text-secondary" size={24} />}
                        title="학습 시간"
                        value="7.5시간"
                        change="+2.5시간"
                        color="secondary"
                    />
                    <StatCard
                        icon={<Sparkles className="text-accent" size={24} />}
                        title="퀴즈 정답률"
                        value="85%"
                        change="+5%"
                        color="accent"
                    />
                </div>

                {/* 메인 위젯 영역 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <STTReportWidget />
                    <AIQuizWidget />
                </div>

                {/* 학습 보관함 */}
                <LearningArchiveWidget />

                {/* 추가 정보 카드 */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <InfoCard
                        title="오늘 할 일"
                        items={['React 스터디 준비', '퀴즈 복습', 'TypeScript 과제']}
                        color="primary"
                    />
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

// 통계 카드 컴포넌트
interface StatCardProps {
    icon: React.ReactNode;
    title: string;
    value: string;
    change: string;
    color: 'primary' | 'secondary' | 'accent';
}

const COLOR_STYLES = {
    primary: 'from-primary/10 to-primary/5 border-primary/20',
    secondary: 'from-secondary/10 to-secondary/5 border-secondary/20',
    accent: 'from-accent/10 to-accent/5 border-accent/20',
} as const;

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

const StatCard: React.FC<StatCardProps> = ({ icon, title, value, change, color }) => {
    return (
        <motion.div
            whileHover={{ y: -4 }}
            className={cn(
                'bg-gradient-to-br rounded-2xl p-6 border shadow-md transition-all',
                COLOR_STYLES[color]
            )}
        >
            <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-white rounded-xl shadow-sm">{icon}</div>
                <span className="text-xs font-medium text-accent px-2 py-1 bg-white rounded-full">
                    {change}
                </span>
            </div>
            <p className="text-sm text-text-secondary mb-1">{title}</p>
            <p className="text-3xl font-black text-text-primary">{value}</p>
        </motion.div>
    );
};

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
