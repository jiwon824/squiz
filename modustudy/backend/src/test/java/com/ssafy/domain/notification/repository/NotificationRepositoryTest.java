package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.Notification;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationRepositoryTest {

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
        // 1. User 생성 (부모 엔티티)
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

        // 2. Notification 생성 (referenceId는 다양한 엔티티를 가리키므로 테스트에서는 null 허용)
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
                .content("알고리즘 스터디 출석 체크가 시작되었습니다.")
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
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 알림 목록 조회 - 페이징")
    void findByUserIdOrderByCreatedAtDesc_WithPaging() {
        // when
        Page<Notification> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 2));

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자별 + 타입별 알림 목록 조회")
    void findByUserIdAndTypeOrderByCreatedAtDesc_Success() {
        // when
        Page<Notification> page = notificationRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, NotificationType.CHAT, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Notification::getType)
                .containsOnly(NotificationType.CHAT);
    }

    @Test
    @DisplayName("사용자별 알림 목록 조회 - 전체")
    void findByUserIdOrderByCreatedAtDesc_All() {
        // when
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        // then
        assertThat(notifications).hasSize(4);
        assertThat(notifications).extracting(Notification::getUserId)
                .containsOnly(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 목록 반환")
    void findByUserId_NotFound() {
        // when
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(999L);

        // then
        assertThat(notifications).isEmpty();
    }

    @Test
    @DisplayName("사용자 + 알림 ID로 조회")
    void findByIdAndUserId_Success() {
        // when
        Optional<Notification> result = notificationRepository
                .findByIdAndUserId(notification1.getId(), userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("스터디 일정 알림");
    }

    @Test
    @DisplayName("다른 사용자의 알림 ID로 조회 시 빈 Optional 반환")
    void findByIdAndUserId_DifferentUser() {
        // when
        Optional<Notification> result = notificationRepository
                .findByIdAndUserId(notification1.getId(), 999L);

        // then
        assertThat(result).isEmpty();
    }

    // ============================================================
    // 읽지 않은 알림 수 조회 테스트
    // ============================================================

    @Test
    @DisplayName("읽지 않은 알림 수 조회 - 전체")
    void countByUserIdAndIsReadFalse_Success() {
        // when
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);

        // then
        assertThat(count).isEqualTo(3);  // notification1, 2, 4는 읽지 않음
    }

    @Test
    @DisplayName("읽지 않은 알림 수 조회 - 타입별")
    void countByUserIdAndTypeAndIsReadFalse_Success() {
        // when
        long chatCount = notificationRepository
                .countByUserIdAndTypeAndIsReadFalse(userId, NotificationType.CHAT);
        long scheduleCount = notificationRepository
                .countByUserIdAndTypeAndIsReadFalse(userId, NotificationType.SCHEDULE);
        long attendanceCount = notificationRepository
                .countByUserIdAndTypeAndIsReadFalse(userId, NotificationType.ATTENDANCE);

        // then
        assertThat(chatCount).isEqualTo(2);      // notification2, 4
        assertThat(scheduleCount).isEqualTo(1);   // notification1
        assertThat(attendanceCount).isEqualTo(0); // notification3은 읽음
    }

    @Test
    @DisplayName("알림이 없는 사용자의 읽지 않은 알림 수는 0")
    void countByUserIdAndIsReadFalse_NoNotifications() {
        // when
        long count = notificationRepository.countByUserIdAndIsReadFalse(999L);

        // then
        assertThat(count).isEqualTo(0);
    }

    // ============================================================
    // 전체 읽음 처리 테스트
    // ============================================================

    @Test
    @DisplayName("전체 읽음 처리")
    void markAllAsRead_Success() {
        // when
        int updatedCount = notificationRepository.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(updatedCount).isEqualTo(3);  // 읽지 않은 3개 업데이트

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 모두 읽은 경우 전체 읽음 처리")
    void markAllAsRead_AlreadyRead() {
        // given - 먼저 전체 읽음 처리
        notificationRepository.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // when - 다시 전체 읽음 처리
        int updatedCount = notificationRepository.markAllAsRead(userId);

        // then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("다른 사용자 알림은 읽음 처리되지 않음")
    void markAllAsRead_OnlyTargetUser() {
        // given - 다른 사용자 생성
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

        notificationRepository.save(Notification.builder()
                .userId(otherUser.getId())
                .type(NotificationType.SYSTEM)
                .title("시스템 알림")
                .content("시스템 점검 안내")
                .isRead(false)
                .build());
        notificationRepository.flush();

        // when
        notificationRepository.markAllAsRead(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        long otherUserUnreadCount = notificationRepository
                .countByUserIdAndIsReadFalse(otherUser.getId());
        assertThat(otherUserUnreadCount).isEqualTo(1);
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("알림 단건 삭제")
    void deleteById_Success() {
        // given
        Long notificationId = notification1.getId();

        // when
        notificationRepository.deleteById(notificationId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<Notification> result = notificationRepository.findById(notificationId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자별 알림 전체 삭제")
    void deleteByUserId_Success() {
        // when
        notificationRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(notifications).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자 알림은 삭제되지 않음")
    void deleteByUserId_OnlyTargetUser() {
        // given - 다른 사용자 생성
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

        notificationRepository.save(Notification.builder()
                .userId(otherUser.getId())
                .type(NotificationType.QUIZ)
                .title("퀴즈 알림")
                .content("퀴즈 대회가 시작됩니다.")
                .isRead(false)
                .build());
        notificationRepository.flush();

        // when
        notificationRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<Notification> remainingNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(otherUser.getId());
        assertThat(remainingNotifications).hasSize(1);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("알림 생성")
    void save_Success() {
        // given
        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.STUDY_UPDATE)
                .title("스터디 업데이트")
                .content("스터디 일정이 변경되었습니다.")
                .referenceType("study")
                .build();

        // when
        Notification saved = notificationRepository.save(notification);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("스터디 업데이트");
        assertThat(saved.getType()).isEqualTo(NotificationType.STUDY_UPDATE);
        assertThat(saved.getIsRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 단건 조회")
    void findById_Success() {
        // when
        Optional<Notification> result = notificationRepository.findById(notification1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("스터디 일정 알림");
        assertThat(result.get().getType()).isEqualTo(NotificationType.SCHEDULE);
    }

    @Test
    @DisplayName("알림 읽음 처리 - 엔티티 메서드")
    void markAsRead_EntityMethod() {
        // given
        Notification notification = notificationRepository.findById(notification1.getId()).get();
        assertThat(notification.getIsRead()).isFalse();

        // when
        notification.markAsRead();
        notificationRepository.flush();
        entityManager.clear();

        // then
        Notification updated = notificationRepository.findById(notification1.getId()).get();
        assertThat(updated.getIsRead()).isTrue();
    }
}
