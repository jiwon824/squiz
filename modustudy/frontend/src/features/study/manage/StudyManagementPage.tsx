import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
import ManagementSidebar from './components/ManagementSidebar';
import TeamDashboard from './components/TeamDashboard';
import ApplicantManagement from './components/ApplicantManagement';
import MemberManagement from './components/MemberManagement';
import AttendanceManagement from './components/AttendanceManagement';
import ExcuseManagement from './components/ExcuseManagement';
import { studyService, Study } from '../services/studyService';
import { Settings, ArrowLeft } from 'lucide-react';

export type ManageTab = 'dashboard' | 'applicants' | 'members' | 'attendance' | 'excuse';

const StudyManagementPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [study, setStudy] = useState<Study | null>(null);
    const [activeTab, setActiveTab] = useState<ManageTab>('dashboard');

    useEffect(() => {
        if (id) {
            const data = studyService.getStudyById(Number(id));
            if (data) {
                setStudy(data);
            } else {
                navigate('/study');
            }
        }
    }, [id, navigate]);

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
        <MainLayout>
            <div className="max-w-7xl mx-auto px-4 py-6">
                {/* 헤더 */}
                <header className="mb-8">
                    <button 
                        onClick={() => navigate(`/study/${study.id}`)}
                        className="flex items-center gap-2 text-text-secondary hover:text-primary mb-4 transition-colors"
                    >
                        <ArrowLeft size={18} />
                        <span className="text-sm font-medium">스터디로 돌아가기</span>
                    </button>
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
                    />
                    <main className="flex-1 min-w-0">
                        <div className="bg-surface rounded-3xl border border-border-light p-6 shadow-sm">
                            {renderContent()}
                        </div>
                    </main>
                </div>
            </div>
        </MainLayout>
    );
};

export default StudyManagementPage;
