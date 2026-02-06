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
    const response = await api.get(`/api/v1/studies/${studyId}/materials`, { params });
    // 백엔드 Page 응답 형식: { content: [...], totalElements, totalPages, ... }
    const data = response.data as { content?: MaterialListResponse[] } | MaterialListResponse[];
    // Page 응답이면 content 추출, 배열이면 그대로, 아니면 빈 배열
    if (!Array.isArray(data) && data?.content && Array.isArray(data.content)) {
      return data.content;
    }
    if (Array.isArray(data)) {
      return data;
    }
    return [];
  },

  /**
   * 자료 상세 조회
   * GET /api/v1/studies/{studyId}/materials/{materialId}
   */
  getMaterialDetail: async (studyId: number, materialId: number) => {
    const response = await api.get<MaterialDetailResponse>(`/api/v1/studies/${studyId}/materials/${materialId}`);
    // 백엔드가 MaterialDetailResponse를 직접 반환
    return response.data;
  },

  /**
   * 링크 자료 생성
   * POST /api/v1/studies/{studyId}/materials
   */
  createMaterial: async (studyId: number, data: MaterialCreateRequest) => {
    const response = await api.post<MaterialCreateResponse>(`/api/v1/studies/${studyId}/materials`, data);
    // 백엔드가 MaterialCreateResponse를 직접 반환
    return response.data;
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

    const response = await api.post<MaterialCreateResponse>(
      `/api/v1/studies/${studyId}/materials/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    // 백엔드가 MaterialCreateResponse를 직접 반환
    return response.data;
  },

  /**
   * 자료 수정
   * PUT /api/v1/studies/{studyId}/materials/{materialId}
   */
  updateMaterial: async (studyId: number, materialId: number, data: MaterialUpdateRequest) => {
    const response = await api.put<void>(
      `/api/v1/studies/${studyId}/materials/${materialId}`,
      data
    );
    // 백엔드가 void를 반환 (ResponseEntity.ok().build())
    return response.data;
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
   * GET /api/v1/studies/{studyId}/materials/{materialId}/comments
   */
  getComments: async (studyId: number, materialId: number) => {
    const response = await api.get<MaterialCommentResponse[]>(`/api/v1/studies/${studyId}/materials/${materialId}/comments`);
    // 백엔드가 List<MaterialCommentResponse>를 직접 반환
    return response.data || [];
  },

  /**
   * 댓글 작성
   * POST /api/v1/studies/{studyId}/materials/{materialId}/comments
   */
  createComment: async (studyId: number, materialId: number, data: MaterialCommentCreateRequest) => {
    const response = await api.post<MaterialCommentCreateResponse>(`/api/v1/studies/${studyId}/materials/${materialId}/comments`, data);
    return response.data;
  },

  /**
   * 댓글 삭제
   * DELETE /api/v1/studies/{studyId}/materials/{materialId}/comments/{commentId}
   */
  deleteComment: async (studyId: number, materialId: number, commentId: number) => {
    await api.delete(`/api/v1/studies/${studyId}/materials/${materialId}/comments/${commentId}`);
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
