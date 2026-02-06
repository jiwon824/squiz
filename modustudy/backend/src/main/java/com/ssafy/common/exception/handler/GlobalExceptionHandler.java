package com.ssafy.common.exception.handler;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 글로벌 예외 처리 핸들러.
 */
 @Slf4j
 @RestControllerAdvice
 public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse response = ErrorResponse.of(
                e.getStatus().value(),
                e.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    /**
     * IllegalStateException 처리 (400 Bad Request)
     * 예: 중복 신청, 이미 처리된 신청, 본인 스터디 신청
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * IllegalArgumentException 처리 (400 Bad Request)
     * 예: 존재하지 않는 사용자, 잘못된 파라미터
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_ARGUMENT",
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Validation 에러 처리 (400 Bad Request)
     * 예: @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message != null ? message : "입력값이 올바르지 않습니다"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.name(),
                message
        );
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Optimistic Locking 충돌 처리 (409 Conflict).
     *
     * 동시에 같은 데이터를 수정하려 할 때 발생.
     * 프론트엔드는 이 응답을 받으면 데이터를 다시 로드하고 재시도해야 함.
     *
     * 기술적 배경:
     * - Hibernate의 @Version 필드가 UPDATE 시 WHERE 절에 포함됨
     * - UPDATE ... WHERE id = ? AND version = ?
     * - 다른 트랜잭션이 먼저 커밋하여 version이 증가하면 UPDATE 대상이 0건이 됨
     * - Hibernate가 이를 감지하고 ObjectOptimisticLockingFailureException을 던짐
     */
    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(Exception e) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONCURRENT_MODIFICATION",
                "다른 요청과 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.of(
                500,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );

        return ResponseEntity.status(500).body(response);
    }
}

