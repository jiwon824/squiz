package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.StudySessionCreateRequest;
import com.ssafy.domain.study.dto.request.StudySessionUpdateRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudySessionController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudySessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User leader;
    private User otherUser;
    private Study study;

    private static final String BASE_URL = "/api/v1/studies/{studyId}/sessions";

    @BeforeEach
    void setUp() {
        // 1. 스터디장 생성
        leader = userRepository.save(User.builder()
                .userId("leader")
                .email("leader@test.com")
                .nickname("스터디장")
                .name("리더")
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

        // 2. 다른 사용자 생성
        otherUser = userRepository.save(User.builder()
                .userId("other")
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

        // 3. 스터디 생성
        study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .name("테스트 스터디")
                .topic("Java")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();
    }

    @Nested
    @DisplayName("POST /api/v1/studies/{studyId}/sessions - 세션 생성")
    class CreateSession {

        @Test
        @DisplayName("성공 - 201 Created")
        void createSession_Success() throws Exception {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차: OT")
                    .description("오리엔테이션")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .durationMinutes(90)
                    .location("Zoom")
                    .isOnline(true)
                    .build();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL, study.getId())
                    .header("User-Id", leader.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.studyId").value(study.getId()))
                    .andExpect(jsonPath("$.sessionNumber").value(1))
                    .andExpect(jsonPath("$.title").value("1회차: OT"))
                    .andExpect(jsonPath("$.durationMinutes").value(90))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("실패 - 스터디장이 아닌 사용자 - 403 Forbidden")
        void createSession_NotLeader_Forbidden() throws Exception {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL, study.getId())
                    .header("User-Id", otherUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락 - 400 Bad Request")
        void createSession_MissingRequired_BadRequest() throws Exception {
            // given - scheduledAt 누락
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차")
                    .build();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL, study.getId())
                    .header("User-Id", leader.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디 - 404 Not Found")
        void createSession_StudyNotFound_NotFound() throws Exception {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL, 999L)
                    .header("User-Id", leader.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/studies/{studyId}/sessions/{sessionId} - 세션 단건 조회")
    class GetSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void getSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차: OT");
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/{sessionId}",
                    study.getId(), session.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(session.getId()))
                    .andExpect(jsonPath("$.title").value("1회차: OT"))
                    .andExpect(jsonPath("$.sessionNumber").value(1));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션 - 404 Not Found")
        void getSession_NotFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/{sessionId}",
                    study.getId(), 999L));

            // then
            result.andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/studies/{studyId}/sessions/number/{sessionNumber} - 회차로 조회")
    class GetSessionByNumber {

        @Test
        @DisplayName("성공 - 200 OK")
        void getSessionByNumber_Success() throws Exception {
            // given
            createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/number/{sessionNumber}",
                    study.getId(), 2));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionNumber").value(2))
                    .andExpect(jsonPath("$.title").value("2회차"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/studies/{studyId}/sessions - 세션 목록 조회")
    class GetSessions {

        @Test
        @DisplayName("성공 - 전체 목록 (회차 순)")
        void getSessions_All_Success() throws Exception {
            // given
            createTestSession(study.getId(), 3, "3회차");
            createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL, study.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].sessionNumber").value(1))
                    .andExpect(jsonPath("$[1].sessionNumber").value(2))
                    .andExpect(jsonPath("$[2].sessionNumber").value(3));
        }

        @Test
        @DisplayName("성공 - 상태별 필터링")
        void getSessions_ByStatus_Success() throws Exception {
            // given
            StudySession session1 = createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            createTestSession(study.getId(), 3, "3회차");
            studySessionRepository.flush();

            // session1 완료 처리
            session1.start();
            session1.complete();
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL, study.getId())
                    .param("status", "SCHEDULED"));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/studies/{studyId}/sessions/next - 다음 예정 세션")
    class GetNextSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void getNextSession_Success() throws Exception {
            // given
            LocalDateTime now = LocalDateTime.now();
            createTestSessionWithScheduledAt(study.getId(), 1, "1회차", now.plusDays(7));
            createTestSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(14));
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/next", study.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionNumber").value(1));
        }

        @Test
        @DisplayName("실패 - 예정된 세션 없음 - 404 Not Found")
        void getNextSession_NotFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/next", study.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/studies/{studyId}/sessions/{sessionId} - 세션 수정")
    class UpdateSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void updateSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                    .title("1회차: OT (수정됨)")
                    .description("수정된 설명")
                    .durationMinutes(120)
                    .build();

            // when
            ResultActions result = mockMvc.perform(put(BASE_URL + "/{sessionId}",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("1회차: OT (수정됨)"))
                    .andExpect(jsonPath("$.description").value("수정된 설명"))
                    .andExpect(jsonPath("$.durationMinutes").value(120));
        }

        @Test
        @DisplayName("실패 - 진행 중인 세션 수정 - 400 Bad Request")
        void updateSession_InProgress_BadRequest() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            studySessionRepository.flush();

            StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when
            ResultActions result = mockMvc.perform(put(BASE_URL + "/{sessionId}",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/studies/{studyId}/sessions/{sessionId} - 세션 삭제")
    class DeleteSession {

        @Test
        @DisplayName("성공 - 204 No Content")
        void deleteSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();
            Long sessionId = session.getId();

            // when
            ResultActions result = mockMvc.perform(delete(BASE_URL + "/{sessionId}",
                    study.getId(), sessionId)
                    .header("User-Id", leader.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();
            assertThat(studySessionRepository.findById(sessionId)).isEmpty();
        }

        @Test
        @DisplayName("실패 - 완료된 세션 삭제 - 400 Bad Request")
        void deleteSession_Completed_BadRequest() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            session.complete();
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(delete(BASE_URL + "/{sessionId}",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/studies/{studyId}/sessions/{sessionId}/start - 세션 시작")
    class StartSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void startSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL + "/{sessionId}/start",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/studies/{studyId}/sessions/{sessionId}/complete - 세션 완료")
    class CompleteSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void completeSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL + "/{sessionId}/complete",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.completedAt").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/studies/{studyId}/sessions/{sessionId}/cancel - 세션 취소")
    class CancelSession {

        @Test
        @DisplayName("성공 - 200 OK")
        void cancelSession_Success() throws Exception {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(post(BASE_URL + "/{sessionId}/cancel",
                    study.getId(), session.getId())
                    .header("User-Id", leader.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/studies/{studyId}/sessions/statistics - 세션 통계")
    class GetSessionStatistics {

        @Test
        @DisplayName("성공 - 200 OK")
        void getSessionStatistics_Success() throws Exception {
            // given
            StudySession session1 = createTestSession(study.getId(), 1, "1회차");
            StudySession session2 = createTestSession(study.getId(), 2, "2회차");
            createTestSession(study.getId(), 3, "3회차");
            StudySession session4 = createTestSession(study.getId(), 4, "4회차");
            studySessionRepository.flush();

            // session1, session2 완료
            session1.start();
            session1.complete();
            session2.start();
            session2.complete();
            // session4 취소
            session4.cancel();
            studySessionRepository.flush();

            // when
            ResultActions result = mockMvc.perform(get(BASE_URL + "/statistics", study.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(4))
                    .andExpect(jsonPath("$.completedCount").value(2))
                    .andExpect(jsonPath("$.scheduledCount").value(1))
                    .andExpect(jsonPath("$.cancelledCount").value(1))
                    .andExpect(jsonPath("$.completionRate").value(50.0));
        }
    }

    // ==================== Helper Methods ====================

    private StudySession createTestSession(Long studyId, int sessionNumber, String title) {
        return studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(sessionNumber)
                .title(title)
                .description(title + " 설명")
                .scheduledAt(LocalDateTime.now().plusDays(sessionNumber * 7L))
                .durationMinutes(60)
                .isOnline(true)
                .build());
    }

    private StudySession createTestSessionWithScheduledAt(Long studyId, int sessionNumber,
                                                          String title, LocalDateTime scheduledAt) {
        return studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(sessionNumber)
                .title(title)
                .description(title + " 설명")
                .scheduledAt(scheduledAt)
                .durationMinutes(60)
                .isOnline(true)
                .build());
    }

}