/**
 * 공통 API 응답 래퍼 타입
 * 백엔드가 { success: true, data: T } 형식으로 응답하는 경우 사용
 */
export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: {
        code: string;
        message: string;
    };
}

/**
 * 백엔드 응답이 래퍼({ success, data })를 사용하거나 직접 반환할 수 있는 경우 사용
 * data.data || data 패턴에서 타입 안전하게 처리
 */
export type MaybeWrapped<T> = T & { data?: T; success?: boolean };
