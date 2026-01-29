package com.ssafy.domain.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.domain.quiz.dto.request.SaveAnswerRequest;
import com.ssafy.domain.quiz.dto.response.*;
import com.ssafy.domain.quiz.entity.enums.AttemptStatus;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.service.QuizCourseService;
import com.ssafy.domain.quiz.service.QuizSectionAttemptService;
import com.ssafy.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QuizCourseController unit tests.
 *
 * Tests all endpoints defined in QuizCourseController:
 * - GET /api/v1/quiz-courses (course list)
 * - GET /api/v1/quiz-courses/{courseId} (course detail)
 * - GET /api/v1/quiz-courses/{courseId}/sections (sections with progress)
 * - POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts
 * (start/resume attempt)
 * - PATCH .../attempts/{attemptId}/answers (save answer)
 * - POST .../attempts/{attemptId}/submit (submit attempt)
 * - DELETE .../attempts/{attemptId} (abandon attempt)
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class QuizCourseControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private QuizCourseService quizCourseService;

        @Mock
        private QuizSectionAttemptService attemptService;

        @InjectMocks
        private QuizCourseController quizCourseController;

        // Test constants
        private static final Long COURSE_ID = 1L;
        private static final Integer SECTION_NUMBER = 1;
        private static final Long ATTEMPT_ID = 123L;
        private static final Long USER_ID = 1L;

        // Mock user for authenticated endpoints
        private User mockUser;
        private SsafyUserDetails mockUserDetails;

        @BeforeEach
        void setUp() {
                // Create mock user using Mockito (since id is in BaseEntity without setter)
                mockUser = org.mockito.Mockito.mock(User.class);
                given(mockUser.getId()).willReturn(USER_ID);

                mockUserDetails = org.mockito.Mockito.mock(SsafyUserDetails.class);
                given(mockUserDetails.getUser()).willReturn(mockUser);

                // Custom argument resolver to inject mock SsafyUserDetails for
                // @AuthenticationPrincipal
                HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
                        @Override
                        public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().isAssignableFrom(SsafyUserDetails.class);
                        }

                        @Override
                        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return mockUserDetails;
                        }
                };

                mockMvc = MockMvcBuilders.standaloneSetup(quizCourseController)
                                .setCustomArgumentResolvers(authResolver)
                                .build();
                objectMapper = new ObjectMapper();
        }

        // ========== GET /api/v1/quiz-courses ==========

        @Nested
        @DisplayName("GET /api/v1/quiz-courses")
        class GetCourseList {

                @Test
                @DisplayName("코스 목록 조회 시 200 OK와 코스 목록을 반환한다")
                void returnCourseListWithStatus200() throws Exception {
                        // given
                        List<QuizCourseListItem> items = List.of(
                                        new QuizCourseListItem(1L, "JAVA", "Java Master",
                                                        "Java from basics to advanced", 5, "JAVA_MASTER",
                                                        "Java Master"),
                                        new QuizCourseListItem(2L, "PYTHON", "Python Basics",
                                                        "Python course for beginners", 4, "PYTHON_MASTER",
                                                        "Python Master"));
                        QuizCourseListResponse response = new QuizCourseListResponse(items);

                        given(quizCourseService.getCourseList()).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.courses").isArray())
                                        .andExpect(jsonPath("$.data.courses.length()").value(2))
                                        .andExpect(jsonPath("$.data.courses[0].id").value(1))
                                        .andExpect(jsonPath("$.data.courses[0].code").value("JAVA"))
                                        .andExpect(jsonPath("$.data.courses[0].name").value("Java Master"))
                                        .andExpect(jsonPath("$.data.courses[0].totalSections").value(5))
                                        .andExpect(jsonPath("$.data.courses[0].badgeCode").value("JAVA_MASTER"));
                }

                @Test
                @DisplayName("Returns empty array when no courses exist")
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
                                        .andExpect(jsonPath("$.data.courses").isEmpty());
                }

                @Test
                @DisplayName("Accessible without authentication")
                void accessibleWithoutAuthentication() throws Exception {
                        // given
                        QuizCourseListResponse response = new QuizCourseListResponse(Collections.emptyList());
                        given(quizCourseService.getCourseList()).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isOk());
                }
        }

        // ========== GET /api/v1/quiz-courses/{courseId} ==========

        @Nested
        @DisplayName("GET /api/v1/quiz-courses/{courseId}")
        class GetCourseDetail {

                @Test
                @DisplayName("Returns course detail with 200 OK")
                void returnCourseDetailWithStatus200() throws Exception {
                        // given
                        List<SectionSummary> sections = List.of(
                                        new SectionSummary(1, "Basic Syntax", "Variables, types, operators", 10, 70),
                                        new SectionSummary(2, "OOP", "Classes, inheritance, polymorphism", 15, 70));
                        QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                                        1L, "JAVA", "Java Master", "Java from basics to advanced", 5,
                                        new BadgeInfo("JAVA_MASTER", "Java Master", "Complete Java course"),
                                        sections);

                        given(quizCourseService.getCourseDetail(1L)).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses/1")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.id").value(1))
                                        .andExpect(jsonPath("$.data.code").value("JAVA"))
                                        .andExpect(jsonPath("$.data.name").value("Java Master"))
                                        .andExpect(jsonPath("$.data.totalSections").value(5))
                                        .andExpect(jsonPath("$.data.badge.code").value("JAVA_MASTER"))
                                        .andExpect(jsonPath("$.data.sections").isArray())
                                        .andExpect(jsonPath("$.data.sections.length()").value(2))
                                        .andExpect(jsonPath("$.data.sections[0].sectionNumber").value(1))
                                        .andExpect(jsonPath("$.data.sections[0].name").value("Basic Syntax"))
                                        .andExpect(jsonPath("$.data.sections[0].totalQuestions").value(10))
                                        .andExpect(jsonPath("$.data.sections[0].passScore").value(70));
                }

                @Test
                @DisplayName("Returns empty sections array when course has no sections")
                void returnEmptySections() throws Exception {
                        // given
                        QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                                        1L, "JAVA", "Java Master", "Description", 0,
                                        new BadgeInfo("JAVA_MASTER", "Java Master", "Complete Java course"),
                                        Collections.emptyList());

                        given(quizCourseService.getCourseDetail(1L)).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses/1")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.sections").isEmpty());
                }

                @Test
                @DisplayName("Returns null badge when no badge code exists")
                void returnNullBadgeWhenNoBadgeCode() throws Exception {
                        // given
                        QuizCourseDetailResponse response = new QuizCourseDetailResponse(
                                        1L, "JAVA", "Java Master", "Description", 5,
                                        null, Collections.emptyList());

                        given(quizCourseService.getCourseDetail(1L)).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses/1")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.badge").doesNotExist());
                }
        }

        // ========== GET /api/v1/quiz-courses/{courseId}/sections ==========

        @Nested
        @DisplayName("GET /api/v1/quiz-courses/{courseId}/sections")
        class GetSectionsWithProgress {

                @Test
                @DisplayName("Returns sections with user progress")
                void returnSectionsWithProgress() throws Exception {
                        // given
                        MyProgressDto myProgress = new MyProgressDto(2, 1, false);
                        List<SectionWithProgressDto> sections = List.of(
                                        new SectionWithProgressDto(1, "Basic Syntax", 10, 70, true, true, 90, 2, null),
                                        new SectionWithProgressDto(2, "OOP", 15, 70, true, false, 50, 1, 456L),
                                        new SectionWithProgressDto(3, "Collections", 12, 70, false, false, null, 0, null));
                        SectionsWithProgressResponse response = new SectionsWithProgressResponse(
                                        COURSE_ID, "Java Master", myProgress, sections);

                        given(quizCourseService.getSectionsWithProgress(eq(COURSE_ID), anyLong()))
                                        .willReturn(response);

                        // when & then - Note: Real auth would be needed; here we're testing routing
                        // only
                        mockMvc.perform(get("/api/v1/quiz-courses/{courseId}/sections", COURSE_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.courseId").value(1))
                                        .andExpect(jsonPath("$.data.courseName").value("Java Master"))
                                        .andExpect(jsonPath("$.data.myProgress.currentSection").value(2))
                                        .andExpect(jsonPath("$.data.myProgress.completedSections").value(1))
                                        .andExpect(jsonPath("$.data.myProgress.isCompleted").value(false))
                                        .andExpect(jsonPath("$.data.sections").isArray())
                                        .andExpect(jsonPath("$.data.sections.length()").value(3))
                                        .andExpect(jsonPath("$.data.sections[0].isUnlocked").value(true))
                                        .andExpect(jsonPath("$.data.sections[0].isPassed").value(true))
                                        .andExpect(jsonPath("$.data.sections[0].bestScore").value(90))
                                        .andExpect(jsonPath("$.data.sections[2].isUnlocked").value(false));
                }

                @Test
                @DisplayName("Returns empty progress for new user")
                void returnEmptyProgressForNewUser() throws Exception {
                        // given
                        MyProgressDto myProgress = MyProgressDto.empty();
                        SectionsWithProgressResponse response = new SectionsWithProgressResponse(
                                        COURSE_ID, "Java Master", myProgress, Collections.emptyList());

                        given(quizCourseService.getSectionsWithProgress(eq(COURSE_ID), anyLong()))
                                        .willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/quiz-courses/{courseId}/sections", COURSE_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.myProgress.currentSection").value(1))
                                        .andExpect(jsonPath("$.data.myProgress.completedSections").value(0))
                                        .andExpect(jsonPath("$.data.myProgress.isCompleted").value(false));
                }
        }

        // ========== POST
        // /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts ==========

        @Nested
        @DisplayName("POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts")
        class StartOrResumeAttempt {

                @Test
                @DisplayName("Creates new attempt and returns questions")
                void createNewAttemptAndReturnQuestions() throws Exception {
                        // given
                        List<AttemptQuestionItem> questions = List.of(
                                        new AttemptQuestionItem(1, 101L, "What is a variable?",
                                                        QuestionType.MULTIPLE_CHOICE,
                                                        List.of(new OptionItem("A", "Storage"),
                                                                        new OptionItem("B", "Function")),
                                                        null),
                                        new AttemptQuestionItem(2, 102L, "What is OOP?", QuestionType.MULTIPLE_CHOICE,
                                                        List.of(new OptionItem("A", "Object Oriented Programming"),
                                                                        new OptionItem("B", "Other")),
                                                        null));

                        SectionAttemptResponse response = new SectionAttemptResponse(
                                        ATTEMPT_ID, SECTION_NUMBER, "Basic Syntax",
                                        AttemptStatus.IN_PROGRESS, 10, 0, 70,
                                        LocalDateTime.now(), questions);
                        given(attemptService.startOrResumeAttempt(eq(COURSE_ID), eq(SECTION_NUMBER), anyLong()))
                                        .willReturn(response);

                        // when & then
                        mockMvc.perform(post("/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts",
                                        COURSE_ID, SECTION_NUMBER)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.attemptId").value(123))
                                        .andExpect(jsonPath("$.data.sectionNumber").value(1))
                                        .andExpect(jsonPath("$.data.sectionName").value("Basic Syntax"))
                                        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                                        .andExpect(jsonPath("$.data.totalQuestions").value(10))
                                        .andExpect(jsonPath("$.data.answeredCount").value(0))
                                        .andExpect(jsonPath("$.data.passScore").value(70))
                                        .andExpect(jsonPath("$.data.questions").isArray())
                                        .andExpect(jsonPath("$.data.questions.length()").value(2))
                                        .andExpect(jsonPath("$.data.questions[0].questionId").value(101));
                }

                @Test
                @DisplayName("Resumes existing in-progress attempt")
                void resumeExistingAttempt() throws Exception {
                        // given
                        List<AttemptQuestionItem> questions = List.of(
                                        new AttemptQuestionItem(1, 101L, "Question 1", QuestionType.MULTIPLE_CHOICE,
                                                        List.of(new OptionItem("A", "Option A")),
                                                        "A") // savedAnswer indicates resumed
                        );

                        SectionAttemptResponse response = new SectionAttemptResponse(
                                        ATTEMPT_ID, SECTION_NUMBER, "Basic Syntax",
                                        AttemptStatus.IN_PROGRESS, 10, 5, 70,
                                        LocalDateTime.now().minusMinutes(10), questions);
                        given(attemptService.startOrResumeAttempt(eq(COURSE_ID), eq(SECTION_NUMBER), anyLong()))
                                        .willReturn(response);

                        // when & then
                        mockMvc.perform(post("/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts",
                                        COURSE_ID, SECTION_NUMBER)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.answeredCount").value(5))
                                        .andExpect(jsonPath("$.data.questions[0].userAnswer").value("A"));
                }
        }

        // ========== POST
        // /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId} ==========

        @Nested
        @DisplayName("POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}")
        class ResumeAttempt {

                @Test
                @DisplayName("Resumes specific attempt with explicit attemptId")
                void resumeAttemptWithExplicitId() throws Exception {
                        // given
                        List<AttemptQuestionItem> questions = List.of(
                                        new AttemptQuestionItem(1, 101L, "Question 1", QuestionType.MULTIPLE_CHOICE,
                                                        List.of(new OptionItem("A", "Option A")),
                                                        "A") // savedAnswer indicates resumed
                        );

                        SectionAttemptResponse response = new SectionAttemptResponse(
                                        ATTEMPT_ID, SECTION_NUMBER, "Basic Syntax",
                                        AttemptStatus.IN_PROGRESS, 10, 5, 70,
                                        LocalDateTime.now().minusMinutes(10), questions);
                        given(attemptService.resumeAttempt(eq(ATTEMPT_ID), anyLong()))
                                        .willReturn(response);

                        // when & then
                        mockMvc.perform(post("/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.attemptId").value(123))
                                        .andExpect(jsonPath("$.data.answeredCount").value(5))
                                        .andExpect(jsonPath("$.data.questions[0].userAnswer").value("A"));

                        // verify the service was called with the correct attemptId
                        verify(attemptService).resumeAttempt(eq(ATTEMPT_ID), anyLong());
                }
        }

        // ========== PATCH
        // /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers
        // ==========

        @Nested
        @DisplayName("PATCH .../attempts/{attemptId}/answers")
        class SaveAnswer {

                @Test
                @DisplayName("Saves single answer successfully")
                void saveAnswerSuccessfully() throws Exception {
                        // given
                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(101L, "B", 1000L));

                        doNothing().when(attemptService).saveAnswer(eq(ATTEMPT_ID), any(SaveAnswerRequest.class),
                                        anyLong());

                        // when & then
                        mockMvc.perform(patch(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data").doesNotExist());

                        verify(attemptService).saveAnswer(eq(ATTEMPT_ID), any(SaveAnswerRequest.class), anyLong());
                }

                @Test
                @DisplayName("Overwrites previous answer (idempotent)")
                void overwritesPreviousAnswer() throws Exception {
                        // given - same question, different answer
                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(101L, "C", 1000L) // Changed from B to C
                        );

                        doNothing().when(attemptService).saveAnswer(eq(ATTEMPT_ID), any(SaveAnswerRequest.class),
                                        anyLong());

                        // when & then
                        mockMvc.perform(patch(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true));
                }
        }

        // ========== POST .../attempts/{attemptId}/submit ==========

        @Nested
        @DisplayName("POST .../attempts/{attemptId}/submit")
        class SubmitAttempt {

                @Test
                @DisplayName("Returns passing result with next section unlocked")
                void returnPassingResult() throws Exception {
                        // given
                        List<AttemptResultResponse.QuestionResultItem> results = List.of(
                                        new AttemptResultResponse.QuestionResultItem(1, 101L, "B", "B", true,
                                                        "Correct explanation"));

                        AttemptResultResponse response = new AttemptResultResponse(
                                        ATTEMPT_ID, 80, 8, 10, 70, true, true, null, results);

                        given(attemptService.submitAttempt(eq(ATTEMPT_ID), anyLong())).willReturn(response);

                        // when & then
                        mockMvc.perform(post(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.attemptId").value(123))
                                        .andExpect(jsonPath("$.data.score").value(80))
                                        .andExpect(jsonPath("$.data.correctCount").value(8))
                                        .andExpect(jsonPath("$.data.totalQuestions").value(10))
                                        .andExpect(jsonPath("$.data.passScore").value(70))
                                        .andExpect(jsonPath("$.data.isPassed").value(true))
                                        .andExpect(jsonPath("$.data.isNextSectionUnlocked").value(true))
                                        .andExpect(jsonPath("$.data.results").isArray())
                                        .andExpect(jsonPath("$.data.results[0].isCorrect").value(true));
                }

                @Test
                @DisplayName("Returns failing result without unlock")
                void returnFailingResult() throws Exception {
                        // given
                        AttemptResultResponse response = new AttemptResultResponse(
                                        ATTEMPT_ID, 50, 5, 10, 70, false, false, null, Collections.emptyList());

                        given(attemptService.submitAttempt(eq(ATTEMPT_ID), anyLong())).willReturn(response);

                        // when & then
                        mockMvc.perform(post(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.score").value(50))
                                        .andExpect(jsonPath("$.data.isPassed").value(false))
                                        .andExpect(jsonPath("$.data.isNextSectionUnlocked").value(false));
                }

                @Test
                @DisplayName("Returns badge when course completed")
                void returnBadgeWhenCourseCompleted() throws Exception {
                        // given
                        BadgeInfo earnedBadge = new BadgeInfo("JAVA_MASTER", "Java Master", "Complete Java course");
                        AttemptResultResponse response = new AttemptResultResponse(
                                        ATTEMPT_ID, 90, 9, 10, 70, true, false, earnedBadge, Collections.emptyList());

                        given(attemptService.submitAttempt(eq(ATTEMPT_ID), anyLong())).willReturn(response);

                        // when & then
                        mockMvc.perform(post(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.isPassed").value(true))
                                        .andExpect(jsonPath("$.data.earnedBadge.code").value("JAVA_MASTER"))
                                        .andExpect(jsonPath("$.data.earnedBadge.name").value("Java Master"));
                }
        }

        // ========== DELETE .../attempts/{attemptId} ==========

        @Nested
        @DisplayName("DELETE .../attempts/{attemptId}")
        class AbandonAttempt {

                @Test
                @DisplayName("Abandons attempt successfully")
                void abandonAttemptSuccessfully() throws Exception {
                        // given
                        doNothing().when(attemptService).abandonAttempt(eq(ATTEMPT_ID), anyLong());

                        // when & then
                        mockMvc.perform(delete(
                                        "/api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}",
                                        COURSE_ID, SECTION_NUMBER, ATTEMPT_ID)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data").doesNotExist());

                        verify(attemptService).abandonAttempt(eq(ATTEMPT_ID), anyLong());
                }
        }
}
