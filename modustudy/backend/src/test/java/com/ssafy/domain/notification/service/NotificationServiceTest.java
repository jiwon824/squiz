package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.response.NotificationListResponse;
import com.ssafy.domain.notification.dto.response.ReadAllResponse;
import com.ssafy.domain.notification.dto.response.UnreadCountResponse;
import com.ssafy.domain.notification.entity.Notification;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.repository.NotificationRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private Notification notification1;
    private Notification notification2;
    private Notification notification3;
    private Notification notification4;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        userId = user.getId();

        // 2. Notification 생성
        notification1 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.SCHEDULE)
                .title("스터디 일정 알림")
                .content("알고리즘 스터디가 1시간 후에 시작됩니다.")
                .referenceType("study_session")
                .isRead(false)
                .build());

        notification2 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.CHAT)
                .title("새 메시지")
                .content("홍길동: 안녕하세요!")
                .referenceType("channel")
                .isRead(false)
                .build());

        notification3 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.ATTENDANCE)
                .title("출석 체크 시작")
                .content("출석 체크가 시작되었습니다.")
                .referenceType("study_session")
                .isRead(true)  // 읽음 처리된 알림
                .build());

        notification4 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.CHAT)
                .title("새 메시지 2")
                .content("김철수: 반갑습니다!")
                .referenceType("channel")
                .isRead(false)
                .build());

        notificationRepository.flush();
    }

    // ============================================================
    // 알림 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("알림 목록 조회 - 전체")
    void getNotifications_All() {
        // when
        NotificationListResponse response = notificationService
                .getNotifications(userId, null, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent()).hasSize(4);
        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getUnreadCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("알림 목록 조회 - 타입별 필터")
    void getNotifications_ByType() {
        // when
        NotificationListResponse response = notificationService
                .getNotifications(userId, NotificationType.CHAT, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).allMatch(n -> n.getType() == NotificationType.CHAT);
    }

    @Test
    @DisplayName("알림 목록 조회 - 페이징")
    void getNotifications_WithPaging() {
        // when
        NotificationListResponse response = notificationService
                .getNotifications(userId, null, PageRequest.of(0, 2));

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("알림이 없는 사용자 조회")
    void getNotifications_Empty() {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .email("other@test.com")
                .nickname("다른유저")
                .name("다른")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // when
        NotificationListResponse response = notificationService
                .getNotifications(otherUser.getId(), null, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    // ============================================================
    // 읽지 않은 알림 수 조회 테스트
    // ============================================================

    @Test
    @DisplayName("읽지 않은 알림 수 조회")
    void getUnreadCount_Success() {
        // when
        UnreadCountResponse response = notificationService.getUnreadCount(userId);

        // then
        assertThat(response.getUnreadCount()).isEqualTo(3);
        assertThat(response.getByType().get(NotificationType.CHAT)).isEqualTo(2);
        assertThat(response.getByType().get(NotificationType.SCHEDULE)).isEqualTo(1);
        assertThat(response.getByType().get(NotificationType.ATTENDANCE)).isEqualTo(0);
    }

    @Test
    @DisplayName("알림이 없는 사용자의 읽지 않은 알림 수")
    void getUnreadCount_NoNotifications() {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser2")
                .email("other2@test.com")
                .nickname("다른유저2")
                .name("다른2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // when
        UnreadCountResponse response = notificationService.getUnreadCount(otherUser.getId());

        // then
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    // ============================================================
    // 알림 읽음 처리 테스트
    // ============================================================

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() {
        // given
        assertThat(notification1.getIsRead()).isFalse();

        // when
        notificationService.markAsRead(userId, notification1.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        Notification updated = notificationRepository.findById(notification1.getId()).get();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외 발생")
    void markAsRead_NotFound() {
        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(userId, 999L))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 알림 읽음 처리 시 예외 발생")
    void markAsRead_NotOwner() {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser3")
                .email("other3@test.com")
                .nickname("다른유저3")
                .name("다른3")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(otherUser.getId(), notification1.getId()))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
    }

    // ============================================================
    // 전체 읽음 처리 테스트
    // ============================================================

    @Test
    @DisplayName("전체 읽음 처리 성공")
    void markAllAsRead_Success() {
        // when
        ReadAllResponse response = notificationService.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(response.getReadCount()).isEqualTo(3);

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 모두 읽은 경우 전체 읽음 처리")
    void markAllAsRead_AlreadyRead() {
        // given
        notificationService.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // when
        ReadAllResponse response = notificationService.markAllAsRead(userId);

        // then
        assertThat(response.getReadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("다른 사용자 알림은 읽음 처리되지 않음")
    void markAllAsRead_OnlyTargetUser() {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser4")
                .email("other4@test.com")
                .nickname("다른유저4")
                .name("다른4")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        notificationRepository.save(Notification.builder()
                .userId(otherUser.getId())
                .type(NotificationType.SYSTEM)
                .title("시스템 알림")
                .content("시스템 점검 안내")
                .isRead(false)
                .build());
        notificationRepository.flush();

        // when
        notificationService.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        long otherUserUnreadCount = notificationRepository.countByUserIdAndIsReadFalse(otherUser.getId());
        assertThat(otherUserUnreadCount).isEqualTo(1);
    }

    // ============================================================
    // 알림 생성 테스트
    // ============================================================

    @Test
    @DisplayName("알림 생성 성공")
    void createNotification_Success() {
        // when
        Notification notification = notificationService.createNotification(
                userId,
                NotificationType.QUIZ,
                "퀴즈 알림",
                "퀴즈 대회가 시작됩니다.",
                "quiz_contest",
                100L
        );

        // then
        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getType()).isEqualTo(NotificationType.QUIZ);
        assertThat(notification.getTitle()).isEqualTo("퀴즈 알림");
        assertThat(notification.getIsRead()).isFalse();
    }

    // ============================================================
    // 알림 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("알림 삭제 성공")
    void deleteNotification_Success() {
        // given
        Long notificationId = notification1.getId();

        // when
        notificationService.deleteNotification(userId, notificationId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(notificationRepository.findById(notificationId)).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 알림 삭제 시 예외 발생")
    void deleteNotification_NotFound() {
        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(userId, 999L))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 알림 삭제 시 예외 발생")
    void deleteNotification_NotOwner() {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser5")
                .email("other5@test.com")
                .nickname("다른유저5")
                .name("다른5")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(otherUser.getId(), notification1.getId()))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
    }
}
