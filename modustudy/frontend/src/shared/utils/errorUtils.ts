/**
 * 에러 처리 유틸리티
 * catch (error: unknown) 패턴에서 안전하게 에러 메시지를 추출
 */
import axios from 'axios';

/**
 * API 에러에서 사용자 표시용 메시지를 추출
 * Axios 에러의 경우 서버 응답 메시지를 우선 사용
 */
export function getErrorMessage(error: unknown, fallback = '오류가 발생했습니다.'): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message
      || error.response?.data?.error?.message
      || error.message
      || fallback;
  }
  if (error instanceof Error) {
    return error.message || fallback;
  }
  return fallback;
}

/**
 * API 에러에서 HTTP 상태 코드를 추출
 */
export function getErrorStatus(error: unknown): number | undefined {
  if (axios.isAxiosError(error)) {
    return error.response?.status;
  }
  return undefined;
}
