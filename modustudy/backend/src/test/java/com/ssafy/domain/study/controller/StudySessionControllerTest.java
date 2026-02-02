package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.StudySessionCreateRequest;
import com.ssafy.domain.study.dto.request.StudySessionUpdateRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudySessionController 통합 테스트
 *
 * 실제 엔드포인트:
 * - Base: /api/v1/studies/{studyId}/sessions
 * - 상태 변경: POST (PATCH가 아님)
 * - 목록 조회: List 반환 (Page가 아님)
 * - 통계: /statistics (stats가 아님)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class StudySessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudySessionRepository sessionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    private User leader;
    private User member;
    private Study testStudy;
    private StudySession testSession;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. 스터디장 생성
        leader = userRepository.save(User.builder()
                .userId("leader123")
                .email("leader@test.com")
                .nickname("스터디장")
                .name("김리더")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(5)
                .levelName("Gold")
                .build());
        userRepository.flush();

        // 4. 일반 멤버 생성
        member = userRepository.save(User.builder()
                .userId("member123")
                .email("member@test.com")
                .nickname("멤버")
                .name("이멤버")
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

        // 5. 테스트용 스터디 생성
        testStudy = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.IN_PROGRESS)
                .maxMembers(10)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // 6. 테스트용 세션 생성 (미래 날짜로 설정)
        testSession = sessionRepository.save(StudySession.builder()
                .studyId(testStudy.getId())
                .sessionNumber(1)
                .title("1회차: 배열과 문자열")
                .description("배열과 문자열 기초 문제 풀이")
                .scheduledAt(LocalDateTime.now().plusDays(7))  // 미래 날짜
                .durationMinutes(120)
                .location("온라인 (Zoom)")
                .isOnline(true)
                .status(SessionStatus.SCHEDULED)
                .build());
        sessionRepository.flush();
    }

    // ============================================================
    // 세션 생성 테스트
    // ============================================================

    @Test
    @DisplayName("세션 생성 성공")
    void createSession_Success() throws Exception {
        // given - 컨트롤러가 List를 받도록 변경되어 배열로 전송
        StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                .title("2회차: 스택과 큐")
                .description("스택과 큐 자료구조 학습")
                .scheduledAt(LocalDateTime.of(2025, 2, 12, 19, 0))
                .durationMinutes(120)
                .isOnline(true)
                .build();

        // when & then - 배열로 전송하고 첫 번째 요소 검증
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions", testStudy.getId())
                        .header("User-Id", leader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.List.of(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].studyId").value(testStudy.getId()))
                .andExpect(jsonPath("$[0].sessionNumber").value(2))
                .andExpect(jsonPath("$[0].title").value("2회차: 스택과 큐"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("세션 생성 실패 - 권한 없음")
    void createSession_NotLeader() throws Exception {
        // given - 컨트롤러가 List를 받도록 변경되어 배열로 전송
        StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                .title("2회차: 스택과 큐")
                .scheduledAt(LocalDateTime.of(2025, 2, 12, 19, 0))
                .durationMinutes(120)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions", testStudy.getId())
                        .header("User-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.List.of(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("스터디장만")));
    }

    @Test
    @DisplayName("세션 생성 실패 - 존재하지 않는 스터디")
    void createSession_StudyNotFound() throws Exception {
        // given - 컨트롤러가 List를 받도록 변경되어 배열로 전송
        StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                .title("새 세션")
                .scheduledAt(LocalDateTime.of(2025, 2, 12, 19, 0))
                .durationMinutes(120)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions", 99999L)
                        .header("User-Id", leader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.List.of(request))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    // ============================================================
    // 세션 목록 조회 테스트 (List 반환)
    // ============================================================

    @Test
    @DisplayName("스터디별 세션 목록 조회 성공")
    void getSessionsByStudy_Success() throws Exception {
        // when & then - List 반환 (Page가 아님)
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sessionNumber").value(1))
                .andExpect(jsonPath("$[0].title").value("1회차: 배열과 문자열"));
    }

    @Test
    @DisplayName("세션 목록 조회 - 상태 필터링")
    void getSessionsByStudy_WithStatusFilter() throws Exception {
        // given - 다른 상태의 세션 추가
        sessionRepository.save(StudySession.builder()
                .studyId(testStudy.getId())
                .sessionNumber(0)
                .title("0회차: OT")
                .scheduledAt(LocalDateTime.of(2025, 2, 1, 19, 0))
                .durationMinutes(60)
                .isOnline(true)
                .status(SessionStatus.COMPLETED)
                .build());
        sessionRepository.flush();

        // when & then - SCHEDULED만 조회
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions", testStudy.getId())
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("다음 예정 세션 조회")
    void getNextSession_Success() throws Exception {
        // when & then - /next 엔드포인트 사용
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/next", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionNumber").value(1));
    }

    // ============================================================
    // 세션 상세 조회 테스트
    // ============================================================

    @Test
    @DisplayName("세션 상세 조회 성공")
    void getSession_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSession.getId()))
                .andExpect(jsonPath("$.title").value("1회차: 배열과 문자열"))
                .andExpect(jsonPath("$.sessionNumber").value(1))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("세션 상세 조회 실패 - 존재하지 않는 세션")
    void getSession_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("세션을 찾을 수 없습니다")));
    }

    // ============================================================
    // 세션 수정 테스트
    // ============================================================

    @Test
    @DisplayName("세션 수정 성공")
    void updateSession_Success() throws Exception {
        // given
        StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                .title("수정된 제목: 배열과 문자열 심화")
                .description("심화 문제 풀이")
                .scheduledAt(LocalDateTime.of(2025, 2, 6, 20, 0))
                .durationMinutes(150)
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목: 배열과 문자열 심화"))
                .andExpect(jsonPath("$.durationMinutes").value(150));
    }

    @Test
    @DisplayName("세션 수정 실패 - 권한 없음")
    void updateSession_NotLeader() throws Exception {
        // given
        StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                .title("수정 시도")
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("스터디장만")));
    }

    // ============================================================
    // 세션 상태 변경 테스트 (POST 방식)
    // ============================================================

    @Test
    @DisplayName("세션 시작 성공")
    void startSession_Success() throws Exception {
        // when & then - POST 사용 (PATCH가 아님)
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions/{sessionId}/start",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("세션 완료 성공")
    void completeSession_Success() throws Exception {
        // given - 세션 시작 상태로 변경
        testSession.start();
        sessionRepository.save(testSession);
        sessionRepository.flush();

        // when & then - POST 사용
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions/{sessionId}/complete",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("세션 취소 성공")
    void cancelSession_Success() throws Exception {
        // when & then - POST 사용
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions/{sessionId}/cancel",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("세션 상태 변경 실패 - 잘못된 상태 전환")
    void invalidStatusTransition() throws Exception {
        // given - 완료 상태로 변경
        testSession.start();
        testSession.complete();
        sessionRepository.save(testSession);
        sessionRepository.flush();

        // when & then - 완료된 세션을 시작하려고 시도
        mockMvc.perform(post("/api/v1/studies/{studyId}/sessions/{sessionId}/start",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ============================================================
    // 세션 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("세션 삭제 성공")
    void deleteSession_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", leader.getId()))
                .andExpect(status().isNoContent());

        // 삭제 확인
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("세션 삭제 실패 - 권한 없음")
    void deleteSession_NotLeader() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/studies/{studyId}/sessions/{sessionId}",
                        testStudy.getId(), testSession.getId())
                        .header("User-Id", member.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("스터디장만")));
    }

    // ============================================================
    // 세션 번호로 조회 테스트
    // ============================================================

    @Test
    @DisplayName("세션 번호로 조회 성공")
    void getSessionByNumber_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/number/{sessionNumber}",
                        testStudy.getId(), 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionNumber").value(1))
                .andExpect(jsonPath("$.title").value("1회차: 배열과 문자열"));
    }

    @Test
    @DisplayName("세션 번호로 조회 실패 - 존재하지 않는 번호")
    void getSessionByNumber_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/number/{sessionNumber}",
                        testStudy.getId(), 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("세션을 찾을 수 없습니다")));
    }

    // ============================================================
    // 세션 통계 테스트 (/statistics 엔드포인트)
    // ============================================================

    @Test
    @DisplayName("세션 통계 조회 성공")
    void getSessionStatistics_Success() throws Exception {
        // when & then - /statistics 사용 (stats가 아님)
        mockMvc.perform(get("/api/v1/studies/{studyId}/sessions/statistics", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.scheduledCount").value(1))
                .andExpect(jsonPath("$.completedCount").value(0));
    }
}