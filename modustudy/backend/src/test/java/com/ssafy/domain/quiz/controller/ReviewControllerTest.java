package com.ssafy.domain.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.util.JwtTokenUtil;
import com.ssafy.domain.quiz.dto.request.ReviewSubmitRequest;
import com.ssafy.domain.quiz.dto.response.ReviewResult;
import com.ssafy.domain.quiz.dto.response.ReviewSubmitResponse;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.service.FsrsService;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.service.UserService;
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

                given(fsrsService.getWrongAnswersWithQuestions(TEST_USER_ID,
                                com.ssafy.domain.quiz.entity.WrongAnswerSortType.MOST_WRONG))
                                .willReturn(List.of(dto));

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .param("sortType", "MOST_WRONG")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(2L));
        }

        @Test
        @DisplayName("오답 노트 조회 API 성공 테스트 - FSRS 복습 우선순위")
        void getWrongAnswers_ShouldReturnListSortedByFsrsRecommended() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                3L, ReviewContentType.COURSE_QUESTION, 102L, 0.3, 7.0, 2, 4, 1, LocalDateTime.now(),
                                null);

                given(fsrsService.getWrongAnswersWithQuestions(TEST_USER_ID,
                                com.ssafy.domain.quiz.entity.WrongAnswerSortType.FSRS_RECOMMENDED))
                                .willReturn(List.of(dto));

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .param("sortType", "FSRS_RECOMMENDED")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(3L));
        }

        @Test
        @DisplayName("오답 노트 조회 API - sortType 없으면 기본값(MOST_WRONG) 사용")
        void getWrongAnswers_ShouldUseDefaultSortType() throws Exception {
                // given
                TodayReviewResponse.ReviewItemDto dto = new TodayReviewResponse.ReviewItemDto(
                                2L, ReviewContentType.COURSE_QUESTION, 101L, 0.5, 8.0, 3, 5, 2, LocalDateTime.now(),
                                null);

                given(fsrsService.getWrongAnswersWithQuestions(TEST_USER_ID,
                                com.ssafy.domain.quiz.entity.WrongAnswerSortType.MOST_WRONG))
                                .willReturn(List.of(dto));

                // when & then
                mockMvc.perform(get("/api/v1/reviews/wrong-answers")
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.items").isArray())
                                .andExpect(jsonPath("$.data.items[0].reviewItemId").value(2L));
        }
}
