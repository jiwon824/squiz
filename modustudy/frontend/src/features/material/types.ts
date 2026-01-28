/**
 * 자료실 도메인 타입 정의
 */

// 자료 타입 enum
export type MaterialType = 'LINK' | 'FILE' | 'IMAGE' | 'VIDEO';

// 업로더 정보
export interface UploaderInfo {
  id: number;
  nickname: string;
  profileImage: string | null;
}

// 자료 목록 응답
export interface MaterialListResponse {
  id: number;
  title: string;
  description: string | null;
  materialType: MaterialType;
  // 링크 타입
  url: string | null;
  // 파일 타입
  fileUrl: string | null;
  fileSize: number | null;
  weekNumber: number | null;
  viewCount: number;
  commentCount: number;
  uploader: UploaderInfo;
  createdAt: string;
}

// 자료 상세 응답
export interface MaterialDetailResponse {
  id: number;
  title: string;
  description: string | null;
  materialType: MaterialType;
  // 링크 타입
  url: string | null;
  // 파일 타입
  fileUrl: string | null;
  fileName: string | null;
  fileSize: number | null;
  weekNumber: number | null;
  viewCount: number;
  uploader: UploaderInfo;
  createdAt: string;
}

// 자료 생성 응답
export interface MaterialCreateResponse {
  id: number;
  title: string;
  materialType: MaterialType;
  filePath: string | null;
  createdAt: string;
}

// 자료 생성 요청 (링크)
export interface MaterialCreateRequest {
  title: string;
  description?: string;
  url: string;
  weekNumber?: number;
}

// 자료 수정 요청
export interface MaterialUpdateRequest {
  title: string;
  description?: string;
  weekNumber?: number;
}

// 자료 검색 조건
export interface MaterialSearchCondition {
  keyword?: string;
  materialType?: MaterialType;
  weekNumber?: number;
}

// 댓글 응답
export interface MaterialCommentResponse {
  id: number;
  content: string;
  author: UploaderInfo;
  createdAt: string;
  updatedAt: string | null;
}

// 댓글 생성 요청
export interface MaterialCommentCreateRequest {
  content: string;
}

// 댓글 생성 응답
export interface MaterialCommentCreateResponse {
  id: number;
  content: string;
  createdAt: string;
}

// 정렬 옵션
export type MaterialSortOption = 'latest' | 'oldest' | 'popular' | 'week';
