package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    private User leader;
    private Study testStudy;

    @BeforeEach
    void setUp() {
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
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        leader = userRepository.save(leader);

        // 테스트용 스터디 생성
        testStudy = Study.builder()
                .leaderId(leader.getId())
                .name("테스트 스터디")
                .description("테스트용 스터디입니다")
                .topic("알고리즘")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build();

        testStudy = studyRepository.save(testStudy);
    }

    // ============================================================
    // 스터디 생성 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 생성 API 성공")
    void createStudy_Success() throws Exception {
        // given
        StudyCreateRequest request = StudyCreateRequest.builder()
                .name("새로운 스터디")
                .description("스터디 설명")
                .topic("백엔드")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/study")
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("새로운 스터디"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("스터디 생성 실패 - 오프라인 스터디 지역 정보 없음")
    void createStudy_OfflineWithoutRegion_Fail() throws Exception {
        // given
        StudyCreateRequest request = StudyCreateRequest.builder()
                .name("오프라인 스터디")
                .topic("알고리즘")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.OFFLINE)
                .regionId(null)  // 지역 없음
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/study")
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오프라인/혼합 스터디는 지역 정보가 필수입니다"));
    }

    @Test
    @DisplayName("스터디 생성 실패 - 잘못된 날짜 범위")
    void createStudy_InvalidDateRange_Fail() throws Exception {
        // given
        StudyCreateRequest request = StudyCreateRequest.builder()
                .name("테스트 스터디")
                .topic("알고리즘")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .startDate(LocalDate.of(2025, 5, 1))
                .endDate(LocalDate.of(2025, 2, 1))  // 시작일보다 앞섬
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/study")
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("종료일은 시작일보다 늦어야 합니다"));
    }

    // ============================================================
    // 스터디 조회 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 상세 조회 성공")
    void getStudyDetail_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStudy.getId()))
                .andExpect(jsonPath("$.name").value("테스트 스터디"))
                .andExpect(jsonPath("$.topic").value("알고리즘"));
    }

    @Test
    @DisplayName("존재하지 않는 스터디 조회 실패")
    void getStudyDetail_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    @Test
    @DisplayName("전체 스터디 목록 조회 성공")
    void getAllStudies_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("모집중인 스터디 목록 조회 성공")
    void getRecruitingStudies_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/recruiting")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("스터디 존재 여부 확인 성공")
    void existsStudy_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/exists", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    // ============================================================
    // 스터디 수정 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 수정 API 성공")
    void updateStudy_Success() throws Exception {
        // given
        StudyUpdateRequest request = StudyUpdateRequest.builder()
                .name("수정된 스터디")
                .maxMembers(15)
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 스터디"))
                .andExpect(jsonPath("$.maxMembers").value(15));
    }

    @Test
    @DisplayName("스터디 수정 실패 - 권한 없음")
    void updateStudy_Forbidden() throws Exception {
        // given
        StudyUpdateRequest request = StudyUpdateRequest.builder()
                .name("수정된 스터디")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", "999")  // 다른 사용자
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("권한")));
    }

    // ============================================================
    // 스터디 삭제 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 삭제 API 성공")
    void deleteStudy_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("스터디 삭제 실패 - 진행 중인 스터디")
    void deleteStudy_InProgress_Fail() throws Exception {
        // given
        testStudy.updateStatus(Status.IN_PROGRESS);
        studyRepository.save(testStudy);

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("진행 중이거나 완료된 스터디는 삭제할 수 없습니다"));
    }

    // ============================================================
    // 스터디 상태 변경 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 상태 변경 API 성공")
    void updateStudyStatus_Success() throws Exception {
        // given
        String requestBody = """
            {
                "status": "RECRUITING"
            }
            """;

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECRUITING"));
    }

    @Test
    @DisplayName("완료된 스터디 상태 변경 실패")
    void updateStudyStatus_CompletedStudy_Fail() throws Exception {
        // given
        testStudy.updateStatus(Status.COMPLETED);
        studyRepository.save(testStudy);

        String requestBody = """
            {
                "status": "RECRUITING"
            }
            """;

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("완료된 스터디는 상태를 변경할 수 없습니다"));
    }

    // ============================================================
    // 모집 기간 연장 API 테스트
    // ============================================================

    @Test
    @DisplayName("모집 기간 연장 API 성공")
    void extendRecruitment_Success() throws Exception {
        // given
        testStudy.updateStatus(Status.RECRUITING);
        studyRepository.save(testStudy);

        String requestBody = """
            {
                "newEndDate": "2025-02-15"
            }
            """;

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recruitEndDate").value("2025-02-15"))
                .andExpect(jsonPath("$.extensionCount").value(1));
    }

    @Test
    @DisplayName("모집 기간 연장 실패 - 2회 시도")
    void extendRecruitment_SecondAttempt_Fail() throws Exception {
        // given - 먼저 정상적으로 1회 연장
        testStudy.updateStatus(Status.RECRUITING);
        studyRepository.save(testStudy);

        // 첫 번째 연장 (성공)
        String firstRequest = """
            {
                "newEndDate": "2025-02-10"
            }
            """;

        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isOk());

        // 두 번째 연장 시도 (실패해야 함)
        String secondRequest = """
            {
                "newEndDate": "2025-02-20"
            }
            """;

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequest))
                .andDo(result -> {
                    System.out.println("=== 2회 연장 시도 응답 ===");
                    System.out.println("Status: " + result.getResponse().getStatus());
                    System.out.println("Body: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모집 기간 연장 실패 - 모집 중이 아님")
    void extendRecruitment_NotRecruiting_Fail() throws Exception {
        // given
        String requestBody = """
            {
                "newEndDate": "2025-02-15"
            }
            """;

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("모집 중인 스터디만 기간을 연장할 수 있습니다"));
    }

    // ============================================================
    // 특정 상태 스터디 개수 조회 API 테스트
    // ============================================================

    @Test
    @DisplayName("특정 상태 스터디 개수 조회 성공")
    void countStudiesByStatus_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/count")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }
}