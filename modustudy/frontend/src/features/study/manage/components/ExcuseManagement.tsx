import React, { useState, useEffect } from 'react';
import {
    FileWarning, Check, X, MessageSquare,
    Calendar, ChevronDown, Filter
} from 'lucide-react';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

interface ExcuseManagementProps {
    studyId: number;
}

type ExcuseStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

interface Excuse {
    // 출석 정보
    attendanceId: number;
    sessionId: number;
    userId: number;
    // 사용자 정보
    userName: string;
    userNickname: string;
    // 세션 정보
    sessionTitle: string;
    scheduledAt: string;
    // 소명 정보
    excuseReason: string;
    excuseStatus: ExcuseStatus;
    checkedAt: string;
}

const ExcuseManagement: React.FC<ExcuseManagementProps> = ({ studyId }) => {
    const [excuses, setExcuses] = useState<Excuse[]>([]);
    const [filterStatus, setFilterStatus] = useState<ExcuseStatus | 'all'>('all');
    const [expandedId, setExpandedId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const { showToast } = useUIStore();

    // 소명 목록 조회
    useEffect(() => {
        fetchExcuses();
    }, [studyId]);

    const fetchExcuses = async () => {
        setLoading(true);
        try {
            // 1. 세션 목록 조회
            const sessionsResponse = await studyApi.getStudySessions(studyId);
            const sessions = sessionsResponse || [];

            // 2. 각 세션의 출석 정보 조회
            const allExcuses: Excuse[] = [];

            for (const session of sessions) {
                try {
                    const attendanceResponse = await studyApi.getSessionAttendance(studyId, session.id);
                    const attendances = attendanceResponse?.data || attendanceResponse || [];

                    // 소명이 있는 출석 정보만 필터링
                    const sessionExcuses = attendances
                        .filter((att: any) => att.excuseReason && att.excuseStatus)
                        .map((att: any) => ({
                            attendanceId: att.id,
                            sessionId: session.id,
                            userId: att.userId,
                            userName: att.userName || '익명',
                            userNickname: att.userNickname || att.userName || '익명',
                            sessionTitle: session.title || `${session.sessionNumber}회차`,
                            scheduledAt: session.scheduledAt,
                            excuseReason: att.excuseReason,
                            excuseStatus: att.excuseStatus,
                            checkedAt: att.checkedAt || new Date().toISOString(),
                        }));

                    allExcuses.push(...sessionExcuses);
                } catch (error) {
                    console.warn(`세션 ${session.id} 출석 정보 조회 실패:`, error);
                }
            }

            // 최신순 정렬
            allExcuses.sort((a, b) => new Date(b.checkedAt).getTime() - new Date(a.checkedAt).getTime());

            setExcuses(allExcuses);
        } catch (error) {
            console.error('소명 목록 조회 실패:', error);
            showToast('소명 목록을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (
        sessionId: number,
        userId: number,
        decision: 'APPROVED' | 'REJECTED'
    ) => {
        try {
            await studyApi.decideExcuse(studyId, sessionId, userId, decision);
            showToast(
                decision === 'APPROVED' ? '소명을 승인했습니다.' : '소명을 거절했습니다.',
                decision === 'APPROVED' ? 'success' : 'info'
            );

            // 목록 새로고침
            await fetchExcuses();
        } catch (error) {
            console.error('소명 처리 실패:', error);
            showToast('소명 처리에 실패했습니다.', 'error');
        }
    };

    const filteredExcuses = excuses.filter(excuse =>
        filterStatus === 'all' ? true : excuse.excuseStatus === filterStatus
    );

    const pendingCount = excuses.filter(e => e.excuseStatus === 'PENDING').length;

    const getStatusStyle = (status: ExcuseStatus) => {
        switch (status) {
            case 'APPROVED': return 'bg-success/10 text-success';
            case 'REJECTED': return 'bg-error/10 text-error';
            default: return 'bg-warning/10 text-warning';
        }
    };

    const getStatusLabel = (status: ExcuseStatus) => {
        switch (status) {
            case 'APPROVED': return '승인됨';
            case 'REJECTED': return '거절됨';
            default: return '대기 중';
        }
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) return '오늘';
        if (days === 1) return '어제';
        if (days < 7) return `${days}일 전`;
        return `${date.getMonth() + 1}/${date.getDate()}`;
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">소명 목록을 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary flex items-center gap-2">
                        소명 관리
                        {pendingCount > 0 && (
                            <span className="px-2 py-0.5 text-xs font-bold bg-error/10 text-error rounded-full">
                                {pendingCount}
                            </span>
                        )}
                    </h2>
                    <p className="text-sm text-text-secondary mt-1">멤버들의 결석 소명 요청을 처리하세요</p>
                </div>
            </div>

            {/* 필터 */}
            <div className="flex items-center gap-3">
                <Filter size={18} className="text-text-tertiary" />
                <div className="flex gap-2">
                    {[
                        { value: 'all', label: '전체' },
                        { value: 'PENDING', label: '대기 중' },
                        { value: 'APPROVED', label: '승인됨' },
                        { value: 'REJECTED', label: '거절됨' },
                    ].map((filter) => (
                        <button
                            key={filter.value}
                            onClick={() => setFilterStatus(filter.value as any)}
                            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all
                                ${filterStatus === filter.value
                                    ? 'bg-primary text-white'
                                    : 'bg-background-secondary text-text-secondary hover:bg-background-tertiary'
                                }`}
                        >
                            {filter.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* 소명 목록 */}
            {filteredExcuses.length === 0 ? (
                <div className="text-center py-12 bg-background-secondary rounded-2xl">
                    <FileWarning size={48} className="mx-auto text-text-muted mb-4" />
                    <p className="text-text-secondary">처리할 소명 요청이 없습니다</p>
                </div>
            ) : (
                <div className="space-y-3">
                    {filteredExcuses.map((excuse) => (
                        <div
                            key={excuse.attendanceId}
                            className="bg-background-secondary rounded-2xl border border-border-light overflow-hidden"
                        >
                            {/* 카드 헤더 */}
                            <div
                                className="p-4 flex items-center gap-4 cursor-pointer hover:bg-surface/50 transition-colors"
                                onClick={() => setExpandedId(expandedId === excuse.attendanceId ? null : excuse.attendanceId)}
                            >
                                {/* 아바타 */}
                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                                    {excuse.userNickname.charAt(0).toUpperCase()}
                                </div>

                                {/* 정보 */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="font-medium text-text-primary">{excuse.userNickname}</span>
                                        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getStatusStyle(excuse.excuseStatus)}`}>
                                            {getStatusLabel(excuse.excuseStatus)}
                                        </span>
                                    </div>
                                    <div className="text-xs text-text-tertiary flex items-center gap-3 mt-1">
                                        <span className="flex items-center gap-1">
                                            <Calendar size={12} />
                                            {excuse.sessionTitle}
                                        </span>
                                        <span>
                                            {formatDate(excuse.scheduledAt)}
                                        </span>
                                    </div>
                                </div>

                                {/* 확장 아이콘 */}
                                <ChevronDown
                                    size={18}
                                    className={`text-text-tertiary transition-transform ${expandedId === excuse.attendanceId ? 'rotate-180' : ''}`}
                                />
                            </div>

                            {/* 확장된 내용 */}
                            {expandedId === excuse.attendanceId && (
                                <div className="px-4 pb-4 pt-0 space-y-4 border-t border-border-light mt-0">
                                    <div className="pt-4">
                                        {/* 소명 사유 */}
                                        <div className="bg-surface rounded-xl p-4 mb-4">
                                            <div className="flex items-center gap-2 text-sm font-medium text-text-secondary mb-2">
                                                <MessageSquare size={14} />
                                                소명 사유
                                            </div>
                                            <p className="text-text-primary text-sm leading-relaxed">
                                                {excuse.excuseReason}
                                            </p>
                                        </div>

                                        {/* 액션 버튼 */}
                                        {excuse.excuseStatus === 'PENDING' && (
                                            <div className="flex gap-3">
                                                <button
                                                    onClick={() => handleAction(excuse.sessionId, excuse.userId, 'APPROVED')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-success/10 text-success font-medium hover:bg-success/20 transition-colors"
                                                >
                                                    <Check size={18} />
                                                    소명 승인
                                                </button>
                                                <button
                                                    onClick={() => handleAction(excuse.sessionId, excuse.userId, 'REJECTED')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-error/10 text-error font-medium hover:bg-error/20 transition-colors"
                                                >
                                                    <X size={18} />
                                                    소명 거절
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ExcuseManagement;
