import React from 'react';
import { Mail, Calendar, Edit2, Loader2 } from 'lucide-react';
import { LevelBadge } from '@/features/gamification/components';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

// 프로필 이미지 URL 변환 함수
// - /uploads/로 시작하는 로컬 경로는 API 서버 URL을 붙임
// - http/https로 시작하는 외부 URL은 그대로 사용
const getProfileImageUrl = (avatar?: string): string => {
    if (!avatar) return DEFAULT_PROFILE_IMAGE;

    // 외부 URL인 경우 그대로 반환
    if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
        return avatar;
    }

    // /uploads/로 시작하는 로컬 경로인 경우 API URL 붙이기
    if (avatar.startsWith('/uploads/')) {
        const apiUrl = import.meta.env.VITE_API_URL || '';
        return `${apiUrl}${avatar}`;
    }

    return avatar;
};

interface ProfileHeaderProps {
    userData: {
        name: string;
        nickname?: string;
        email: string;
        avatar?: string;
        bio?: string;
    };
    levelInfo?: {
        level: number;
        levelName: string;
    } | null;
    isEditable?: boolean;
    onEditClick?: () => void;
    onImageEditClick?: () => void;
    isImageUploading?: boolean;
}

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({
    userData,
    levelInfo,
    isEditable = false,
    onEditClick,
    onImageEditClick,
    isImageUploading = false,
}) => {
    return (
        <div className="profile-header">
            <div className="profile-avatar-wrapper">
                <img
                    src={getProfileImageUrl(userData.avatar)}
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
                <div className="flex items-center gap-3">
                    <h1 className="profile-name">{userData.nickname || userData.name || '사용자'}</h1>
                    {levelInfo && (
                        <LevelBadge
                            level={levelInfo.level}
                            levelName={levelInfo.levelName}
                            size="md"
                            variant="text"
                            showName={true}
                        />
                    )}
                </div>
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
