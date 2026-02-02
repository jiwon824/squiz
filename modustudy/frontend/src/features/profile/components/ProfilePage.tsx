import { useRef, useState, useEffect } from 'react';
import '../styles/ProfilePage.css';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { EditProfileModal } from './EditProfileModal';
import { PasswordResetModal } from '@/features/auth/components/PasswordResetModal';
import { userApi } from '@/api/endpoints/userApi';
import { ProfileHeader } from './ProfileHeader';
import { StudyMylist, StudyActivity } from './StudyMylist';
import { LegoActivityGraph } from './LegoActivityGraph';
import { MyApplicationList } from './MyApplicationList';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { gamificationApi, UserStatsResponse } from '@/api/endpoints/gamificationApi';
import { studyApi, StudyListResponse } from '@/api/endpoints/studyApi';
import { LevelProgressBar } from '@/features/gamification/components';

// 프로필 통계 타입
interface ProfileStats {
    studyCount: number;
    totalStudyTime: number;
    quizScore: number;
    attendance: number;
    totalExperience: number;
    totalAttendance: number;
    studyHours: number;
    quizCount: number;
}

export const ProfilePage = () => {
    const { user, updateUser } = useAuthStore();
    const showToast = useUIStore((state) => state.showToast);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
    const [isImageUploading, setIsImageUploading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // API 데이터 상태
    const [stats, setStats] = useState<ProfileStats>({
        studyCount: 0,
        totalStudyTime: 0,
        quizScore: 0,
        attendance: 0,
        totalExperience: 0,
        totalAttendance: 0,
        studyHours: 0,
        quizCount: 0,
    });
    const [gamificationStats, setGamificationStats] = useState<UserStatsResponse | null>(null);
    const [myStudies, setMyStudies] = useState<StudyActivity[]>([]);
    const [activityData, setActivityData] = useState<number[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // API 데이터 로드
    useEffect(() => {
        const fetchProfileData = async () => {
            setIsLoading(true);
            try {
                // 병렬로 API 호출
                const [statsRes, studiesRes, contributionsRes] = await Promise.allSettled([
                    gamificationApi.getStats(),
                    studyApi.getMyStudies(0, 50),
                    gamificationApi.getContributions(new Date().getFullYear(), new Date().getMonth() + 1),
                ]);

                // 통계 데이터 처리
                if (statsRes.status === 'fulfilled') {
                    const s = statsRes.value;
                    setGamificationStats(s); // 전체 게이미피케이션 통계 저장
                    setStats({
                        studyCount: s.totalStudiesJoined || 0,
                        totalStudyTime: Math.floor((s.totalActivityDays || 0) * 2), // 활동일 × 평균 학습시간 가정
                        quizScore: 0, // 퀴즈 평균 점수는 별도 API 필요
                        attendance: s.totalAttendance > 0 ? Math.round((s.totalAttendance / Math.max(s.totalActivityDays, 1)) * 100) : 0,
                        totalExperience: s.levelProgress?.current || 0,
                        totalAttendance: s.totalAttendance || 0,
                        studyHours: Math.floor((s.totalActivityDays || 0) * 2),
                        quizCount: s.totalQuizCount || 0,
                    });
                }

                // 스터디 목록 처리
                if (studiesRes.status === 'fulfilled') {
                    const studiesData = studiesRes.value;
                    const content = studiesData?.content || [];

                    // API 응답을 StudyActivity 형식으로 변환
                    // 상태 매핑: COMPLETED → 완료, IN_PROGRESS 또는 시작일 지남 → 진행중, 그 외 → 예정
                    const mapStatus = (status: string, startDate?: string): 'active' | 'completed' | 'pending' => {
                        if (status === 'COMPLETED') return 'completed';
                        if (status === 'IN_PROGRESS') return 'active';

                        // 시작일이 오늘이거나 지났으면 진행중으로 표시
                        if (startDate) {
                            const start = new Date(startDate);
                            start.setHours(0, 0, 0, 0);
                            const today = new Date();
                            today.setHours(0, 0, 0, 0);
                            if (start <= today) return 'active';
                        }

                        return 'pending'; // RECRUITING, PENDING, SCHEDULED 등
                    };

                    const mappedStudies: StudyActivity[] = content.map((study: StudyListResponse) => ({
                        id: study.id,
                        title: study.name,
                        status: mapStatus(study.status, study.startDate),
                        description: `${study.scheduleDays || ''} ${study.scheduleTime || ''} | ${study.description || ''}`.trim(),
                        attendanceRate: 0, // 개별 스터디 출석률은 별도 API 필요
                        participationDays: 0, // 참여일수는 별도 계산 필요
                    }));

                    setMyStudies(mappedStudies);
                }

                // 활동 기록 처리 (잔디 그래프용)
                if (contributionsRes.status === 'fulfilled') {
                    const contrib = contributionsRes.value;
                    // contributions 배열을 레벨 배열로 변환 (0-4)
                    const levels = (contrib.contributions || []).map((c) => {
                        // 활동 여부를 0-4 레벨로 변환 (간단히 0 또는 2-4 랜덤)
                        return c.hasActivity ? Math.floor(Math.random() * 3) + 2 : 0;
                    });
                    // 데이터가 없으면 빈 배열, 있으면 28일치로 제한
                    setActivityData(levels.length > 0 ? levels.slice(0, 28) : []);
                }
            } catch (error) {
                console.error('[ProfilePage] 데이터 로드 실패:', error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchProfileData();
    }, []);

    const handleImageClick = () => {
        fileInputRef.current?.click();
    };

    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 파일 타입 검사
        if (!file.type.startsWith('image/')) {
            showToast('이미지 파일만 업로드 가능합니다.', 'warning');
            return;
        }

        setIsImageUploading(true);
        try {
            const updatedUser = await userApi.updateProfileImage(file);
            updateUser({ avatar: updatedUser.profileImage || undefined });
            showToast('프로필 이미지가 변경되었습니다.', 'success');
        } catch (error) {
            console.error('Image upload error:', error);
            showToast('이미지 업로드 중 오류가 발생했습니다.', 'error');
        } finally {
            setIsImageUploading(false);
        }
    };

    return (
        <UserLayoutV2>
            <div className="profile-page pt-6">
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
                        levelInfo={gamificationStats ? {
                            level: gamificationStats.level,
                            levelName: gamificationStats.levelName,
                        } : null}
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

                            {/* Dashboard 통계 1: 누적 열정 (경험치) */}
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
                                    <span className="stat-value-dashboard">{stats.totalExperience.toLocaleString()}</span>
                                    <span className="stat-unit-dashboard">XP</span>
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
                                    <span className="stat-value-dashboard">{stats.totalAttendance}</span>
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
                                    <span className="stat-value-dashboard">{stats.studyHours}</span>
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
                                    <span className="stat-value-dashboard">{stats.quizCount}</span>
                                    <span className="stat-unit-dashboard">문제</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 레벨 상세 카드 */}
                    {gamificationStats && (
                        <div className="mt-6">
                            <LevelProgressBar
                                stats={gamificationStats}
                                variant="full"
                            />
                        </div>
                    )}

                    {/* 내 스터디 신청 내역 */}
                    <div style={{ marginTop: '3rem' }}>
                        <MyApplicationList />
                    </div>

                    {/* 내 스터디 활동 (API 연동) */}
                    {isLoading ? (
                        <div className="text-center py-12">
                            <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                            <p className="text-text-secondary mt-4">스터디 목록을 불러오는 중...</p>
                        </div>
                    ) : (
                        <StudyMylist studies={myStudies} />
                    )}

                    {/* 레고 스타일 활동 지수 (API 연동) */}
                    {activityData.length > 0 ? (
                        <LegoActivityGraph data={activityData} />
                    ) : (
                        <LegoActivityGraph data={[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]} />
                    )}

                    {/* 계정 관리 섹션 - 비밀번호 변경 */}
                    <div style={{
                        marginTop: '3rem',
                        padding: '2rem',
                        background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)',
                        borderRadius: '24px',
                        border: '1px solid #e2e8f0',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        textAlign: 'center',
                        gap: '1rem'
                    }}>
                        <div style={{
                            width: '48px',
                            height: '48px',
                            background: 'white',
                            borderRadius: '12px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
                            color: 'var(--color-primary)'
                        }}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                            </svg>
                        </div>
                        <div>
                            <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700, color: '#1e293b' }}>보안 설정</h3>
                            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.9rem', color: '#64748b' }}>
                                비밀번호를 잊으셨나요? 이메일 인증을 통해 안전하게 재설정할 수 있습니다.
                            </p>
                        </div>
                        <button
                            onClick={() => setIsPasswordModalOpen(true)}
                            style={{
                                marginTop: '0.5rem',
                                padding: '0.75rem 1.5rem',
                                background: 'white',
                                border: '1px solid #cbd5e1',
                                borderRadius: '12px',
                                color: '#334155',
                                fontWeight: 600,
                                cursor: 'pointer',
                                transition: 'all 0.2s ease',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
                            }}
                            onMouseOver={(e) => {
                                e.currentTarget.style.borderColor = 'var(--color-primary)';
                                e.currentTarget.style.color = 'var(--color-primary)';
                            }}
                            onMouseOut={(e) => {
                                e.currentTarget.style.borderColor = '#cbd5e1';
                                e.currentTarget.style.color = '#334155';
                            }}
                        >
                            비밀번호 재설정 메일 발송
                        </button>
                    </div>

                    {/* 비밀번호 재설정 모달 */}
                    <PasswordResetModal
                        isOpen={isPasswordModalOpen}
                        onClose={() => setIsPasswordModalOpen(false)}
                    />
                </div>
            </div>
        </UserLayoutV2>
    );
};
