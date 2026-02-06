package com.ssafy.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.domain.user.dto.request.StudyPreferenceRequest;
import com.ssafy.domain.user.dto.response.StudyPreferenceResponse;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 스터디 선호 설정 API 컨트롤러 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 class StudyPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private Authentication mockAuth;
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
                .build();

        // ID를 리플렉션으로 설정 (BaseEntity의 id는 @GeneratedValue)
        try {
            var idField = testUser.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SsafyUserDetails userDetails = new SsafyUserDetails(testUser);
        mockAuth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/study-preference")
    class GetStudyPreference {

        @Test
        @DisplayName("스터디 선호 설정 조회 - 성공")
        void getStudyPreference_success() throws Exception {
            // given
            StudyPreferenceResponse response = StudyPreferenceResponse.builder()
                    .techStacks(List.of("Java", "Spring", "React"))
                    .availableDays(List.of("MON", "WED", "FRI"))
                    .preferredTimeSlots(List.of("EVENING"))
                    .preferredDurationWeeks(4)
                    .build();

            given(userService.getStudyPreference(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/study-preference")
                            .with(authentication(mockAuth)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.techStacks", hasSize(3)))
                    .andExpect(jsonPath("$.data.techStacks[0]").value("Java"))
                    .andExpect(jsonPath("$.data.availableDays", hasSize(3)))
                    .andExpect(jsonPath("$.data.availableDays[0]").value("MON"))
                    .andExpect(jsonPath("$.data.preferredTimeSlots[0]").value("EVENING"))
                    .andExpect(jsonPath("$.data.preferredDurationWeeks").value(4));
        }

        @Test
        @DisplayName("스터디 선호 설정 조회 - 빈 데이터")
        void getStudyPreference_empty() throws Exception {
            // given
            StudyPreferenceResponse response = StudyPreferenceResponse.builder()
                    .techStacks(List.of())
                    .availableDays(List.of())
                    .preferredTimeSlots(List.of())
                    .preferredDurationWeeks(null)
                    .build();

            given(userService.getStudyPreference(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/study-preference")
                            .with(authentication(mockAuth)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.techStacks", hasSize(0)))
                    .andExpect(jsonPath("$.data.availableDays", hasSize(0)))
                    .andExpect(jsonPath("$.data.preferredDurationWeeks").isEmpty());
        }

        @Test
        @DisplayName("스터디 선호 설정 조회 - 인증 없이 요청")
        void getStudyPreference_unauthorized() throws Exception {
            // when & then (인증 없이 요청 → 401 또는 403)
            mockMvc.perform(get("/api/v1/users/me/study-preference"))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/study-preference")
    class UpdateStudyPreference {

        @Test
        @DisplayName("스터디 선호 설정 수정 - 전체 업데이트")
        void updateStudyPreference_full() throws Exception {
            // given
            StudyPreferenceRequest request = new StudyPreferenceRequest();
            // 리플렉션으로 필드 설정 (Lombok @NoArgsConstructor + @Getter only)
            setField(request, "techStacks", List.of("Python", "Docker", "Kubernetes"));
            setField(request, "availableDays", List.of("TUE", "THU", "SAT"));
            setField(request, "preferredTimeSlots", List.of("EVENING", "NIGHT"));
            setField(request, "preferredDurationWeeks", 6);

            StudyPreferenceResponse response = StudyPreferenceResponse.builder()
                    .techStacks(List.of("Python", "Docker", "Kubernetes"))
                    .availableDays(List.of("TUE", "THU", "SAT"))
                    .preferredTimeSlots(List.of("EVENING", "NIGHT"))
                    .preferredDurationWeeks(6)
                    .build();

            given(userService.updateStudyPreference(eq(1L), any(StudyPreferenceRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/users/me/study-preference")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.techStacks", hasSize(3)))
                    .andExpect(jsonPath("$.data.techStacks[0]").value("Python"))
                    .andExpect(jsonPath("$.data.availableDays", hasSize(3)))
                    .andExpect(jsonPath("$.data.preferredTimeSlots", hasSize(2)))
                    .andExpect(jsonPath("$.data.preferredDurationWeeks").value(6));
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - 기술 스택만 업데이트")
        void updateStudyPreference_techStacksOnly() throws Exception {
            // given
            StudyPreferenceRequest request = new StudyPreferenceRequest();
            setField(request, "techStacks", List.of("Go", "gRPC"));

            StudyPreferenceResponse response = StudyPreferenceResponse.builder()
                    .techStacks(List.of("Go", "gRPC"))
                    .availableDays(List.of("MON"))
                    .preferredTimeSlots(List.of("MORNING"))
                    .preferredDurationWeeks(4)
                    .build();

            given(userService.updateStudyPreference(eq(1L), any(StudyPreferenceRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/users/me/study-preference")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.techStacks[0]").value("Go"));
        }

        @Test
        @DisplayName("스터디 선호 설정 수정 - 인증 없이 요청")
        void updateStudyPreference_unauthorized() throws Exception {
            // when & then (인증 없이 요청 → 401 또는 403)
            mockMvc.perform(put("/api/v1/users/me/study-preference")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"techStacks\":[\"Java\"]}"))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }

    // ========== 헬퍼 ==========

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("필드 설정 실패: " + fieldName, e);
        }
    }
}
