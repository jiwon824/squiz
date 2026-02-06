package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyBookmarkRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudyBookmarkController 통합 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 @WithMockUser(username = "testuser", roles = {"USER"})
 class StudyBookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyBookmarkRepository bookmarkRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Study study1;
    private Study study2;
    private Study study3;
    private StudyBookmark bookmark1;
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
                .name("CS")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        // Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 1. User 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
                .name("테스트1")
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

        user2 = userRepository.save(User.builder()
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .name("테스트2")
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

        // 2. Study 생성
        study1 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("알고리즘 스터디")
                .description("백준 문제 풀이")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .difficulty(Difficulty.INTERMEDIATE)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        study2 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("CS 스터디")
                .description("운영체제 학습")
                .topic(topic2)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(5)
                .difficulty(Difficulty.BEGINNER)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 4, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 25))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        study3 = studyRepository.save(Study.builder()
                .leaderId(user2.getId())
                .name("스프링 스터디")
                .description("스프링 부트 학습")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(8)
                .difficulty(Difficulty.INTERMEDIATE)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // 3. Bookmark 생성 - User1이 Study1을 북마크
        bookmark1 = bookmarkRepository.save(StudyBookmark.create(user1.getId(), study1.getId()));
        bookmarkRepository.flush();
    }

    // ============================================================
    // 북마크 토글 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 추가 성공 - 기존 북마크 없음")
    void toggleBookmark_Add_Success() throws Exception {
        // when & then - user1이 study2를 북마크 (기존에 없음)
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", study2.getId())
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(study2.getId()))
                .andExpect(jsonPath("$.isBookmarked").value(true));
    }

    @Test
    @DisplayName("북마크 삭제 성공 - 기존 북마크 있음")
    void toggleBookmark_Remove_Success() throws Exception {
        // when & then - user1이 study1을 토글 (이미 북마크됨 → 삭제)
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", study1.getId())
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(study1.getId()))
                .andExpect(jsonPath("$.isBookmarked").value(false));
    }

    @Test
    @DisplayName("북마크 토글 실패 - 존재하지 않는 스터디")
    void toggleBookmark_StudyNotFound() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", 99999L)
                        .header("user-id", user1.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    // ============================================================
    // 내 북마크 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("내 북마크 목록 조회 성공")
    void getMyBookmarks_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/my/bookmarks")
                        .header("user-id", user1.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].studyId").value(study1.getId()))
                .andExpect(jsonPath("$.content[0].studyName").value("알고리즘 스터디"))
                .andExpect(jsonPath("$.content[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("내 북마크 목록 조회 - 빈 목록")
    void getMyBookmarks_Empty() throws Exception {
        // when & then - user2는 북마크 없음
        mockMvc.perform(get("/api/v1/my/bookmarks")
                        .header("user-id", user2.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("내 북마크 목록 조회 - 여러 개")
    void getMyBookmarks_Multiple() throws Exception {
        // given - user1이 study2도 북마크
        bookmarkRepository.save(StudyBookmark.create(user1.getId(), study2.getId()));
        bookmarkRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/my/bookmarks")
                        .header("user-id", user1.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ============================================================
    // 북마크 여부 확인 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 여부 확인 - 북마크함")
    void isBookmarked_True() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/bookmark/check", study1.getId())
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("북마크 여부 확인 - 북마크 안함")
    void isBookmarked_False() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/bookmark/check", study2.getId())
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    // ============================================================
    // 통계 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 북마크 개수 조회")
    void getBookmarkCount_Success() throws Exception {
        // given - user2도 study1 북마크
        bookmarkRepository.save(StudyBookmark.create(user2.getId(), study1.getId()));
        bookmarkRepository.flush();

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/bookmark/count", study1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    @DisplayName("스터디 북마크 개수 조회 - 0개")
    void getBookmarkCount_Zero() throws Exception {
        // when & then - study3는 북마크 없음
        mockMvc.perform(get("/api/v1/study/{studyId}/bookmark/count", study3.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    @DisplayName("내 북마크 개수 조회")
    void getMyBookmarkCount_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/my/bookmarks/count")
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    @DisplayName("내 북마크 개수 조회 - 0개")
    void getMyBookmarkCount_Zero() throws Exception {
        // when & then - user2는 북마크 없음
        mockMvc.perform(get("/api/v1/my/bookmarks/count")
                        .header("user-id", user2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    // ============================================================
    // 토글 연속 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 토글 연속 - 추가 → 삭제 → 추가")
    void toggleBookmark_Consecutive() throws Exception {
        Long studyId = study2.getId();

        // 1. 추가 (기존에 없음)
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", studyId)
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBookmarked").value(true));

        // 2. 삭제 (방금 추가됨)
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", studyId)
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBookmarked").value(false));

        // 3. 다시 추가
        mockMvc.perform(post("/api/v1/study/{studyId}/bookmark", studyId)
                        .header("user-id", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBookmarked").value(true));
    }
}
