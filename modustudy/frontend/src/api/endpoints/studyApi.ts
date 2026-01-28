/**
 * 스터디 API
 * 스터디 조회/생성/수정 및 멤버 관련 API 엔드포인트
 */

import api from '../axios';

// 토픽 정보
export interface TopicInfo {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
}

// 형식 정보
export interface FormatInfo {
  id: number;
  name: string;
  description: string | null;
  icon: string | null;
  sortOrder: number;
}

// 스터디 상세 응답
export interface StudyDetailResponse {
  id: number;
  leaderId: number;
  name: string;
  description: string | null;
  topic: TopicInfo;
  format: FormatInfo | null;
  studyType: 'PLANNED' | 'LIGHTNING';
  meetingType: 'ONLINE' | 'OFFLINE' | 'HYBRID';
  regionId: number | null;
  locationDetail: string | null;
  scheduleSummary: string | null;
  scheduleDays: string | null;
  scheduleTime: string | null;
  maxMembers: number;
  isPublic: boolean;
  status: 'DRAFT' | 'SCHEDULED' | 'RECRUITING' | 'RECRUIT_CLOSED' | 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  penaltyPolicy: string | null;
  startDate: string;
  endDate: string;
  totalSessions: number | null;
  recruitStartDate: string | null;
  recruitEndDate: string | null;
  extensionCount: number;
  textbook: string | null;
  goal: string | null;
  difficulty: 'BEGINNER' | 'ELEMENTARY' | 'INTERMEDIATE' | 'ADVANCED' | null;
  prerequisites: string | null;
  processDetail: string | null;
  targetOrgType: string | null;
  targetOrgCriteria: Record<string, unknown> | null;
  createdAt: string;
  updatedAt: string;
}

// 스터디 목록 응답
export interface StudyListResponse {
  id: number;
  name: string;
  description: string | null;
  topic: TopicInfo;
  format: FormatInfo | null;
  studyType: 'PLANNED' | 'LIGHTNING';
  meetingType: 'ONLINE' | 'OFFLINE' | 'HYBRID';
  status: string;
  maxMembers: number;
  difficulty: string | null;
  regionId: number | null;
  locationDetail: string | null;
  scheduleDays: string | null;
  scheduleTime: string | null;
  startDate: string;
  endDate: string;
  recruitStartDate: string | null;
  recruitEndDate: string | null;
  createdAt: string;
  updatedAt: string;
}

// 스터디 멤버 응답
export interface StudyMemberResponse {
  memberId: number;
  studyId: number;
  userId: number;
  userName: string;
  userNickname: string;
  userEmail: string;
  userProfileImage?: string | null;
  role: 'LEADER' | 'MEMBER';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  isProbation: boolean;
  joinedAt: string;
}

// 페이징 응답
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export const studyApi = {
  /**
   * 스터디 상세 조회
   * GET /api/v1/study/{studyId}
   */
  getStudyDetail: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}`);
    return response.data as StudyDetailResponse;
  },

  /**
   * 스터디 존재 여부 확인
   * GET /api/v1/study/{studyId}/exists
   */
  checkStudyExists: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}/exists`);
    return response.data as boolean;
  },

  /**
   * 내 스터디 목록 조회
   * GET /api/v1/study/my
   */
  getMyStudies: async (page = 0, size = 20) => {
    const response = await api.get<any>('/api/v1/study/my', {
      params: { page, size },
    });
    return response.data as PageResponse<StudyListResponse>;
  },

  /**
   * 스터디 멤버 목록 조회
   * GET /api/v1/study/{studyId}/members
   */
  getStudyMembers: async (studyId: number, page = 0, size = 50) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}/members`, {
      params: { page, size },
    });
    return response.data as PageResponse<StudyMemberResponse>;
  },

  /**
   * 스터디 멤버 수 조회
   * GET /api/v1/study/{studyId}/members/count
   */
  getMemberCount: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}/members/count`);
    return response.data as number;
  },

  /**
   * 스터디 멤버 여부 확인
   * GET /api/v1/study/{studyId}/members/{userId}/check
   */
  checkMembership: async (studyId: number, userId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}/members/${userId}/check`);
    return response.data as boolean;
  },
};
