package com.ssafy.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.ssafy.domain.notification.entity.FcmToken;
import com.ssafy.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FCM 푸시 알림 전송 서비스
 * Data 메시지 방식으로 전송하여 포그라운드/백그라운드 모두 앱에서 처리
 */
 @Service
 @RequiredArgsConstructor
 @Slf4j
 public class FcmPushService {

    private final FcmTokenService fcmTokenService;

    /**
     * 단일 사용자에게 푸시 알림 전송
     */
    @Async
    public void sendToUser(Long userId, String title, String body, NotificationType type, Long notificationId) {
        sendToUser(userId, title, body, type, notificationId, null);
    }

    /**
     * 단일 사용자에게 푸시 알림 전송 (추가 데이터 포함)
     */
    @Async
    public void sendToUser(Long userId, String title, String body, NotificationType type,
                           Long notificationId, Map<String, String> extraData) {
        if (!isFirebaseInitialized()) {
            return;
        }

        List<FcmToken> tokens = fcmTokenService.getActiveTokens(userId);

        if (tokens.isEmpty()) {
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                sendDataMessage(fcmToken.getToken(), title, body, type.name(), notificationId, extraData);
} catch (FirebaseMessagingException e) {
                handleMessagingException(fcmToken, e);
            }
        }
    }

    /**
     * 여러 사용자에게 푸시 알림 전송
     */
    @Async
    public void sendToUsers(List<Long> userIds, String title, String body, NotificationType type) {
        for (Long userId : userIds) {
            sendToUser(userId, title, body, type, null);
        }
    }

    /**
     * Data 메시지 전송 (포그라운드에서도 앱이 직접 처리)
     */
    private void sendDataMessage(String token, String title, String body, String type,
                                  Long notificationId, Map<String, String> extraData) throws FirebaseMessagingException {
        // Data payload 구성
        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("type", type);
        data.put("click_action", "OPEN_NOTIFICATION");

        if (notificationId != null) {
            data.put("notificationId", notificationId.toString());
        }

        // 추가 데이터 병합
        if (extraData != null) {
            data.putAll(extraData);
        }

        // Android 설정 (높은 우선순위)
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        // iOS 설정 (APNs)
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .putHeader("apns-priority", "10")
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .build())
                .build();

        // 메시지 구성 (Data만, Notification 없음)
        Message message = Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
}

    /**
     * Firebase 초기화 여부 확인
     */
    private boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    /**
     * FCM 전송 실패 처리
     */
    private void handleMessagingException(FcmToken fcmToken, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED ||
            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            // 유효하지 않은 토큰 - 비활성화
            fcmToken.deactivate();
        } else {
}
    }
}

