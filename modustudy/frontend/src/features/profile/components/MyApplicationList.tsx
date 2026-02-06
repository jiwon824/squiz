import React, { useState, useEffect } from 'react';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';
import { Clock, CheckCircle, XCircle, FileText, Calendar, Filter } from 'lucide-react';
import { cn, classBuilder } from '@/shared/utils/cn';
import { useNavigate } from 'react-router-dom';

type ApplicationStatus = 'all' | 'PENDING' | 'REJECTED';

interface Application {
    applicationId: number;
    studyId: number;
    studyName: string;
    message: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    createdAt: string;
    processedAt?: string;
}

export const MyApplicationList: React.FC = () => {
    const [applications, setApplications] = useState<Application[]>([]);
    const [filterStatus, setFilterStatus] = useState<ApplicationStatus>('all');
    const [loading, setLoading] = useState(true);
    const { showToast } = useUIStore();
    const navigate = useNavigate();

    useEffect(() => {
        fetchApplications();
    }, [filterStatus]);

    const fetchApplications = async () => {
        setLoading(true);
        try {
            const response = await studyApi.getMyApplications(
                filterStatus === 'all' ? undefined : filterStatus,
                0,
                50
            );

            const content = response?.data?.content || response?.content || [];

            const mappedApplications: Application[] = content
                // 승인된 신청은 "내 스터디 활동"에 표시되므로 여기서는 제외
                .filter((app: any) => app.status !== 'APPROVED')
                .map((app: any) => ({
                    applicationId: app.applicationId,
                    studyId: app.studyId,
                    studyName: app.studyName || '스터디명 없음',
                    message: app.message || '',
                    status: app.status,
                    createdAt: app.createdAt,
                    processedAt: app.processedAt,
                }));

            setApplications(mappedApplications);
        } catch (error) {
            showToast('신청 내역을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'APPROVED':
                return 'bg-success/10 text-success border-success/30';
            case 'REJECTED':
                return 'bg-error/10 text-error border-error/30';
            default:
                return 'bg-warning/10 text-warning border-warning/30';
        }
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'APPROVED':
                return <CheckCircle size={16} />;
            case 'REJECTED':
                return <XCircle size={16} />;
            default:
                return <Clock size={16} />;
        }
    };

    const getStatusLabel = (status: string) => {
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
        return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
    };

    const handleStudyClick = (studyId: number) => {
        navigate(`/study/${studyId}`);
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">신청 내역을 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary">내 스터디 신청 내역</h2>
                    <p className="text-sm text-text-secondary mt-1">대기 중이거나 거절된 신청 내역입니다. 승인된 스터디는 아래 '내 스터디 활동'에서 확인하세요.</p>
                </div>
            </div>

            {/* 필터 */}
            <div className="flex items-center gap-3">
                <Filter size={18} className="text-text-tertiary" />
                <div className="flex gap-2">
                    {[
                        { value: 'all', label: '전체' },
                        { value: 'PENDING', label: '대기 중' },
                        { value: 'REJECTED', label: '거절됨' },
                    ].map((filter) => (
                        <button
                            key={filter.value}
                            onClick={() => setFilterStatus(filter.value as ApplicationStatus)}
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

            {/* 신청 내역 목록 */}
            {applications.length === 0 ? (
                <div className={cn(classBuilder.card('elevated'), 'text-center py-12')}>
                    <FileText size={48} className="mx-auto text-text-muted mb-4" />
                    <p className="text-text-secondary">
                        {filterStatus === 'all' ? '아직 신청한 스터디가 없습니다' : '해당 상태의 신청 내역이 없습니다'}
                    </p>
                </div>
            ) : (
                <div className="space-y-3">
                    {applications.map((app) => (
                        <div
                            key={app.applicationId}
                            className={cn(classBuilder.card('elevated'), 'p-5 transition-all cursor-pointer')}
                            onClick={() => handleStudyClick(app.studyId)}
                        >
                            <div className="flex items-start justify-between gap-4">
                                {/* 스터디 정보 */}
                                <div className="flex-1 min-w-0">
                                    <h3 className="font-bold text-text-primary mb-2 line-clamp-1">{app.studyName}</h3>
                                    <p className="text-sm text-text-secondary mb-3 line-clamp-2">{app.message}</p>
                                    <div className="flex items-center gap-3 text-xs text-text-tertiary">
                                        <span className="flex items-center gap-1">
                                            <Calendar size={12} />
                                            신청일: {formatDate(app.createdAt)}
                                        </span>
                                        {app.processedAt && (
                                            <span className="flex items-center gap-1">
                                                <Clock size={12} />
                                                처리일: {formatDate(app.processedAt)}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* 상태 배지 */}
                                <div
                                    className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium border ${getStatusStyle(
                                        app.status
                                    )}`}
                                >
                                    {getStatusIcon(app.status)}
                                    {getStatusLabel(app.status)}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
