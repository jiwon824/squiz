package com.ssafy.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 인증된 사용자의 ID를 주입받기 위한 어노테이션
 * Controller 메서드 파라미터에 사용
 *
 * 사용 예시:
 * @GetMapping("/me")
 * public ResponseEntity<?> getMe(@CurrentUserId Long userId) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
