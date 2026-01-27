// 스터디 관련 API
import api from '../axios';

// ========== 타입 정의 ==========

export interface TopicChild {
    id: number;
    name: string;
    icon: string | null;
    sortOrder: number;
}

export interface TopicParent {
    id: number;
    name: string;
    icon: string | null;
    sortOrder: number;
    children: TopicChild[];
}

export interface FormatItem {
    id: number;
    name: string;
    description: string | null;
    icon: string | null;
    sortOrder: number;
}

export interface RegionItem {
    id: number;
    code: string;
    name: string;
    fullName: string;
    level: number;
    parentId: number | null;
}

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

// ========== API 호출 ==========

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

// ========== AI 스터디 계획 생성 ==========

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

// 사용자 맞춤 스터디 추천 목록
export const getStudyRecommendations = async (limit: number = 10): Promise<StudyRecommendItem[]> => {
    const response = await api.get(`/api/v1/study/recommend?limit=${limit}`);
    return response.data;
};

// AI 스터디 계획 생성 (inference server)
export interface AiStudyPlanRequest {
    topic: string;         // 사용자가 입력한 자유 텍스트 주제
    techStack?: string[];  // 프로필에서 자동 로드
    schedule?: string[];   // 프로필에서 자동 로드
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
    scheduleSuggestion: {
        days: string[];
        time: string;
    };
}

export const generateStudyPlan = async (data: AiStudyPlanRequest): Promise<AiStudyPlanResponse> => {
    const response = await api.post('/api/v1/study/template/ai-generate', data);
    return response.data;
};
