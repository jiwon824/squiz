import React, { useState, useEffect } from 'react';
import { Study } from '../../services/studyService';
import {
    Users, TrendingUp, Calendar, Clock,
    CheckCircle2, AlertCircle, Target, Award,
    Play, CalendarPlus, Loader2
} from 'lucide-react';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

interface TeamDashboardProps {
    study: Study;
    onStudyUpdate?: () => void;
}

interface DashboardStats {
    totalMembers: number;
    maxMembers: number;
    avgAttendance: number;
    totalSessions: number;
    upcomingSessions: number;
    pendingApplicants: number;
    pendingExcuses: number;
    studyDays: number;
}

const TeamDashboard: React.FC<TeamDashboardProps> = ({ study, onStudyUpdate }) => {
    const { showToast } = useUIStore();
    const [stats, setStats] = useState<DashboardStats>({
        totalMembers: 0,
        maxMembers: study.maxMembers || 0,
        avgAttendance: 0,
        totalSessions: 0,
        upcomingSessions: 0,
        pendingApplicants: 0,
        pendingExcuses: 0,
        studyDays: 0,
    });
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<'start' | 'extend' | null>(null);

    useEffect(() => {
        fetchDashboardData();
    }, [study.id]);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            // 멤버 수 조회
            const memberCountResponse = await studyApi.getStudyMemberCount(study.id);
            const memberCount = memberCountResponse || 0;

            // 세션 목록 조회
            const sessionsResponse = await studyApi.getStudySessions(study.id);
            const sessions = sessionsResponse || [];

            // 대기중 신청자 수
            const pendingApplicants = await studyApi.getPendingApplicationCount(study.id);

            // 대기중 소명 수
            const pendingExcuses = await studyApi.getPendingExcuseCount(study.id);

            // 스터디 진행일 계산
            const startDate = new Date(study.startDate || Date.now());
            const today = new Date();
            const studyDays = Math.max(0, Math.floor((today.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)));

            // 예정된 세션 수 계산
            const upcomingSessions = sessions.filter((s: any) =>
                new Date(s.scheduledAt) > today
            ).length;

            setStats({
                totalMembers: memberCount,
                maxMembers: study.maxMembers || 0,
                avgAttendance: 85, // TODO: 실제 평균 출석률 계산
                totalSessions: sessions.length,
                upcomingSessions,
                pendingApplicants,
                pendingExcuses,
                studyDays,
            });
        } catch (error) {
            console.error('대시보드 데이터 조회 실패:', error);
        } finally {
            setLoading(false);
        }
    };

    // 스터디 시작 핸들러
    const handleStartStudy = async () => {
        if (actionLoading) return;

        setActionLoading('start');
        try {
            await studyApi.startStudy(study.id);
            showToast('스터디가 시작되었습니다!', 'success');
            onStudyUpdate?.();
        } catch (error: any) {
            const message = error.response?.data?.message || error.response?.data?.error?.message || '스터디 시작에 실패했습니다.';
            showToast(message, 'error');
        } finally {
            setActionLoading(null);
        }
    };

    // 모집 연장 핸들러
    const handleExtendRecruitment = async () => {
        if (actionLoading) return;

        // 현재 모집 종료일 + 7일 계산
        const currentEndDate = (study as any).recruitEndDate
            ? new Date((study as any).recruitEndDate)
            : new Date();
        currentEndDate.setDate(currentEndDate.getDate() + 7);
        const newEndDate = currentEndDate.toISOString().split('T')[0]; // YYYY-MM-DD 형식

        setActionLoading('extend');
        try {
            await studyApi.extendRecruitment(study.id, newEndDate);
            showToast('모집 기간이 7일 연장되었습니다!', 'success');
            onStudyUpdate?.();
        } catch (error: any) {
            const message = error.response?.data?.message || error.response?.data?.error?.message || '모집 연장에 실패했습니다.';
            showToast(message, 'error');
        } finally {
            setActionLoading(null);
        }
    };

    // 최소 인원 충족 여부 (스터디 시작 가능 조건)
    const hasMinimumMembers = stats.totalMembers >= ((study as any).minMembers || 2);

    // 최대 인원 충족 여부
    const hasMaximumMembers = stats.totalMembers >= stats.maxMembers;

    // 모집 마감일 확인 (프론트엔드에서 직접 체크)
    const recruitEndDate = (study as any).recruitEndDate ? new Date((study as any).recruitEndDate) : null;
    const today = new Date();
    today.setHours(23, 59, 59, 999);  // 마감일 당일까지는 모집 중으로 취급
    const isRecruitmentEnded = recruitEndDate ? today > new Date(recruitEndDate) : false;

    // 스터디 시작 가능 여부
    // 1. RECRUIT_CLOSED: 최대 인원 충족 (마감일 전이라도 시작 가능)
    // 2. 마감일 지남 + 최소 인원 충족
    const canStartStudy =
        study.status === 'RECRUIT_CLOSED' ||  // 최대 인원 충족
        (isRecruitmentEnded && hasMinimumMembers);  // 마감일 지남 + 최소 인원 충족

    // 모집 연장 가능 여부
    // - 마감일 지남 + 최대 인원 미충족 + 연장 횟수 1회 미만
    const extensionCount = (study as any).extensionCount || 0;
    const canExtendRecruitment = isRecruitmentEnded && !hasMaximumMembers && extensionCount < 1;

    // 상태 텍스트 변환 함수
    const getStatusText = (status: string): string => {
        switch (status) {
            case 'RECRUITING':
                return '모집 중';
            case 'RECRUIT_CLOSED':
                return '모집 완료';
            case 'PENDING':
                return '확정 대기';
            case 'IN_PROGRESS':
                return '진행 중';
            case 'COMPLETED':
                return '완료';
            case 'CANCELLED':
                return '취소됨';
            default:
                return '준비 중';
        }
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">대시보드 데이터를 불러오는 중...</p>
            </div>
        );
    }

    const statCards = [
        {
            label: '현재 인원',
            value: `${stats.totalMembers}/${stats.maxMembers}`,
            icon: <Users size={20} />,
            color: 'primary',
            subtext: stats.totalMembers < stats.maxMembers ? '정원 여유 있음' : '정원 마감',
        },
        {
            label: '평균 출석률',
            value: `${stats.avgAttendance}%`,
            icon: <TrendingUp size={20} />,
            color: 'success',
            subtext: '스터디 전체 평균',
        },
        {
            label: '총 세션 수',
            value: stats.totalSessions,
            icon: <Calendar size={20} />,
            color: 'info',
            subtext: `예정 ${stats.upcomingSessions}건`,
        },
        {
            label: '스터디 진행일',
            value: `${stats.studyDays}일`,
            icon: <Clock size={20} />,
            color: 'secondary',
            subtext: getStatusText(study.status),
        },
    ];

    const alertItems = [
        {
            type: 'warning',
            icon: <AlertCircle size={16} />,
            text: `대기 중인 지원자 ${stats.pendingApplicants}명`,
            action: '확인하기',
        },
        {
            type: 'info',
            icon: <CheckCircle2 size={16} />,
            text: `처리 대기 소명 ${stats.pendingExcuses}건`,
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

            {/* 스터디 관리 액션 버튼 */}
            {(canStartStudy || canExtendRecruitment) && (
                <div className="bg-primary/5 border border-primary/20 rounded-2xl p-5">
                    <h3 className="text-sm font-bold text-text-primary mb-3 flex items-center gap-2">
                        <Target size={16} className="text-primary" />
                        스터디 관리
                    </h3>
                    <div className="flex flex-wrap gap-3">
                        {/* 스터디 시작 버튼 */}
                        {canStartStudy && (
                            <button
                                onClick={handleStartStudy}
                                disabled={actionLoading !== null}
                                className="flex items-center gap-2 px-5 py-3 bg-success hover:bg-success/90 text-white font-medium rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {actionLoading === 'start' ? (
                                    <Loader2 size={18} className="animate-spin" />
                                ) : (
                                    <Play size={18} />
                                )}
                                <span>스터디 시작하기</span>
                            </button>
                        )}

                        {/* 모집 연장 버튼 */}
                        {canExtendRecruitment && (
                            <button
                                onClick={handleExtendRecruitment}
                                disabled={actionLoading !== null}
                                className="flex items-center gap-2 px-5 py-3 bg-info hover:bg-info/90 text-white font-medium rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {actionLoading === 'extend' ? (
                                    <Loader2 size={18} className="animate-spin" />
                                ) : (
                                    <CalendarPlus size={18} />
                                )}
                                <span>모집 7일 연장</span>
                            </button>
                        )}
                    </div>
                    <p className="text-xs text-text-tertiary mt-3">
                        {canStartStudy && '모집이 완료되면 스터디를 시작할 수 있습니다. '}
                        {canExtendRecruitment && `모집 연장은 1회만 가능합니다. (현재 ${extensionCount}회 사용)`}
                    </p>
                </div>
            )}

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
