package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.ApplicationCreateRequest;
import com.ssafy.domain.study.dto.request.ApplicationProcessRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyApplicationRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApplicationController 통합 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 @WithMockUser(username = "testuser", roles = {"USER"})
 class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyApplicationRepository applicationRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    private User leader;
    private User applicant1;
    private User applicant2;
    private Study testStudy;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 스터디장 생성
        leader = User.builder()
                .userId("leader123")
                .email("leader@test.com")
                .nickname("리더")
                .name("김리더")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(5)
                .levelName("Gold")
                .build();
        leader = userRepository.save(leader);

        // 신청자 1
        applicant1 = User.builder()
                .userId("applicant1")
                .email("applicant1@test.com")
                .nickname("신청자1")
                .name("이신청")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        applicant1 = userRepository.save(applicant1);

        // 신청자 2
        applicant2 = User.builder()
                .userId("applicant2")
                .email("applicant2@test.com")
                .nickname("신청자2")
                .name("박신청")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        applicant2 = userRepository.save(applicant2);

        userRepository.flush();

        // 테스트 스터디 생성
        testStudy = Study.builder()
                .leaderId(leader.getId())
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build();
        testStudy = studyRepository.save(testStudy);
        studyRepository.flush();
    }

    // ============================================================
    // 신청 생성 테스트
    // ============================================================

    @Test
    @DisplayName("신청 생성 성공")
    void createApplication_Success() throws Exception {
        // given
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다! 알고리즘에 관심이 많습니다.")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/applications", testStudy.getId())
                        .header("user-id", applicant1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").exists())
                .andExpect(jsonPath("$.studyId").value(testStudy.getId()))
                .andExpect(jsonPath("$.userId").value(applicant1.getId()))
                .andExpect(jsonPath("$.message").value(request.getMessage()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.studyName").value("알고리즘 스터디"))
                .andExpect(jsonPath("$.userName").value("이신청"));
    }

    @Test
    @DisplayName("신청 생성 실패 - 메시지 너무 짧음")
    void createApplication_MessageTooShort() throws Exception {
        // given
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("짧음")  // 10자 미만
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/applications", testStudy.getId())
                        .header("user-id", applicant1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("신청 생성 실패 - 중복 신청")
    void createApplication_Duplicate() throws Exception {
        // given - 이미 신청한 상태
        StudyApplication existingApp = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("이미 신청함")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(existingApp);
        applicationRepository.flush();

        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("다시 신청합니다!다시신청합니다")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/applications", testStudy.getId())
                        .header("user-id", applicant1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이미 신청한 스터디")));
    }

    @Test
    @DisplayName("신청 생성 실패 - 본인 스터디 신청")
    void createApplication_OwnStudy() throws Exception {
        // given
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("본인 스터디에 신청합니다!")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/applications", testStudy.getId())
                        .header("user-id", leader.getId())  // 스터디장이 신청!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("본인이 만든 스터디")));
    }

    // ============================================================
    // 신청 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 신청 목록 조회 성공")
    void getApplicationsByStudy_Success() throws Exception {
        // given - 신청 데이터 생성
        StudyApplication app1 = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다 1")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(app1);

        StudyApplication app2 = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant2.getId())
                .message("신청합니다 2")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(app2);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/applications", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].studyName").value("알고리즘 스터디"));
    }

    @Test
    @DisplayName("스터디별 신청 목록 조회 - 상태 필터링")
    void getApplicationsByStudy_WithStatusFilter() throws Exception {
        // given
        StudyApplication pending = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("대기중")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(pending);

        StudyApplication approved = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant2.getId())
                .message("승인됨")
                .status(ApplicationStatus.APPROVED)
                .build();
        approved.approve();
        applicationRepository.save(approved);
        applicationRepository.flush();

        // when & then - PENDING만 조회
        mockMvc.perform(get("/api/v1/study/{studyId}/applications", testStudy.getId())
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("사용자별 신청 내역 조회 성공")
    void getApplicationsByUser_Success() throws Exception {
        // given
        StudyApplication app1 = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(app1);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/user/{userId}/applications", applicant1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(applicant1.getId()));
    }

    @Test
    @DisplayName("내 신청 내역 조회 성공")
    void getMyApplications_Success() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(app);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/my/applications")
                        .header("user-id", applicant1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("신청 상세 조회 성공")
    void getApplication_Success() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/applications/{applicationId}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(saved.getId()))
                .andExpect(jsonPath("$.studyName").value("알고리즘 스터디"))
                .andExpect(jsonPath("$.userName").value("이신청"));
    }

    // ============================================================
    // 신청 승인 테스트
    // ============================================================

    @Test
    @DisplayName("신청 승인 성공")
    void approveApplication_Success() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/applications/{applicationId}/approve",
                        testStudy.getId(), saved.getId())
                        .header("user-id", leader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(saved.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.processedAt").exists());
    }

    @Test
    @DisplayName("신청 승인 실패 - 권한 없음")
    void approveApplication_NotLeader() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        // when & then - 스터디장이 아닌 사람이 승인 시도
        mockMvc.perform(patch("/api/v1/study/{studyId}/applications/{applicationId}/approve",
                        testStudy.getId(), saved.getId())
                        .header("user-id", applicant2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("스터디장만")));
    }

    @Test
    @DisplayName("신청 승인 실패 - 이미 처리된 신청")
    void approveApplication_AlreadyProcessed() throws Exception {
        // given - 이미 승인된 신청
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.APPROVED)
                .build();
        app.approve();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/applications/{applicationId}/approve",
                        testStudy.getId(), saved.getId())
                        .header("user-id", leader.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이미 처리된 신청")));
    }

    // ============================================================
    // 신청 거절 테스트
    // ============================================================

    @Test
    @DisplayName("신청 거절 성공")
    void rejectApplication_Success() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        ApplicationProcessRequest request = ApplicationProcessRequest.builder()
                .rejectedReason("정원 초과")
                .build();

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/applications/{applicationId}/reject",
                        testStudy.getId(), saved.getId())
                        .header("user-id", leader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(saved.getId()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedReason").value("정원 초과"))
                .andExpect(jsonPath("$.processedAt").exists());
    }

    @Test
    @DisplayName("신청 거절 실패 - 권한 없음")
    void rejectApplication_NotLeader() throws Exception {
        // given
        StudyApplication app = StudyApplication.builder()
                .studyId(testStudy.getId())
                .userId(applicant1.getId())
                .message("신청합니다")
                .status(ApplicationStatus.PENDING)
                .build();
        StudyApplication saved = applicationRepository.save(app);
        applicationRepository.flush();

        ApplicationProcessRequest request = ApplicationProcessRequest.builder()
                .rejectedReason("정원 초과")
                .build();

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/applications/{applicationId}/reject",
                        testStudy.getId(), saved.getId())
                        .header("user-id", applicant2.getId())  // 스터디장 아님
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("스터디장만")));
    }
}
