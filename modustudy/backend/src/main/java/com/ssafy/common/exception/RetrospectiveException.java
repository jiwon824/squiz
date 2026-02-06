package com.ssafy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 회고 관련 예외 클래스
 */
 public class RetrospectiveException {

    /**
     * 회고를 찾을 수 없음 (404)
     */
    public static class RetrospectiveNotFoundException extends BusinessException {
        public RetrospectiveNotFoundException() {
            super(HttpStatus.NOT_FOUND, "RETROSPECTIVE_NOT_FOUND",
                    "존재하지 않는 회고입니다.");
        }

        public RetrospectiveNotFoundException(Long retrospectiveId) {
            super(HttpStatus.NOT_FOUND, "RETROSPECTIVE_NOT_FOUND",
                    "존재하지 않는 회고입니다: " + retrospectiveId);
        }
    }

    /**
     * 회고 항목을 찾을 수 없음 (404)
     */
    public static class RetrospectiveItemNotFoundException extends BusinessException {
        public RetrospectiveItemNotFoundException(Long itemId) {
            super(HttpStatus.NOT_FOUND, "RETROSPECTIVE_ITEM_NOT_FOUND",
                    "존재하지 않는 회고 항목입니다: " + itemId);
        }
    }

    /**
     * 권한 없음 (403)
     */
    public static class NotRetrospectiveOwnerException extends BusinessException {
        public NotRetrospectiveOwnerException() {
            super(HttpStatus.FORBIDDEN, "NOT_RETROSPECTIVE_OWNER",
                    "해당 회고 항목을 수정/삭제할 권한이 없습니다.");
        }
    }

    /**
     * 잘못된 요청 (400)
     */
    public static class InvalidRetrospectiveRequestException extends BusinessException {
        public InvalidRetrospectiveRequestException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_RETROSPECTIVE_REQUEST", message);
        }
    }
}
