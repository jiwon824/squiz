import React, { useState, useEffect } from 'react';
import { Study } from '../../services/studyService';
import {
    Users, TrendingUp, Calendar, Clock,
    CheckCircle2, AlertCircle, Target, Award,
    Play, CalendarPlus
} from 'lucide-react';
import { ButtonSpinner } from '@/shared/components/Spinner';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

interface TeamDashboardProps {
    study: Study;
    onStudyUpdate?: () => void;
    onTabChange?: (tab: string) => void; // 탭 변경 핸들러
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

// 세션 데이터 타입
interface SessionData {
    id: number;
    title?: string;
    description?: string;
    scheduledAt?: string;
    sessionNumber?: number;
    status?: string;
}

// 출석률 Top3 멤버 타입
interface AttendanceTopMember {
    name: string;
    rate: number;
    rank: number;
}

const TeamDashboard: React.FC<TeamDashboardProps> = ({ study, onStudyUpdate, onTabChange }) => {
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
    // 다가오는 일정 데이터
    const [upcomingEvents, setUpcomingEvents] = useState<SessionData[]>([]);
    // 출석률 Top3 데이터
    const [attendanceTop3, setAttendanceTop3] = useState<AttendanceTopMember[]>([]);

    useEffect(() => {
        fetchDashboardData();
    }, [study.id]);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            // 멤버 목록 조회 (출석률 계산용)
            const membersResponse = await studyApi.getStudyMembers(study.id);
            const members = membersResponse?.content || [];
            const memberCount = members.length;

            // 세션 목록 조회
            const sessionsResponse = await studyApi.getStudySessions(study.id);
            const sessions: SessionData[] = sessionsResponse || [];

            // 대기중 신청자 수
            const pendingApplicants = await studyApi.getPendingApplicationCount(study.id);

            // 대기중 소명 수
            const pendingExcuses = await studyApi.getPendingExcuseCount(study.id);

            // 스터디 진행일 계산 (첫 세션 날짜 기준, 진행중 상태일 때만)
            const today = new Date();
            // 오늘 날짜의 자정 (시간 제거)
            const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());
            let studyDays = 0;
            if (study.status === 'IN_PROGRESS') {
                // 우선순위: 첫 번째 세션의 scheduledAt → startDate → createdAt
                let referenceDate: Date | null = null;

                // 1. 첫 번째 세션의 scheduledAt 사용 (가장 신뢰할 수 있음)
                if (sessions.length > 0) {
                    const sortedSessions = [...sessions]
                        .filter((s: SessionData) => s.scheduledAt)
                        .sort((a, b) => new Date(a.scheduledAt!).getTime() - new Date(b.scheduledAt!).getTime());
                    if (sortedSessions.length > 0 && sortedSessions[0].scheduledAt) {
                        const sessionDate = new Date(sortedSessions[0].scheduledAt);
                        // 날짜만 추출 (시간 제거)
                        referenceDate = new Date(sessionDate.getFullYear(), sessionDate.getMonth(), sessionDate.getDate());
                    }
                }

                // 2. 세션이 없으면 startDate 사용
                if (!referenceDate && study.startDate) {
                    const startDateObj = new Date(study.startDate);
                    referenceDate = new Date(startDateObj.getFullYear(), startDateObj.getMonth(), startDateObj.getDate());
                }

                // 3. 마지막 fallback: createdAt
                if (!referenceDate && study.createdAt) {
                    const createdDateObj = new Date(study.createdAt);
                    referenceDate = new Date(createdDateObj.getFullYear(), createdDateObj.getMonth(), createdDateObj.getDate());
                }

                if (referenceDate && referenceDate <= todayMidnight) {
                    // +1: 첫 날을 1일로 카운트 (2/3 시작 → 2/4 오늘 = 2일째)
                    studyDays = Math.floor((todayMidnight.getTime() - referenceDate.getTime()) / (1000 * 60 * 60 * 24)) + 1;
                }
            }

            // 예정된 세션 수 및 다가오는 일정 추출
            const upcoming = sessions
                .filter((s: SessionData) => s.scheduledAt && new Date(s.scheduledAt) > today)
                .sort((a, b) => new Date(a.scheduledAt!).getTime() - new Date(b.scheduledAt!).getTime())
                .slice(0, 3); // 최대 3개

            setUpcomingEvents(upcoming);

            // 출석률 계산 (세션별 출석 정보 조회)
            let avgAttendance = 0;
            const memberAttendanceMap: Record<number, { attended: number; total: number; name: string }> = {};

            // 멤버별 출석 데이터 초기화
            members.forEach((member: any) => {
                memberAttendanceMap[member.userId] = {
                    attended: 0,
                    total: 0,
                    name: member.userNickname || member.userName || '알 수 없음',
                };
            });

            // 완료된 세션들의 출석 정보 조회
            const completedSessions = sessions.filter(
                (s: SessionData) => s.status === 'COMPLETED' || (s.scheduledAt && new Date(s.scheduledAt) < today)
            );

            if (completedSessions.length > 0 && members.length > 0) {
                let totalAttendance = 0;
                let totalRecords = 0;

                for (const session of completedSessions) {
                    try {
                        const attendanceData = await studyApi.getSessionAttendance(study.id, session.id);
                        // API 응답 구조: { sessionId, sessionTitle, totalMembers, presentCount, members: [...] }
                        const sessionData = attendanceData?.data || attendanceData || {};
                        const attendances = sessionData.members || [];

                        attendances.forEach((att: any) => {
                            if (memberAttendanceMap[att.userId]) {
                                memberAttendanceMap[att.userId].total += 1;
                                if (att.status === 'PRESENT' || att.status === 'LATE') {
                                    memberAttendanceMap[att.userId].attended += 1;
                                    totalAttendance += 1;
                                }
                                totalRecords += 1;
                            }
                        });
                    } catch (error) {
                        console.warn(`세션 ${session.id} 출석 정보 조회 실패:`, error);
                    }
                }

                // 평균 출석률 계산
                if (totalRecords > 0) {
                    avgAttendance = Math.round((totalAttendance / totalRecords) * 100);
                }
            }

            // 출석률 Top 3 계산
            const memberRates = Object.entries(memberAttendanceMap)
                .filter(([, data]) => data.total > 0)
                .map(([userId, data]) => ({
                    userId: Number(userId),
                    name: data.name,
                    rate: Math.round((data.attended / data.total) * 100),
                }))
                .sort((a, b) => b.rate - a.rate)
                .slice(0, 3)
                .map((member, idx) => ({
                    name: member.name,
                    rate: member.rate,
                    rank: idx + 1,
                }));

            setAttendanceTop3(memberRates);

            setStats({
                totalMembers: memberCount,
                maxMembers: study.maxMembers || 0,
                avgAttendance,
                totalSessions: sessions.length,
                upcomingSessions: upcoming.length,
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

    // 모집 마감일 확인 (프론트엔드에서 직접 체크, 시간대 문제 방지)
    const recruitEndDateStr = (study as any).recruitEndDate;
    const todayDate = new Date();
    const todayStr = `${todayDate.getFullYear()}-${String(todayDate.getMonth() + 1).padStart(2, '0')}-${String(todayDate.getDate()).padStart(2, '0')}`;
    const isRecruitmentEnded = recruitEndDateStr ? todayStr > recruitEndDateStr : false;

    // 이미 시작된 스터디인지 확인 (진행중, 완료, 취소 상태)
    const isStudyStarted = ['IN_PROGRESS', 'COMPLETED', 'CANCELLED'].includes(study.status);

    // 번개스터디 여부
    const isLightningStudy = study.studyType === 'LIGHTNING';

    // 스터디 시작 가능 여부
    // - 이미 시작된 스터디가 아니어야 함
    // - 번개스터디: 최소 인원(2명) 충족하면 마감일 관계없이 바로 시작 가능
    // - 계획형: 1. RECRUIT_CLOSED OR 2. 최대 인원 충족 OR 3. 마감일 지남 + 최소 인원 충족
    const canStartStudy = !isStudyStarted && (
        study.status === 'RECRUIT_CLOSED' ||
        hasMaximumMembers ||
        (isLightningStudy && hasMinimumMembers) ||  // 번개스터디: 최소 인원만 충족하면 바로 시작 가능
        (isRecruitmentEnded && hasMinimumMembers)
    );

    // 모집 연장 가능 여부
    // - 이미 시작된 스터디가 아니어야 함
    // - 마감일 지남 + 최대 인원 미충족 + 연장 횟수 1회 미만
    const extensionCount = (study as any).extensionCount || 0;
    const canExtendRecruitment = !isStudyStarted && isRecruitmentEnded && !hasMaximumMembers && extensionCount < 1;

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

    // 지원자 확인 클릭 핸들러
    const handleApplicantsClick = () => {
        // 스터디 관리 페이지의 지원자 관리 탭으로 이동
        if (onTabChange) {
            onTabChange('applicants');
        }
    };

    // 소명 처리 클릭 핸들러
    const handleExcusesClick = () => {
        // 스터디 관리 페이지의 소명 관리 탭으로 이동
        if (onTabChange) {
            onTabChange('excuse');
        }
    };

    const alertItems = [
        {
            type: 'warning',
            icon: <AlertCircle size={16} />,
            text: `대기 중인 지원자 ${stats.pendingApplicants}명`,
            action: '확인하기',
            onClick: handleApplicantsClick,
        },
        {
            type: 'info',
            icon: <CheckCircle2 size={16} />,
            text: `처리 대기 소명 ${stats.pendingExcuses}건`,
            action: '처리하기',
            onClick: handleExcusesClick,
        },
    ];

    // 날짜 포맷 함수
    const formatSessionDate = (dateString?: string) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const month = date.getMonth() + 1;
        const day = date.getDate();
        const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
        const dayName = dayNames[date.getDay()];
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${month}/${day} (${dayName}) ${hours}:${minutes}`;
    };

    return (
        <div className="space-y-6">
            {/* 제목 */}
            <div>
                <h2 className="text-xl font-bold text-text-primary">팀 대시보드</h2>
                <p className="text-sm text-text-secondary mt-1">스터디 현황을 한눈에 확인하세요</p>
            </div>

            {/* 스터디 관리 액션 버튼 */}
            {(canStartStudy || canExtendRecruitment) && (
                <div className="bg-white rounded-2xl shadow-[0_4px_15px_rgba(0,0,0,0.05)] p-5">
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
                                    <ButtonSpinner />
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
                                    <ButtonSpinner />
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
                        className="bg-white rounded-2xl p-5 shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] transition-shadow"
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
            <div className="bg-white rounded-2xl shadow-[0_4px_15px_rgba(0,0,0,0.05)] p-4">
                <h3 className="text-sm font-bold text-text-primary mb-3 flex items-center gap-2">
                    <Target size={16} className="text-warning" />
                    처리가 필요한 항목
                </h3>
                <div className="space-y-2">
                    {alertItems.map((item, idx) => (
                        <div
                            key={idx}
                            className="flex items-center justify-between bg-gray-50 rounded-xl px-4 py-3"
                        >
                            <div className="flex items-center gap-3">
                                <span className={`text-${item.type}`}>{item.icon}</span>
                                <span className="text-sm text-text-primary">{item.text}</span>
                            </div>
                            <button
                                onClick={item.onClick}
                                className="text-xs font-medium text-primary hover:underline"
                            >
                                {item.action}
                            </button>
                        </div>
                    ))}
                </div>
            </div>

            {/* 최근 활동 & 출석 현황 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 출석률 Top 3 */}
                <div className="bg-white rounded-2xl p-5 shadow-[0_4px_15px_rgba(0,0,0,0.05)]">
                    <h3 className="text-sm font-bold text-text-primary mb-4 flex items-center gap-2">
                        <Award size={16} className="text-warning" />
                        출석률 TOP 3
                    </h3>
                    <div className="space-y-3">
                        {attendanceTop3.length > 0 ? (
                            attendanceTop3.map((member) => (
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
                            ))
                        ) : (
                            <div className="text-sm text-text-tertiary text-center py-4">
                                아직 출석 데이터가 없습니다
                            </div>
                        )}
                    </div>
                </div>

                {/* 다가오는 일정 */}
                <div className="bg-white rounded-2xl p-5 shadow-[0_4px_15px_rgba(0,0,0,0.05)]">
                    <h3 className="text-sm font-bold text-text-primary mb-4 flex items-center gap-2">
                        <Calendar size={16} className="text-info" />
                        다가오는 일정
                    </h3>
                    <div className="space-y-3">
                        {upcomingEvents.length > 0 ? (
                            upcomingEvents.map((event, idx) => (
                                <div key={event.id || idx} className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl">
                                    <div className="w-10 h-10 rounded-xl bg-info/10 flex items-center justify-center">
                                        <Calendar size={18} className="text-info" />
                                    </div>
                                    <div className="flex-1">
                                        <div className="text-sm font-medium text-text-primary">
                                            {event.title || `${event.sessionNumber || idx + 1}회차`}
                                        </div>
                                        <div className="text-xs text-text-tertiary">
                                            {formatSessionDate(event.scheduledAt)}
                                        </div>
                                    </div>
                                    <span className="text-xs px-2 py-1 rounded-full font-medium bg-primary/10 text-primary">
                                        정기
                                    </span>
                                </div>
                            ))
                        ) : (
                            <div className="text-sm text-text-tertiary text-center py-4">
                                예정된 일정이 없습니다
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TeamDashboard;
