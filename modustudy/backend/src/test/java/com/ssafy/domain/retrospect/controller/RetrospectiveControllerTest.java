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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro2 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(null)
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
        retrospectiveItemRepository.flush();
    }

    @Test
    @DisplayName("회고 목록 조회 API 성공")
    void getRetrospectives_Success() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("회고 목록 조회 API - 세션 정보 포함")
    void getRetrospectives_WithSessionInfo() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].session.sessionNumber").value(1));
    }

    @Test
    @DisplayName("회고 목록 조회 API - 세션 없는 회고")
    void getRetrospectives_WithoutSession() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                // 자유 회고가 존재하고 session이 null인지 확인
                .andExpect(jsonPath("$.content[?(@.title == '자유 회고')]").exists())
                .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].session", hasItem(nullValue())));
    }

    @Test
    @DisplayName("회고 목록 조회 API - itemCount, participantCount, hasMyItem 포함")
    void getRetrospectives_WithCounts() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", studyId)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].itemCount").value(2))
                .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].participantCount").value(1))
                .andExpect(jsonPath("$.content[?(@.title == '1회차 회고')].hasMyItem").value(true))
                .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].itemCount").value(0))
                .andExpect(jsonPath("$.content[?(@.title == '자유 회고')].hasMyItem").value(false));
    }

    @Test
    @DisplayName("회고 목록 조회 API - 페이징")
    void getRetrospectives_WithPaging() throws Exception {
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
    @DisplayName("회고 목록 조회 API - 존재하지 않는 스터디")
    void getRetrospectives_StudyNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/retrospectives", 999L)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

}