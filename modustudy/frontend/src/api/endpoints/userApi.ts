import api from '../axios';
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
        const response = await api.put<any>('/api/v1/users/me', data);
        return response.data.data as UserDTO;
    },

    /**
     * 프로필 이미지 수정
     * POST /api/v1/users/me/profile-image
     */
    updateProfileImage: async (file: File) => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await api.post<any>('/api/v1/users/me/profile-image', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data.data as UserDTO;
    },
};
