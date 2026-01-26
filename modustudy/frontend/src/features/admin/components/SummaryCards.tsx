import React from 'react';
import { Users, BookOpen, UserPlus, AlertTriangle } from 'lucide-react';
import { DashboardSummary } from '../../../api/endpoints/adminApi';

interface SummaryCardsProps {
    summary: DashboardSummary | null;
}

interface CardProps {
    title: string;
    value: number;
    icon: React.ReactNode;
    color: string;
}

const Card: React.FC<CardProps> = ({ title, value, icon, color }) => (
    <div className={`bg-white rounded-lg shadow-md p-6 border-l-4 ${color}`}>
        <div className="flex items-center justify-between">
            <div>
                <p className="text-sm text-gray-500 font-medium">{title}</p>
                <p className="text-3xl font-bold text-gray-800 mt-1">
                    {value.toLocaleString()}
                </p>
            </div>
            <div className={`p-3 rounded-full ${color.replace('border-', 'bg-').replace('-500', '-100')}`}>
                {icon}
            </div>
        </div>
    </div>
);

const SummaryCards: React.FC<SummaryCardsProps> = ({ summary }) => {
    if (!summary) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="bg-white rounded-lg shadow-md p-6 animate-pulse">
                        <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
                        <div className="h-8 bg-gray-200 rounded w-1/3"></div>
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card
                title="총 회원수"
                value={summary.totalUsers}
                icon={<Users className="w-6 h-6 text-blue-500" />}
                color="border-blue-500"
            />
            <Card
                title="활성 스터디"
                value={summary.activeStudies}
                icon={<BookOpen className="w-6 h-6 text-green-500" />}
                color="border-green-500"
            />
            <Card
                title="오늘 가입자"
                value={summary.todaySignups}
                icon={<UserPlus className="w-6 h-6 text-purple-500" />}
                color="border-purple-500"
            />
            <Card
                title="미처리 신고"
                value={summary.pendingReports}
                icon={<AlertTriangle className="w-6 h-6 text-red-500" />}
                color="border-red-500"
            />
        </div>
    );
};

export default SummaryCards;
