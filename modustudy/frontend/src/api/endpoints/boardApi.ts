import api from '../axios';

export interface RecruitmentStudy {
  id: number;
  name: string;
  topicName: string | null;
  studyType: string;
  meetingType: string;
  maxMembers: number | null;
  currentMembers: number;
  status: string;
}

export interface RecruitmentPostSummary {
  id: number;
  title: string;
  authorId: number;
  authorName: string;
  authorProfileImage?: string | null;
  recruitmentField: string;
  meetingType: string;
  targetMembers: number | null;
  viewCount: number;
  createdAt: string;
  recruitmentStatus: string;
}

export interface RecruitmentComment {
  id: number;
  postId: number;
  authorId: number;
  authorName: string;
  authorProfileImage?: string | null;
  parentId?: number | null;
  content: string;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RecruitmentPostDetail {
  id: number;
  meetingType: string;
  recruitmentField: string;
  targetMembers: number | null;
  recruitmentStatus: string;
  title: string;
  content: string;
  authorId: number;
  authorName: string;
  authorProfileImage?: string | null;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
  comments: RecruitmentComment[];
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

const unwrap = <T,>(response: { data: ApiResponse<T> | T }): T => {
  const data = response.data as ApiResponse<T>;
  if (data && typeof (data as ApiResponse<T>).success === 'boolean' && 'data' in data) {
    return (data as ApiResponse<T>).data;
  }
  return response.data as T;
};

export const getRecruitingStudiesForBoard = async (): Promise<RecruitmentStudy[]> => {
  const response = await api.get('/api/v1/boards/recruitments/studies');
  return unwrap<RecruitmentStudy[]>(response);
};

export const createRecruitmentPost = async (payload: {
  title: string;
  content: string;
  recruitmentField: string;
  meetingType: string;
  targetMembers: number;
}): Promise<RecruitmentPostDetail> => {
  const response = await api.post('/api/v1/boards/recruitments', payload);
  return unwrap<RecruitmentPostDetail>(response);
};

export const updateRecruitmentPost = async (
  postId: number,
  payload: {
    title: string;
    content: string;
    recruitmentField: string;
    meetingType: string;
    targetMembers: number;
    recruitmentStatus: string;
  }
): Promise<RecruitmentPostDetail> => {
  const response = await api.put(`/api/v1/boards/recruitments/${postId}`, payload);
  return unwrap<RecruitmentPostDetail>(response);
};

export const deleteRecruitmentPost = async (postId: number): Promise<void> => {
  await api.delete(`/api/v1/boards/recruitments/${postId}`);
};

export const getRecruitmentPosts = async (params?: { page?: number; size?: number }) => {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set('page', String(params.page));
  if (params?.size !== undefined) searchParams.set('size', String(params.size));
  const response = await api.get(`/api/v1/boards/recruitments?${searchParams.toString()}`);
  return unwrap<{
    content: RecruitmentPostSummary[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }>(response);
};

export const getRecruitmentPostDetail = async (postId: number): Promise<RecruitmentPostDetail> => {
  const response = await api.get(`/api/v1/boards/recruitments/${postId}`);
  return unwrap<RecruitmentPostDetail>(response);
};

export const addRecruitmentComment = async (
  postId: number,
  payload: { parentId?: number | null; content: string }
): Promise<RecruitmentComment> => {
  const response = await api.post(`/api/v1/boards/recruitments/${postId}/comments`, payload);
  return unwrap<RecruitmentComment>(response);
};

export const deleteRecruitmentComment = async (postId: number, commentId: number): Promise<void> => {
  await api.delete(`/api/v1/boards/recruitments/${postId}/comments/${commentId}`);
};

export const reportRecruitmentPost = async (
  postId: number,
  payload: { reason: string }
): Promise<void> => {
  await api.post(`/api/v1/boards/recruitments/${postId}/report`, payload);
};
