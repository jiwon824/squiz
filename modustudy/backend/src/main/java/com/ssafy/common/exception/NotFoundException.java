package com.ssafy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외.
 */
 public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    public static NotFoundException course() {
        return new NotFoundException("COURSE_NOT_FOUND", "코스를 찾을 수 없습니다.");
    }

    public static NotFoundException section() {
        return new NotFoundException("SECTION_NOT_FOUND", "섹션을 찾을 수 없습니다.");
    }

    public static NotFoundException user() {
        return new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
    }

    public static NotFoundException attempt() { return new NotFoundException("ATTEMPT_NOT_FOUND", "시도를 찾을 수 없습니다."); }
}
