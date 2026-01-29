/**
 * 스터디 API
 * 스터디 조회/생성/수정 및 멤버 관련 API 엔드포인트
 */

import api from '../axios';

// ========== 공통 타입 정의 ==========

// 토픽 정보
export interface TopicInfo {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
}

// 토픽 자식 (계층 구조용)
export interface TopicChild {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
}

// 토픽 부모 (계층 구조용)
export interface TopicParent {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
  children: TopicChild[];
}

// 형식 정보
export interface FormatInfo {
  id: number;
  name: string;
  description: string | null;
  icon: string | null;
  sortOrder: number;
}

// 형식 아이템 (별칭)
export interface FormatItem {
  id: number;
  name: string;
  description: string | null;
  icon: string | null;
  sortOrder: number;
}

// 지역 아이템
export interface RegionItem {
  id: number;
  code: string;
  name: string;
  fullName: string;
  level: number;
  parentId: number | null;
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

// 스터디 생성 페이로드
export interface StudyCreatePayload {
  name: string;
  intro?: string;
  description?: string;
  topicId: number;
  formatId?: number;
  studyType: string;
  meetingType: string;
  regionId?: number;
  locationDetail?: string;
  scheduleSummary?: string;
  scheduleDays?: string;
  scheduleTime?: string;
  maxMembers?: number;
  isPublic?: boolean;
  penaltyPolicy?: string;
  startDate?: string;
  endDate?: string;
  totalSessions?: number;
  recruitStartDate?: string;
  recruitEndDate?: string;
  textbook?: string;
  goal?: string;
  difficulty?: string;
  prerequisites?: string;
  processDetail?: string;
  targetOrgType?: string;
}

// 스터디 추천 아이템
export interface StudyRecommendItem {
  studyId: number;
  studyName: string;
  topicName: string;
  parentTopicName: string | null;
  formatName: string;
  difficulty: string;
  matchingScore: number;
  matchReason: string;
}

// AI 스터디 계획 요청/응답
export interface AiStudyPlanRequest {
  topic: string;
  techStack?: string[];
  schedule?: string[];
  durationWeeks?: number;  // 선호 스터디 기간 (주)
  totalSessions?: number;  // 총 회차 (요일수 × 주수)
}

// 주차별 커리큘럼 아이템
export interface AiCurriculumItem {
  week: number;
  title: string;
  description: string;
  learning_goals?: string[];
  assignments?: string[];
  resources?: string[];
}

export interface AiStudyPlanResponse {
  name: string;
  intro: string;
  description: string;
  topic: string;
  format: string;
  difficulty: string;
  goal: string;
  textbook: string;
  prerequisites: string;
  processDetail: string;
  durationWeeks?: number;
  scheduleSuggestion?: {
    days: string[];
    time: string;
  };
  // 새로 추가: 주차별 커리큘럼
  curriculum?: AiCurriculumItem[];
}

// ========== studyApi 객체 (워크스페이스 등에서 사용) ==========

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

// ========== 개별 함수 export (스터디 생성 페이지 등에서 사용) ==========

// 주제 목록 조회 (계층 구조)
export const getTopics = async (): Promise<TopicParent[]> => {
  const response = await api.get('/api/v1/topics');
  return response.data;
};

// 형식 목록 조회
export const getFormats = async (): Promise<FormatItem[]> => {
  const response = await api.get('/api/v1/formats');
  return response.data;
};

// 시/도 목록 조회
export const getProvinces = async (): Promise<RegionItem[]> => {
  const response = await api.get('/api/regions/provinces');
  return response.data;
};

// 특정 시/도의 시/군/구 목록 조회
export const getDistricts = async (provinceId: number): Promise<RegionItem[]> => {
  const response = await api.get(`/api/regions/provinces/${provinceId}/districts`);
  return response.data;
};

// 스터디 생성
export const createStudy = async (data: StudyCreatePayload) => {
  const response = await api.post('/api/v1/study', data);
  return response.data;
};

// 내 스터디 템플릿 목록 조회
export interface StudyTemplateItem {
  id: number;
  name: string;
  templateType: string;
  topic: string;
  format: string;
  difficulty: string;
  goal: string;
  textbook: string;
  description: string;
  prerequisites: string;
  processDetail: string;
  createdAt: string;
}

export const getMyTemplates = async (): Promise<StudyTemplateItem[]> => {
  const response = await api.get('/api/v1/study-templates/my');
  return response.data;
};

// 템플릿 생성 (임시저장)
export interface CreateTemplatePayload {
  name: string;
  templateType?: string;
  topic?: string;
  format?: string;
  meetingType?: string;
  description?: string;
  textbook?: string;
  goal?: string;
  difficulty?: string;
  prerequisites?: string;
  processDetail?: string;
  penaltyPolicy?: string;
}

export const createTemplate = async (data: CreateTemplatePayload): Promise<StudyTemplateItem> => {
  const response = await api.post('/api/v1/study-templates', data);
  return response.data;
};

// 사용자 맞춤 스터디 추천 목록
export const getStudyRecommendations = async (limit: number = 10): Promise<StudyRecommendItem[]> => {
  const response = await api.get(`/api/v1/study/recommend?limit=${limit}`);
  return response.data;
};

// AI 스터디 계획 생성
export const generateStudyPlan = async (data: AiStudyPlanRequest): Promise<AiStudyPlanResponse> => {
  // 백엔드 DTO 필드명에 맞게 camelCase로 전달
  const requestData = {
    topicInput: data.topic,
    durationWeeks: data.durationWeeks,
    totalSessions: data.totalSessions,  // 총 회차 (요일수 × 주수)
  };

  const response = await api.post('/api/v1/study-templates/recommend', requestData);
  const res = response.data;

  // 백엔드 응답을 프론트엔드 타입에 맞게 변환
  return {
    name: res.name || '',
    intro: res.intro || '',
    description: res.description || '',
    topic: res.topic || '',
    format: res.format || '',
    difficulty: res.difficulty || 'INTERMEDIATE',
    goal: res.goal || '',
    textbook: res.textbook || '',
    prerequisites: res.prerequisites || '',
    processDetail: res.processDetail || res.process_detail || '',
    durationWeeks: res.durationWeeks || res.duration_weeks,
    scheduleSuggestion: res.scheduleSuggestion || res.schedule_suggestion,
    curriculum: res.curriculum,
  };
};
