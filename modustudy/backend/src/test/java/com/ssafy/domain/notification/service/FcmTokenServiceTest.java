package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.request.FcmTokenDeleteRequest;
import com.ssafy.domain.notification.dto.request.FcmTokenRequest;
import com.ssafy.domain.notification.entity.DeviceType;
import com.ssafy.domain.notification.entity.FcmToken;
import com.ssafy.domain.notification.repository.FcmTokenRepository;
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
class FcmTokenServiceTest {

    @Autowired
    private FcmTokenService fcmTokenService;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private User otherUser;
    private Long userId;
    private Long otherUserId;
    private FcmToken token1;
    private FcmToken token2;

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

        otherUser = userRepository.save(User.builder()
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

        userId = user.getId();
        otherUserId = otherUser.getId();

        // 2. FcmToken 생성
        token1 = fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token("fcm_token_android_001")
                .deviceType(DeviceType.ANDROID)
                .isActive(true)
                .build());

        token2 = fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token("fcm_token_ios_001")
                .deviceType(DeviceType.IOS)
                .isActive(false)  // 비활성화
                .build());

        fcmTokenRepository.flush();
    }

    // ============================================================
    // FCM 토큰 등록 테스트
    // ============================================================

    @Test
    @DisplayName("FCM 토큰 등록 - 새 토큰")
    void registerToken_NewToken() {
        // given
        FcmTokenRequest request = FcmTokenRequest.builder()
                .token("new_fcm_token_001")
                .deviceType(DeviceType.ANDROID)
                .build();

        // when
        fcmTokenService.registerToken(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<FcmToken> saved = fcmTokenRepository.findByToken("new_fcm_token_001");
        assertThat(saved).isPresent();
        assertThat(saved.get().getUserId()).isEqualTo(userId);
        assertThat(saved.get().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 기존 토큰 활성화 (같은 사용자)")
    void registerToken_ExistingToken_SameUser() {
        // given
        FcmTokenRequest request = FcmTokenRequest.builder()
                .token("fcm_token_ios_001")  // 비활성화된 기존 토큰
                .deviceType(DeviceType.IOS)
                .build();

        // when
        fcmTokenService.registerToken(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        FcmToken updated = fcmTokenRepository.findByToken("fcm_token_ios_001").get();
        assertThat(updated.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 다른 사용자 토큰 비활성화 후 새로 등록")
    void registerToken_ExistingToken_DifferentUser() {
        // given
        FcmTokenRequest request = FcmTokenRequest.builder()
                .token("fcm_token_android_001")  // user의 기존 토큰
                .deviceType(DeviceType.ANDROID)
                .build();

        // when - otherUser가 같은 토큰으로 등록 시도
        fcmTokenService.registerToken(otherUserId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        // 기존 토큰은 비활성화
        FcmToken oldToken = fcmTokenRepository.findById(token1.getId()).get();
        assertThat(oldToken.getIsActive()).isFalse();

        // 새 토큰 생성됨
        List<FcmToken> otherUserTokens = fcmTokenRepository.findByUserId(otherUserId);
        assertThat(otherUserTokens).hasSize(1);
        assertThat(otherUserTokens.get(0).getIsActive()).isTrue();
    }

    // ============================================================
    // FCM 토큰 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("FCM 토큰 삭제 성공")
    void deleteToken_Success() {
        // given
        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("fcm_token_android_001")
                .build();

        // when
        fcmTokenService.deleteToken(userId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(fcmTokenRepository.findByToken("fcm_token_android_001")).isEmpty();
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 존재하지 않는 토큰")
    void deleteToken_NotFound() {
        // given
        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("nonexistent_token")
                .build();

        // when & then
        assertThatThrownBy(() -> fcmTokenService.deleteToken(userId, request))
                .isInstanceOf(NotificationException.FcmTokenNotFoundException.class);
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 다른 사용자의 토큰")
    void deleteToken_NotOwner() {
        // given
        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("fcm_token_android_001")  // user의 토큰
                .build();

        // when & then - otherUser가 삭제 시도
        assertThatThrownBy(() -> fcmTokenService.deleteToken(otherUserId, request))
                .isInstanceOf(NotificationException.FcmTokenNotFoundException.class);
    }

    // ============================================================
    // 활성 토큰 조회 테스트
    // ============================================================

    @Test
    @DisplayName("활성화된 FCM 토큰 목록 조회")
    void getActiveTokens_Success() {
        // when
        List<FcmToken> activeTokens = fcmTokenService.getActiveTokens(userId);

        // then
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isEqualTo("fcm_token_android_001");
    }

    @Test
    @DisplayName("활성화된 토큰이 없는 경우")
    void getActiveTokens_Empty() {
        // when
        List<FcmToken> activeTokens = fcmTokenService.getActiveTokens(otherUserId);

        // then
        assertThat(activeTokens).isEmpty();
    }

    // ============================================================
    // 모든 토큰 비활성화 테스트
    // ============================================================

    @Test
    @DisplayName("모든 FCM 토큰 비활성화")
    void deactivateAllTokens_Success() {
        // given - 활성 토큰 추가
        fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token("fcm_token_web_001")
                .deviceType(DeviceType.WEB)
                .isActive(true)
                .build());
        fcmTokenRepository.flush();

        // when
        fcmTokenService.deactivateAllTokens(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> activeTokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        assertThat(activeTokens).isEmpty();

        List<FcmToken> allTokens = fcmTokenRepository.findByUserId(userId);
        assertThat(allTokens).hasSize(3);  // 토큰은 삭제되지 않음
    }

    @Test
    @DisplayName("다른 사용자 토큰은 비활성화되지 않음")
    void deactivateAllTokens_OnlyTargetUser() {
        // given
        fcmTokenRepository.save(FcmToken.builder()
                .userId(otherUserId)
                .token("other_user_token")
                .deviceType(DeviceType.ANDROID)
                .isActive(true)
                .build());
        fcmTokenRepository.flush();

        // when
        fcmTokenService.deactivateAllTokens(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> otherUserActiveTokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(otherUserId);
        assertThat(otherUserActiveTokens).hasSize(1);
    }

    // ============================================================
    // 모든 토큰 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("모든 FCM 토큰 삭제")
    void deleteAllTokens_Success() {
        // when
        fcmTokenService.deleteAllTokens(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자 토큰은 삭제되지 않음")
    void deleteAllTokens_OnlyTargetUser() {
        // given
        fcmTokenRepository.save(FcmToken.builder()
                .userId(otherUserId)
                .token("other_user_token_2")
                .deviceType(DeviceType.IOS)
                .isActive(true)
                .build());
        fcmTokenRepository.flush();

        // when
        fcmTokenService.deleteAllTokens(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> otherUserTokens = fcmTokenRepository.findByUserId(otherUserId);
        assertThat(otherUserTokens).hasSize(1);
    }
}
