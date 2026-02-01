import React, { useState, useEffect } from 'react';
import { FileWarning, Check, X, MessageSquare, Calendar, ChevronDown, Filter } from 'lucide-react';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

interface ExcuseManagementProps {
    studyId: number;
}

type ExcuseStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

interface Excuse {
    attendanceId: number;
    sessionId: number;
    userId: number;
    userName: string;
    userNickname: string;
    sessionTitle: string;
    scheduledAt: string;
    excuseReason: string;
    excuseStatus: ExcuseStatus;
    checkedAt: string;
}

const ExcuseManagement: React.FC<ExcuseManagementProps> = ({ studyId }) => {
    const [excuses, setExcuses] = useState<Excuse[]>([]);
    const [filterStatus, setFilterStatus] = useState<ExcuseStatus | 'all'>('all');
    const [expandedId, setExpandedId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(1);
    const { showToast } = useUIStore();

    useEffect(() => {
        fetchExcuses();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [studyId]);

    useEffect(() => {
        setCurrentPage(1);
    }, [filterStatus, excuses.length]);

    const fetchExcuses = async () => {
        setLoading(true);
        try {
            const membersResponse = await studyApi.getStudyMembers(studyId, 0, 200);
            const membersPayload = (membersResponse as any)?.data ?? membersResponse;
            const members = Array.isArray(membersPayload?.content)
                ? membersPayload.content
                : membersPayload?.data?.content || [];
            const memberNameById = new Map<number, string>();
            members.forEach((member: any) => {
                if (member?.userId) {
                    memberNameById.set(member.userId, member.userName || member.userNickname || '이름 없음');
                }
            });

            const sessionsResponse = await studyApi.getStudySessions(studyId);
            const sessionsPayload = (sessionsResponse as any)?.data ?? sessionsResponse;
            const sessions = Array.isArray(sessionsPayload?.data) ? sessionsPayload.data : sessionsPayload || [];

            const allExcuses: Excuse[] = [];
            for (const session of sessions) {
                try {
                    const attendanceResponse = await studyApi.getSessionAttendance(studyId, session.id);
                    const attendancePayload = (attendanceResponse as any)?.data ?? attendanceResponse;
                    const attendances = Array.isArray(attendancePayload?.data)
                        ? attendancePayload.data
                        : attendancePayload || [];

                    const sessionExcuses = attendances
                        .filter((att: any) => att.excuseReason && att.excuseStatus)
                        .map((att: any) => ({
                            attendanceId: att.id,
                            sessionId: session.id,
                            userId: att.userId,
                            userName: memberNameById.get(att.userId) || '이름 없음',
                            userNickname: memberNameById.get(att.userId) || '이름 없음',
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

            const statusOrder: Record<ExcuseStatus, number> = {
                PENDING: 0,
                REJECTED: 1,
                APPROVED: 2,
            };
            allExcuses.sort((a, b) => {
                const statusDiff = statusOrder[a.excuseStatus] - statusOrder[b.excuseStatus];
                if (statusDiff !== 0) return statusDiff;
                const aTime = new Date(a.checkedAt || a.scheduledAt).getTime();
                const bTime = new Date(b.checkedAt || b.scheduledAt).getTime();
                if (aTime !== bTime) return bTime - aTime;
                return b.attendanceId - a.attendanceId;
            });
            setExcuses(allExcuses);
        } catch (error) {
            console.error('소명 목록 조회 실패:', error);
            showToast('소명 목록을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (excuse: Excuse, decision: 'APPROVED' | 'REJECTED') => {
        try {
            await studyApi.decideExcuse(studyId, excuse.sessionId, excuse.userId, decision);

            setExcuses((prev) =>
                prev.map((item) =>
                    item.attendanceId === excuse.attendanceId
                        ? { ...item, excuseStatus: decision }
                        : item
                )
            );

            showToast(
                decision === 'APPROVED' ? '소명을 승인했습니다.' : '소명을 거절했습니다.',
                decision === 'APPROVED' ? 'success' : 'info'
            );

            await fetchExcuses();
        } catch (error) {
            console.error('소명 처리 실패:', error);
            showToast('소명 처리에 실패했습니다.', 'error');
        }
    };

    const filteredExcuses = excuses.filter((excuse) =>
        filterStatus === 'all' ? true : excuse.excuseStatus === filterStatus
    );
    const pageSize = 10;
    const pageGroupSize = 5;
    const totalPages = Math.max(1, Math.ceil(filteredExcuses.length / pageSize));
    const safePage = Math.min(currentPage, totalPages);
    const startIndex = (safePage - 1) * pageSize;
    const pagedExcuses = filteredExcuses.slice(startIndex, startIndex + pageSize);
    const currentGroup = Math.floor((safePage - 1) / pageGroupSize);
    const groupStart = currentGroup * pageGroupSize + 1;
    const groupEnd = Math.min(groupStart + pageGroupSize - 1, totalPages);

    const pendingCount = excuses.filter((e) => e.excuseStatus === 'PENDING').length;

    const getStatusStyle = (status: ExcuseStatus) => {
        switch (status) {
            case 'APPROVED':
                return 'bg-success/10 text-success';
            case 'REJECTED':
                return 'bg-error/10 text-error';
            default:
                return 'bg-warning/10 text-warning';
        }
    };

    const getStatusLabel = (status: ExcuseStatus) => {
        switch (status) {
            case 'APPROVED':
                return '승인됨';
            case 'REJECTED':
                return '거절됨';
            default:
                return '대기 중';
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
                    <p className="text-sm text-text-secondary mt-1">
                        멤버들의 결석 소명 요청을 처리하세요
                    </p>
                </div>
            </div>

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

            {filteredExcuses.length === 0 ? (
                <div className="text-center py-12 bg-background-secondary rounded-2xl">
                    <FileWarning size={48} className="mx-auto text-text-muted mb-4" />
                    <p className="text-text-secondary">처리할 소명 요청이 없습니다</p>
                </div>
            ) : (
                <div className="space-y-3">
                    {pagedExcuses.map((excuse) => (
                        <div
                            key={excuse.attendanceId}
                            className="bg-background-secondary rounded-2xl border border-border-light overflow-hidden"
                        >
                            <div
                                className="p-4 flex items-center gap-4 cursor-pointer hover:bg-surface/50 transition-colors"
                                onClick={() => setExpandedId(expandedId === excuse.attendanceId ? null : excuse.attendanceId)}
                            >
                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                                    {excuse.userNickname.charAt(0).toUpperCase()}
                                </div>

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
                                        <span>{formatDate(excuse.scheduledAt)}</span>
                                    </div>
                                </div>

                                <ChevronDown
                                    size={18}
                                    className={`text-text-tertiary transition-transform ${expandedId === excuse.attendanceId ? 'rotate-180' : ''}`}
                                />
                            </div>

                            {expandedId === excuse.attendanceId && (
                                <div className="px-4 pb-4 pt-0 space-y-4 border-t border-border-light mt-0">
                                    <div className="pt-4">
                                        <div className="bg-surface rounded-xl p-4 mb-4">
                                            <div className="flex items-center gap-2 text-sm font-medium text-text-secondary mb-2">
                                                <MessageSquare size={14} />
                                                소명 사유
                                            </div>
                                            <p className="text-text-primary text-sm leading-relaxed">
                                                {excuse.excuseReason}
                                            </p>
                                        </div>

                                        {excuse.excuseStatus === 'PENDING' && (
                                            <div className="flex gap-3">
                                                <button
                                                    onClick={() => handleAction(excuse, 'APPROVED')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-success/10 text-success font-medium hover:bg-success/20 transition-colors"
                                                >
                                                    <Check size={18} />
                                                    소명 승인
                                                </button>
                                                <button
                                                    onClick={() => handleAction(excuse, 'REJECTED')}
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
            {filteredExcuses.length > 0 && (
                <div className="flex items-center justify-center gap-2 pt-2">
                    <button
                        type="button"
                        className="px-3 py-1.5 rounded-lg text-sm font-medium bg-background-secondary text-text-secondary hover:bg-background-tertiary disabled:opacity-50"
                        onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                        disabled={safePage === 1}
                    >
                        이전
                    </button>
                    <div className="flex items-center gap-1">
                        {Array.from({ length: groupEnd - groupStart + 1 }, (_, idx) => {
                            const page = groupStart + idx;
                            return (
                                <button
                                    key={page}
                                    type="button"
                                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                                        page === safePage
                                            ? 'bg-primary text-white'
                                            : 'bg-background-secondary text-text-secondary hover:bg-background-tertiary'
                                    }`}
                                    onClick={() => setCurrentPage(page)}
                                >
                                    {page}
                                </button>
                            );
                        })}
                    </div>
                    <button
                        type="button"
                        className="px-3 py-1.5 rounded-lg text-sm font-medium bg-background-secondary text-text-secondary hover:bg-background-tertiary disabled:opacity-50"
                        onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                        disabled={safePage === totalPages}
                    >
                        다음
                    </button>
                </div>
            )}
        </div>
    );
};

export default ExcuseManagement;
