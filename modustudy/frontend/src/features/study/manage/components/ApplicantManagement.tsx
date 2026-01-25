import React, { useState, useEffect } from 'react';
import { studyService } from '../../services/studyService';
import { Applicant } from '../../mockData';
import { Check, X, Clock, MessageSquare, User, Filter, ChevronDown, Calendar } from 'lucide-react';

interface ApplicantManagementProps {
    studyId: number;
}

type FilterStatus = 'all' | 'PENDING' | 'APPROVED' | 'REJECTED';

const ApplicantManagement: React.FC<ApplicantManagementProps> = ({ studyId }) => {
    const [applicants, setApplicants] = useState<Applicant[]>([]);
    const [filterStatus, setFilterStatus] = useState<FilterStatus>('all');
    const [expandedId, setExpandedId] = useState<number | null>(null);

    useEffect(() => {
        const data = studyService.getApplicantsByStudyId(studyId);
        setApplicants(data);
    }, [studyId]);

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

    const handleAction = (applicantId: number, status: 'APPROVED' | 'REJECTED') => {
        const success = studyService.updateApplicantStatus(applicantId, status);
        if (success) {
            setApplicants(prev => prev.map(app =>
                app.id === applicantId ? { ...app, status } : app
            ));
        }
    };

    const filteredApplicants = applicants.filter(app => 
        filterStatus === 'all' ? true : app.status === filterStatus
    );

    const pendingCount = applicants.filter(a => a.status === 'PENDING').length;

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'APPROVED': return 'bg-success/10 text-success';
            case 'REJECTED': return 'bg-error/10 text-error';
            default: return 'bg-warning/10 text-warning';
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case 'APPROVED': return '승인됨';
            case 'REJECTED': return '거절됨';
            default: return '대기 중';
        }
    };

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary flex items-center gap-2">
                        지원자 관리
                        {pendingCount > 0 && (
                            <span className="px-2 py-0.5 text-xs font-bold bg-primary/10 text-primary rounded-full">
                                {pendingCount}명 대기
                            </span>
                        )}
                    </h2>
                    <p className="text-sm text-text-secondary mt-1">스터디 참여 신청을 검토하세요</p>
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
                            onClick={() => setFilterStatus(filter.value as FilterStatus)}
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

            {/* 지원자 목록 */}
            {filteredApplicants.length === 0 ? (
                <div className="text-center py-12 bg-background-secondary rounded-2xl">
                    <User size={48} className="mx-auto text-text-muted mb-4" />
                    <p className="text-text-secondary">
                        {filterStatus === 'all' ? '아직 신청자가 없습니다' : '해당 상태의 신청자가 없습니다'}
                    </p>
                </div>
            ) : (
                <div className="space-y-3">
                    {filteredApplicants.map((app) => (
                        <div 
                            key={app.id}
                            className="bg-background-secondary rounded-2xl border border-border-light overflow-hidden"
                        >
                            {/* 카드 헤더 */}
                            <div 
                                className="p-4 flex items-center gap-4 cursor-pointer hover:bg-surface/50 transition-colors"
                                onClick={() => setExpandedId(expandedId === app.id ? null : app.id)}
                            >
                                {/* 아바타 */}
                                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-white font-bold text-lg">
                                    {app.nickname.charAt(0)}
                                </div>

                                {/* 정보 */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="font-bold text-text-primary">{app.nickname}</span>
                                        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getStatusStyle(app.status)}`}>
                                            {getStatusLabel(app.status)}
                                        </span>
                                    </div>
                                    <div className="text-xs text-text-tertiary flex items-center gap-1 mt-1">
                                        <Calendar size={12} />
                                        {formatDate(app.createdAt)} 신청
                                    </div>
                                </div>

                                {/* 빠른 액션 (PENDING일 때만) */}
                                {app.status === 'PENDING' && (
                                    <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                                        <button
                                            onClick={() => handleAction(app.id, 'APPROVED')}
                                            className="w-9 h-9 rounded-xl bg-success/10 text-success flex items-center justify-center hover:bg-success/20 transition-colors"
                                            title="승인"
                                        >
                                            <Check size={18} />
                                        </button>
                                        <button
                                            onClick={() => handleAction(app.id, 'REJECTED')}
                                            className="w-9 h-9 rounded-xl bg-error/10 text-error flex items-center justify-center hover:bg-error/20 transition-colors"
                                            title="거절"
                                        >
                                            <X size={18} />
                                        </button>
                                    </div>
                                )}

                                {/* 확장 아이콘 */}
                                <ChevronDown 
                                    size={18} 
                                    className={`text-text-tertiary transition-transform ${expandedId === app.id ? 'rotate-180' : ''}`}
                                />
                            </div>

                            {/* 확장된 내용 */}
                            {expandedId === app.id && (
                                <div className="px-4 pb-4 border-t border-border-light">
                                    <div className="pt-4">
                                        {/* 지원 메시지 */}
                                        <div className="bg-surface rounded-xl p-4">
                                            <div className="flex items-center gap-2 text-sm font-medium text-text-secondary mb-2">
                                                <MessageSquare size={14} />
                                                지원 메시지
                                            </div>
                                            <p className="text-text-primary text-sm leading-relaxed">
                                                {app.message || '작성된 메시지가 없습니다.'}
                                            </p>
                                        </div>

                                        {/* PENDING 상태일 때 전체 액션 버튼 */}
                                        {app.status === 'PENDING' && (
                                            <div className="flex gap-3 mt-4">
                                                <button
                                                    onClick={() => handleAction(app.id, 'APPROVED')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-success text-white font-medium hover:bg-success/90 transition-colors shadow-sm"
                                                >
                                                    <Check size={18} />
                                                    참여 승인
                                                </button>
                                                <button
                                                    onClick={() => handleAction(app.id, 'REJECTED')}
                                                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-error/10 text-error font-medium hover:bg-error/20 transition-colors"
                                                >
                                                    <X size={18} />
                                                    신청 거절
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

export default ApplicantManagement;
