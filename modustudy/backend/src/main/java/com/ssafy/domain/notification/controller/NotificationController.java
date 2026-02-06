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

                NotificationListResponse response = notificationService.getNotifications(userId, type, pageable);

                return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 읽지 않은 알림 수 조회
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @RequestHeader("User-Id") Long userId) {

                UnreadCountResponse response = notificationService.getUnreadCount(userId);

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

                notificationService.markAsRead(userId, notificationId);

                return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다."));
    }

    /**
     * 전체 읽음 처리
     * PUT /api/v1/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<ReadAllResponse>> markAllAsRead(
            @RequestHeader("User-Id") Long userId) {

                ReadAllResponse response = notificationService.markAllAsRead(userId);

                return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 조회
     * GET /api/v1/notifications/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingListResponse>> getSettings(
            @RequestHeader("User-Id") Long userId) {

                NotificationSettingListResponse response = notificationSettingService.getSettings(userId);

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

                notificationSettingService.updateSettings(userId, request);

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

                fcmTokenService.registerToken(userId, request);

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

                fcmTokenService.deleteToken(userId, request);

                return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 삭제되었습니다."));
    }
}
