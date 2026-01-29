/**
 * 스터디 세션 API
 * GET /api/v1/studies/{studyId}/sessions - 세션 목록 조회
 * POST /api/v1/studies/{studyId}/sessions - 세션 생성
 * PUT /api/v1/studies/{studyId}/sessions/{sessionId} - 세션 수정
 * DELETE /api/v1/studies/{studyId}/sessions/{sessionId} - 세션 삭제
 */

import api from '../axios';

// 세션 상태 타입
export type SessionStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

// 세션 응답 타입
export interface StudySessionResponse {
  id: number;
  studyId: number;
  sessionNumber: number;
  title: string;
  description: string | null;
  scheduledAt: string; // ISO DateTime
  durationMinutes: number | null;
  location: string | null;
  isOnline: boolean;
  status: SessionStatus;
  completedAt: string | null;
  createdAt: string;
}

// 세션 생성 요청 타입
export interface SessionCreateRequest {
  title?: string;
  description?: string;
  scheduledAt: string; // ISO DateTime
  durationMinutes?: number;
  location?: string;
  isOnline?: boolean;
}

// 세션 수정 요청 타입
export interface SessionUpdateRequest {
  title?: string;
  description?: string;
  scheduledAt?: string;
  durationMinutes?: number;
  location?: string;
  isOnline?: boolean;
}

// 세션 통계 응답 타입
export interface SessionStatistics {
  totalSessions: number;
  completedSessions: number;
  scheduledSessions: number;
  cancelledSessions: number;
  completionRate: number;
}

export const sessionApi = {
  /**
   * 세션 목록 조회
   * GET /api/v1/studies/{studyId}/sessions
   */
  getSessions: async (studyId: number, status?: SessionStatus) => {
    const params = status ? { status } : {};
    const response = await api.get<StudySessionResponse[]>(
      `/api/v1/studies/${studyId}/sessions`,
      { params }
    );
    return response.data;
  },

  /**
   * 세션 단건 조회
   * GET /api/v1/studies/{studyId}/sessions/{sessionId}
   */
  getSession: async (studyId: number, sessionId: number) => {
    const response = await api.get<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}`
    );
    return response.data;
  },

  /**
   * 다음 예정 세션 조회
   * GET /api/v1/studies/{studyId}/sessions/next
   */
  getNextSession: async (studyId: number) => {
    const response = await api.get<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/next`
    );
    return response.data;
  },

  /**
   * 세션 생성
   * POST /api/v1/studies/{studyId}/sessions
   */
  createSession: async (studyId: number, data: SessionCreateRequest) => {
    const response = await api.post<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions`,
      data
    );
    return response.data;
  },

  /**
   * 세션 수정
   * PUT /api/v1/studies/{studyId}/sessions/{sessionId}
   */
  updateSession: async (
    studyId: number,
    sessionId: number,
    data: SessionUpdateRequest
  ) => {
    const response = await api.put<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}`,
      data
    );
    return response.data;
  },

  /**
   * 세션 삭제
   * DELETE /api/v1/studies/{studyId}/sessions/{sessionId}
   */
  deleteSession: async (studyId: number, sessionId: number) => {
    await api.delete(`/api/v1/studies/${studyId}/sessions/${sessionId}`);
  },

  /**
   * 세션 시작
   * POST /api/v1/studies/{studyId}/sessions/{sessionId}/start
   */
  startSession: async (studyId: number, sessionId: number) => {
    const response = await api.post<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}/start`
    );
    return response.data;
  },

  /**
   * 세션 완료
   * POST /api/v1/studies/{studyId}/sessions/{sessionId}/complete
   */
  completeSession: async (studyId: number, sessionId: number) => {
    const response = await api.post<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}/complete`
    );
    return response.data;
  },

  /**
   * 세션 취소
   * POST /api/v1/studies/{studyId}/sessions/{sessionId}/cancel
   */
  cancelSession: async (studyId: number, sessionId: number) => {
    const response = await api.post<StudySessionResponse>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}/cancel`
    );
    return response.data;
  },

  /**
   * 세션 통계 조회
   * GET /api/v1/studies/{studyId}/sessions/statistics
   */
  getStatistics: async (studyId: number) => {
    const response = await api.get<SessionStatistics>(
      `/api/v1/studies/${studyId}/sessions/statistics`
    );
    return response.data;
  },
};
