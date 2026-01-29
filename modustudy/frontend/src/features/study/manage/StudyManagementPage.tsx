import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import ManagementSidebar from './components/ManagementSidebar';
import TeamDashboard from './components/TeamDashboard';
import ApplicantManagement from './components/ApplicantManagement';
import MemberManagement from './components/MemberManagement';
import AttendanceManagement from './components/AttendanceManagement';
import ExcuseManagement from './components/ExcuseManagement';
import { Study } from '../services/studyService';
import { studyApi } from '@/api/endpoints/studyApi';
import { BackButton } from '@/shared/components';
import { Settings } from 'lucide-react';
import { useUIStore } from '@/store/uiStore';

export type ManageTab = 'dashboard' | 'applicants' | 'members' | 'attendance' | 'excuse';

const StudyManagementPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { showToast } = useUIStore();
    const [study, setStudy] = useState<Study | null>(null);
    const [activeTab, setActiveTab] = useState<ManageTab>('dashboard');
    const [pendingApplicantCount, setPendingApplicantCount] = useState(0);
    const [pendingExcuseCount, setPendingExcuseCount] = useState(0);

    useEffect(() => {
        const fetchStudyData = async () => {
            if (!id) return;

            try {
                const data = await studyApi.getStudyDetail(Number(id));
                if (data) {
                    setStudy(data as unknown as Study);
                    // 대기중 인원수 조회
                    fetchPendingCounts(Number(id));
                } else {
                    showToast('스터디를 찾을 수 없습니다.', 'error');
                    navigate('/study');
                }
            } catch (error) {
                console.error('스터디 정보 조회 실패:', error);
                showToast('스터디 정보를 불러오는데 실패했습니다.', 'error');
                navigate('/study');
            }
        };

        fetchStudyData();
    }, [id, navigate, showToast]);

    const fetchPendingCounts = async (studyId: number) => {
        try {
            // 지원자 대기 중 인원수
            const applicantCount = await studyApi.getPendingApplicationCount(studyId);
            setPendingApplicantCount(applicantCount);

            // 소명 대기 중 인원수
            const excuseCount = await studyApi.getPendingExcuseCount(studyId);
            setPendingExcuseCount(excuseCount);
        } catch (error) {
            console.error('대기 중 인원수 조회 실패:', error);
        }
    };

    if (!study) return null;

    const renderContent = () => {
        switch (activeTab) {
            case 'dashboard':
                return <TeamDashboard study={study} />;
            case 'applicants':
                return <ApplicantManagement studyId={study.id} />;
            case 'members':
                return <MemberManagement studyId={study.id} maxMembers={study.maxMembers} />;
            case 'attendance':
                return <AttendanceManagement studyId={study.id} />;
            case 'excuse':
                return <ExcuseManagement studyId={study.id} />;
            default:
                return null;
        }
    };

    return (
        <UserLayoutV2>
            <div className="max-w-7xl mx-auto px-4 py-6">
                {/* 헤더 */}
                <header className="mb-8">
                    <div className="flex items-center gap-2 mb-4">
                        <BackButton
                            variant="icon-only"
                            onClick={() => navigate(`/study/${study.id}`)}
                        />
                        <span className="text-sm font-medium text-gray-600">스터디로 돌아가기</span>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-primary/10 flex items-center justify-center">
                            <Settings size={24} className="text-primary" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-text-primary">{study.name}</h1>
                            <p className="text-text-secondary text-sm">스터디 관리 대시보드</p>
                        </div>
                    </div>
                </header>

                {/* 레이아웃 */}
                <div className="flex gap-6">
                    <ManagementSidebar
                        studyId={study.id}
                        activeTab={activeTab}
                        setActiveTab={setActiveTab}
                        pendingApplicantCount={pendingApplicantCount}
                        pendingExcuseCount={pendingExcuseCount}
                        onRefreshCounts={() => fetchPendingCounts(study.id)}
                    />
                    <main className="flex-1 min-w-0">
                        <div className="bg-surface rounded-3xl border border-border-light p-6 shadow-sm">
                            {renderContent()}
                        </div>
                    </main>
                </div>
            </div>
        </UserLayoutV2>
    );
};

export default StudyManagementPage;
