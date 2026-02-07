package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.ssafy.domain.notification.dto.response.NotificationSettingListResponse;
import com.ssafy.domain.notification.dto.response.NotificationSettingResponse;
import com.ssafy.domain.notification.entity.NotificationSetting;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.repository.NotificationSettingRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificationSettingServiceTest {

    @Autowired
    private NotificationSettingService notificationSettingService;

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

        // 2. NotificationSetting 생성 (일부만)
        setting1 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("CHAT")
                .isEnabled(true)
                .build());

        setting2 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("QUIZ")
                .isEnabled(false)  // 비활성화
                .build());

        notificationSettingRepository.flush();
    }

    // ============================================================
    // 알림 설정 조회 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 조회 - 모든 타입 반환")
    void getSettings_AllTypes() {
        // when
        NotificationSettingListResponse response = notificationSettingService.getSettings(userId);

        // then
        assertThat(response.getSettings()).hasSize(NotificationType.values().length);
    }

    @Test
    @DisplayName("알림 설정 조회 - 기존 설정 반영")
    void getSettings_ExistingSettings() {
        // when
        NotificationSettingListResponse response = notificationSettingService.getSettings(userId);

        // then
        Optional<NotificationSettingResponse> chatSetting = response.getSettings().stream()
                .filter(s -> s.getType().equals("CHAT"))
                .findFirst();
        Optional<NotificationSettingResponse> quizSetting = response.getSettings().stream()
                .filter(s -> s.getType().equals("QUIZ"))
                .findFirst();

        assertThat(chatSetting).isPresent();
        assertThat(chatSetting.get().getIsEnabled()).isTrue();

        assertThat(quizSetting).isPresent();
        assertThat(quizSetting.get().getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정 조회 - 설정 없는 타입은 기본값(true)")
    void getSettings_DefaultValue() {
        // when
        NotificationSettingListResponse response = notificationSettingService.getSettings(userId);

        // then - SCHEDULE은 설정 안 했으니 기본값 true
        Optional<NotificationSettingResponse> scheduleSetting = response.getSettings().stream()
                .filter(s -> s.getType().equals("SCHEDULE"))
                .findFirst();

        assertThat(scheduleSetting).isPresent();
        assertThat(scheduleSetting.get().getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 조회 - 설정 없는 사용자")
    void getSettings_NoSettings() {
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
        NotificationSettingListResponse response = notificationSettingService.getSettings(otherUser.getId());

        // then - 모든 타입이 기본값(true)으로 반환
        assertThat(response.getSettings()).hasSize(NotificationType.values().length);
        assertThat(response.getSettings()).allMatch(s -> s.getIsEnabled());
    }

    // ============================================================
    // 알림 설정 수정 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 수정 - 기존 설정 업데이트")
    void updateSettings_ExistingSetting() {
        // given
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("CHAT")
                                .isEnabled(false)  // true -> false
                                .build()
                ))
                .build();

        // when
        notificationSettingService.updateSettings(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        NotificationSetting updated = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, "CHAT").get();
        assertThat(updated.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정 수정 - 새 설정 생성")
    void updateSettings_NewSetting() {
        // given
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("SCHEDULE")  // 기존에 없던 설정
                                .isEnabled(false)
                                .build()
                ))
                .build();

        // when
        notificationSettingService.updateSettings(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<NotificationSetting> created = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, "SCHEDULE");
        assertThat(created).isPresent();
        assertThat(created.get().getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정 수정 - 여러 설정 동시 수정")
    void updateSettings_MultipleSettings() {
        // given
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("CHAT")
                                .isEnabled(false)
                                .build(),
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("QUIZ")
                                .isEnabled(true)
                                .build(),
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("SYSTEM")
                                .isEnabled(false)
                                .build()
                ))
                .build();

        // when
        notificationSettingService.updateSettings(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(notificationSettingRepository.findByUserIdAndNotificationType(userId, "CHAT").get().getIsEnabled())
                .isFalse();
        assertThat(notificationSettingRepository.findByUserIdAndNotificationType(userId, "QUIZ").get().getIsEnabled())
                .isTrue();
        assertThat(notificationSettingRepository.findByUserIdAndNotificationType(userId, "SYSTEM").get().getIsEnabled())
                .isFalse();
    }

    @Test
    @DisplayName("알림 설정 수정 - 유효하지 않은 타입")
    void updateSettings_InvalidType() {
        // given
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("INVALID_TYPE")
                                .isEnabled(true)
                                .build()
                ))
                .build();

        // when & then
        assertThatThrownBy(() -> notificationSettingService.updateSettings(userId, request))
                .isInstanceOf(NotificationException.InvalidNotificationTypeException.class);
    }

    // ============================================================
    // 알림 활성화 여부 확인 테스트
    // ============================================================

    @Test
    @DisplayName("알림 활성화 여부 확인 - 활성화된 경우")
    void isNotificationEnabled_True() {
        // when
        boolean enabled = notificationSettingService.isNotificationEnabled(userId, NotificationType.CHAT);

        // then
        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("알림 활성화 여부 확인 - 비활성화된 경우")
    void isNotificationEnabled_False() {
        // when
        boolean enabled = notificationSettingService.isNotificationEnabled(userId, NotificationType.QUIZ);

        // then
        assertThat(enabled).isFalse();
    }

    @Test
    @DisplayName("알림 활성화 여부 확인 - 설정 없으면 기본값 true")
    void isNotificationEnabled_Default() {
        // when - SCHEDULE은 설정 안 함
        boolean enabled = notificationSettingService.isNotificationEnabled(userId, NotificationType.SCHEDULE);

        // then
        assertThat(enabled).isTrue();
    }

    // ============================================================
    // 기본 설정 초기화 테스트
    // ============================================================

    @Test
    @DisplayName("기본 알림 설정 초기화")
    void initializeDefaultSettings_Success() {
        // given
        User newUser = userRepository.save(User.builder()
                .userId("newuser")
                .email("new@test.com")
                .nickname("새유저")
                .name("새")
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
        notificationSettingService.initializeDefaultSettings(newUser.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        List<NotificationSetting> settings = notificationSettingRepository.findByUserId(newUser.getId());
        assertThat(settings).hasSize(NotificationType.values().length);
        assertThat(settings).allMatch(s -> s.getIsEnabled());
    }
}
