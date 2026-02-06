import api from '../axios';
import type { ApiResponse } from '../types';
import { UserDTO } from '@/features/auth/types';

export interface ProfileUpdateRequest {
    name: string;
    nickname: string;
    bio?: string;
}

export const userApi = {
    /**
     * 프로필 기본 정보 수정
     * PUT /api/v1/users/me
     */
    updateProfile: async (data: ProfileUpdateRequest) => {
        const response = await api.put<ApiResponse<UserDTO>>('/api/v1/users/me', data);
        return response.data.data;
    },

    /**
     * 프로필 이미지 수정
     * POST /api/v1/users/me/profile-image
     */
    updateProfileImage: async (file: File) => {
        const formData = new FormData();
        formData.append('file', file);

        // fetch API 사용 - FormData를 자동으로 multipart/form-data로 처리
        const token = localStorage.getItem('accessToken');
        const response = await fetch(
            `${import.meta.env.VITE_API_URL || ''}/api/v1/users/me/profile-image`,
            {
                method: 'POST',
                headers: {
                    Authorization: token ? `Bearer ${token}` : '',
                },
                body: formData,
                credentials: 'include',
            }
        );

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || '이미지 업로드에 실패했습니다.');
        }

        const data = await response.json();
        return data.data as UserDTO;
    },

    /**
     * 프로필 이미지 삭제 (기본 이미지로 변경)
     * DELETE /api/v1/users/me/profile-image
     */
    deleteProfileImage: async () => {
        const response = await api.delete<ApiResponse<UserDTO>>('/api/v1/users/me/profile-image');
        return response.data.data;
    },
};
