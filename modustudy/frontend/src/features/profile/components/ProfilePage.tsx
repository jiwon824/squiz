import { useRef, useState, useEffect } from 'react';
import { Camera, Trash2, X, User, BookOpen, Bookmark, Shield, Activity } from 'lucide-react';
import '../styles/ProfilePage.css';
import '@/features/setting/styles/SettingPage.css';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { Spinner } from '@/shared/components/Spinner';
import { EditProfileModal } from './EditProfileModal';
import { PasswordResetModal } from '@/features/auth/components/PasswordResetModal';
import { userApi } from '@/api/endpoints/userApi';
import { ProfileHeader } from './ProfileHeader';
import { StudyMylist, StudyActivity } from './StudyMylist';
import { LegoActivityGraph } from './LegoActivityGraph';
import { MyApplicationList } from './MyApplicationList';
import { MyBookmarkList } from './MyBookmarkList';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { PageSidebar, SidebarItem } from '@/shared/components/PageSidebar';
import { gamificationApi, UserStatsResponse } from '@/api/endpoints/gamificationApi';
import { studyApi, StudyListResponse } from '@/api/endpoints/studyApi';
import { LevelProgressBar } from '@/features/gamification/components';

// 프로필 섹션 타입
type ProfileSection = 'profile' | 'study' | 'bookmark' | 'activity' | 'security';

// 사이드바 메뉴 아이템
const menuItems: Array<SidebarItem & { id: ProfileSection }> = [
    {
        id: 'profile',
        label: '프로필 정보',
        description: '기본 정보 및 레벨',
        icon: <User size={18} />,
    },
    {
        id: 'study',
        label: '스터디 활동',
        description: '참여 스터디 목록',
        icon: <BookOpen size={18} />,
    },
    {
        id: 'bookmark',
        label: '북마크',
        description: '관심 스터디',
        icon: <Bookmark size={18} />,
    },
    {
        id: 'activity',
        label: '활동 기록',
        description: '활동 잔디 그래프',
        icon: <Activity size={18} />,
    },
    {
        id: 'security',
        label: '보안',
        description: '비밀번호 설정',
        icon: <Shield size={18} />,
    },
];

export const ProfilePage = () => {
    const { user, updateUser } = useAuthStore();
    const showToast = useUIStore((state) => state.showToast);
    const [activeSection, setActiveSection] = useState<ProfileSection>('profile');
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
    const [isImageActionModalOpen, setIsImageActionModalOpen] = useState(false);
    const [isImageUploading, setIsImageUploading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // API 데이터 상태
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

                // 게이미피케이션 통계 처리
                if (statsRes.status === 'fulfilled') {
                    setGamificationStats(statsRes.value);
                }

                // 스터디 목록 처리
                if (studiesRes.status === 'fulfilled') {
                    const studiesData = studiesRes.value;
                    const content = studiesData?.content || [];

                    // API 응답을 StudyActivity 형식으로 변환
                    const mapStatus = (status: string, startDate?: string): 'active' | 'completed' | 'pending' => {
                        if (status === 'COMPLETED') return 'completed';
                        if (status === 'IN_PROGRESS') return 'active';

                        if (startDate) {
                            const start = new Date(startDate);
                            start.setHours(0, 0, 0, 0);
                            const today = new Date();
                            today.setHours(0, 0, 0, 0);
                            if (start <= today) return 'active';
                        }

                        return 'pending';
                    };

                    const mappedStudies: StudyActivity[] = content.map((study: StudyListResponse) => ({
                        id: study.id,
                        title: study.name,
                        status: mapStatus(study.status, study.startDate),
                        description: `${study.scheduleDays || ''} ${study.scheduleTime || ''} | ${study.description || ''}`.trim(),
                        attendanceRate: 0,
                        participationDays: 0,
                    }));

                    setMyStudies(mappedStudies);
                }

                // 활동 기록 처리 (잔디 그래프용)
                if (contributionsRes.status === 'fulfilled') {
                    const contrib = contributionsRes.value;
                    const levels = (contrib.contributions || []).map((c) => {
                        const count = c.activityCount || 0;
                        if (count === 0) return 0;
                        if (count === 1) return 1;
                        if (count <= 3) return 2;
                        if (count <= 5) return 3;
                        return 4;
                    });
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

    // 이미지 편집 버튼 클릭 시 액션 선택 모달 열기
    const handleImageClick = () => {
        setIsImageActionModalOpen(true);
    };

    // 이미지 변경 선택 시 파일 입력창 열기
    const handleImageChangeSelect = () => {
        setIsImageActionModalOpen(false);
        fileInputRef.current?.click();
    };

    // 이미지 삭제 (기본 이미지로 변경)
    const handleImageDelete = async () => {
        setIsImageActionModalOpen(false);
        setIsImageUploading(true);
        try {
            const updatedUser = await userApi.deleteProfileImage();
            updateUser({ avatar: updatedUser.profileImage || undefined });
            showToast('프로필 이미지가 삭제되었습니다.', 'success');
        } catch (error) {
            console.error('Image delete error:', error);
            showToast('이미지 삭제 중 오류가 발생했습니다.', 'error');
        } finally {
            setIsImageUploading(false);
        }
    };

    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

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
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    // 섹션별 렌더링
    const renderSection = () => {
        switch (activeSection) {
            case 'profile':
                return (
                    <>
                        {/* 프로필 헤더 */}
                        <div className="setting-section">
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
                        </div>

                        {/* 레벨 상세 카드 */}
                        {gamificationStats && (
                            <div className="setting-section">
                                <LevelProgressBar
                                    stats={gamificationStats}
                                    variant="full"
                                />
                            </div>
                        )}

                        {/* 내 스터디 신청 내역 */}
                        <div className="setting-section">
                            <MyApplicationList />
                        </div>
                    </>
                );

            case 'study':
                return (
                    <div className="setting-section !p-0 !bg-transparent !shadow-none">
                        {isLoading ? (
                            <Spinner variant="center" size="lg" label="스터디 목록을 불러오는 중..." />
                        ) : (
                            <StudyMylist studies={myStudies} />
                        )}
                    </div>
                );

            case 'bookmark':
                return (
                    <div className="setting-section">
                        <MyBookmarkList />
                    </div>
                );

            case 'activity':
                return (
                    <div className="setting-section">
                        <div className="section-header">
                            <h2 className="section-title">
                                <Activity size={24} className="text-primary" />
                                활동 기록
                            </h2>
                            <p className="section-description">최근 28일간의 학습 활동을 확인하세요.</p>
                        </div>
                        {activityData.length > 0 ? (
                            <LegoActivityGraph data={activityData} />
                        ) : (
                            <LegoActivityGraph data={[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]} />
                        )}
                    </div>
                );

            case 'security':
                return (
                    <div className="setting-section">
                        <div className="section-header">
                            <h2 className="section-title">
                                <Shield size={24} className="text-primary" />
                                보안 설정
                            </h2>
                            <p className="section-description">계정 보안을 관리합니다.</p>
                        </div>

                        <div className="setting-item">
                            <div className="setting-item-info">
                                <div className="setting-item-icon" style={{ color: 'var(--color-primary)' }}>
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                    </svg>
                                </div>
                                <div className="setting-item-text">
                                    <div className="setting-item-label">비밀번호 재설정</div>
                                    <div className="setting-item-desc">이메일 인증을 통해 비밀번호를 변경합니다</div>
                                </div>
                            </div>
                            <button
                                onClick={() => setIsPasswordModalOpen(true)}
                                className="btn-secondary"
                            >
                                재설정 메일 발송
                            </button>
                        </div>
                    </div>
                );

            default:
                return null;
        }
    };

    return (
        <UserLayoutV2>
            <div className="setting-page">
                <div className="setting-container">
                    {/* 좌측 네비게이션 */}
                    <PageSidebar
                        items={menuItems}
                        activeId={activeSection}
                        onSelect={(id) => setActiveSection(id as ProfileSection)}
                        title="프로필"
                    />

                    {/* 우측 컨텐츠 영역 */}
                    <main className="setting-content">
                        {isLoading && activeSection === 'profile' ? (
                            <div className="loading-spinner">
                                <div className="spinner" />
                            </div>
                        ) : (
                            renderSection()
                        )}
                    </main>
                </div>
            </div>

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

            {/* 비밀번호 재설정 모달 */}
            <PasswordResetModal
                isOpen={isPasswordModalOpen}
                onClose={() => setIsPasswordModalOpen(false)}
            />

            {/* 프로필 이미지 액션 선택 모달 */}
            {isImageActionModalOpen && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
                    onClick={() => setIsImageActionModalOpen(false)}
                >
                    <div
                        className="bg-white rounded-2xl p-6 w-[320px] shadow-xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-lg font-bold text-gray-900">프로필 사진</h3>
                            <button
                                onClick={() => setIsImageActionModalOpen(false)}
                                className="p-1 rounded-full hover:bg-gray-100 transition-colors"
                            >
                                <X size={20} className="text-gray-500" />
                            </button>
                        </div>
                        <div className="space-y-3">
                            <button
                                onClick={handleImageChangeSelect}
                                className="w-full flex items-center gap-3 p-4 rounded-xl bg-primary/10 hover:bg-primary/20 transition-colors text-left"
                            >
                                <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                                    <Camera size={20} className="text-primary" />
                                </div>
                                <div>
                                    <p className="font-semibold text-gray-900">사진 변경</p>
                                    <p className="text-sm text-gray-500">새로운 프로필 사진 업로드</p>
                                </div>
                            </button>
                            <button
                                onClick={handleImageDelete}
                                className="w-full flex items-center gap-3 p-4 rounded-xl bg-red-50 hover:bg-red-100 transition-colors text-left"
                            >
                                <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                                    <Trash2 size={20} className="text-red-500" />
                                </div>
                                <div>
                                    <p className="font-semibold text-gray-900">사진 삭제</p>
                                    <p className="text-sm text-gray-500">기본 이미지로 변경</p>
                                </div>
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </UserLayoutV2>
    );
};
