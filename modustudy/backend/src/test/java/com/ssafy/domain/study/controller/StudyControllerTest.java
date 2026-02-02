package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;

import java.time.LocalDateTime;
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

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    private User leader;
    private Study testStudy;
    private Topic topic;
    private Topic topic2;
    private Format format;

    @BeforeEach
    void setUp() {
        // Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        topic2 = topicRepository.save(Topic.builder()
                .name("백엔드")
                .sortOrder(2)
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
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        leader = userRepository.save(leader);
        userRepository.flush();

        // 테스트용 스터디 생성
        testStudy = Study.builder()
                .leaderId(leader.getId())
                .name("테스트 스터디")
                .description("테스트용 스터디입니다")
                .topic(topic)
                .format(format)
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
        studyRepository.flush();
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
                .topicId(topic2.getId())
                .formatId(format.getId())
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
                .topicId(topic.getId())
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
                .topicId(topic.getId())
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
                .andExpect(jsonPath("$.name").value("테스트 스터디"));
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
        User otherUser = userRepository.save(User.builder()
                .userId("other123")
                .email("other@test.com")
                .nickname("다른유저")
                .name("다른사람")
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

        StudyUpdateRequest request = StudyUpdateRequest.builder()
                .name("수정된 스터디")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", otherUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("권한")));
    }

    // ============================================================
    // 스터디 상태 변경 API 테스트 (범용 /status 엔드포인트 사용)
    // ============================================================

    @Test
    @DisplayName("스터디 모집 시작 성공")
    void startRecruiting_Success() throws Exception {
        // given - DRAFT -> RECRUITING
        String requestBody = objectMapper.writeValueAsString(
                new StudyController.StatusUpdateRequest(Status.RECRUITING));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECRUITING"));
    }

    @Test
    @DisplayName("스터디 모집 종료 성공")
    void closeRecruiting_Success() throws Exception {
        // given - 모집 중 상태로 변경
        testStudy.updateStatus(Status.RECRUITING);
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.StatusUpdateRequest(Status.RECRUIT_CLOSED));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECRUIT_CLOSED"));
    }

    @Test
    @DisplayName("스터디 시작 성공")
    void startStudy_Success() throws Exception {
        // given - 모집 완료 상태로 변경
        testStudy.updateStatus(Status.RECRUIT_CLOSED);
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.StatusUpdateRequest(Status.IN_PROGRESS));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("스터디 완료 성공")
    void completeStudy_Success() throws Exception {
        // given - 진행 중 상태로 변경
        testStudy.updateStatus(Status.IN_PROGRESS);
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.StatusUpdateRequest(Status.COMPLETED));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("스터디 취소 성공")
    void cancelStudy_Success() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new StudyController.StatusUpdateRequest(Status.CANCELLED));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/status", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ============================================================
    // 스터디 삭제 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 삭제 성공")
    void deleteStudy_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isNoContent());

        // 삭제 확인
        mockMvc.perform(get("/api/v1/study/{studyId}", testStudy.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("스터디 삭제 실패 - 진행 중인 스터디")
    void deleteStudy_InProgress_Fail() throws Exception {
        // given - 진행 중 상태로 변경
        testStudy.updateStatus(Status.IN_PROGRESS);
        studyRepository.save(testStudy);
        studyRepository.flush();

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("삭제할 수 없습니다")));
    }

    // ============================================================
    // 모집 연장 API 테스트 (RequestBody 방식)
    // ============================================================

    @Test
    @DisplayName("모집 연장 성공")
    void extendRecruitment_Success() throws Exception {
        // given - 모집 중 상태로 변경
        testStudy.updateStatus(Status.RECRUITING);
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.RecruitmentExtensionRequest(LocalDate.of(2025, 2, 15)));

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
    @DisplayName("모집 연장 실패 - 이미 1회 연장")
    void extendRecruitment_AlreadyExtended_Fail() throws Exception {
        // given - 모집 중 상태 + 이미 1회 연장
        testStudy.updateStatus(Status.RECRUITING);
        testStudy.extendRecruitment(LocalDate.of(2025, 2, 15));
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.RecruitmentExtensionRequest(LocalDate.of(2025, 2, 28)));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("연장")));
    }

    // ============================================================
    // 검색 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 검색 - 키워드로 검색")
    void searchStudies_ByKeyword() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/search")
                        .param("keyword", "테스트")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("스터디 검색 - 주제로 검색")
    void searchStudies_ByTopic() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/search")
                        .param("topicId", topic.getId().toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("스터디 검색 - 상태로 검색")
    void searchStudies_ByStatus() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/search")
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ============================================================
    // 스터디 시작 API 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 시작 API 성공 - 모집완료 상태에서 시작")
    void startStudy_FromRecruitClosed_Success() throws Exception {
        // given - 모집 완료 상태로 변경 + 스터디장을 멤버로 추가
        testStudy.updateStatus(Status.RECRUIT_CLOSED);
        studyRepository.save(testStudy);
        studyRepository.flush();

        StudyMember leaderMember = StudyMember.builder()
                .studyId(testStudy.getId())
                .userId(leader.getId())
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .joinedAt(LocalDateTime.now())
                .build();
        studyMemberRepository.save(leaderMember);
        studyMemberRepository.flush();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/start", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("스터디 시작 API 성공 - 확정대기 상태에서 시작")
    void startStudy_FromPending_Success() throws Exception {
        // given - 확정대기 상태로 변경 + 스터디장을 멤버로 추가
        testStudy.updateStatus(Status.PENDING);
        studyRepository.save(testStudy);
        studyRepository.flush();

        StudyMember leaderMember = StudyMember.builder()
                .studyId(testStudy.getId())
                .userId(leader.getId())
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .joinedAt(LocalDateTime.now())
                .build();
        studyMemberRepository.save(leaderMember);
        studyMemberRepository.flush();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/start", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("스터디 시작 API 실패 - 모집중 상태에서는 시작 불가")
    void startStudy_FromRecruiting_Fail() throws Exception {
        // given - 모집중 상태로 변경
        testStudy.updateStatus(Status.RECRUITING);
        studyRepository.save(testStudy);
        studyRepository.flush();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/start", testStudy.getId())
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("시작할 수 있습니다")));
    }

    @Test
    @DisplayName("스터디 시작 API 실패 - 권한 없음")
    void startStudy_NotLeader_Fail() throws Exception {
        // given
        User otherUser = userRepository.save(User.builder()
                .userId("other456")
                .email("other2@test.com")
                .nickname("다른유저2")
                .name("다른사람2")
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

        testStudy.updateStatus(Status.RECRUIT_CLOSED);
        studyRepository.save(testStudy);
        studyRepository.flush();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/start", testStudy.getId())
                        .header("User-Id", otherUser.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("권한")));
    }

    @Test
    @DisplayName("스터디 시작 API 실패 - 존재하지 않는 스터디")
    void startStudy_NotFound_Fail() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/start", 99999L)
                        .header("User-Id", leader.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    // ============================================================
    // 모집 기간 연장 API 테스트 (확정대기 상태 추가)
    // ============================================================

    @Test
    @DisplayName("모집 연장 성공 - 확정대기 상태에서 연장 시 모집중으로 변경")
    void extendRecruitment_FromPending_Success() throws Exception {
        // given - 확정대기 상태로 변경
        testStudy.updateStatus(Status.PENDING);
        studyRepository.save(testStudy);
        studyRepository.flush();

        String requestBody = objectMapper.writeValueAsString(
                new StudyController.RecruitmentExtensionRequest(LocalDate.of(2025, 2, 15)));

        // when & then
        mockMvc.perform(patch("/api/v1/study/{studyId}/extend-recruitment", testStudy.getId())
                        .header("User-Id", leader.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recruitEndDate").value("2025-02-15"))
                .andExpect(jsonPath("$.status").value("RECRUITING"))  // PENDING -> RECRUITING
                .andExpect(jsonPath("$.extensionCount").value(1));
    }
}