package com.ssafy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 잠긴 섹션에 접근할 때 발생하는 예외.
 */
 public class SectionLockedException extends BusinessException {

    public SectionLockedException() {
        super(HttpStatus.FORBIDDEN, "SECTION_LOCKED", "이전 섹션을 먼저 완료해주세요.");
    }

    public SectionLockedException(String message) {
        super(HttpStatus.FORBIDDEN, "SECTION_LOCKED", message);
    }
}
