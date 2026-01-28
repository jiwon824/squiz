/**
 * 워크스페이스 API
 * 워크스페이스 및 메시지 관련 API 엔드포인트
 */

import api from '../axios';
import type {
  WorkspaceResponse,
  MessageResponse,
  MessagePageResponse,
  MessageCreateRequest,
  MessageUpdateRequest,
} from '@/features/workspace/types';

// 백엔드 메시지 응답 형식 (author 대신 개별 필드 사용)
interface BackendMessageResponse {
  id: number;
  workspaceId: number;
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  content: string;
  messageType: string;
  fileUrl: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string | null;
}

// 백엔드 응답을 프론트엔드 타입으로 변환
const transformMessage = (msg: BackendMessageResponse): MessageResponse => ({
  id: msg.id,
  workspaceId: msg.workspaceId,
  author: {
    id: msg.userId,
    nickname: msg.nickname,
    profileImageUrl: msg.profileImageUrl,
  },
  content: msg.isDeleted ? '삭제된 메시지입니다.' : msg.content,
  messageType: msg.messageType as MessageResponse['messageType'],
  createdAt: msg.createdAt,
  updatedAt: msg.updatedAt,
});

export const workspaceApi = {
  /**
   * 스터디 ID로 워크스페이스 조회
   * GET /api/v1/workspaces/study/{studyId}
   */
  getWorkspaceByStudyId: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/workspaces/study/${studyId}`);
    return response.data as WorkspaceResponse;
  },

  /**
   * 워크스페이스 ID로 조회
   * GET /api/v1/workspaces/{workspaceId}
   */
  getWorkspace: async (workspaceId: number) => {
    const response = await api.get<any>(`/api/v1/workspaces/${workspaceId}`);
    return response.data as WorkspaceResponse;
  },

  /**
   * 워크스페이스 존재 여부 확인
   * GET /api/v1/workspaces/study/{studyId}/exists
   */
  checkWorkspaceExists: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/workspaces/study/${studyId}/exists`);
    return response.data as boolean;
  },

  /**
   * 워크스페이스 생성
   * POST /api/v1/workspaces/study/{studyId}
   */
  createWorkspace: async (studyId: number) => {
    const response = await api.post<any>(`/api/v1/workspaces/study/${studyId}`);
    return response.data as WorkspaceResponse;
  },

  // ============ 메시지 API ============

  /**
   * 메시지 목록 조회 (페이징)
   * GET /api/v1/workspaces/{workspaceId}/messages
   */
  getMessages: async (workspaceId: number, page = 0, size = 50) => {
    const response = await api.get<any>(`/api/v1/workspaces/${workspaceId}/messages`, {
      params: { page, size },
    });

    // 백엔드 페이지 응답 형식 처리
    const data = response.data;
    const content = (data.content || []).map(transformMessage);

    return {
      content,
      page: data.pageNumber ?? data.page ?? 0,
      size: data.pageSize ?? data.size ?? size,
      totalElements: data.totalElements ?? 0,
      totalPages: data.totalPages ?? 1,
      first: data.pageNumber === 0,
      last: data.last ?? true,
    } as MessagePageResponse;
  },

  /**
   * 최근 메시지 조회
   * GET /api/v1/workspaces/{workspaceId}/messages/recent
   */
  getRecentMessages: async (workspaceId: number, limit = 20) => {
    const response = await api.get<any>(`/api/v1/workspaces/${workspaceId}/messages/recent`, {
      params: { limit },
    });
    const data = response.data;
    const messages = Array.isArray(data) ? data : (data.data || []);
    return messages.map(transformMessage) as MessageResponse[];
  },

  /**
   * 특정 시간 이후 메시지 조회 (폴링용)
   * GET /api/v1/workspaces/{workspaceId}/messages/after
   */
  getMessagesAfter: async (workspaceId: number, after: string) => {
    const response = await api.get<any>(`/api/v1/workspaces/${workspaceId}/messages/after`, {
      params: { after },
    });
    const data = response.data;
    const messages = Array.isArray(data) ? data : (data.data || []);
    return messages.map(transformMessage) as MessageResponse[];
  },

  /**
   * 메시지 전송
   * POST /api/v1/workspaces/{workspaceId}/messages
   */
  sendMessage: async (workspaceId: number, data: MessageCreateRequest) => {
    const response = await api.post<any>(`/api/v1/workspaces/${workspaceId}/messages`, {
      workspaceId,
      content: data.content,
      messageType: data.messageType || 'TEXT',
    });
    return transformMessage(response.data) as MessageResponse;
  },

  /**
   * 메시지 수정
   * PUT /api/v1/workspaces/{workspaceId}/messages/{messageId}
   */
  updateMessage: async (
    workspaceId: number,
    messageId: number,
    data: MessageUpdateRequest
  ) => {
    const response = await api.put<any>(
      `/api/v1/workspaces/${workspaceId}/messages/${messageId}`,
      data
    );
    return transformMessage(response.data) as MessageResponse;
  },

  /**
   * 메시지 삭제
   * DELETE /api/v1/workspaces/{workspaceId}/messages/{messageId}
   */
  deleteMessage: async (workspaceId: number, messageId: number) => {
    await api.delete(`/api/v1/workspaces/${workspaceId}/messages/${messageId}`);
  },

  /**
   * 메시지 검색
   * GET /api/v1/workspaces/{workspaceId}/messages/search
   */
  searchMessages: async (
    workspaceId: number,
    keyword: string,
    page = 0,
    size = 20
  ) => {
    const response = await api.get<any>(
      `/api/v1/workspaces/${workspaceId}/messages/search`,
      { params: { keyword, page, size } }
    );

    const data = response.data;
    const content = (data.content || []).map(transformMessage);

    return {
      content,
      page: data.pageNumber ?? 0,
      size: data.pageSize ?? size,
      totalElements: data.totalElements ?? 0,
      totalPages: data.totalPages ?? 1,
      first: data.pageNumber === 0,
      last: data.last ?? true,
    } as MessagePageResponse;
  },

  /**
   * 메시지 수 조회
   * GET /api/v1/workspaces/{workspaceId}/messages/count
   */
  getMessageCount: async (workspaceId: number) => {
    const response = await api.get<any>(`/api/v1/workspaces/${workspaceId}/messages/count`);
    return response.data as number;
  },
};
