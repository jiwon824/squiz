// 워크스페이스 관련 타입 정의

// 메시지 타입
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM' | 'MEETING_SUMMARY';

// 워크스페이스 응답
export interface WorkspaceResponse {
  id: number;
  studyId: number;
  createdAt: string;
}

// 워크스페이스 간략 응답 (미팅 등에서 사용)
export interface WorkspaceBriefResponse {
  id: number;
  name: string;
}

// 메시지 작성자 정보
export interface MessageAuthorResponse {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
}

// 메시지 응답
export interface MessageResponse {
  id: number;
  workspaceId: number;
  author: MessageAuthorResponse;
  content: string;
  messageType: MessageType;
  isPinned?: boolean;
  createdAt: string;
  updatedAt: string | null;
}

// 메시지 페이지 응답
export interface MessagePageResponse {
  content: MessageResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// 메시지 생성 요청
export interface MessageCreateRequest {
  content: string;
  messageType?: MessageType;
}

// 메시지 수정 요청
export interface MessageUpdateRequest {
  content: string;
}
