import { useRef, useState } from 'react';
import '../styles/ProfilePage.css';
import { useAuthStore } from '@/store/authStore';
import { EditProfileModal } from './EditProfileModal';
import { userApi } from '@/api/endpoints/userApi';
import { ProfileHeader } from './ProfileHeader';
import { StudyMylist } from './StudyMylist';
import { LegoActivityGraph } from './LegoActivityGraph';
import { MainLayout } from '@/layouts/MainLayout';

export const ProfilePage = () => {
    const { user, updateUser } = useAuthStore();
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isImageUploading, setIsImageUploading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // 기본 통계 데이터 (추후 API 연동 필요)
    const stats = {
        studyCount: 5,
        totalStudyTime: 127,
        quizScore: 85,
        attendance: 92,
    };

    const handleImageClick = () => {
        fileInputRef.current?.click();
    };

    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 파일 타입 검사
        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 업로드 가능합니다.');
            return;
        }

        setIsImageUploading(true);
        try {
            const updatedUser = await userApi.updateProfileImage(file);
            updateUser({ avatar: updatedUser.profileImage || undefined });
            alert('프로필 이미지가 변경되었습니다.');
        } catch (error) {
            console.error('Image upload error:', error);
            alert('이미지 업로드 중 오류가 발생했습니다.');
        } finally {
            setIsImageUploading(false);
        }
    };

    return (
        <MainLayout>
            <div className="profile-page">
                <div className="profile-container">
                    {/* 분리된 프로필 헤더 컴포넌트 사용 */}
                    <ProfileHeader
                        userData={{
                            name: user?.name || '',
                            nickname: user?.nickname,
                            email: user?.email || '',
                            avatar: user?.avatar,
                            bio: user?.bio
                        }}
                        isEditable={true}
                        onEditClick={() => setIsEditModalOpen(true)}
                        onImageEditClick={handleImageClick}
                        isImageUploading={isImageUploading}
                    />

                    {/* 숨겨진 파일 입력창 */}
                    <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleImageChange}
                        accept="image/*"
                        className="hidden"
                    />

                    {/* 편집 모달 */}
                    <EditProfileModal
                        isOpen={isEditModalOpen}
                        onClose={() => setIsEditModalOpen(false)}
                    />

                    {/* 학습 통계 - 8개 칩 (기존 4개 + Dashboard 4개) */}
                    <div className="stats-section">
                        <h2 className="section-title">학습 통계</h2>
                        <div className="stats-grid-dashboard">
                            {/* 기존 통계 1: 참여 스터디 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-study">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">참여 스터디</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">{stats.studyCount}</span>
                                    <span className="stat-unit-dashboard">개</span>
                                </div>
                            </div>

                            {/* 기존 통계 2: 총 학습 시간 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-time">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">총 학습 시간</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">{stats.totalStudyTime}</span>
                                    <span className="stat-unit-dashboard">시간</span>
                                </div>
                            </div>

                            {/* 기존 통계 3: 평균 퀴즈 점수 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-quiz">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">평균 퀴즈 점수</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">{stats.quizScore}</span>
                                    <span className="stat-unit-dashboard">점</span>
                                </div>
                            </div>

                            {/* 기존 통계 4: 출석률 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-attendance">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">출석률</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">{stats.attendance}</span>
                                    <span className="stat-unit-dashboard">%</span>
                                </div>
                            </div>

                            {/* Dashboard 통계 1: 누적 열정 (불꽃) */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-passion">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">누적 열정</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">2,450</span>
                                    <span className="stat-unit-dashboard">FP</span>
                                </div>
                            </div>

                            {/* Dashboard 통계 2: 누적 출석수 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-attendance">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">누적 출석</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">42</span>
                                    <span className="stat-unit-dashboard">일</span>
                                </div>
                            </div>

                            {/* Dashboard 통계 3: 누적 스터디 학습 시간 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-study">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">스터디 학습</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">56</span>
                                    <span className="stat-unit-dashboard">시간</span>
                                </div>
                            </div>

                            {/* Dashboard 통계 4: 퀴즈 푼 문제 누적 */}
                            <div className="stat-chip-dashboard">
                                <div className="stat-header-dashboard">
                                    <span className="stat-icon-dashboard icon-time">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                                        </svg>
                                    </span>
                                    <span className="stat-label-dashboard">퀴즈 해결</span>
                                </div>
                                <div className="stat-value-content-dashboard">
                                    <span className="stat-value-dashboard">384</span>
                                    <span className="stat-unit-dashboard">문제</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 내 스터디 활동 (고도화된 컴포넌트 적용: 그리드/스크롤/상세이동) */}
                    <StudyMylist studies={[
                        {
                            id: 1,
                            title: '알고리즘 마스터 스터디',
                            status: 'active',
                            description: '매주 화, 목 오후 7시 | 실전 코딩 테스트 대비',
                            attendanceRate: 98,
                            participationDays: 45
                        },
                        {
                            id: 2,
                            title: 'React 고도화 프로젝트',
                            status: 'active',
                            description: '매주 월, 수 오후 8시 | Vite + TS 실전 프로젝트',
                            attendanceRate: 92,
                            participationDays: 30
                        },
                        {
                            id: 4,
                            title: '컴퓨터 구조 기반 CS 스터디',
                            status: 'active',
                            description: '매주 금 오후 9시 | 운영체제 및 가상 메모리',
                            attendanceRate: 85,
                            participationDays: 12
                        },
                        {
                            id: 5,
                            title: 'Next.js 14 서버 사이드 렌더링',
                            status: 'active',
                            description: '매주 일 오후 2시 | App Router 심화 학습',
                            attendanceRate: 100,
                            participationDays: 8
                        },
                        {
                            id: 3,
                            title: '기초 데이터 구조 스터디',
                            status: 'completed',
                            description: 'Stack, Queue, Tree 핵심 이론 완성',
                            attendanceRate: 100,
                            participationDays: 60
                        },
                        {
                            id: 6,
                            title: 'Java Spring 부트 캠프',
                            status: 'completed',
                            description: 'REST API 설계 및 JPA 실무 과정',
                            attendanceRate: 94,
                            participationDays: 90
                        },
                        {
                            id: 7,
                            title: '타입스크립트 정복기',
                            status: 'completed',
                            description: '정적 타이핑의 기초부터 제네릭까지',
                            attendanceRate: 88,
                            participationDays: 40
                        }
                    ]} />

                    {/* 레고 스타일 활동 지수 (Activity Graph) */}
                    <LegoActivityGraph data={[
                        1, 2, 0, 3, 4, 1, 2,
                        3, 0, 1, 4, 2, 3, 1,
                        0, 2, 4, 3, 1, 2, 0,
                        4, 3, 2, 1, 4, 3, 2
                    ]} />
                </div>
            </div>
        </MainLayout>
    );
};
