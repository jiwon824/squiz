import React from 'react';
import { Mail, Calendar, Edit2, Loader2 } from 'lucide-react';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

interface ProfileHeaderProps {
    userData: {
        name: string;
        nickname?: string;
        email: string;
        avatar?: string;
        bio?: string;
    };
    isEditable?: boolean;
    onEditClick?: () => void;
    onImageEditClick?: () => void;
    isImageUploading?: boolean;
}

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({
    userData,
    isEditable = false,
    onEditClick,
    onImageEditClick,
    isImageUploading = false,
}) => {
    return (
        <div className="profile-header">
            <div className="profile-avatar-wrapper">
                <img
                    src={userData.avatar || DEFAULT_PROFILE_IMAGE}
                    alt="프로필 이미지"
                    className={`profile-avatar ${isImageUploading ? 'opacity-50' : ''}`}
                    onError={(e) => { (e.target as HTMLImageElement).src = DEFAULT_PROFILE_IMAGE; }}
                />

                {isImageUploading && (
                    <div className="absolute inset-0 flex items-center justify-center">
                        <Loader2 className="animate-spin text-white" size={32} />
                    </div>
                )}

                {isEditable && (
                    <button
                        className="avatar-edit-btn"
                        title="프로필 사진 변경"
                        onClick={onImageEditClick}
                        disabled={isImageUploading}
                    >
                        <Edit2 size={16} />
                    </button>
                )}
            </div>

            <div className="profile-info">
                <h1 className="profile-name">{userData.nickname || userData.name || '사용자'}</h1>
                <div className="profile-meta">
                    <span className="meta-item">
                        <Mail size={16} />
                        {userData.email}
                    </span>
                    <span className="meta-item">
                        <Calendar size={16} />
                        {userData.bio || 'SQUIZ와 함께 성장 중'}
                    </span>
                </div>
            </div>

            {isEditable && (
                <button
                    className="btn-edit-profile"
                    onClick={onEditClick}
                >
                    <Edit2 size={18} />
                    프로필 편집
                </button>
            )}
        </div>
    );
};
