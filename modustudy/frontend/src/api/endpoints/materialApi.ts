/**
 * 자료실 API
 */

import api from '../axios';
import type {
  MaterialListResponse,
  MaterialDetailResponse,
  MaterialCreateRequest,
  MaterialCreateResponse,
  MaterialUpdateRequest,
  MaterialCommentResponse,
  MaterialCommentCreateRequest,
  MaterialCommentCreateResponse,
  MaterialSearchCondition,
} from '@/features/material/types';

export const materialApi = {
  /**
   * 자료 목록 조회
   * GET /api/v1/studies/{studyId}/materials
   */
  getMaterials: async (studyId: number, params?: MaterialSearchCondition) => {
    const response = await api.get<any>(`/api/v1/studies/${studyId}/materials`, { params });
    // 백엔드 응답 형식에 따라 처리: { data: [...] } 또는 { success: true, data: [...] }
    const data = response.data?.data ?? response.data ?? [];
    return Array.isArray(data) ? data as MaterialListResponse[] : [];
  },

  /**
   * 자료 상세 조회
   * GET /api/v1/studies/{studyId}/materials/{materialId}
   */
  getMaterialDetail: async (studyId: number, materialId: number) => {
    const response = await api.get<any>(`/api/v1/studies/${studyId}/materials/${materialId}`);
    return response.data.data as MaterialDetailResponse;
  },

  /**
   * 링크 자료 생성
   * POST /api/v1/studies/{studyId}/materials
   */
  createMaterial: async (studyId: number, data: MaterialCreateRequest) => {
    const response = await api.post<any>(`/api/v1/studies/${studyId}/materials`, data);
    return response.data.data as MaterialCreateResponse;
  },

  /**
   * 파일 자료 업로드
   * POST /api/v1/studies/{studyId}/materials/upload
   */
  uploadMaterial: async (
    studyId: number,
    file: File,
    title: string,
    description?: string,
    weekNumber?: number
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    if (description) formData.append('description', description);
    if (weekNumber) formData.append('weekNumber', String(weekNumber));

    const response = await api.post<any>(
      `/api/v1/studies/${studyId}/materials/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data.data as MaterialCreateResponse;
  },

  /**
   * 자료 수정
   * PUT /api/v1/studies/{studyId}/materials/{materialId}
   */
  updateMaterial: async (studyId: number, materialId: number, data: MaterialUpdateRequest) => {
    const response = await api.put<any>(
      `/api/v1/studies/${studyId}/materials/${materialId}`,
      data
    );
    return response.data.data as MaterialDetailResponse;
  },

  /**
   * 자료 삭제
   * DELETE /api/v1/studies/{studyId}/materials/{materialId}
   */
  deleteMaterial: async (studyId: number, materialId: number) => {
    await api.delete(`/api/v1/studies/${studyId}/materials/${materialId}`);
  },

  /**
   * 댓글 목록 조회
   * GET /api/v1/materials/{materialId}/comments
   */
  getComments: async (materialId: number) => {
    const response = await api.get<any>(`/api/v1/materials/${materialId}/comments`);
    return response.data.data as MaterialCommentResponse[];
  },

  /**
   * 댓글 작성
   * POST /api/v1/materials/{materialId}/comments
   */
  createComment: async (materialId: number, data: MaterialCommentCreateRequest) => {
    const response = await api.post<any>(`/api/v1/materials/${materialId}/comments`, data);
    return response.data.data as MaterialCommentCreateResponse;
  },

  /**
   * 댓글 수정
   * PUT /api/v1/materials/comments/{commentId}
   */
  updateComment: async (commentId: number, content: string) => {
    const response = await api.put<any>(`/api/v1/materials/comments/${commentId}`, { content });
    return response.data.data as MaterialCommentResponse;
  },

  /**
   * 댓글 삭제
   * DELETE /api/v1/materials/comments/{commentId}
   */
  deleteComment: async (commentId: number) => {
    await api.delete(`/api/v1/materials/comments/${commentId}`);
  },

  /**
   * 파일 다운로드 URL 생성
   * GET /files/{path}?download=true
   */
  getFileDownloadUrl: (filePath: string) => {
    const baseUrl = import.meta.env.VITE_API_URL || '';
    return `${baseUrl}/files/${filePath}?download=true`;
  },

  /**
   * 파일 미리보기 URL 생성
   * GET /files/{path}
   */
  getFilePreviewUrl: (filePath: string) => {
    const baseUrl = import.meta.env.VITE_API_URL || '';
    return `${baseUrl}/files/${filePath}`;
  },
};
