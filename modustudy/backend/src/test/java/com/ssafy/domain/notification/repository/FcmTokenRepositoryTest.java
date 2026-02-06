package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.DeviceType;
import com.ssafy.domain.notification.entity.FcmToken;
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
class FcmTokenRepositoryTest {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private FcmToken token1;
    private FcmToken token2;
    private FcmToken token3;

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
                .isActive(true)
                .build());

        token3 = fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token("fcm_token_web_001")
                .deviceType(DeviceType.WEB)
                .isActive(false)  // 비활성화된 토큰
                .build());

        fcmTokenRepository.flush();
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 활성화된 FCM 토큰 목록 조회")
    void findByUserIdAndIsActiveTrue_Success() {
        // when
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);

        // then
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(FcmToken::getIsActive)
                .containsOnly(true);
    }

    @Test
    @DisplayName("사용자별 FCM 토큰 전체 조회")
    void findByUserId_Success() {
        // when
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);

        // then
        assertThat(tokens).hasSize(3);
        assertThat(tokens).extracting(FcmToken::getUserId)
                .containsOnly(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 목록 반환")
    void findByUserId_NotFound() {
        // when
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(999L);

        // then
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("토큰으로 조회")
    void findByToken_Success() {
        // when
        Optional<FcmToken> result = fcmTokenRepository.findByToken("fcm_token_android_001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(result.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 시 빈 Optional 반환")
    void findByToken_NotFound() {
        // when
        Optional<FcmToken> result = fcmTokenRepository.findByToken("nonexistent_token");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 + 토큰으로 조회")
    void findByUserIdAndToken_Success() {
        // when
        Optional<FcmToken> result = fcmTokenRepository
                .findByUserIdAndToken(userId, "fcm_token_android_001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceType()).isEqualTo(DeviceType.ANDROID);
    }

    @Test
    @DisplayName("다른 사용자의 토큰으로 조회 시 빈 Optional 반환")
    void findByUserIdAndToken_DifferentUser() {
        // when
        Optional<FcmToken> result = fcmTokenRepository
                .findByUserIdAndToken(999L, "fcm_token_android_001");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("토큰 존재 여부 확인 - 존재하는 경우")
    void existsByToken_True() {
        // when
        boolean exists = fcmTokenRepository.existsByToken("fcm_token_android_001");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("토큰 존재 여부 확인 - 존재하지 않는 경우")
    void existsByToken_False() {
        // when
        boolean exists = fcmTokenRepository.existsByToken("nonexistent_token");

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("토큰 단건 삭제 - ID")
    void deleteById_Success() {
        // given
        Long tokenId = token1.getId();

        // when
        fcmTokenRepository.deleteById(tokenId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<FcmToken> result = fcmTokenRepository.findById(tokenId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("토큰 삭제 - 토큰값으로")
    void deleteByToken_Success() {
        // when
        fcmTokenRepository.deleteByToken("fcm_token_android_001");
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<FcmToken> result = fcmTokenRepository.findByToken("fcm_token_android_001");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자별 토큰 전체 삭제")
    void deleteByUserId_Success() {
        // when
        fcmTokenRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자 토큰은 삭제되지 않음")
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

        fcmTokenRepository.save(FcmToken.builder()
                .userId(otherUser.getId())
                .token("other_user_token")
                .deviceType(DeviceType.ANDROID)
                .isActive(true)
                .build());
        fcmTokenRepository.flush();

        // when
        fcmTokenRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<FcmToken> remainingTokens = fcmTokenRepository.findByUserId(otherUser.getId());
        assertThat(remainingTokens).hasSize(1);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("FCM 토큰 생성")
    void save_Success() {
        // given
        FcmToken token = FcmToken.builder()
                .userId(userId)
                .token("new_fcm_token_001")
                .deviceType(DeviceType.ANDROID)
                .build();

        // when
        FcmToken saved = fcmTokenRepository.save(token);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("new_fcm_token_001");
        assertThat(saved.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("FCM 토큰 단건 조회")
    void findById_Success() {
        // when
        Optional<FcmToken> result = fcmTokenRepository.findById(token1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("fcm_token_android_001");
        assertThat(result.get().getDeviceType()).isEqualTo(DeviceType.ANDROID);
    }

    // ============================================================
    // 엔티티 메서드 테스트
    // ============================================================

    @Test
    @DisplayName("토큰 비활성화 - 엔티티 메서드")
    void deactivate_EntityMethod() {
        // given
        FcmToken token = fcmTokenRepository.findById(token1.getId()).get();
        assertThat(token.getIsActive()).isTrue();

        // when
        token.deactivate();
        fcmTokenRepository.flush();
        entityManager.clear();

        // then
        FcmToken updated = fcmTokenRepository.findById(token1.getId()).get();
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("토큰 활성화 - 엔티티 메서드")
    void activate_EntityMethod() {
        // given
        FcmToken token = fcmTokenRepository.findById(token3.getId()).get();
        assertThat(token.getIsActive()).isFalse();

        // when
        token.activate();
        fcmTokenRepository.flush();
        entityManager.clear();

        // then
        FcmToken updated = fcmTokenRepository.findById(token3.getId()).get();
        assertThat(updated.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("토큰 갱신 - 엔티티 메서드")
    void updateToken_EntityMethod() {
        // given
        FcmToken token = fcmTokenRepository.findById(token3.getId()).get();
        assertThat(token.getIsActive()).isFalse();

        // when
        token.updateToken("updated_fcm_token");
        fcmTokenRepository.flush();
        entityManager.clear();

        // then
        FcmToken updated = fcmTokenRepository.findById(token3.getId()).get();
        assertThat(updated.getToken()).isEqualTo("updated_fcm_token");
        assertThat(updated.getIsActive()).isTrue();  // 갱신 시 활성화됨
    }

    @Test
    @DisplayName("디바이스 타입별 토큰 조회")
    void findByDeviceType_Success() {
        // when
        List<FcmToken> allTokens = fcmTokenRepository.findByUserId(userId);

        // then
        long androidCount = allTokens.stream()
                .filter(t -> t.getDeviceType() == DeviceType.ANDROID)
                .count();
        long iosCount = allTokens.stream()
                .filter(t -> t.getDeviceType() == DeviceType.IOS)
                .count();
        long webCount = allTokens.stream()
                .filter(t -> t.getDeviceType() == DeviceType.WEB)
                .count();

        assertThat(androidCount).isEqualTo(1);
        assertThat(iosCount).isEqualTo(1);
        assertThat(webCount).isEqualTo(1);
    }
}
