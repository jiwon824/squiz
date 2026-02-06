package com.ssafy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외 기본 클래스.
 */
 @Getter
 public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }
}
