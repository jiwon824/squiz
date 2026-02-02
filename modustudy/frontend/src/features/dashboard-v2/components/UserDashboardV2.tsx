import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Calendar, Trophy, LucideIcon } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { PostIt, getPostItTheme, PostItColor } from '@/shared/components/layouts/PostIt';
import { STTReportWidget } from './STTReportWidget';
import { MeetingTestWidget } from './MeetingTestWidget';
import { MyQuizWidget } from './MyQuizWidget';
import { LearningArchiveWidget } from './LearningArchiveWidget';
import { TodayGoalsCard } from './TodayGoalsCard';
import { MyCreatedStudiesWidget } from './MyCreatedStudiesWidget';
import { MyApplicationsWidget } from './MyApplicationsWidget';


export const UserDashboardV2: React.FC = () => {
    const { user } = useAuthStore();

    return (
        <div
            className="flex-1 overflow-y-auto"
            style={{
                backgroundImage:
                    'linear-gradient(to right, rgba(0,0,0,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(0,0,0,0.03) 3px, transparent 2px)',
                backgroundSize: '40px 40px',
            }}
        >
            <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 sm:px-6 sm:py-8 sm:space-y-8 lg:px-8">
                {/* 환영 메시지 */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-gradient-to-r from-primary/10 to-secondary/10 rounded-2xl pt-6 px-4 pb-3 sm:pt-8 sm:px-8 sm:pb-4"
                >
                    <div className="flex items-center gap-3 mb-3">
                        <Sparkles className="text-primary flex-shrink-0" size={28} />
                        <h1 className="text-xl sm:text-3xl font-black text-text-primary mb-0">
                            안녕하세요 {user?.nickname || user?.name || ''}님! 👋
                        </h1>
                    </div>
                    <p className="text-text-secondary text-sm sm:text-lg">오늘도 즐거운 학습 되세요!</p>
                </motion.div>

                {/* 내 스터디 위젯 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                    <MyCreatedStudiesWidget />
                    <MyApplicationsWidget />
                </div>

                {/* 미팅 리포트 (전체 행) */}
                <STTReportWidget />

                {/* 테스트 + 복습 퀴즈 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                    <MeetingTestWidget />
                    <MyQuizWidget />
                </div>

                {/* 학습 보관함 */}
                <LearningArchiveWidget />

                {/* 추가 정보 카드 (포스트잇) */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 pt-2">
                    {/* 오늘의 목표 - Zustand store로 캘린더와 동기화 */}
                    <TodayGoalsCard />
                    <InfoCard
                        icon={Calendar}
                        title="다가오는 일정"
                        items={['React 스터디 (오늘 오후 7시)', 'TypeScript 스터디 (내일 오후 8시)']}
                        color="green"
                        rotate={-1.5}
                        tapeRotate={2}
                    />
                    <InfoCard
                        icon={Trophy}
                        title="최근 성취"
                        items={['React Hooks 마스터', 'TypeScript 제네릭 완료', '3주 연속 출석']}
                        color="pink"
                        rotate={-0.8}
                        tapeRotate={-1}
                    />
                </div>
            </div>
        </div>
    );
};

// 포스트잇 스타일 정보 카드 컴포넌트
interface InfoCardProps {
    icon: LucideIcon;
    title: string;
    items: string[];
    color: PostItColor;
    rotate?: number;
    tapeRotate?: number;
}

const InfoCard: React.FC<InfoCardProps> = ({ icon: Icon, title, items, color, rotate = 0, tapeRotate }) => {
    const theme = getPostItTheme(color);

    return (
        <PostIt color={color} rotate={rotate} tapeRotate={tapeRotate}>
            {/* 헤더 */}
            <div className="flex items-center gap-2.5 mb-4 mt-1">
                <Icon size={20} style={{ color: theme.text }} />
                <h3
                    className="font-bold text-lg leading-6 mb-0"
                    style={{ color: theme.text }}
                >
                    {title}
                </h3>
            </div>

            {/* 리스트 */}
            <ul className="space-y-2.5">
                {items.map((item, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-sm" style={{ color: theme.sub }}>
                        <span className="mt-1 text-xs leading-none">•</span>
                        <span>{item}</span>
                    </li>
                ))}
            </ul>
        </PostIt>
    );
};
