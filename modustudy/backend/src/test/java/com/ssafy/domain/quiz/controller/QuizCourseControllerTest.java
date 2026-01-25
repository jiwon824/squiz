package com.ssafy.domain.quiz.controller;

import com.ssafy.domain.quiz.dto.response.BadgeInfo;
import com.ssafy.domain.quiz.dto.response.QuizCourseDetailResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseListItem;
import com.ssafy.domain.quiz.dto.response.QuizCourseListResponse;
import com.ssafy.domain.quiz.dto.response.SectionSummary;
import com.ssafy.domain.quiz.service.QuizCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QuizCourseController 단위 테스트.
 *
 * 테스트 대상: {@link QuizCourseController#getCourseList()}, {@link QuizCourseController#getCourseDetail(Long)}
 */
@ExtendWith(MockitoExtension.class)
class QuizCourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuizCourseService quizCourseService;

    @InjectMocks
    private QuizCourseController quizCourseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizCourseController).build();
    }

    @Nested
    @DisplayName("GET /api/v1/quiz-courses")
    class GetCourseList {

        @Test
        @DisplayName("코스 목록 조회 시 200 OK와 코스 목록을 반환한다")
        void returnCourseListWithStatus200() throws Exception {
            // given
            List<QuizCourseListItem> items = List.of(
                    new QuizCourseListItem(1L, "JAVA", "Java 마스터",
                            "Java 기초부터 고급까지", 5, "JAVA_MASTER", "Java 마스터"),
                    new QuizCourseListItem(2L, "PYTHON", "Python 기초",
                            "Python 입문자를 위한 코스", 4, "PYTHON_MASTER", "Python 마스터"),
                    new QuizCourseListItem(3L, "CS_BASIC", "CS 기초",
                            "컴퓨터 과학 기초 개념", 6, "CS_MASTER", "CS 마스터")
            );
            QuizCourseListResponse response = new QuizCourseListResponse(items);

            given(quizCourseService.getCourseList()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.courses").isArray())
                    .andExpect(jsonPath("$.data.courses.length()").value(3))
                    .andExpect(jsonPath("$.data.courses[0].id").value(1))
                    .andExpect(jsonPath("$.data.courses[0].code").value("JAVA"))
                    .andExpect(jsonPath("$.data.courses[0].name").value("Java 마스터"))
                    .andExpect(jsonPath("$.data.courses[0].description").value("Java 기초부터 고급까지"))
                    .andExpect(jsonPath("$.data.courses[0].totalSections").value(5))
                    .andExpect(jsonPath("$.data.courses[0].badgeCode").value("JAVA_MASTER"))
                    .andExpect(jsonPath("$.data.courses[0].badgeName").value("Java 마스터"));
        }

        @Test
        @DisplayName("코스가 없으면 빈 배열을 반환한다")
        void returnEmptyListWhenNoCoursesExist() throws Exception {
            // given
            QuizCourseListResponse response = new QuizCourseListResponse(Collections.emptyList());
            given(quizCourseService.getCourseList()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.courses").isArray())
                    .andExpect(jsonPath("$.data.courses").isEmpty());
        }

        @Test
        @DisplayName("배지 코드가 없는 코스는 badgeName이 null이다")
        void returnNullBadgeNameWhenNoBadgeCode() throws Exception {
            // given
            List<QuizCourseListItem> items = List.of(
                    new QuizCourseListItem(1L, "INTRO", "입문 코스",
                            "입문 설명", 2, null, null)
            );
            QuizCourseListResponse response = new QuizCourseListResponse(items);

            given(quizCourseService.getCourseList()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.courses[0].badgeCode").doesNotExist())
                    .andExpect(jsonPath("$.data.courses[0].badgeName").doesNotExist());
        }

        @Test
        @DisplayName("인증 없이도 접근 가능하다")
        void accessibleWithoutAuthentication() throws Exception {
            // given
            QuizCourseListResponse response = new QuizCourseListResponse(Collections.emptyList());
            given(quizCourseService.getCourseList()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/quiz-courses/{courseId}")
    class GetCourseDetail {

        @Test
        @DisplayName("코스 상세 조회 시 200 OK와 상세 정보를 반환한다")
        void returnCourseDetailWithStatus200() throws Exception {
            // given
            List<SectionSummary> sections = List.of(
                    new SectionSummary(1, "기본 문법", "변수, 자료형, 연산자", 10, 70),
                    new SectionSummary(2, "객체지향", "클래스, 상속, 다형성", 15, 70)
            );
            QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                    1L,
                    "JAVA",
                    "Java 마스터",
                    "Java 기초부터 고급까지",
                    5,
                    new BadgeInfo("JAVA_MASTER", "Java 마스터", "Java 코스 완료"),
                    sections
            );

            given(quizCourseService.getCourseDetail(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.code").value("JAVA"))
                    .andExpect(jsonPath("$.data.name").value("Java 마스터"))
                    .andExpect(jsonPath("$.data.description").value("Java 기초부터 고급까지"))
                    .andExpect(jsonPath("$.data.totalSections").value(5))
                    .andExpect(jsonPath("$.data.badge.code").value("JAVA_MASTER"))
                    .andExpect(jsonPath("$.data.badge.name").value("Java 마스터"))
                    .andExpect(jsonPath("$.data.badge.description").value("Java 코스 완료"))
                    .andExpect(jsonPath("$.data.sections").isArray())
                    .andExpect(jsonPath("$.data.sections.length()").value(2))
                    .andExpect(jsonPath("$.data.sections[0].sectionNumber").value(1))
                    .andExpect(jsonPath("$.data.sections[0].name").value("기본 문법"))
                    .andExpect(jsonPath("$.data.sections[0].description").value("변수, 자료형, 연산자"))
                    .andExpect(jsonPath("$.data.sections[0].totalQuestions").value(10))
                    .andExpect(jsonPath("$.data.sections[0].passScore").value(70));
        }

        @Test
        @DisplayName("섹션이 없는 코스는 빈 배열을 반환한다")
        void returnEmptySections() throws Exception {
            // given
            QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                    1L,
                    "JAVA",
                    "Java 마스터",
                    "Java 기초부터 고급까지",
                    5,
                    new BadgeInfo("JAVA_MASTER", "Java 마스터", "Java 코스 완료"),
                    Collections.emptyList()
            );

            given(quizCourseService.getCourseDetail(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sections").isArray())
                    .andExpect(jsonPath("$.data.sections").isEmpty());
        }

        @Test
        @DisplayName("배지 코드가 없으면 badge는 null이다")
        void returnNullBadgeWhenNoBadgeCode() throws Exception {
            // given
            QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                    1L,
                    "JAVA",
                    "Java 마스터",
                    "Java 기초부터 고급까지",
                    5,
                    null,
                    Collections.emptyList()
            );

            given(quizCourseService.getCourseDetail(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.badge").doesNotExist());
        }

        @Test
        @DisplayName("배지 정보가 일부 비어 있어도 정상 반환한다")
        void returnBadgeWithMissingFields() throws Exception {
            // given
            QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                    1L,
                    "JAVA",
                    "Java 마스터",
                    "Java 기초부터 고급까지",
                    5,
                    new BadgeInfo("JAVA_MASTER", null, null),
                    Collections.emptyList()
            );

            given(quizCourseService.getCourseDetail(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.badge.code").value("JAVA_MASTER"))
                    .andExpect(jsonPath("$.data.badge.name").doesNotExist())
                    .andExpect(jsonPath("$.data.badge.description").doesNotExist());
        }

        @Test
        @DisplayName("인증 없이도 접근 가능하다")
        void accessibleWithoutAuthentication() throws Exception {
            // given
            QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                    1L,
                    "JAVA",
                    "Java 마스터",
                    "Java 기초부터 고급까지",
                    5,
                    new BadgeInfo("JAVA_MASTER", "Java 마스터", "Java 코스 완료"),
                    Collections.emptyList()
            );
            given(quizCourseService.getCourseDetail(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/quiz-courses/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
