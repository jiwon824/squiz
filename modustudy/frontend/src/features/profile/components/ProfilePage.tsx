import { useRef, useState, useEffect } from 'react';
import { Camera, Trash2, X } from 'lucide-react';
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
import { MyBookmarkList } from './MyBookmarkList';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { PageNavHeader } from '@/shared/components/layouts';
import { gamificationApi, UserStatsResponse } from '@/api/endpoints/gamificationApi';
import { studyApi, StudyListResponse } from '@/api/endpoints/studyApi';
import { LevelProgressBar } from '@/features/gamification/components';

export const ProfilePage = () => {
    const { user, updateUser } = useAuthStore();
    const showToast = useUIStore((state) => state.showToast);
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
                    // activityCount 기반 레벨: 0=없음, 1=1회, 2=2-3회, 3=4-5회, 4=6+회
                    const levels = (contrib.contributions || []).map((c) => {
                        const count = c.activityCount || 0;
                        if (count === 0) return 0;
                        if (count === 1) return 1;
                        if (count <= 3) return 2;
                        if (count <= 5) return 3;
                        return 4;
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
            // 파일 입력 초기화 (같은 파일 재선택 가능하도록)
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    return (
        <UserLayoutV2>
            <div className="profile-page pt-6">
                <div className="profile-container">
                    {/* 브레드크럼 + 헤더 */}
                    <PageNavHeader
                        title="프로필"
                        breadcrumbs={[
                            { label: '프로필' },
                        ]}
                        hideBackButton
                    />

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

                    {/* 내 스터디 북마크 */}
                    <div style={{ marginTop: '2rem' }}>
                        <MyBookmarkList />
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
                </div>
            </div>
        </UserLayoutV2>
    );
};
