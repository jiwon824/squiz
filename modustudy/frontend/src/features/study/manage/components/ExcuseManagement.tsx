import React, { useState } from 'react';
import { 
    FileWarning, Check, X, Clock, MessageSquare, 
    Calendar, User, ChevronDown, Filter
} from 'lucide-react';

interface ExcuseManagementProps {
    studyId: number;
}

type ExcuseStatus = 'pending' | 'approved' | 'rejected';

interface Excuse {
    id: number;
    memberId: number;
    memberName: string;
    memberAvatar: string;
    meetingDate: string;
    meetingTitle: string;
    reason: string;
    status: ExcuseStatus;
    createdAt: string;
    evidence?: string;
}

// Mock 데이터
const mockExcuses: Excuse[] = [
    {
        id: 1,
        memberId: 5,
        memberName: '최준호',
        memberAvatar: 'C',
        meetingDate: '2026-01-20',
        meetingTitle: '10주차 정기 미팅',
        reason: '회사 야근으로 인해 참석이 어려웠습니다. 다음 미팅에는 꼭 참석하겠습니다.',
        status: 'pending',
        createdAt: '2026-01-21 09:30',
        evidence: '야근 증빙 캡처',
    },
    {
        id: 2,
        memberId: 3,
        memberName: '박민수',
        memberAvatar: 'P',
        meetingDate: '2026-01-23',
        meetingTitle: '11주차 정기 미팅',
        reason: '개인 사정으로 불참하게 되었습니다. 죄송합니다.',
        status: 'pending',
        createdAt: '2026-01-24 10:15',
    },
    {
        id: 3,
        memberId: 6,
        memberName: '한소희',
        memberAvatar: 'H',
        meetingDate: '2026-01-16',
        meetingTitle: '9주차 정기 미팅',
        reason: '병원 진료 일정과 겹쳐서 참석하지 못했습니다.',
        status: 'approved',
        createdAt: '2026-01-17 14:20',
        evidence: '진료 영수증',
    },
    {
        id: 4,
        memberId: 4,
        memberName: '정다은',
        memberAvatar: 'J',
        meetingDate: '2026-01-13',
        meetingTitle: '8주차 정기 미팅',
        reason: '깜빡하고 잊어버렸습니다.',
        status: 'rejected',
        createdAt: '2026-01-14 08:00',
    },
];

const ExcuseManagement: React.FC<ExcuseManagementProps> = ({ studyId }) => {
    const [excuses, setExcuses] = useState<Excuse[]>(mockExcuses);
    const [filterStatus, setFilterStatus] = useState<ExcuseStatus | 'all'>('all');
    const [expandedId, setExpandedId] = useState<number | null>(null);

    const handleAction = (excuseId: number, newStatus: 'approved' | 'rejected') => {
        setExcuses(prev => prev.map(excuse => 
            excuse.id === excuseId ? { ...excuse, status: newStatus } : excuse
        ));
    };

    const filteredExcuses = excuses.filter(excuse => 
        filterStatus === 'all' ? true : excuse.status === filterStatus
    );

    const pendingCount = excuses.filter(e => e.status === 'pending').length;

    const getStatusStyle = (status: ExcuseStatus) => {
        switch (status) {
            case 'approved': return 'bg-success/10 text-success';
            case 'rejected': return 'bg-error/10 text-error';
            default: return 'bg-warning/10 text-warning';
        }
    };

    const getStatusLabel = (status: ExcuseStatus) => {
        switch (status) {
            case 'approved': return '승인됨';
            case 'rejected': return '거절됨';
            default: return '대기 중';
        }
    };

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
                        { value: 'pending', label: '대기 중' },
                        { value: 'approved', label: '승인됨' },
                        { value: 'rejected', label: '거절됨' },
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
                            key={excuse.id}
                            className="bg-background-secondary rounded-2xl border border-border-light overflow-hidden"
                        >
                            {/* 카드 헤더 */}
                            <div 
                                className="p-4 flex items-center gap-4 cursor-pointer hover:bg-surface/50 transition-colors"
                                onClick={() => setExpandedId(expandedId === excuse.id ? null : excuse.id)}
                            >
                                {/* 아바타 */}
                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                                    {excuse.memberAvatar}
                                </div>

                                {/* 정보 */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="font-medium text-text-primary">{excuse.memberName}</span>
                                        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getStatusStyle(excuse.status)}`}>
                                            {getStatusLabel(excuse.status)}
                                        </span>
                                    </div>
                                    <div className="text-xs text-text-tertiary flex items-center gap-3 mt-1">
                                        <span className="flex items-center gap-1">
                                            <Calendar size={12} />
                                            {excuse.meetingTitle}
                                        </span>
                                        <span className="flex items-center gap-1">
                                            <Clock size={12} />
                                            {excuse.createdAt}
                                        </span>
                                    </div>
                                </div>

                                {/* 확장 아이콘 */}
                                <ChevronDown 
                                    size={18} 
                                    className={`text-text-tertiary transition-transform ${expandedId === excuse.id ? 'rotate-180' : ''}`}
                                />
                            </div>

                            {/* 확장된 내용 */}
                            {expandedId === excuse.id && (
                                <div className="px-4 pb-4 pt-0 space-y-4 border-t border-border-light mt-0">
                                    <div className="pt-4">
                                        {/* 소명 사유 */}
                                        <div className="bg-surface rounded-xl p-4 mb-4">
                                            <div className="flex items-center gap-2 text-sm font-medium text-text-secondary mb-2">
                                                <MessageSquare size={14} />
                                                소명 사유
                                            </div>
                                            <p className="text-text-primary text-sm leading-relaxed">
                                                {excuse.reason}
                                            </p>
                                            {excuse.evidence && (
                                                <div className="mt-3 pt-3 border-t border-border-light">
                                                    <span className="text-xs text-text-tertiary">
                                                        📎 첨부: {excuse.evidence}
                                                    </span>
                                                </div>
                                            )}
                                        </div>

                                        {/* 액션 버튼 */}
                                        {excuse.status === 'pending' && (
                                            <div className="flex gap-3">
                                                <button
                                                    onClick={() => handleAction(excuse.id, 'approved')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-success/10 text-success font-medium hover:bg-success/20 transition-colors"
                                                >
                                                    <Check size={18} />
                                                    소명 승인
                                                </button>
                                                <button
                                                    onClick={() => handleAction(excuse.id, 'rejected')}
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
