package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.response.NotificationListResponse;
import com.ssafy.domain.notification.dto.response.NotificationResponse;
import com.ssafy.domain.notification.dto.response.ReadAllResponse;
import com.ssafy.domain.notification.dto.response.UnreadCountResponse;
import com.ssafy.domain.notification.entity.Notification;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmPushService fcmPushService;

    /**
     * 알림 목록 조회 (페이징)
     */
    public NotificationListResponse getNotifications(Long userId, NotificationType type, Pageable pageable) {
        Page<Notification> page;
        if (type != null) {
            page = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        Page<NotificationResponse> responsePage = page.map(NotificationResponse::from);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return NotificationListResponse.of(responsePage, unreadCount);
    }

    /**
     * 읽지 않은 알림 수 조회 (전체 + 타입별)
     */
    public UnreadCountResponse getUnreadCount(Long userId) {
        long totalUnread = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Map<NotificationType, Long> byType = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            long count = notificationRepository.countByUserIdAndTypeAndIsReadFalse(userId, type);
            byType.put(type, count);
        }

        return UnreadCountResponse.of(totalUnread, byType);
    }

    /**
     * 알림 읽음 처리 (단건)
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException.NotificationNotFoundException(notificationId));

        notification.markAsRead();

}

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public ReadAllResponse markAllAsRead(Long userId) {
        int readCount = notificationRepository.markAllAsRead(userId);

        return ReadAllResponse.of(readCount);
    }

    /**
     * 알림 생성 (내부용 - 다른 서비스에서 호출)
     * DB 저장 + FCM 푸시 전송
     */
    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String content,
                                           String referenceType, Long referenceId) {
                                               Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notification);

// FCM 푸시 알림 전송 (비동기)
        fcmPushService.sendToUser(userId, title, content, type, saved.getId());

        return saved;
    }

    /**
     * 알림 삭제 (단건)
     */
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException.NotificationNotFoundException(notificationId));

        notificationRepository.delete(notification);

}
}
