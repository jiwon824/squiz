package com.ssafy.domain.retrospect.controller;

import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.retrospect.repository.RetrospectiveItemRepository;
import com.ssafy.domain.retrospect.repository.RetrospectiveRepository;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RetrospectiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private RetrospectiveItemRepository retrospectiveItemRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private UserRepository userRepository;

    private Study study;
    private Long studyId;
    private User user;
    private User otherUser;
    private Long userId;
    private StudySession session;
    private Retrospective retro1;
    private Retrospective retro2;

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
        userRepository.flush();
        userId = user.getId();

        // 다른 유저
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

        // 2. Topic 생성
        Topic topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 3. Format 생성
        Format format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 4. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(userId)
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.IN_PROGRESS)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .extensionCount(0)
                .build());
        studyRepository.flush();
        studyId = study.getId();

        // 5. StudySession 생성
        session = studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(1)
                .title("1회차 세션")
                .scheduledAt(LocalDateTime.of(2025, 1, 10, 19, 0))
                .status(SessionStatus.COMPLETED)
                .build());
        studySessionRepository.flush();

        // 6. Retrospective 생성
        retro1 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(session.getId())
                .createdBy(userId)
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro2 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(null)
                .createdBy(otherUser.getId())
                .title("자유 회고")
                .retrospectiveType(RetrospectiveType.FREE)
                .build());
        retrospectiveRepository.flush();

        // 7. RetrospectiveItem 생성
        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(userId)
                .category(Category.KEEP)
                .content("좋았던 점")
                .build());

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(userId)
                .category(Category.PROBLEM)
                .content("아쉬웠던 점")
                .build());

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(otherUser.getId())
                .category(Category.TRY)
                .content("시도해볼 점")
                .build());
        retrospectiveItemRepository.flush();
    }

    // ============================================================
    // 회고 목록 조회 API 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 목록 조회 API")
    class GetRetrospectives {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("세션 정보 포함")
        void withSessionInfo() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].session.sessionNumber").value(1));
        }

        @Test
        @DisplayName("세션 없는 회고")
        void withoutSession() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.title == '자유 회고')]").exists())
                    .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].session", hasItem(nullValue())));
        }

        @Test
        @DisplayName("itemCount, participantCount, hasMyItem 포함")
        void withCounts() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].itemCount").value(3))
                    .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].participantCount").value(2))
                    .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].hasMyItem").value(true))
                    .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].itemCount").value(0))
                    .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].hasMyItem").value(false));
        }

        @Test
        @DisplayName("페이징")
        void withPaging() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .param("page", "0")
                            .param("size", "1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("존재하지 않는 스터디")
        void studyNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", 999L)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
        }
    }

    // ============================================================
    // 회고 상세 조회 API 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 상세 조회 API")
    class GetRetrospectiveDetail {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(retro1.getId()))
                    .andExpect(jsonPath("$.title").value("1회차 회고"))
                    .andExpect(jsonPath("$.retrospectiveType").value("KPT"));
        }

        @Test
        @DisplayName("세션 정보 포함")
        void withSessionInfo() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.session").isNotEmpty())
                    .andExpect(jsonPath("$.session.id").value(session.getId()))
                    .andExpect(jsonPath("$.session.sessionNumber").value(1))
                    .andExpect(jsonPath("$.session.title").value("1회차 세션"));
        }

        @Test
        @DisplayName("세션 없는 회고")
        void withoutSession() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro2.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.session").doesNotExist());
        }

        @Test
        @DisplayName("카테고리별 항목 그룹핑")
        void itemsGroupedByCategory() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.KEEP").isArray())
                    .andExpect(jsonPath("$.items.KEEP", hasSize(1)))
                    .andExpect(jsonPath("$.items.PROBLEM").isArray())
                    .andExpect(jsonPath("$.items.PROBLEM", hasSize(1)))
                    .andExpect(jsonPath("$.items.TRY").isArray())
                    .andExpect(jsonPath("$.items.TRY", hasSize(1)))
                    .andExpect(jsonPath("$.items.KEEP[0].content").value("좋았던 점"))
                    .andExpect(jsonPath("$.items.PROBLEM[0].content").value("아쉬웠던 점"))
                    .andExpect(jsonPath("$.items.TRY[0].content").value("시도해볼 점"));
        }

        @Test
        @DisplayName("항목에 사용자 정보 포함")
        void itemsWithUserInfo() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.KEEP[0].user").isNotEmpty())
                    .andExpect(jsonPath("$.items.KEEP[0].user.id").value(userId))
                    .andExpect(jsonPath("$.items.KEEP[0].user.nickname").value("테스트유저"));
        }

        @Test
        @DisplayName("항목이 없는 회고")
        void emptyItems() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro2.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.KEEP").isArray())
                    .andExpect(jsonPath("$.items.KEEP", hasSize(0)))
                    .andExpect(jsonPath("$.items.PROBLEM").isArray())
                    .andExpect(jsonPath("$.items.PROBLEM", hasSize(0)))
                    .andExpect(jsonPath("$.items.TRY").isArray())
                    .andExpect(jsonPath("$.items.TRY", hasSize(0)));
        }

        @Test
        @DisplayName("존재하지 않는 스터디")
        void studyNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", 999L, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않는 회고")
        void retrospectiveNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, 999L)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RETROSPECTIVE_NOT_FOUND"));
        }

        @Test
        @DisplayName("다른 스터디의 회고 조회 시 실패")
        void wrongStudyId() throws Exception {
            // given - 다른 스터디 생성
            Topic topic2 = topicRepository.save(Topic.builder()
                    .name("CS")
                    .sortOrder(2)
                    .build());
            topicRepository.flush();

            Study otherStudy = studyRepository.save(Study.builder()
                    .leaderId(userId)
                    .name("다른 스터디")
                    .topic(topic2)
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .status(Status.IN_PROGRESS)
                    .maxMembers(10)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 6, 30))
                    .extensionCount(0)
                    .build());
            studyRepository.flush();

            // when & then
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", otherStudy.getId(), retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RETROSPECTIVE_NOT_FOUND"));
        }
    }

    // ============================================================
    // 회고 생성 API 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 생성 API")
    class CreateRetrospective {

        @Test
        @DisplayName("성공 - 기본 생성")
        void success() throws Exception {
            String requestBody = """
                    {
                        "title": "새 회고",
                        "retrospectiveType": "KPT"
                    }
                    """;

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("새 회고"))
                    .andExpect(jsonPath("$.retrospectiveType").value("KPT"));
        }

        @Test
        @DisplayName("성공 - 세션과 함께 생성")
        void successWithSession() throws Exception {
            String requestBody = String.format("""
                    {
                        "title": "세션 회고",
                        "retrospectiveType": "KPT",
                        "sessionId": %d
                    }
                    """, session.getId());

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.session").isNotEmpty())
                    .andExpect(jsonPath("$.session.id").value(session.getId()));
        }

        @Test
        @DisplayName("성공 - FREE 타입")
        void successWithFreeType() throws Exception {
            String requestBody = """
                    {
                        "title": "자유 형식 회고",
                        "retrospectiveType": "FREE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.retrospectiveType").value("FREE"));
        }

        @Test
        @DisplayName("실패 - 제목 누락")
        void failWithoutTitle() throws Exception {
            String requestBody = """
                    {
                        "retrospectiveType": "KPT"
                    }
                    """;

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void studyNotFound() throws Exception {
            String requestBody = """
                    {
                        "title": "새 회고",
                        "retrospectiveType": "KPT"
                    }
                    """;

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", 999L)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션")
        void sessionNotFound() throws Exception {
            String requestBody = """
                    {
                        "title": "새 회고",
                        "sessionId": 999
                    }
                    """;

            mockMvc.perform(post("/api/v1/studies/{studyId}/retrospectives", studyId)
                            .header("User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_RETROSPECTIVE_REQUEST"));
        }
    }

    // ============================================================
    // 회고 삭제 API 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 삭제 API")
    class DeleteRetrospective {

        @Test
        @DisplayName("성공 - 생성자가 삭제")
        void successByCreator() throws Exception {
            mockMvc.perform(delete("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("성공 - 스터디장이 다른 사람의 회고 삭제")
        void successByLeader() throws Exception {
            // retro2는 otherUser가 생성, study의 리더는 user
            mockMvc.perform(delete("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro2.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - 권한 없음 (생성자도 아니고 스터디장도 아님)")
        void noPermission() throws Exception {
            // retro1은 user가 생성, user가 스터디장
            // otherUser는 생성자도 아니고 스터디장도 아님
            mockMvc.perform(delete("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, retro1.getId())
                            .header("User-Id", otherUser.getId()))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_RETROSPECTIVE_OWNER"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void studyNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/studies/{studyId}/retrospectives/{retroId}", 999L, retro1.getId())
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회고")
        void retrospectiveNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/studies/{studyId}/retrospectives/{retroId}", studyId, 999L)
                            .header("User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RETROSPECTIVE_NOT_FOUND"));
        }
    }
}
