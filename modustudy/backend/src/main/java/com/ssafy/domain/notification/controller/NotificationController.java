package com.ssafy.domain.notification.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.notification.dto.request.FcmTokenDeleteRequest;
import com.ssafy.domain.notification.dto.request.FcmTokenRequest;
import com.ssafy.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.ssafy.domain.notification.dto.response.*;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.FcmTokenService;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.notification.service.NotificationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSettingService notificationSettingService;
    private final FcmTokenService fcmTokenService;

    /**
     * 알림 목록 조회
     * GET /api/v1/notifications?page=0&size=20&type=SCHEDULE
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @RequestHeader("User-Id") Long userId,
            @RequestParam(required = false) NotificationType type,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 알림 목록 조회: userId={}, type={}, page={}, size={}",
                userId, type, pageable.getPageNumber(), pageable.getPageSize());

        NotificationListResponse response = notificationService.getNotifications(userId, type, pageable);

        log.info("API 응답 - 알림 목록: userId={}, count={}, unreadCount={}",
                userId, response.getContent().size(), response.getUnreadCount());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 읽지 않은 알림 수 조회
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 읽지 않은 알림 수 조회: userId={}", userId);

        UnreadCountResponse response = notificationService.getUnreadCount(userId);

        log.info("API 응답 - 읽지 않은 알림 수: userId={}, unreadCount={}", userId, response.getUnreadCount());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 읽음 처리
     * PUT /api/v1/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<?>> markAsRead(
            @RequestHeader("User-Id") Long userId,
            @PathVariable Long notificationId) {

        log.info("API 호출 - 알림 읽음 처리: userId={}, notificationId={}", userId, notificationId);

        notificationService.markAsRead(userId, notificationId);

        log.info("API 응답 - 알림 읽음 처리 완료: notificationId={}", notificationId);

        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다."));
    }

    /**
     * 전체 읽음 처리
     * PUT /api/v1/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<ReadAllResponse>> markAllAsRead(
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 전체 읽음 처리: userId={}", userId);

        ReadAllResponse response = notificationService.markAllAsRead(userId);

        log.info("API 응답 - 전체 읽음 처리 완료: userId={}, readCount={}", userId, response.getReadCount());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 조회
     * GET /api/v1/notifications/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingListResponse>> getSettings(
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 알림 설정 조회: userId={}", userId);

        NotificationSettingListResponse response = notificationSettingService.getSettings(userId);

        log.info("API 응답 - 알림 설정 조회 완료: userId={}, settingCount={}",
                userId, response.getSettings().size());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 수정
     * PUT /api/v1/notifications/settings
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<?>> updateSettings(
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody NotificationSettingUpdateRequest request) {

        log.info("API 호출 - 알림 설정 수정: userId={}", userId);

        notificationSettingService.updateSettings(userId, request);

        log.info("API 응답 - 알림 설정 수정 완료: userId={}", userId);

        return ResponseEntity.ok(ApiResponse.success("알림 설정이 저장되었습니다."));
    }

    /**
     * FCM 토큰 등록 (모바일)
     * POST /api/v1/notifications/fcm-token
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<?>> registerFcmToken(
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody FcmTokenRequest request) {

        log.info("API 호출 - FCM 토큰 등록: userId={}, deviceType={}", userId, request.getDeviceType());

        fcmTokenService.registerToken(userId, request);

        log.info("API 응답 - FCM 토큰 등록 완료: userId={}", userId);

        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 등록되었습니다."));
    }

    /**
     * FCM 토큰 삭제
     * DELETE /api/v1/notifications/fcm-token
     */
    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<?>> deleteFcmToken(
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody FcmTokenDeleteRequest request) {

        log.info("API 호출 - FCM 토큰 삭제: userId={}", userId);

        fcmTokenService.deleteToken(userId, request);

        log.info("API 응답 - FCM 토큰 삭제 완료: userId={}", userId);

        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 삭제되었습니다."));
    }
}