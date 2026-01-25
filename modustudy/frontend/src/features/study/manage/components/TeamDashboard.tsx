import React from 'react';
import { Study } from '../../services/studyService';
import { 
    Users, TrendingUp, Calendar, Clock, 
    CheckCircle2, AlertCircle, Target, Award
} from 'lucide-react';

interface TeamDashboardProps {
    study: Study;
}

// Mock 통계 데이터
const mockStats = {
    totalMembers: 6,
    maxMembers: 8,
    avgAttendance: 87,
    totalMeetings: 12,
    upcomingMeetings: 2,
    pendingApplicants: 3,
    pendingExcuses: 2,
    studyDays: 45,
};

const TeamDashboard: React.FC<TeamDashboardProps> = ({ study }) => {
    const statCards = [
        {
            label: '현재 인원',
            value: `${mockStats.totalMembers}/${mockStats.maxMembers}`,
            icon: <Users size={20} />,
            color: 'primary',
            subtext: '정원 여유 있음',
        },
        {
            label: '평균 출석률',
            value: `${mockStats.avgAttendance}%`,
            icon: <TrendingUp size={20} />,
            color: 'success',
            subtext: '전주 대비 +3%',
        },
        {
            label: '총 미팅 횟수',
            value: mockStats.totalMeetings,
            icon: <Calendar size={20} />,
            color: 'info',
            subtext: `예정 ${mockStats.upcomingMeetings}건`,
        },
        {
            label: '스터디 진행일',
            value: `${mockStats.studyDays}일`,
            icon: <Clock size={20} />,
            color: 'secondary',
            subtext: study.status === 'IN_PROGRESS' ? '진행 중' : '모집 중',
        },
    ];

    const alertItems = [
        {
            type: 'warning',
            icon: <AlertCircle size={16} />,
            text: `대기 중인 지원자 ${mockStats.pendingApplicants}명`,
            action: '확인하기',
        },
        {
            type: 'info',
            icon: <CheckCircle2 size={16} />,
            text: `처리 대기 소명 ${mockStats.pendingExcuses}건`,
            action: '처리하기',
        },
    ];

    return (
        <div className="space-y-6">
            {/* 제목 */}
            <div>
                <h2 className="text-xl font-bold text-text-primary">팀 대시보드</h2>
                <p className="text-sm text-text-secondary mt-1">스터디 현황을 한눈에 확인하세요</p>
            </div>

            {/* 통계 카드 그리드 */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {statCards.map((stat, idx) => (
                    <div
                        key={idx}
                        className="bg-background-secondary rounded-2xl p-5 border border-border-light hover:shadow-md transition-shadow"
                    >
                        <div className="flex items-center justify-between mb-3">
                            <span className={`w-10 h-10 rounded-xl flex items-center justify-center bg-${stat.color}/10 text-${stat.color}`}>
                                {stat.icon}
                            </span>
                            <span className="text-xs text-text-tertiary font-medium">{stat.subtext}</span>
                        </div>
                        <div className="text-2xl font-bold text-text-primary">{stat.value}</div>
                        <div className="text-sm text-text-secondary mt-1">{stat.label}</div>
                    </div>
                ))}
            </div>

            {/* 알림 섹션 */}
            <div className="bg-warning/5 border border-warning/20 rounded-2xl p-4">
                <h3 className="text-sm font-bold text-text-primary mb-3 flex items-center gap-2">
                    <Target size={16} className="text-warning" />
                    처리가 필요한 항목
                </h3>
                <div className="space-y-2">
                    {alertItems.map((item, idx) => (
                        <div 
                            key={idx}
                            className="flex items-center justify-between bg-surface rounded-xl px-4 py-3"
                        >
                            <div className="flex items-center gap-3">
                                <span className={`text-${item.type}`}>{item.icon}</span>
                                <span className="text-sm text-text-primary">{item.text}</span>
                            </div>
                            <button className="text-xs font-medium text-primary hover:underline">
                                {item.action}
                            </button>
                        </div>
                    ))}
                </div>
            </div>

            {/* 최근 활동 & 출석 현황 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 출석률 Top 3 */}
                <div className="bg-background-secondary rounded-2xl p-5 border border-border-light">
                    <h3 className="text-sm font-bold text-text-primary mb-4 flex items-center gap-2">
                        <Award size={16} className="text-warning" />
                        출석률 TOP 3
                    </h3>
                    <div className="space-y-3">
                        {[
                            { name: '김철수', rate: 100, rank: 1 },
                            { name: '이영희', rate: 95, rank: 2 },
                            { name: '박민수', rate: 90, rank: 3 },
                        ].map((member) => (
                            <div key={member.rank} className="flex items-center gap-3">
                                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold
                                    ${member.rank === 1 ? 'bg-warning/20 text-warning' : 'bg-background-tertiary text-text-tertiary'}`}>
                                    {member.rank}
                                </span>
                                <div className="flex-1">
                                    <div className="flex items-center justify-between mb-1">
                                        <span className="text-sm font-medium text-text-primary">{member.name}</span>
                                        <span className="text-sm font-bold text-success">{member.rate}%</span>
                                    </div>
                                    <div className="h-1.5 bg-border-light rounded-full overflow-hidden">
                                        <div 
                                            className="h-full bg-success rounded-full transition-all"
                                            style={{ width: `${member.rate}%` }}
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 다가오는 일정 */}
                <div className="bg-background-secondary rounded-2xl p-5 border border-border-light">
                    <h3 className="text-sm font-bold text-text-primary mb-4 flex items-center gap-2">
                        <Calendar size={16} className="text-info" />
                        다가오는 일정
                    </h3>
                    <div className="space-y-3">
                        {[
                            { title: '12주차 정기 미팅', date: '1/27 (월) 20:00', type: '정기' },
                            { title: '프로젝트 발표 준비', date: '1/30 (목) 19:00', type: '특별' },
                        ].map((event, idx) => (
                            <div key={idx} className="flex items-center gap-3 p-3 bg-surface rounded-xl">
                                <div className="w-10 h-10 rounded-xl bg-info/10 flex items-center justify-center">
                                    <Calendar size={18} className="text-info" />
                                </div>
                                <div className="flex-1">
                                    <div className="text-sm font-medium text-text-primary">{event.title}</div>
                                    <div className="text-xs text-text-tertiary">{event.date}</div>
                                </div>
                                <span className={`text-xs px-2 py-1 rounded-full font-medium
                                    ${event.type === '정기' ? 'bg-primary/10 text-primary' : 'bg-secondary/10 text-secondary'}`}>
                                    {event.type}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TeamDashboard;
