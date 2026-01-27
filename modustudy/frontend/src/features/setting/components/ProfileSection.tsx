/**
 * ProfileSection 컴포넌트
 * 프로필 설정 섹션을 담당합니다.
 * 기존 /profile 페이지의 ProfileHeader, EditProfileModal 컴포넌트를 재사용합니다.
 */

import { useRef, useState } from 'react';
import { User } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { userApi } from '@/api/endpoints/userApi';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { EditProfileModal } from '@/features/profile/components/EditProfileModal';

export const ProfileSection = () => {
    const { user, updateUser } = useAuthStore();
    const fileInputRef = useRef<HTMLInputElement>(null);

    // 모달 상태
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // 이미지 업로드 상태
    const [isImageUploading, setIsImageUploading] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    // 프로필 이미지 클릭 핸들러
    const handleImageClick = () => {
        fileInputRef.current?.click();
    };

    // 프로필 이미지 업로드 핸들러
    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 파일 타입 검사
        if (!file.type.startsWith('image/')) {
            setMessage({ type: 'error', text: '이미지 파일만 업로드 가능합니다.' });
            return;
        }

        // 파일 크기 검사 (5MB)
        if (file.size > 5 * 1024 * 1024) {
            setMessage({ type: 'error', text: '파일 크기는 5MB 이하여야 합니다.' });
            return;
        }

        setIsImageUploading(true);
        setMessage(null);

        try {
            const updatedUser = await userApi.updateProfileImage(file);
            updateUser({ avatar: updatedUser.profileImage || undefined });
            setMessage({ type: 'success', text: '프로필 이미지가 변경되었습니다.' });
        } catch (error) {
            console.error('Image upload error:', error);
            setMessage({ type: 'error', text: '이미지 업로드 중 오류가 발생했습니다.' });
        } finally {
            setIsImageUploading(false);
        }
    };

    return (
        <section className="setting-section">
            {/* 섹션 헤더 */}
            <div className="section-header">
                <h2 className="section-title">
                    <User className="section-title-icon" />
                    프로필 설정
                </h2>
                <p className="section-description">
                    공개 프로필 정보를 관리합니다. 다른 사용자에게 보여지는 정보입니다.
                </p>
            </div>

            {/* 기존 ProfileHeader 컴포넌트 재사용 */}
            <div className="profile-header-wrapper">
                <ProfileHeader
                    userData={{
                        name: user?.name || '',
                        nickname: user?.nickname,
                        email: user?.email || '',
                        avatar: user?.avatar,
                        bio: user?.bio,
                    }}
                    isEditable={true}
                    onEditClick={() => setIsEditModalOpen(true)}
                    onImageEditClick={handleImageClick}
                    isImageUploading={isImageUploading}
                />
            </div>

            {/* 숨겨진 파일 입력창 */}
            <input
                type="file"
                ref={fileInputRef}
                onChange={handleImageChange}
                accept="image/*"
                style={{ display: 'none' }}
            />

            {/* 메시지 표시 */}
            {message && (
                <div
                    className="warning-message"
                    style={{
                        marginTop: '1rem',
                        background: message.type === 'success' ? '#f0fdf4' : '#fef2f2',
                        borderColor: message.type === 'success' ? '#86efac' : '#fecaca',
                    }}
                >
                    <span
                        style={{
                            color: message.type === 'success' ? '#166534' : '#991b1b',
                        }}
                    >
                        {message.text}
                    </span>
                </div>
            )}

            {/* 프로필 편집 모달 (기존 컴포넌트 재사용) */}
            <EditProfileModal
                isOpen={isEditModalOpen}
                onClose={() => setIsEditModalOpen(false)}
            />
        </section>
    );
};
