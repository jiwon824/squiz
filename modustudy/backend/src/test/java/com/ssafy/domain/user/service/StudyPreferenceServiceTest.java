package com.ssafy.domain.user.service;

import com.ssafy.domain.user.dto.request.StudyPreferenceRequest;
import com.ssafy.domain.user.dto.response.StudyPreferenceResponse;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 스터디 선호 설정 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class StudyPreferenceServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스터")
                .name("테스트유저")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .techStacks("[\"Java\",\"Spring\"]")
                .availableDays("[\"MON\",\"WED\"]")
                .preferredTimeSlots("[\"EVENING\"]")
                .preferredDurationWeeks(4)
                .build();

        // ID 설정
        try {
            var idField = testUser.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getStudyPreference")
    class GetStudyPreference {

        @Test
        @DisplayName("스터디 선호 설정 조회 - 성공")
        void getStudyPreference_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            StudyPreferenceResponse response = userService.getStudyPreference(1L);

            // then
            assertThat(response.getTechStacks()).containsExactly("Java", "Spring");
            assertThat(response.getAvailableDays()).containsExactly("MON", "WED");
            assertThat(response.getPreferredTimeSlots()).containsExactly("EVENING");
            assertThat(response.getPreferredDurationWeeks()).isEqualTo(4);
        }

        @Test
        @DisplayName("스터디 선호 설정 조회 - 빈 JSON 필드")
        void getStudyPreference_emptyFields() {
            // given
            testUser.setTechStacks(null);
            testUser.setAvailableDays(null);
            testUser.setPreferredTimeSlots(null);
            testUser.setPreferredDurationWeeks(null);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            StudyPreferenceResponse response = userService.getStudyPreference(1L);

            // then
            assertThat(response.getTechStacks()).isEmpty();
            assertThat(response.getAvailableDays()).isEmpty();
            assertThat(response.getPreferredTimeSlots()).isEmpty();
            assertThat(response.getPreferredDurationWeeks()).isNull();
        }

        @Test
        @DisplayName("스터디 선호 설정 조회 - 사용자 없음")
        void getStudyPreference_userNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getStudyPreference(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("updateStudyPreference")
    class UpdateStudyPreference {

        @Test
        @DisplayName("스터디 선호 설정 수정 - 전체 업데이트")
        void updateStudyPreference_full() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            StudyPreferenceRequest request = createRequest(
                    List.of("Python", "Django"),
                    List.of("TUE", "THU"),
                    List.of("MORNING", "AFTERNOON"),
                    6
            );

            // when
            StudyPreferenceResponse response = userService.updateStudyPreference(1L, request);

            // then
            verify(userRepository).save(any(User.class));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - 기간 범위 클램핑 (최소 2주)")
        void updateStudyPreference_durationClamped_min() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            StudyPreferenceRequest request = createRequest(null, null, null, 1);

            // when
            userService.updateStudyPreference(1L, request);

            // then: 1주 요청 시 2주로 클램핑
            assertThat(testUser.getPreferredDurationWeeks()).isEqualTo(2);
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - 기간 범위 클램핑 (최대 8주)")
        void updateStudyPreference_durationClamped_max() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            StudyPreferenceRequest request = createRequest(null, null, null, 12);

            // when
            userService.updateStudyPreference(1L, request);

            // then: 12주 요청 시 8주로 클램핑
            assertThat(testUser.getPreferredDurationWeeks()).isEqualTo(8);
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - null 필드는 업데이트 안함")
        void updateStudyPreference_partialUpdate() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // 기술 스택만 업데이트 (나머지 null)
            StudyPreferenceRequest request = createRequest(
                    List.of("Rust"), null, null, null);

            // when
            userService.updateStudyPreference(1L, request);

            // then: 기존 값 유지 확인
            assertThat(testUser.getAvailableDays()).isEqualTo("[\"MON\",\"WED\"]");
            assertThat(testUser.getPreferredDurationWeeks()).isEqualTo(4);
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - 사용자 없음")
        void updateStudyPreference_userNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            StudyPreferenceRequest request = createRequest(
                    List.of("Java"), null, null, null);

            // when & then
            assertThatThrownBy(() -> userService.updateStudyPreference(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");
        }
    }

    // ========== 헬퍼 ==========

    private StudyPreferenceRequest createRequest(
            List<String> techStacks,
            List<String> availableDays,
            List<String> preferredTimeSlots,
            Integer preferredDurationWeeks) {
        StudyPreferenceRequest request = new StudyPreferenceRequest();
        try {
            if (techStacks != null) setField(request, "techStacks", techStacks);
            if (availableDays != null) setField(request, "availableDays", availableDays);
            if (preferredTimeSlots != null) setField(request, "preferredTimeSlots", preferredTimeSlots);
            if (preferredDurationWeeks != null) setField(request, "preferredDurationWeeks", preferredDurationWeeks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return request;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
