package com.ssafy.domain.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.util.JwtTokenUtil;
import com.ssafy.domain.quiz.dto.request.ReviewSubmitRequest;
import com.ssafy.domain.quiz.dto.response.ReviewCourseStatsResponse;
import com.ssafy.domain.quiz.dto.response.ReviewResult;
import com.ssafy.domain.quiz.dto.response.ReviewSubmitResponse;
import com.ssafy.domain.quiz.dto.response.ReviewStatsResponse;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.service.FsrsService;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@MockBean(JpaMetamodelMappingContext.class)
class ReviewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FsrsService fsrsService;

        @MockBean
        private UserService userService;

        @MockBean
        private JwtTokenUtil jwtTokenUtil;

        @Autowired
        private ObjectMapper objectMapper;

        private static final Long TEST_USER_ID = 1L;

        private SsafyUserDetails userDetails;

        @BeforeEach
        void setup() {
                User user = User.builder().email("test@example.com").nickname("tester").role(Role.USER).isActive(true)
                                .build();
                ReflectionTestUtils.setField(user, "id", TEST_USER_ID);
                userDetails = new SsafyUserDetails(user);
        }

        @Test
        @DisplayName("복습 결과 제출 API 성공 테스트")
        void submitReview_ShouldReturnSuccess() throws Exception {
                // given
                ReviewSubmitRequest request = new ReviewSubmitRequest(
                                ReviewContentType.COURSE_QUESTION,
                                100L,
                                "0", // 사용자 답안 (서버에서 채점)
                                1500L);

                UserReviewItem mockItem = UserReviewItem.builder()
                                .id(1L)
                                .userId(TEST_USER_ID)
                                .contentType(ReviewContentType.COURSE_QUESTION)
                                .contentId(100L)
                                .state(2) // Review
                                .nextReviewAt(LocalDateTime.now().plusDays(1))
                                .build();
                ReviewResult mockResult = new ReviewResult(mockItem, true, "A");

                given(fsrsService.processReview(anyLong(), any(), anyLong(), any(String.class), anyLong()))
                                .willReturn(mockResult);

                // when & then
                mockMvc.perform(post("/api/v1/reviews")
                                .with(user(userDetails))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.reviewItemId").value(1L))
                                .andExpect(jsonPath("$.data.state").value(2));
        }

        @Test
        @DisplayName("오늘 복습 예정 항목 조회 API 성공 테스트")
        void getTodayReviews_ShouldReturnList() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                1L, ReviewContentType.COURSE_QUESTION, 100L, 2.5, 5.0, 2, 3, 0, LocalDateTime.now(),
                                null);

                given(fsrsService.getTodayReviewsWithQuestions(TEST_USER_ID))
                                .willReturn(List.of(dto));

                // when & then
                mockMvc.perform(get("/api/v1/reviews/today")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(1L))
                                .andExpect(jsonPath("$.data.totalCount").value(1));
        }

        @Test
        @DisplayName("오답 노트 조회 API 성공 테스트 - 많이 틀린 순")
        void getWrongAnswers_ShouldReturnListSortedByMostWrong() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                2L, ReviewContentType.COURSE_QUESTION, 101L, 0.5, 8.0, 3, 5, 2, LocalDateTime.now(),
                                null);
                Page<TodayReviewResponse.ReviewItemDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

                given(fsrsService.getWrongAnswersWithQuestions(eq(TEST_USER_ID),
                                eq(com.ssafy.domain.quiz.entity.WrongAnswerSortType.MOST_WRONG),
                                any(Pageable.class)))
                                .willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .param("sortType", "MOST_WRONG")
                                .param("page", "0")
                                .param("size", "5")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(2L))
                                .andExpect(jsonPath("$.data.totalCount").value(1))
                                .andExpect(jsonPath("$.data.totalPages").value(1))
                                .andExpect(jsonPath("$.data.number").value(0))
                                .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("오답 노트 조회 API 성공 테스트 - FSRS 복습 우선순위")
        void getWrongAnswers_ShouldReturnListSortedByFsrsRecommended() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                3L, ReviewContentType.COURSE_QUESTION, 102L, 0.3, 7.0, 2, 4, 1, LocalDateTime.now(),
                                null);
                Page<TodayReviewResponse.ReviewItemDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

                given(fsrsService.getWrongAnswersWithQuestions(eq(TEST_USER_ID),
                                eq(com.ssafy.domain.quiz.entity.WrongAnswerSortType.FSRS_RECOMMENDED),
                                any(Pageable.class)))
                                .willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .param("sortType", "FSRS_RECOMMENDED")
                                .param("page", "0")
                                .param("size", "5")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(3L))
                                .andExpect(jsonPath("$.data.totalCount").value(1));
        }

        @Test
        @DisplayName("오답 노트 조회 API 성공 테스트 - 최신순")
        void getWrongAnswers_ShouldReturnListSortedByLatest() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                4L, ReviewContentType.COURSE_QUESTION, 103L, 0.6, 6.0, 2, 3, 1, LocalDateTime.now(),
                                null);
                Page<TodayReviewResponse.ReviewItemDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

                given(fsrsService.getWrongAnswersWithQuestions(eq(TEST_USER_ID),
                                eq(com.ssafy.domain.quiz.entity.WrongAnswerSortType.LATEST),
                                any(Pageable.class)))
                                .willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .param("sortType", "LATEST")
                                .param("page", "0")
                                .param("size", "5")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(4L))
                                .andExpect(jsonPath("$.data.totalCount").value(1));
        }

        @Test
        @DisplayName("오답 노트 조회 API - sortType 없으면 기본값(MOST_WRONG) 사용")
        void getWrongAnswers_ShouldUseDefaultSortType() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                2L, ReviewContentType.COURSE_QUESTION, 101L, 0.5, 8.0, 3, 5, 2, LocalDateTime.now(),
                                null);
                Page<TodayReviewResponse.ReviewItemDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

                given(fsrsService.getWrongAnswersWithQuestions(eq(TEST_USER_ID),
                                eq(com.ssafy.domain.quiz.entity.WrongAnswerSortType.MOST_WRONG),
                                any(Pageable.class)))
                                .willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(2L))
                                .andExpect(jsonPath("$.data.totalCount").value(1));
        }

        // ========== 코스별 정답 통계 조회 테스트 ==========

        @Test
        @DisplayName("코스별 정답 통계 조회 API - 여러 코스에서 문제를 맞힌 경우 + sortOrder 정렬 검증")
        void getCourseStats_ShouldReturnCourseStatsWithMultipleCoursesAndVerifySortOrder() throws Exception {
                // given: sortOrder 순서대로 정렬된 Mock 데이터 (Java=1, Python=2, Algorithm=3)
                ReviewCourseStatsResponse.CourseStatDto javaCourse = ReviewCourseStatsResponse.CourseStatDto.from(
                                1L, "Java 기초", 50L, 30L);
                ReviewCourseStatsResponse.CourseStatDto pythonCourse = ReviewCourseStatsResponse.CourseStatDto.from(
                                2L, "Python 입문", 40L, 20L);
                ReviewCourseStatsResponse.CourseStatDto algorithmCourse = ReviewCourseStatsResponse.CourseStatDto.from(
                                3L, "알고리즘", 60L, 15L);

                // totalSolvedCount = 30 + 20 + 15 = 65 (중복 제거된 값)
                ReviewCourseStatsResponse mockResponse = ReviewCourseStatsResponse.from(
                                65L, List.of(javaCourse, pythonCourse, algorithmCourse));

                given(fsrsService.getCourseStats(TEST_USER_ID))
                                .willReturn(mockResponse);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/courses/stats")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                // 전체 맞춘 문제 수 검증
                                .andExpect(jsonPath("$.data.totalSolvedCount").value(65))
                                // courseStats 리스트 구조 검증
                                .andExpect(jsonPath("$.data.courseStats").isArray())
                                .andExpect(jsonPath("$.data.courseStats.length()").value(3))
                                // sortOrder 순서 검증 (Java → Python → Algorithm)
                                .andExpect(jsonPath("$.data.courseStats[0].courseId").value(1L))
                                .andExpect(jsonPath("$.data.courseStats[0].courseName").value("Java 기초"))
                                .andExpect(jsonPath("$.data.courseStats[0].totalQuestions").value(50))
                                .andExpect(jsonPath("$.data.courseStats[0].solvedCount").value(30))
                                .andExpect(jsonPath("$.data.courseStats[1].courseId").value(2L))
                                .andExpect(jsonPath("$.data.courseStats[1].courseName").value("Python 입문"))
                                .andExpect(jsonPath("$.data.courseStats[2].courseId").value(3L))
                                .andExpect(jsonPath("$.data.courseStats[2].courseName").value("알고리즘"));
        }

        @Test
        @DisplayName("코스별 정답 통계 조회 API - 정답 이력이 없는 경우 0 반환")
        void getCourseStats_ShouldReturnZeroWhenNoSolvedQuestions() throws Exception {
                // given: 맞춘 문제 수가 0인 Mock 데이터
                ReviewCourseStatsResponse.CourseStatDto javaCourse = ReviewCourseStatsResponse.CourseStatDto.from(
                                1L, "Java 기초", 50L, 0L);
                ReviewCourseStatsResponse.CourseStatDto pythonCourse = ReviewCourseStatsResponse.CourseStatDto.from(
                                2L, "Python 입문", 40L, 0L);

                ReviewCourseStatsResponse mockResponse = ReviewCourseStatsResponse.from(
                                0L, List.of(javaCourse, pythonCourse));

                given(fsrsService.getCourseStats(TEST_USER_ID))
                                .willReturn(mockResponse);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/courses/stats")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                // 전체 맞춘 문제 수 = 0
                                .andExpect(jsonPath("$.data.totalSolvedCount").value(0))
                                // 각 코스의 solvedCount = 0 검증
                                .andExpect(jsonPath("$.data.courseStats[0].solvedCount").value(0))
                                .andExpect(jsonPath("$.data.courseStats[1].solvedCount").value(0));
        }

        @Test
        @DisplayName("코스별 취약점 통계 조회 API 성공 테스트")
        void getCourseWeaknessStats_ShouldReturnSuccess() throws Exception {
                // given
                com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse.CourseWeaknessStatDto stat1 = com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse.CourseWeaknessStatDto
                                .from(1L, "Java", 100L, 20L);
                com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse.CourseWeaknessStatDto stat2 = com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse.CourseWeaknessStatDto
                                .from(2L, "Spring", 50L, 5L);

                com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse mockResponse = com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse
                                .from(List.of(stat1, stat2));

                given(fsrsService.getCourseWeaknessStats(TEST_USER_ID))
                                .willReturn(mockResponse);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/courses/weakness")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.courseWeaknessStats").isArray())
                                .andExpect(jsonPath("$.data.courseWeaknessStats[0].courseId").value(1L))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[0].courseName").value("Java"))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[0].totalReps").value(100))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[0].totalLapses").value(20))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[1].courseId").value(2L))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[1].courseName").value("Spring"))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[1].totalReps").value(50))
                                .andExpect(jsonPath("$.data.courseWeaknessStats[1].totalLapses").value(5));
        }

        @Test
        @DisplayName("복습 통계 조회 API 성공 테스트")
        void getReviewStats_ShouldReturnSuccess() throws Exception {
                // given
                ReviewStatsResponse mockStats = ReviewStatsResponse.of(
                                50, 5, 10, 3, 30, 7,
                                4.5, 120, 15, 0.6,
                                0.85, 25, 12); // New fields added

                given(fsrsService.getStats(TEST_USER_ID))
                                .willReturn(mockStats);

                // when & then
                mockMvc.perform(get("/api/v1/reviews/stats")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalItems").value(50))
                                .andExpect(jsonPath("$.data.averageStability").value(4.5))
                                .andExpect(jsonPath("$.data.averageRetrievability").value(0.85))
                                .andExpect(jsonPath("$.data.matureCards").value(25))
                                .andExpect(jsonPath("$.data.dailyMaxCombo").value(12));
        }

}
