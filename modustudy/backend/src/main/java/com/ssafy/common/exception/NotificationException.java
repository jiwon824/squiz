package com.ssafy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 알림 관련 예외 클래스
 */
 public class NotificationException {

    /**
     * 알림을 찾을 수 없음 (404)
     */
    public static class NotificationNotFoundException extends BusinessException {
        public NotificationNotFoundException() {
            super(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                    "존재하지 않는 알림입니다.");
        }

        public NotificationNotFoundException(Long notificationId) {
            super(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                    "존재하지 않는 알림입니다: " + notificationId);
        }
    }

    /**
     * 알림 설정을 찾을 수 없음 (404)
     */
    public static class NotificationSettingNotFoundException extends BusinessException {
        public NotificationSettingNotFoundException(String notificationType) {
            super(HttpStatus.NOT_FOUND, "NOTIFICATION_SETTING_NOT_FOUND",
                    "존재하지 않는 알림 설정입니다: " + notificationType);
        }
    }

    /**
     * 유효하지 않은 FCM 토큰 (400)
     */
    public static class InvalidFcmTokenException extends BusinessException {
        public InvalidFcmTokenException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
                    "유효하지 않은 FCM 토큰입니다.");
        }

        public InvalidFcmTokenException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", message);
        }
    }

    /**
     * FCM 토큰을 찾을 수 없음 (404)
     */
    public static class FcmTokenNotFoundException extends BusinessException {
        public FcmTokenNotFoundException() {
            super(HttpStatus.NOT_FOUND, "FCM_TOKEN_NOT_FOUND",
                    "존재하지 않는 FCM 토큰입니다.");
        }

        public FcmTokenNotFoundException(String token) {
            super(HttpStatus.NOT_FOUND, "FCM_TOKEN_NOT_FOUND",
                    "존재하지 않는 FCM 토큰입니다: " + token);
        }
    }

    /**
     * 권한 없음 - 본인 알림만 접근 가능 (403)
     */
    public static class NotNotificationOwnerException extends BusinessException {
        public NotNotificationOwnerException() {
            super(HttpStatus.FORBIDDEN, "NOT_NOTIFICATION_OWNER",
                    "해당 알림에 접근할 권한이 없습니다.");
        }
    }

    /**
     * 잘못된 알림 요청 (400)
     */
    public static class InvalidNotificationRequestException extends BusinessException {
        public InvalidNotificationRequestException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_REQUEST", message);
        }
    }

    /**
     * 유효하지 않은 알림 타입 (400)
     */
    public static class InvalidNotificationTypeException extends BusinessException {
        public InvalidNotificationTypeException(String type) {
            super(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_TYPE",
                    "유효하지 않은 알림 타입입니다: " + type);
        }
    }

    /**
     * 중복된 FCM 토큰 (409)
     */
    public static class DuplicateFcmTokenException extends BusinessException {
        public DuplicateFcmTokenException() {
            super(HttpStatus.CONFLICT, "DUPLICATE_FCM_TOKEN",
                    "이미 등록된 FCM 토큰입니다.");
        }
    }
}
