/**
 * 스터디 API
 * 스터디 조회/생성/수정 및 멤버 관련 API 모음
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

// 토픽 자식 (계층 구조)
export interface TopicChild {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
}

// 토픽 부모 (계층 구조)
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

// 형식 아이템(별칭)
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

// 페이지 응답
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
  status?: string; // RECRUITING, PENDING 등
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
  totalSessions?: number;  // 총 회차 (요일 수 × 주수)
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
  // 추가: 주차별 커리큘럼
  curriculum?: AiCurriculumItem[];
}

// ========== 스터디 목록 조회 ==========

export interface StudyListParams {
  page?: number;
  size?: number;
  sort?: string;
  keyword?: string;
  topicId?: number;
  meetingType?: string;
  difficulty?: string;
  status?: string;
}

export interface StudyListItem {
  id: number;
  leaderId?: number; // 구버전 호환
  name: string;
  intro?: string;
  description?: string;
  topic?: {
    id: number;
    name: string;
    icon?: string;
    parent?: {
      id: number;
      name: string;
      icon?: string;
    };
  };
  format?: {
    id: number;
    name: string;
    description?: string;
    icon?: string;
  };
  studyType: string;
  meetingType: string;
  status: string;
  maxMembers: number;
  difficulty?: string;
  regionId?: number;
  scheduleDays?: string;
  scheduleTime?: string;
  startDate?: string;
  endDate?: string;
  recruitStartDate?: string;
  recruitEndDate?: string;
  createdAt: string;
  updatedAt?: string;
  // StudyResponse DTO 구조
  leader?: {
    id: number;
    nickname: string;
    profileImage?: string;
  };
}

export interface StudyListPageResponse {
  content: StudyListItem[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

/**
 * 스터디 목록 조회 (페이지)
 * GET /api/v1/study
 */
export const getStudyList = async (params: StudyListParams = {}): Promise<StudyListPageResponse> => {
  const queryParams = new URLSearchParams();
  if (params.page !== undefined) queryParams.set('page', params.page.toString());
  if (params.size !== undefined) queryParams.set('size', params.size.toString());
  if (params.sort) queryParams.set('sort', params.sort);
  if (params.keyword) queryParams.set('keyword', params.keyword);
  if (params.topicId) queryParams.set('topicId', params.topicId.toString());
  if (params.meetingType) queryParams.set('meetingType', params.meetingType);
  if (params.difficulty) queryParams.set('difficulty', params.difficulty);
  if (params.status) queryParams.set('status', params.status);

  console.log('[getStudyList] 요청:', /api/v1/study?);
  const response = await api.get(`/api/v1/study?${queryParams.toString()}`);
  console.log('[getStudyList] 응답:', response.data);

  // 백엔드 응답 형식: { success: true, data: { content: [...], ... } }
  // 또는 직접 페이지 응답: { content: [...], ... }
  const data = response.data;
  if (data.data && data.data.content !== undefined) {
    // { success: true, data: { content: [...] } } 형식
    return data.data;
  } else if (data.content !== undefined) {
    // { content: [...] } 형식 (직접 페이지 응답)
    return data;
  } else {
    // 예외 응답 처리
    console.warn('[getStudyList] 예상하지 못한 응답 구조:', data);
    return {
      content: [],
      pageable: { pageNumber: 0, pageSize: 12 },
      totalElements: 0,
      totalPages: 0,
      last: true,
      first: true,
    };
  }
};

/**
 * 모집 중인 스터디 목록 조회
 * GET /api/v1/study/recruiting
 */
export const getRecruitingStudies = async (params: StudyListParams = {}): Promise<StudyListPageResponse> => {
  const queryParams = new URLSearchParams();
  if (params.page !== undefined) queryParams.set('page', params.page.toString());
  if (params.size !== undefined) queryParams.set('size', params.size.toString());
  if (params.sort) queryParams.set('sort', params.sort);

  const response = await api.get(`/api/v1/study/recruiting?${queryParams.toString()}`);
  return response.data;
};

// ========== studyApi 객체 (워크스페이스 등에서 사용) ==========

export const studyApi = {
  /**
   * 스터디 상세 조회
   * GET /api/v1/study/{studyId}
   */
  getStudyDetail: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}`);
    let data = response.data;

    // 기존 백엔드 응답이 문자열인 경우 간단 파싱
    if (typeof data === 'string') {
      const nameMatch = data.match(/"name"\s*:\s*"([^"]+)"/);
      const idMatch = data.match(/"id"\s*:\s*(\d+)/);
      const leaderMatch = data.match(/"leaderId"\s*:\s*(\d+)/);
      data = {
        id: idMatch ? parseInt(idMatch[1]) : 0,
        leaderId: leaderMatch ? parseInt(leaderMatch[1]) : undefined,
        name: nameMatch ? nameMatch[1] : 'Study',
      };
    }

    return data as StudyDetailResponse;
  },

  /**
   * ���͵� ���� ���� Ȯ��
   * GET /api/v1/study/{studyId}/exists
   */
  checkStudyExists: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/study/${studyId}/exists`);
    return response.data as boolean;
  },

  /**
   * �� ���͵� ��� ��ȸ
   * GET /api/v1/study/my
   */
  getMyStudies: async (page = 0, size = 20) => {
    const response = await api.get<any>('/api/v1/study/my', {
      params: { page, size },
    });
    return response.data as PageResponse<StudyListResponse>;
  },

  /**
   * ���͵� ��� ��� ��ȸ
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

  // ========== 신청자 관리 (Application) ==========

  /**
   * 스터디 신청
   * POST /api/v1/study/{studyId}/applications
   */
  applyToStudy: async (studyId: number, message: string) => {
    const response = await api.post<any>(`/api/v1/study/${studyId}/applications`, { message });
    return response.data;
  },

  /**
   * 스터디 신청자 목록 조회
   * GET /api/v1/study/{studyId}/applications
   */
  getApplications: async (studyId: number, status?: string, page = 0, size = 20) => {
    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', size.toString());
    if (status && status !== 'all') {
      params.set('status', status);
    }

    const response = await api.get<any>(`/api/v1/study/${studyId}/applications?${params.toString()}`);
    return response.data;
  },

  /**
   * 신청 승인
   * PATCH /api/v1/study/{studyId}/applications/{applicationId}/approve
   */
  approveApplication: async (studyId: number, applicationId: number) => {
    const response = await api.patch<any>(`/api/v1/study/${studyId}/applications/${applicationId}/approve`);
    return response.data;
  },

  /**
   * 신청 거절
   * PATCH /api/v1/study/{studyId}/applications/{applicationId}/reject
   */
  rejectApplication: async (studyId: number, applicationId: number, rejectedReason?: string) => {
    const response = await api.patch<any>(
      `/api/v1/study/${studyId}/applications/${applicationId}/reject`,
      { rejectedReason }
    );
    return response.data;
  },

  /**
   * 대기중인 신청자 수 조회
   * GET /api/v1/study/{studyId}/applications (status=PENDING으로 카운트)
   */
  getPendingApplicationCount: async (studyId: number): Promise<number> => {
    try {
      const response = await api.get<any>(`/api/v1/study/${studyId}/applications?status=PENDING&page=0&size=1`);
      const data = response.data;
      // 응답 구조: { data: { totalElements: N } } 또는 { totalElements: N }
      return data?.data?.totalElements || data?.totalElements || 0;
    } catch (error) {
      console.error('대기중 신청자 수 조회 실패:', error);
      return 0;
    }
  },

  /**
   * 내 스터디 신청 내역 조회
   * GET /api/v1/my/applications
   */
  getMyApplications: async (status?: string, page = 0, size = 20) => {
    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', size.toString());
    if (status && status !== 'all') {
      params.set('status', status);
    }

    const response = await api.get<any>(`/api/v1/my/applications?${params.toString()}`);
    return response.data;
  },

  // ========== 세션 및 출석 관리 ==========

  /**
   * 스터디 세션 목록 조회
   * GET /api/v1/studies/{studyId}/sessions
   */
  getStudySessions: async (studyId: number) => {
    const response = await api.get<any>(`/api/v1/studies/${studyId}/sessions`);
    return response.data;
  },

  /**
   * 세션별 출석 정보 조회 (소명 정보 포함)
   * GET /api/v1/studies/{studyId}/sessions/{sessionId}/attendance
   */
  getSessionAttendance: async (studyId: number, sessionId: number) => {
    const response = await api.get<any>(`/api/v1/studies/${studyId}/sessions/${sessionId}/attendance`);
    return response.data;
  },

  /**
   * 소명 승인/거절
   * PATCH /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/{targetUserId}/excuse
   */
  decideExcuse: async (
    studyId: number,
    sessionId: number,
    targetUserId: number,
    decision: 'APPROVED' | 'REJECTED',
    decisionReason?: string
  ) => {
    const response = await api.patch<any>(
      `/api/v1/studies/${studyId}/sessions/${sessionId}/attendance/${targetUserId}/excuse`,
      {
        decision,
        decisionReason,
      }
    );
    return response.data;
  },

  /**
   * 대기중인 소명 수 조회
   * 모든 세션의 출석 정보를 조회하여 PENDING 상태인 소명 수를 카운트
   */
  getPendingExcuseCount: async (studyId: number): Promise<number> => {
    try {
      // 1. 세션 목록 조회
      const sessionsResponse = await api.get<any>(`/api/v1/studies/${studyId}/sessions`);
      const sessions = sessionsResponse.data || [];

      // 2. 각 세션의 출석 정보 조회 및 PENDING 소명 카운트
      let pendingCount = 0;

      for (const session of sessions) {
        try {
          const attendanceResponse = await api.get<any>(
            `/api/v1/studies/${studyId}/sessions/${session.id}/attendance`
          );
          const attendances = attendanceResponse.data?.data || attendanceResponse.data || [];

          // PENDING 상태의 소명 카운트
          const pending = attendances.filter(
            (att: any) => att.excuseStatus === 'PENDING' && att.excuseReason
          ).length;

          pendingCount += pending;
        } catch (error) {
          // 개별 세션 조회 실패는 무시하고 계속 진행
          console.warn(`세션 ${session.id} 출석 정보 조회 실패:`, error);
        }
      }

      return pendingCount;
    } catch (error) {
      console.error('대기중 소명 수 조회 실패:', error);
      return 0;
    }
  },

  // ========== 북마크 (Bookmark) ==========

  /**
   * 북마크 토글 (추가/삭제)
   * POST /api/v1/study/{studyId}/bookmark
   */
  toggleBookmark: async (studyId: number) => {
    const response = await api.post<any>(`/api/v1/study/${studyId}/bookmark`);
    return response.data;
  },

  /**
   * 북마크 여부 확인
   * GET /api/v1/study/{studyId}/bookmark/check
   */
  checkBookmark: async (studyId: number): Promise<boolean> => {
    try {
      const response = await api.get<any>(`/api/v1/study/${studyId}/bookmark/check`);
      return response.data;
    } catch (error) {
      console.error('북마크 여부 확인 실패:', error);
      return false;
    }
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

// 스터디 수정
export const updateStudy = async (studyId: number, data: StudyCreatePayload) => {
  const response = await api.put(`/api/v1/study/${studyId}`, data);
  return response.data;
};

// 스터디 삭제 (신청자가 0명일 때만 가능)
export const deleteStudy = async (studyId: number) => {
  const response = await api.delete(`/api/v1/study/${studyId}`);
  return response.data;
};

// 내 스터디 템플릿 목록 조회
export interface StudyTemplateItem {
  id: number;
  name: string;
  intro?: string;
  templateType: string;
  topic: string;
  format: string;
  meetingType?: string;
  difficulty: string;
  goal: string;
  textbook: string;
  description: string;
  prerequisites: string;
  processDetail: string;
  penaltyPolicy?: string;
  createdAt: string;
}

export const getMyTemplates = async (): Promise<StudyTemplateItem[]> => {
  const response = await api.get('/api/v1/study-templates/my');
  return response.data;
};

// 템플릿 생성 (임시저장)
export interface CreateTemplatePayload {
  name: string;
  intro?: string;
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

// ========== 스터디 세션(커리큘럼) 조회 ==========

export interface StudySessionItem {
  id: number;
  studyId: number;
  sessionNumber: number;
  title?: string;
  description?: string;
  scheduledAt?: string;
  durationMinutes?: number;
  location?: string;
  isOnline?: boolean;
  status: string;
  completedAt?: string;
  createdAt: string;
}

/**
 * 스터디 세션(커리큘럼) 목록 조회
 * GET /api/v1/studies/{studyId}/sessions
 */
export const getStudySessions = async (studyId: number): Promise<StudySessionItem[]> => {
  const response = await api.get(`/api/v1/studies/${studyId}/sessions`);
  return response.data;
};

/**
 * 세션 생성 요청 타입
 */
export interface SessionCreatePayload {
  title?: string;
  description?: string;
  scheduledAt: string; // ISO datetime string
  durationMinutes?: number;
  location?: string;
  isOnline?: boolean;
}

/**
 * 스터디 세션 생성
 * POST /api/v1/studies/{studyId}/sessions
 */
export const createStudySession = async (studyId: number, data: SessionCreatePayload): Promise<StudySessionItem> => {
  const response = await api.post(`/api/v1/studies/${studyId}/sessions`, data);
  return response.data;
};

/**
 * 스터디에 여러 세션 일괄 생성
 * 커리큘럼 데이터와 시작일 기준으로 세션 생성
 */
export const createStudySessions = async (
  studyId: number,
  curriculum: Array<{ session: number; description: string; date?: string }>,
  startDate: string,
  meetingType: string
): Promise<StudySessionItem[]> => {
  const results: StudySessionItem[] = [];
  const baseDate = new Date(startDate);

  for (const item of curriculum) {
    // 날짜가 지정되어 있으면 사용, 없으면 주차별로 계산
    let scheduledAt: string;
    if (item.date) {
      scheduledAt = new Date(item.date + 'T19:00:00').toISOString();
    } else {
      // 주차별로 날짜 계산 (1주차 = startDate, 2주차 = startDate + 7일, ...)
      const sessionDate = new Date(baseDate);
      sessionDate.setDate(baseDate.getDate() + (item.session - 1) * 7);
      sessionDate.setHours(19, 0, 0, 0);
      scheduledAt = sessionDate.toISOString();
    }

    try {
      const session = await createStudySession(studyId, {
        title: `${item.session}회차`,
        description: item.description,
        scheduledAt,
        durationMinutes: 120,
        isOnline: meetingType === 'ONLINE',
      });
      results.push(session);
    } catch (err) {
      console.error(`세션 ${item.session} 생성 실패:`, err);
    }
  }

  return results;
};

// ========== AI 스터디 계획 생성 (스트리밍) ==========

// 스트리밍 콜백 타입
export interface StreamingCallbacks {
  onToken: (token: string) => void;
  onComplete: (result: AiStudyPlanResponse) => void;
  onError: (error: Error) => void;
}

/**
 * AI 스터디 계획 생성 (스트리밍)
 * - SSE(Server-Sent Events)로 실시간 토큰 수신
 * - 완료 시 파싱된 JSON 결과 반환
 */
// ========== 스터디 댓글 (Comment) ==========

// 댓글 응답 타입
export interface StudyCommentResponse {
  id: number;
  studyId: number;
  userId: number;
  userNickname: string;
  userProfileImage: string | null;
  parentId: number | null;
  content: string;
  imageUrl: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
  replies: StudyCommentResponse[];
  replyCount: number;
}

// 댓글 페이지 응답 타입
export interface StudyCommentPageResponse {
  comments: StudyCommentResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// 댓글 생성 요청 타입
export interface StudyCommentCreateRequest {
  parentId?: number;
  content: string;
  imageUrl?: string;
}

// 댓글 수정 요청 타입
export interface StudyCommentUpdateRequest {
  content: string;
  imageUrl?: string;
}

/**
 * 스터디 댓글 목록 조회 (대댓글 포함)
 * GET /api/v1/study/{studyId}/comments
 */
export const getStudyComments = async (
  studyId: number,
  page = 0,
  size = 20
): Promise<StudyCommentPageResponse> => {
  const response = await api.get(`/api/v1/study/${studyId}/comments`, {
    params: { page, size },
  });
  // 백엔드 응답 형식 처리
  const data = response.data;
  if (data.data) {
    return data.data;
  }
  return data;
};

/**
 * 최상위 댓글만 조회
 * GET /api/v1/study/{studyId}/comments/parents
 */
export const getParentComments = async (
  studyId: number,
  page = 0,
  size = 20
): Promise<StudyCommentPageResponse> => {
  const response = await api.get(`/api/v1/study/${studyId}/comments/parents`, {
    params: { page, size },
  });
  const data = response.data;
  if (data.data) {
    return data.data;
  }
  return data;
};

/**
 * 대댓글 목록 조회
 * GET /api/v1/study/{studyId}/comments/{commentId}/replies
 */
export const getCommentReplies = async (
  studyId: number,
  commentId: number,
  page = 0,
  size = 10
): Promise<StudyCommentPageResponse> => {
  const response = await api.get(
    `/api/v1/study/${studyId}/comments/${commentId}/replies`,
    { params: { page, size } }
  );
  const data = response.data;
  if (data.data) {
    return data.data;
  }
  return data;
};

/**
 * 댓글 생성 (최상위/대댓글)
 * POST /api/v1/study/{studyId}/comments
 */
export const createStudyComment = async (
  studyId: number,
  request: StudyCommentCreateRequest
): Promise<StudyCommentResponse> => {
  const response = await api.post(`/api/v1/study/${studyId}/comments`, request);
  const data = response.data;
  if (data.data) {
    return data.data;
  }
  return data;
};

/**
 * 댓글 수정
 * PUT /api/v1/study/{studyId}/comments/{commentId}
 */
export const updateStudyComment = async (
  studyId: number,
  commentId: number,
  request: StudyCommentUpdateRequest
): Promise<StudyCommentResponse> => {
  const response = await api.put(
    `/api/v1/study/${studyId}/comments/${commentId}`,
    request
  );
  const data = response.data;
  if (data.data) {
    return data.data;
  }
  return data;
};

/**
 * 댓글 삭제 (Soft Delete)
 * DELETE /api/v1/study/{studyId}/comments/{commentId}
 */
export const deleteStudyComment = async (
  studyId: number,
  commentId: number
): Promise<void> => {
  await api.delete(`/api/v1/study/${studyId}/comments/${commentId}`);
};

/**
 * 댓글 개수 조회
 * GET /api/v1/study/{studyId}/comments/count
 */
export const getStudyCommentCount = async (studyId: number): Promise<number> => {
  const response = await api.get(`/api/v1/study/${studyId}/comments/count`);
  const data = response.data;
  if (typeof data === 'number') {
    return data;
  }
  if (data.data !== undefined) {
    return data.data;
  }
  return 0;
};

// ========== AI 스터디 계획 생성 (스트리밍) ==========

export const generateStudyPlanStream = async (
  data: AiStudyPlanRequest,
  callbacks: StreamingCallbacks
): Promise<void> => {
  // 쿼리 파라미터 구성
  const params = new URLSearchParams();
  if (data.topic) params.set('topicInput', data.topic);
  if (data.durationWeeks) params.set('durationWeeks', data.durationWeeks.toString());
  if (data.totalSessions) params.set('totalSessions', data.totalSessions.toString());

  // user-id 헤더를 위해 로컬스토리지에서 가져오기
  let userId = '';
  try {
    const authStorage = localStorage.getItem('auth-storage');
    if (authStorage) {
      const authData = JSON.parse(authStorage);
      userId = authData?.state?.user?.id?.toString() || '';
    }
  } catch (e) {
    console.warn('userId 가져오기 실패:', e);
  }

  const baseUrl = import.meta.env.VITE_API_URL || '';
  const url = `${baseUrl}/api/v1/study-templates/recommend/stream?${params.toString()}`;

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream',
        'user-id': userId,
      },
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('ReadableStream not supported');
    }

    const decoder = new TextDecoder();
    let buffer = '';
    let currentEvent = ''; // 청크 간에 이벤트 타입 유지

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // SSE 이벤트 파싱 (줄바꿈으로 구분)
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // 마지막 불완전한 줄은 버퍼에 남김

      for (const line of lines) {
        const trimmedLine = line.trim();
        if (!trimmedLine) continue; // 빈 줄 무시

        console.log('[SSE 라인]', trimmedLine);

        if (trimmedLine.startsWith('event:')) {
          currentEvent = trimmedLine.substring(6).trim();
          console.log('[SSE 이벤트]', currentEvent);
        } else if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim();
          console.log('[SSE 데이터]', data.substring(0, 100) + '...');

          try {
            const parsed = JSON.parse(data);

            if (currentEvent === 'token' && parsed.token !== undefined) {
              callbacks.onToken(parsed.token);
            } else if (currentEvent === 'complete') {
              console.log('[SSE 완료 이벤트 수신]', parsed);
              // 완료 시 응답 변환
              const result: AiStudyPlanResponse = {
                name: parsed.name || '',
                intro: parsed.intro || '',
                description: parsed.description || '',
                topic: parsed.topic || '',
                format: parsed.format || '',
                difficulty: parsed.difficulty || 'INTERMEDIATE',
                goal: parsed.goal || '',
                textbook: parsed.textbook || '',
                prerequisites: parsed.prerequisites || '',
                processDetail: parsed.processDetail || parsed.process_detail || '',
                durationWeeks: parsed.durationWeeks || parsed.duration_weeks,
                scheduleSuggestion: parsed.scheduleSuggestion || parsed.schedule_suggestion,
                curriculum: parsed.curriculum,
              };
              console.log('[SSE 완료 결과]', result);
              callbacks.onComplete(result);
              return;
            } else if (currentEvent === 'error') {
              console.error('[SSE 에러 이벤트]', parsed);
              callbacks.onError(new Error(parsed.error || 'Unknown error'));
              return;
            }
          } catch (parseError) {
            // JSON 파싱 실패 시 무시 (부분 데이터일 수 있음)
            console.warn('[SSE 파싱 실패]', data);
          }

          currentEvent = ''; // 이벤트 리셋
        }
      }
    }

    // 스트림이 끝났는데 complete 이벤트가 없었던 경우
    // 버퍼에 남은 데이터가 있으면 처리 시도
    if (buffer.trim()) {
      console.log('[SSE 스트림 종료] 남은 버퍼 처리 시도:', buffer.substring(0, 200));
      try {
        // complete 이벤트의 data 부분만 남아있을 수 있음
        const dataMatch = buffer.match(/data:\s*(\{[\s\S]*\})/);
        if (dataMatch) {
          const parsed = JSON.parse(dataMatch[1]);
          console.log('[SSE 버퍼에서 complete 데이터 파싱]', parsed);
          const result: AiStudyPlanResponse = {
            name: parsed.name || '',
            intro: parsed.intro || '',
            description: parsed.description || '',
            topic: parsed.topic || '',
            format: parsed.format || '',
            difficulty: parsed.difficulty || 'INTERMEDIATE',
            goal: parsed.goal || '',
            textbook: parsed.textbook || '',
            prerequisites: parsed.prerequisites || '',
            processDetail: parsed.processDetail || parsed.process_detail || '',
            durationWeeks: parsed.durationWeeks || parsed.duration_weeks,
            scheduleSuggestion: parsed.scheduleSuggestion || parsed.schedule_suggestion,
            curriculum: parsed.curriculum,
          };
          callbacks.onComplete(result);
          return;
        }
      } catch (e) {
        console.warn('[SSE ���� �Ľ� ����]', e);
      }
    }
    console.warn('[SSE ��Ʈ�� ����] complete �̺�Ʈ ���� �����');
    callbacks.onError(new Error('��Ʈ���� �Ϸ� �̺�Ʈ ���� ����Ǿ����ϴ�.'));
  } catch (error) {
    callbacks.onError(error instanceof Error ? error : new Error(String(error)));
  }
};








