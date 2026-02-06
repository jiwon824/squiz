package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.NotificationSetting;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationSettingRepositoryTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private NotificationSetting setting1;
    private NotificationSetting setting2;
    private NotificationSetting setting3;

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

        // 2. NotificationSetting 생성
        setting1 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("CHAT")
                .isEnabled(true)
                .build());

        setting2 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("SCHEDULE")
                .isEnabled(true)
                .build());

        setting3 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("QUIZ")
                .isEnabled(false)  // 비활성화 설정
                .build());

        notificationSettingRepository.flush();
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 알림 설정 목록 조회")
    void findByUserId_Success() {
        // when
        List<NotificationSetting> settings = notificationSettingRepository.findByUserId(userId);

        // then
        assertThat(settings).hasSize(3);
        assertThat(settings).extracting(NotificationSetting::getUserId)
                .containsOnly(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 목록 반환")
    void findByUserId_NotFound() {
        // when
        List<NotificationSetting> settings = notificationSettingRepository.findByUserId(999L);

        // then
        assertThat(settings).isEmpty();
    }

    @Test
    @DisplayName("사용자 + 알림 타입으로 설정 조회")
    void findByUserIdAndNotificationType_Success() {
        // when
        Optional<NotificationSetting> result = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, "CHAT");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo("CHAT");
        assertThat(result.get().getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 타입으로 조회 시 빈 Optional 반환")
    void findByUserIdAndNotificationType_NotFound() {
        // when
        Optional<NotificationSetting> result = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, "NONEXISTENT");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 알림 타입으로 조회 시 빈 Optional 반환")
    void findByUserIdAndNotificationType_DifferentUser() {
        // when
        Optional<NotificationSetting> result = notificationSettingRepository
                .findByUserIdAndNotificationType(999L, "CHAT");

        // then
        assertThat(result).isEmpty();
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 단건 삭제")
    void deleteById_Success() {
        // given
        Long settingId = setting1.getId();

        // when
        notificationSettingRepository.deleteById(settingId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<NotificationSetting> result = notificationSettingRepository.findById(settingId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자별 알림 설정 전체 삭제")
    void deleteByUserId_Success() {
        // when
        notificationSettingRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<NotificationSetting> settings = notificationSettingRepository.findByUserId(userId);
        assertThat(settings).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자 알림 설정은 삭제되지 않음")
    void deleteByUserId_OnlyTargetUser() {
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

        notificationSettingRepository.save(NotificationSetting.builder()
                .userId(otherUser.getId())
                .notificationType("SYSTEM")
                .isEnabled(true)
                .build());
        notificationSettingRepository.flush();

        // when
        notificationSettingRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<NotificationSetting> remainingSettings = notificationSettingRepository
                .findByUserId(otherUser.getId());
        assertThat(remainingSettings).hasSize(1);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 생성")
    void save_Success() {
        // given
        NotificationSetting setting = NotificationSetting.builder()
                .userId(userId)
                .notificationType("ATTENDANCE")
                .isEnabled(true)
                .build();

        // when
        NotificationSetting saved = notificationSettingRepository.save(setting);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNotificationType()).isEqualTo("ATTENDANCE");
        assertThat(saved.getIsEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 설정 단건 조회")
    void findById_Success() {
        // when
        Optional<NotificationSetting> result = notificationSettingRepository.findById(setting1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo("CHAT");
        assertThat(result.get().getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 변경 - 엔티티 메서드")
    void updateEnabled_EntityMethod() {
        // given
        NotificationSetting setting = notificationSettingRepository.findById(setting1.getId()).get();
        assertThat(setting.getIsEnabled()).isTrue();

        // when
        setting.updateEnabled(false);
        notificationSettingRepository.flush();
        entityManager.clear();

        // then
        NotificationSetting updated = notificationSettingRepository.findById(setting1.getId()).get();
        assertThat(updated.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("비활성화된 설정 조회")
    void findDisabledSetting_Success() {
        // when
        Optional<NotificationSetting> result = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, "QUIZ");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIsEnabled()).isFalse();
    }
}
