import { useState } from 'react';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

interface ProfileImageSelectorProps {
    oauthImageUrl?: string;
    onImageSelect: (imageUrl: string | null) => void;
}

/**
 * 프로필 이미지 선택 컴포넌트
 * OAuth 이미지 또는 기본 이미지 중 선택 가능
 */
export const ProfileImageSelector = ({ oauthImageUrl, onImageSelect }: ProfileImageSelectorProps) => {
    const [selectedType, setSelectedType] = useState<'oauth' | 'default'>(
        oauthImageUrl ? 'oauth' : 'default'
    );

    const handleSelect = (type: 'oauth' | 'default') => {
        setSelectedType(type);
        onImageSelect(type === 'oauth' ? oauthImageUrl || null : null);
    };

    return (
        <div className="profile-image-selector">
            <label className="form-label">프로필 이미지</label>

            <div className="image-options">
                {/* OAuth 프로필 이미지 옵션 */}
                {oauthImageUrl && (
                    <div
                        className={`image-option ${selectedType === 'oauth' ? 'selected' : ''}`}
                        onClick={() => handleSelect('oauth')}
                    >
                        <div className="image-preview">
                            <img src={oauthImageUrl} alt="OAuth 프로필" />
                        </div>
                        <p className="image-option-label">카카오 프로필</p>
                        {selectedType === 'oauth' && (
                            <div className="selected-badge">✓</div>
                        )}
                    </div>
                )}

                {/* 기본 이미지 옵션 */}
                <div
                    className={`image-option ${selectedType === 'default' ? 'selected' : ''}`}
                    onClick={() => handleSelect('default')}
                >
                    <div className="image-preview">
                        <img src={DEFAULT_PROFILE_IMAGE} alt="기본 프로필" />
                    </div>
                    <p className="image-option-label">기본 이미지</p>
                    {selectedType === 'default' && (
                        <div className="selected-badge">✓</div>
                    )}
                </div>
            </div>

            <p className="image-helper-text">
                {selectedType === 'oauth'
                    ? '카카오 프로필 이미지를 사용합니다.'
                    : '기본 프로필 이미지를 사용합니다.'}
            </p>

            <style>{`
                .profile-image-selector {
                    margin-bottom: 1.5rem;
                }

                .form-label {
                    display: block;
                    margin-bottom: 0.75rem;
                    font-weight: 600;
                    color: var(--color-text-primary);
                }

                .image-options {
                    display: flex;
                    gap: 1rem;
                    margin-bottom: 0.5rem;
                }

                .image-option {
                    position: relative;
                    flex: 1;
                    padding: 1rem;
                    border: 2px solid var(--color-border);
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.2s;
                    text-align: center;
                }

                .image-option:hover {
                    border-color: var(--color-primary);
                    background-color: var(--color-background-secondary);
                }

                .image-option.selected {
                    border-color: var(--color-primary);
                    background-color: var(--color-background-secondary);
                }

                .image-preview {
                    width: 80px;
                    height: 80px;
                    margin: 0 auto 0.75rem;
                    border-radius: 50%;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background-color: var(--color-background-tertiary);
                }

                .image-preview img {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                }

                .image-preview.default {
                    color: var(--color-text-tertiary);
                }

                .image-option-label {
                    font-size: 0.875rem;
                    font-weight: 500;
                    color: var(--color-text-secondary);
                    margin: 0;
                }

                .selected-badge {
                    position: absolute;
                    top: 0.5rem;
                    right: 0.5rem;
                    width: 24px;
                    height: 24px;
                    background-color: var(--color-primary);
                    color: white;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 0.75rem;
                    font-weight: 700;
                }

                .image-helper-text {
                    font-size: 0.8125rem;
                    color: var(--color-text-tertiary);
                    margin: 0;
                }
            `}</style>
        </div>
    );
};
