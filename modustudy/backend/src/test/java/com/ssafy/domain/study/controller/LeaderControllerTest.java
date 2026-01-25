package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.entity.LeaderReview;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.repository.LeaderReviewRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * LeaderController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class LeaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaderReviewRepository leaderReviewRepository;

    private User leader;
    private User reviewer1;
    private User reviewer2;
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
                .currentLevel(5)
                .levelName("Gold")
                .leaderRating(4.5f)
                .leaderReviewCount(2)
                .build();
        leader = userRepository.save(leader);

        // 평가자 1
        reviewer1 = User.builder()
                .userId("reviewer1")
                .email("reviewer1@test.com")
                .nickname("평가자1")
                .name("이평가")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        reviewer1 = userRepository.save(reviewer1);

        // 평가자 2
        reviewer2 = User.builder()
                .userId("reviewer2")
                .email("reviewer2@test.com")
                .nickname("평가자2")
                .name("박평가")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        reviewer2 = userRepository.save(reviewer2);

        // 테스트 스터디 생성
        testStudy = Study.builder()
                .leaderId(leader.getId())
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이")
                .topic("알고리즘")
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

        // 리뷰 데이터 생성
        LeaderReview review1 = LeaderReview.builder()
                .studyId(testStudy.getId())
                .reviewerId(reviewer1.getId())
                .leaderId(leader.getId())
                .rating(new BigDecimal("5.0"))
                .comment("정말 좋은 스터디장이었습니다!")
                .build();
        leaderReviewRepository.save(review1);

        LeaderReview review2 = LeaderReview.builder()
                .studyId(testStudy.getId())
                .reviewerId(reviewer2.getId())
                .leaderId(leader.getId())
                .rating(new BigDecimal("4.0"))
                .comment("체계적인 진행이 좋았습니다.")
                .build();
        leaderReviewRepository.save(review2);
    }

    // ============================================================
    // 스터디장 정보 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디장 정보 조회 성공")
    void getLeaderInfo_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(leader.getId()))
                .andExpect(jsonPath("$.name").value("김리더"))
                .andExpect(jsonPath("$.nickname").value("리더"))
                .andExpect(jsonPath("$.email").value("leader@test.com"))
                .andExpect(jsonPath("$.leaderRating").value(4.5))
                .andExpect(jsonPath("$.leaderReviewCount").value(2))
                .andExpect(jsonPath("$.currentLevel").value(5))
                .andExpect(jsonPath("$.levelName").value("Gold"));
    }

    @Test
    @DisplayName("존재하지 않는 스터디 - 404 Not Found")
    void getLeaderInfo_StudyNotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    // ============================================================
    // 리뷰 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 기본 페이징")
    void getLeaderReviews_Success_DefaultPaging() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].reviewerId").exists())
                .andExpect(jsonPath("$.content[0].reviewerName").exists())
                .andExpect(jsonPath("$.content[0].reviewerNickname").exists())
                .andExpect(jsonPath("$.content[0].studyName").value("알고리즘 스터디"))
                .andExpect(jsonPath("$.content[0].rating").exists())
                .andExpect(jsonPath("$.content[0].comment").exists());
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 커스텀 페이징")
    void getLeaderReviews_Success_CustomPaging() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("리뷰 목록 조회 - 리뷰 없는 스터디")
    void getLeaderReviews_NoReviews() throws Exception {
        // given - 새로운 스터디장 생성
        User newLeader = User.builder()
                .userId("newleader")
                .email("newleader@test.com")
                .nickname("새리더")
                .name("최리더")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .leaderRating(0.0f)
                .leaderReviewCount(0)
                .build();
        newLeader = userRepository.save(newLeader);

        // 리뷰 없는 새 스터디 생성
        Study newStudy = Study.builder()
                .leaderId(newLeader.getId())  // 새로운 리더!
                .name("새로운 스터디")
                .topic("백엔드")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 6, 1))
                .build();
        newStudy = studyRepository.save(newStudy);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", newStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("리뷰 목록 조회 - 존재하지 않는 스터디")
    void getLeaderReviews_StudyNotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    @Test
    @DisplayName("리뷰 목록 조회 - 정렬 확인 (최신순)")
    void getLeaderReviews_SortByCreatedAtDesc() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId())
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("리뷰 목록 조회 - 평점순 정렬")
    void getLeaderReviews_SortByRatingDesc() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId())
                        .param("sort", "rating,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rating").value(5.0));
    }

    // ============================================================
    // 추가 엣지 케이스 테스트
    // ============================================================

    @Test
    @DisplayName("페이징 파라미터 검증 - 음수 페이지")
    void getLeaderReviews_NegativePage() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId())
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("페이징 파라미터 검증 - 매우 큰 페이지")
    void getLeaderReviews_LargePage() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/leader/reviews", testStudy.getId())
                        .param("page", "100")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}