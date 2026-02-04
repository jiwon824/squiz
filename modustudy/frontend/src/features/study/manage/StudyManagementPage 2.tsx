import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import StudyListContainer from '../components/StudyListContainer';
import ManagementSidebar from './components/ManagementSidebar';
import ApplicantManagement from './components/ApplicantManagement';
import MemberManagement from './components/MemberManagement';
import { studyService, Study } from '../services/studyService';
import { Settings, ClipboardCheck } from 'lucide-react';
import './styles/StudyManagementPage.css';

const StudyManagementPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [study, setStudy] = useState<Study | null>(null);
    const [activeTab, setActiveTab] = useState<'dashboard' | 'applicants' | 'members' | 'attendance' | 'excuse'>('applicants');

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
            case 'applicants':
                return <ApplicantManagement studyId={study.id} />;
            case 'members':
                return <MemberManagement studyId={study.id} maxMembers={study.maxMembers} />;
            case 'attendance':
                return (
                    <div className="management-placeholder">
                        <ClipboardCheck size={48} />
                        <h3>출석 관리</h3>
                        <p>준비 중인 기능입니다. 스터디원의 출석 현황을 관리하고 체크할 수 있습니다.</p>
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <UserLayoutV2>
            <StudyListContainer>
                <div className="study-management-container">
                    <header className="management-header">
                        <div className="header-info">
                            <h1 className="study-title">
                                <Settings size={24} />
                                {study.name} 관리
                            </h1>
                            <p className="study-subtitle">스터디 리더를 위한 통합 관리 대시보드</p>
                        </div>
                    </header>

                    <div className="management-layout">
                        <ManagementSidebar
                            studyId={study.id}
                            activeTab={activeTab}
                            setActiveTab={setActiveTab}
                        />
                        <main className="management-content">
                            <div className="content-card">
                                {renderContent()}
                            </div>
                        </main>
                    </div>
                </div>
            </StudyListContainer>
        </UserLayoutV2>
    );
};

export default StudyManagementPage;
