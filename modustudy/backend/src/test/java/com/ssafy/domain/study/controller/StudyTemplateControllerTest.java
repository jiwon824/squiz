package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.CreateTemplateRequest;
import com.ssafy.domain.study.dto.request.UpdateTemplateRequest;
import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.PenaltyPolicy;
import com.ssafy.domain.study.entity.StudyTemplate;
import com.ssafy.domain.study.repository.StudyTemplateRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.RefreshTokenRepository;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
@DisplayName("StudyTemplateController 통합 테스트")
class StudyTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyTemplateRepository studyTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Long testUserId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        // FK 순서: 자식 테이블 먼저 삭제
        refreshTokenRepository.deleteAll();
        studyTemplateRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트용 사용자 생성
        User testUser = User.builder()
                .userId("testuser1")
                .email("testuser@test.com")
                .nickname("testuser")
                .name("Test User")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();

        User otherUser = User.builder()
                .userId("testuser2")
                .email("otheruser@test.com")
                .nickname("otheruser")
                .name("Other User")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        otherUser = userRepository.save(otherUser);
        otherUserId = otherUser.getId();
    }

    // @Transactional로 인해 각 테스트 후 자동 롤백되므로 수동 삭제 불필요
    // @AfterEach 제거 (FK 제약 조건 문제 해결)

    // ============================================================
    // 템플릿 생성 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 생성 성공")
    void createTemplate_Success() throws Exception {
        // given
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("알고리즘 스터디 템플릿")
                .templateType("ALGORITHM")
                .topic("백준 골드 달성")
                .format("문제 풀이 + 코드 리뷰")
                .meetingType(MeetingType.ONLINE)
                .description("백준 골드 티어를 목표로 하는 알고리즘 스터디")
                .textbook("백준 온라인 저지")
                .goal("3개월 내 골드 티어 달성")
                .difficulty(Difficulty.INTERMEDIATE)
                .prerequisites("실버 티어 이상")
                .processDetail("매주 3문제 풀이 후 코드 리뷰")
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study-templates")
                        .header("user-id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("알고리즘 스터디 템플릿"))
                .andExpect(jsonPath("$.templateType").value("ALGORITHM"));
    }

    @Test
    @DisplayName("템플릿 생성 실패 - 이름 없음")
    void createTemplate_Fail_NoName() throws Exception {
        // given
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .templateType("ALGORITHM")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study-templates")
                        .header("user-id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 템플릿 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("내 템플릿 목록 조회 성공")
    void getMyTemplates_Success() throws Exception {
        // given
        saveTemplate(testUserId, "템플릿 1", false);
        saveTemplate(testUserId, "템플릿 2", false);

        // when & then
        mockMvc.perform(get("/api/v1/study-templates/my")
                        .header("user-id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("내 템플릿 목록 조회 - 템플릿 없음")
    void getMyTemplates_Empty() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study-templates/my")
                        .header("user-id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("시스템 템플릿 전체 조회 성공")
    void getSystemTemplates_All_Success() throws Exception {
        // given
        saveTemplate(null, "시스템 ALGORITHM 템플릿", true, "ALGORITHM");
        saveTemplate(null, "시스템 CS 템플릿", true, "CS");

        // when & then
        mockMvc.perform(get("/api/v1/study-templates/system"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("시스템 템플릿 타입별 조회 성공")
    void getSystemTemplates_ByType_Success() throws Exception {
        // given
        saveTemplate(null, "시스템 ALGORITHM 템플릿", true, "ALGORITHM");
        saveTemplate(null, "시스템 CS 템플릿", true, "CS");

        // when & then
        mockMvc.perform(get("/api/v1/study-templates/system")
                        .param("templateType", "ALGORITHM"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ============================================================
    // 템플릿 상세 조회 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 상세 조회 성공 - 본인 템플릿")
    void getTemplate_Success_OwnTemplate() throws Exception {
        // given
        StudyTemplate template = saveTemplate(testUserId, "내 템플릿", false);

        // when & then
        mockMvc.perform(get("/api/v1/study-templates/{templateId}", template.getId())
                        .header("user-id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("내 템플릿"));
    }

    @Test
    @DisplayName("템플릿 상세 조회 성공 - 시스템 템플릿")
    void getSystemTemplate_Success() throws Exception {
        // given
        StudyTemplate template = saveTemplate(null, "시스템 템플릿", true);

        // when & then
        mockMvc.perform(get("/api/v1/study-templates/{templateId}", template.getId())
                        .header("user-id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("시스템 템플릿"));
    }

    // ============================================================
    // 템플릿 수정 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 수정 성공")
    void updateTemplate_Success() throws Exception {
        // given
        StudyTemplate template = saveTemplate(testUserId, "기존 템플릿", false);

        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .name("수정된 템플릿")
                .topic("백준 플래티넘 달성")
                .difficulty(Difficulty.ADVANCED)
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/study-templates/{templateId}", template.getId())
                        .header("user-id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 템플릿"));
    }

    // ============================================================
    // 템플릿 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 삭제 성공")
    void deleteTemplate_Success() throws Exception {
        // given
        StudyTemplate template = saveTemplate(testUserId, "삭제할 템플릿", false);

        // when & then
        mockMvc.perform(delete("/api/v1/study-templates/{templateId}", template.getId())
                        .header("user-id", testUserId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private StudyTemplate saveTemplate(Long userId, String name, boolean isSystem) {
        return saveTemplate(userId, name, isSystem, "ALGORITHM");
    }

    private StudyTemplate saveTemplate(Long userId, String name, boolean isSystem, String templateType) {
        StudyTemplate template = StudyTemplate.builder()
                .userId(userId)
                .name(name)
                .isSystem(isSystem)
                .templateType(templateType)
                .topic("기본 주제")
                .format("기본 형식")
                .meetingType(MeetingType.ONLINE)
                .description("기본 설명")
                .difficulty(Difficulty.INTERMEDIATE)
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .build();

        return studyTemplateRepository.save(template);
    }
}