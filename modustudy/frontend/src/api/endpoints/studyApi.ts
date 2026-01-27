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
