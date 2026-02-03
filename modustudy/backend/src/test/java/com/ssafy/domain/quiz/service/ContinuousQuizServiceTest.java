package com.ssafy.domain.quiz.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.ContinuousAnswerResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.entity.QuizCourse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.ContinuousQuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ContinuousQuizService 단위 테스트.
 *
 * <p>
 * Mockito 기반으로 Repository와 FsrsService를 모킹하여 서비스 로직을 검증한다.
 * </p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 * <li>submitAnswer: 정답 제출 및 FSRS 상태 업데이트</li>
 * <li>getNextQuestion: 다음 문제 조회</li>
 * <li>processAnswerAndGetNext: Atomic Submit & Fetch Next</li>
 * <li>자동 Rating 산출 로직 검증</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ContinuousQuizServiceTest {

        @InjectMocks
        private ContinuousQuizService continuousQuizService;

        @Mock
        private ContinuousQuizRepository learningRepository;

        @Mock
        private FsrsService fsrsService;

        private static final Long TEST_USER_ID = 1L;
        private static final Long TEST_COURSE_ID = 100L;
        private static final Integer TEST_SECTION_NUMBER = 1;

        private QuizCourseQuestion testQuestion;
        private QuizCourseQuestion nextQuestion;
        private QuizCourse testCourse;
        private QuizCourseSection testSection;
        private UserReviewItem testReviewItem;

        @BeforeEach
        void setUp() {
                // 테스트 코스 생성
                testCourse = createQuizCourse(TEST_COURSE_ID, "TEST", "테스트 코스");

                // 테스트 섹션 생성
                testSection = createQuizCourseSection(TEST_COURSE_ID, TEST_SECTION_NUMBER, "테스트 섹션");

                // 테스트 문제 생성
                testQuestion = createQuizCourseQuestion(
                                1L, 1, "Java에서 정수형 키워드는?",
                                QuestionType.SHORT_ANSWER, null, "int", "정수형은 int입니다.");
                ReflectionTestUtils.setField(testQuestion, "section", testSection);

                // 다음 문제 생성
                nextQuestion = createQuizCourseQuestion(
                                2L, 2, "Java에서 문자열 클래스는?",
                                QuestionType.SHORT_ANSWER, null, "String", "문자열은 String입니다.");
                ReflectionTestUtils.setField(nextQuestion, "section", testSection);

                // 테스트 ReviewItem 생성
                testReviewItem = createUserReviewItem(TEST_USER_ID, 1L);
        }

        // ══════════════════════════════════════════════════════
        // submitAnswer 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("submitAnswer 메서드는")
        class SubmitAnswer {

                @Test
                @DisplayName("정답 제출 시 isCorrect가 true이고 FSRS 상태가 업데이트된다")
                void shouldReturnCorrectAndUpdateFsrsOnCorrectAnswer() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("int", 1500L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true), // 정답
                                        eq(1500L))).willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                        assertThat(response.getUserAnswer()).isEqualTo("int");
                        assertThat(response.getCorrectAnswer()).isEqualTo("int");
                        assertThat(response.getExplanation()).isEqualTo("정수형은 int입니다.");

                        // FSRS 갱신 정보 검증
                        assertThat(response.getStability()).isEqualTo(testReviewItem.getStability());
                        assertThat(response.getDifficulty()).isEqualTo(testReviewItem.getDifficulty());
                        assertThat(response.getReps()).isEqualTo(testReviewItem.getReps());

                        // FsrsService 호출 검증
                        verify(fsrsService).processReviewResult(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        1L,
                                        true,
                                        1500L);
                }

                @Test
                @DisplayName("오답 제출 시 isCorrect가 false이다")
                void shouldReturnIncorrectOnWrongAnswer() {
                        // given
                        // "integer"는 "int"를 포함하므로 정답 처리됨 (서술형 포함 채점)
                        // 오답 테스트를 위해 "int"를 포함하지 않는 답 사용
                        ContinuousAnswerRequest request = createAnswerRequest("float", 3000L);
                        UserReviewItem wrongReviewItem = createUserReviewItem(TEST_USER_ID, 1L);
                        wrongReviewItem.setLapses(1);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(false), // 오답
                                        eq(3000L))).willReturn(wrongReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isFalse();
                        assertThat(response.getUserAnswer()).isEqualTo("float");
                        assertThat(response.getCorrectAnswer()).isEqualTo("int");
                        assertThat(response.getLapses()).isEqualTo(1);
                }

                @Test
                @DisplayName("대소문자를 무시하고 정답을 판정한다")
                void shouldIgnoreCaseWhenCheckingAnswer() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("INT", 2000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true), // 대소문자 무시 → 정답
                                        eq(2000L))).willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("공백을 트림한 후 정답을 판정한다")
                void shouldTrimWhitespaceWhenCheckingAnswer() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("  int  ", 2500L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        anyLong(), any(), anyLong(), eq(true), anyLong())).willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("빈 답변은 오답으로 처리한다")
                void shouldTreatEmptyAnswerAsIncorrect() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("", 1000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        anyLong(), any(), anyLong(), eq(false), anyLong())).willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isFalse();
                }

                @Test
                @DisplayName("null 답변은 오답으로 처리한다")
                void shouldTreatNullAnswerAsIncorrect() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest(null, 1000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        anyLong(), any(), anyLong(), eq(false), anyLong())).willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.isCorrect()).isFalse();
                }

                @Test
                @DisplayName("존재하지 않는 문제 ID로 요청 시 예외를 던진다")
                void shouldThrowExceptionWhenQuestionNotFound() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("answer", 1000L);
                        given(learningRepository.findById(999L))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> continuousQuizService.submitAnswer(TEST_USER_ID, 999L, request))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("문제를 찾을 수 없습니다");
                }

                @Test
                @DisplayName("객관식 문제에서 옵션 ID로 제출 시 정답으로 처리한다 (correctAnswer가 텍스트인 경우)")
                void shouldAcceptOptionIdWhenCorrectAnswerIsText() {
                        // given: correctAnswer가 텍스트 "int"로 저장, 사용자는 옵션 ID "A"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"integer\"}]";
                        QuizCourseQuestion mcQuestion = createQuizCourseQuestion(
                                        10L, 1, "Java에서 정수형 키워드는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "int", "정수형은 int입니다.");
                        ReflectionTestUtils.setField(mcQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("A", 2000L);

                        given(learningRepository.findById(10L))
                                        .willReturn(Optional.of(mcQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(10L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 10L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("객관식 문제에서 옵션 ID로 제출 시 정답으로 처리한다 (correctAnswer가 ID인 경우)")
                void shouldAcceptOptionIdWhenCorrectAnswerIsId() {
                        // given: correctAnswer가 ID "A"로 저장, 사용자는 옵션 ID "A"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"integer\"}]";
                        QuizCourseQuestion mcQuestion = createQuizCourseQuestion(
                                        11L, 1, "Java에서 정수형 키워드는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "A", "정수형은 int입니다.");
                        ReflectionTestUtils.setField(mcQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("A", 2000L);

                        given(learningRepository.findById(11L))
                                        .willReturn(Optional.of(mcQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(11L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 11L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("다중선택 문제에서 정답을 순서 관계없이 판정한다")
                void shouldCheckMultipleSelectionAnswerRegardlessOfOrder() {
                        // given: correctAnswer가 "A,C"로 저장, 사용자는 "C,A"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"String\"}, {\"id\": \"C\", \"text\": \"double\"}]";
                        QuizCourseQuestion mcmQuestion = createQuizCourseQuestion(
                                        12L, 1, "Java에서 기본형 타입을 모두 선택하세요",
                                        QuestionType.MULTIPLE_CHOICE_MULTIPLE, optionsJson, "A,C",
                                        "int와 double은 기본형입니다.");
                        ReflectionTestUtils.setField(mcmQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("C,A", 3000L);

                        given(learningRepository.findById(12L))
                                        .willReturn(Optional.of(mcmQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(12L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 12L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("다중선택 문제에서 옵션 ID와 텍스트를 매칭하여 판정한다")
                void shouldMatchMultipleSelectionByIdAndText() {
                        // given: correctAnswer가 텍스트 "int,double"로 저장, 사용자는 ID "A,C"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"String\"}, {\"id\": \"C\", \"text\": \"double\"}]";
                        QuizCourseQuestion mcmQuestion = createQuizCourseQuestion(
                                        13L, 1, "Java에서 기본형 타입을 모두 선택하세요",
                                        QuestionType.MULTIPLE_CHOICE_MULTIPLE, optionsJson, "int,double",
                                        "int와 double은 기본형입니다.");
                        ReflectionTestUtils.setField(mcmQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("A,C", 3000L);

                        given(learningRepository.findById(13L))
                                        .willReturn(Optional.of(mcmQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(13L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 13L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("객관식 문제에서 오답 ID를 제출하면 오답으로 처리한다")
                void shouldRejectWrongOptionId() {
                        // given: correctAnswer가 "A", 사용자는 "B"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"integer\"}]";
                        QuizCourseQuestion mcQuestion = createQuizCourseQuestion(
                                        14L, 1, "Java에서 정수형 키워드는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "A", "정수형은 int입니다.");
                        ReflectionTestUtils.setField(mcQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("B", 2000L);

                        given(learningRepository.findById(14L))
                                        .willReturn(Optional.of(mcQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(14L), eq(false), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 14L, request);

                        // then
                        assertThat(response.isCorrect()).isFalse();
                }

                @Test
                @DisplayName("correct_answer가 JSON 배열 형식일 때 정답으로 처리한다")
                void shouldParseJsonArrayCorrectAnswer() {
                        // given: correctAnswer가 JSON 배열 "[\"B\"]"로 저장, 사용자는 "B"를 제출
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"integer\"}]";
                        QuizCourseQuestion mcQuestion = createQuizCourseQuestion(
                                        15L, 1, "Java에서 정수형 래퍼 클래스는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "[\"B\"]", "Integer 클래스입니다.");
                        ReflectionTestUtils.setField(mcQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("B", 2000L);

                        given(learningRepository.findById(15L))
                                        .willReturn(Optional.of(mcQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(15L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 15L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                        assertThat(response.getCorrectAnswer()).isEqualTo("B"); // 정규화된 값 반환
                }

                @Test
                @DisplayName("correct_answer가 다중선택 JSON 배열일 때 정답으로 처리한다")
                void shouldParseJsonArrayMultipleCorrectAnswer() {
                        // given: correctAnswer가 JSON 배열 "[\"A\", \"C\"]"로 저장
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"String\"}, {\"id\": \"C\", \"text\": \"double\"}]";
                        QuizCourseQuestion mcmQuestion = createQuizCourseQuestion(
                                        16L, 1, "Java에서 기본형 타입을 모두 선택하세요",
                                        QuestionType.MULTIPLE_CHOICE_MULTIPLE, optionsJson, "[\"A\", \"C\"]",
                                        "int와 double은 기본형입니다.");
                        ReflectionTestUtils.setField(mcmQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("C,A", 3000L);

                        given(learningRepository.findById(16L))
                                        .willReturn(Optional.of(mcmQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(16L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 16L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                }

                @Test
                @DisplayName("correct_answer가 따옴표로 감싸진 문자열일 때 정답으로 처리한다")
                void shouldParseQuotedStringCorrectAnswer() {
                        // given: correctAnswer가 "\"B\""로 저장 (따옴표 포함)
                        String optionsJson = "[{\"id\": \"A\", \"text\": \"int\"}, {\"id\": \"B\", \"text\": \"integer\"}]";
                        QuizCourseQuestion mcQuestion = createQuizCourseQuestion(
                                        17L, 1, "Java에서 정수형 래퍼 클래스는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "\"B\"", "Integer 클래스입니다.");
                        ReflectionTestUtils.setField(mcQuestion, "section", testSection);

                        ContinuousAnswerRequest request = createAnswerRequest("B", 2000L);

                        given(learningRepository.findById(17L))
                                        .willReturn(Optional.of(mcQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(17L), eq(true), anyLong()))
                                        .willReturn(testReviewItem);

                        // when
                        ContinuousAnswerResponse response = continuousQuizService.submitAnswer(
                                        TEST_USER_ID, 17L, request);

                        // then
                        assertThat(response.isCorrect()).isTrue();
                        assertThat(response.getCorrectAnswer()).isEqualTo("B"); // 정규화된 값 반환
                }
        }

        // ══════════════════════════════════════════════════════
        // getNextQuestion 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("getNextQuestion 메서드는")
        class GetNextQuestion {

                @Test
                @DisplayName("다음 문제를 정상적으로 반환한다")
                void shouldReturnNextQuestion() {
                        // given
                        given(learningRepository.findNextQuestionProbabilisticNoExclude(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID))
                                        .willReturn(Optional.of(testQuestion));

                        // when
                        ContinuousQuestionResponse response = continuousQuizService.getNextQuestion(
                                        TEST_USER_ID, TEST_COURSE_ID, TEST_SECTION_NUMBER);

                        // then
                        assertThat(response).isNotNull();
                        assertThat(response.getQuestionId()).isEqualTo(1L);
                        assertThat(response.getQuestionText()).isEqualTo("Java에서 정수형 키워드는?");
                        assertThat(response.getQuestionType()).isEqualTo(QuestionType.SHORT_ANSWER);
                }

                @Test
                @DisplayName("섹션에 문제가 없으면 예외를 던진다")
                void shouldThrowExceptionWhenNoQuestionsAvailable() {
                        // given
                        given(learningRepository.findNextQuestionProbabilisticNoExclude(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> continuousQuizService.getNextQuestion(
                                        TEST_USER_ID, TEST_COURSE_ID, TEST_SECTION_NUMBER))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("문제가 없습니다");
                }
        }

        // ══════════════════════════════════════════════════════
        // processAnswerAndGetNext 테스트 (Atomic API)
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("processAnswerAndGetNext 메서드는")
        class ProcessAnswerAndGetNext {

                @Test
                @DisplayName("정답 제출 결과와 다음 문제를 한 번에 반환한다")
                void shouldReturnBothResultAndNextQuestion() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("int", 2000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID), any(), eq(1L), eq(true), eq(2000L)))
                                        .willReturn(testReviewItem);
                        given(learningRepository.findNextQuestionProbabilistic(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID, 1L))
                                        .willReturn(Optional.of(nextQuestion));

                        // when
                        ContinuousSubmitResponse response = continuousQuizService.processAnswerAndGetNext(
                                        TEST_USER_ID, 1L, request);

                        // then: 제출 결과 검증
                        assertThat(response.getSubmittedQuestionId()).isEqualTo(1L);
                        assertThat(response.isCorrect()).isTrue();
                        assertThat(response.getUserAnswer()).isEqualTo("int");
                        assertThat(response.getCorrectAnswer()).isEqualTo("int");

                        // FSRS 갱신 정보 검증
                        assertThat(response.getStability()).isEqualTo(testReviewItem.getStability());
                        assertThat(response.getDifficulty()).isEqualTo(testReviewItem.getDifficulty());

                        // 다음 문제 검증
                        assertThat(response.getNextQuestion()).isNotNull();
                        assertThat(response.getNextQuestion().getQuestionId()).isEqualTo(2L);
                        assertThat(response.getNextQuestion().getQuestionText()).isEqualTo("Java에서 문자열 클래스는?");
                }

                @Test
                @DisplayName("방금 푼 문제를 제외하고 다음 문제를 선택한다")
                void shouldExcludeCurrentQuestionWhenSelectingNext() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("int", 2000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(anyLong(), any(), anyLong(), anyBoolean(), anyLong()))
                                        .willReturn(testReviewItem);
                        given(learningRepository.findNextQuestionProbabilistic(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID, 1L // excludeId = 1L
                        )).willReturn(Optional.of(nextQuestion));

                        // when
                        continuousQuizService.processAnswerAndGetNext(TEST_USER_ID, 1L, request);

                        // then: excludeId가 현재 문제 ID(1L)로 전달되었는지 검증
                        verify(learningRepository).findNextQuestionProbabilistic(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID, 1L);
                }

                @Test
                @DisplayName("섹션에 문제가 1개뿐이면 같은 문제를 재출제한다 (무한 루프)")
                void shouldReisseSameQuestionWhenOnlyOneQuestionInSection() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("int", 2000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(anyLong(), any(), anyLong(), anyBoolean(), anyLong()))
                                        .willReturn(testReviewItem);
                        // 첫 번째 조회 (excludeId=1L): 결과 없음
                        given(learningRepository.findNextQuestionProbabilistic(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID, 1L))
                                        .willReturn(Optional.empty());
                        // 두 번째 조회 (excludeId 없음): 같은 문제 반환
                        given(learningRepository.findNextQuestionProbabilisticNoExclude(
                                        TEST_COURSE_ID, TEST_SECTION_NUMBER, TEST_USER_ID))
                                        .willReturn(Optional.of(testQuestion));

                        // when
                        ContinuousSubmitResponse response = continuousQuizService.processAnswerAndGetNext(
                                        TEST_USER_ID, 1L, request);

                        // then: 같은 문제가 다음 문제로 반환됨
                        assertThat(response.getNextQuestion()).isNotNull();
                        assertThat(response.getNextQuestion().getQuestionId()).isEqualTo(1L);
                }

                @Test
                @DisplayName("섹션에 문제가 전혀 없으면 nextQuestion이 null이다")
                void shouldReturnNullNextQuestionWhenNoQuestionsInSection() {
                        // given
                        ContinuousAnswerRequest request = createAnswerRequest("int", 2000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(anyLong(), any(), anyLong(), anyBoolean(), anyLong()))
                                        .willReturn(testReviewItem);
                        given(learningRepository.findNextQuestionProbabilistic(
                                        anyLong(), anyInt(), anyLong(), anyLong())).willReturn(Optional.empty());
                        given(learningRepository.findNextQuestionProbabilisticNoExclude(
                                        anyLong(), anyInt(), anyLong())).willReturn(Optional.empty());

                        // when
                        ContinuousSubmitResponse response = continuousQuizService.processAnswerAndGetNext(
                                        TEST_USER_ID, 1L, request);

                        // then
                        assertThat(response.getNextQuestion()).isNull();
                }
        }

        // ══════════════════════════════════════════════════════
        // 자동 Rating 로직 검증 (FsrsService 위임)
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("자동 Rating 로직 검증 (FsrsService 호출)")
        class AutomaticRatingLogic {

                @Test
                @DisplayName("1500ms 응답 시 Easy Rating(4)으로 처리된다")
                void shouldProcessAsEasyRatingFor1500ms() {
                        // given: 1500ms ≤ 2000ms → Easy
                        ContinuousAnswerRequest request = createAnswerRequest("int", 1500L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true),
                                        eq(1500L))).willReturn(testReviewItem);

                        // when
                        continuousQuizService.submitAnswer(TEST_USER_ID, 1L, request);

                        // then: FsrsService가 1500ms로 호출되었는지 검증
                        // FsrsService.calculateRating(true, 1500) → 4 (Easy)
                        ArgumentCaptor<Long> responseTimeCaptor = ArgumentCaptor.forClass(Long.class);
                        verify(fsrsService).processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true),
                                        responseTimeCaptor.capture());
                        assertThat(responseTimeCaptor.getValue()).isEqualTo(1500L);
                }

                @Test
                @DisplayName("6000ms 응답 시 Hard Rating(2)으로 처리된다")
                void shouldProcessAsHardRatingFor6000ms() {
                        // given: 6000ms > 5000ms → Hard
                        ContinuousAnswerRequest request = createAnswerRequest("int", 6000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true),
                                        eq(6000L))).willReturn(testReviewItem);

                        // when
                        continuousQuizService.submitAnswer(TEST_USER_ID, 1L, request);

                        // then: FsrsService가 6000ms로 호출되었는지 검증
                        // FsrsService.calculateRating(true, 6000) → 2 (Hard)
                        ArgumentCaptor<Long> responseTimeCaptor = ArgumentCaptor.forClass(Long.class);
                        verify(fsrsService).processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true),
                                        responseTimeCaptor.capture());
                        assertThat(responseTimeCaptor.getValue()).isEqualTo(6000L);
                }

                @Test
                @DisplayName("3500ms 응답 시 Good Rating(3)으로 처리된다")
                void shouldProcessAsGoodRatingFor3500ms() {
                        // given: 2000 < 3500ms ≤ 5000 → Good
                        ContinuousAnswerRequest request = createAnswerRequest("int", 3500L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(true),
                                        eq(3500L))).willReturn(testReviewItem);

                        // when
                        continuousQuizService.submitAnswer(TEST_USER_ID, 1L, request);

                        // then
                        verify(fsrsService).processReviewResult(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        1L,
                                        true,
                                        3500L);
                }

                @Test
                @DisplayName("오답 시 Again Rating(1)으로 처리된다")
                void shouldProcessAsAgainRatingForIncorrectAnswer() {
                        // given: 오답 → Again
                        ContinuousAnswerRequest request = createAnswerRequest("wrong", 1000L);

                        given(learningRepository.findById(1L))
                                        .willReturn(Optional.of(testQuestion));
                        given(fsrsService.processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        eq(false), // 오답
                                        eq(1000L))).willReturn(testReviewItem);

                        // when
                        continuousQuizService.submitAnswer(TEST_USER_ID, 1L, request);

                        // then: isCorrect=false로 호출되었는지 검증
                        ArgumentCaptor<Boolean> isCorrectCaptor = ArgumentCaptor.forClass(Boolean.class);
                        verify(fsrsService).processReviewResult(
                                        eq(TEST_USER_ID),
                                        eq(ReviewContentType.COURSE_QUESTION),
                                        eq(1L),
                                        isCorrectCaptor.capture(),
                                        eq(1000L));
                        assertThat(isCorrectCaptor.getValue()).isFalse();
                }
        }

        // ══════════════════════════════════════════════════════
        // Helper Methods
        // ══════════════════════════════════════════════════════

        private QuizCourse createQuizCourse(Long id, String code, String name) {
                try {
                        Constructor<QuizCourse> constructor = QuizCourse.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourse course = constructor.newInstance();
                        ReflectionTestUtils.setField(course, "id", id);
                        ReflectionTestUtils.setField(course, "code", code);
                        ReflectionTestUtils.setField(course, "name", name);
                        ReflectionTestUtils.setField(course, "isActive", true);
                        return course;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourse 테스트 객체 생성 실패", e);
                }
        }

        private QuizCourseSection createQuizCourseSection(Long courseId, Integer sectionNumber, String name) {
                try {
                        Constructor<QuizCourseSection> constructor = QuizCourseSection.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourseSection section = constructor.newInstance();
                        ReflectionTestUtils.setField(section, "quizCourseId", courseId);
                        ReflectionTestUtils.setField(section, "sectionNumber", sectionNumber);
                        ReflectionTestUtils.setField(section, "name", name);
                        return section;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourseSection 테스트 객체 생성 실패", e);
                }
        }

        private QuizCourseQuestion createQuizCourseQuestion(
                        Long id, Integer questionNumber, String questionText,
                        QuestionType questionType, String options, String correctAnswer, String explanation) {
                try {
                        Constructor<QuizCourseQuestion> constructor = QuizCourseQuestion.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourseQuestion question = constructor.newInstance();
                        ReflectionTestUtils.setField(question, "id", id);
                        ReflectionTestUtils.setField(question, "questionNumber", questionNumber);
                        ReflectionTestUtils.setField(question, "questionText", questionText);
                        ReflectionTestUtils.setField(question, "questionType", questionType);
                        ReflectionTestUtils.setField(question, "options", options);
                        ReflectionTestUtils.setField(question, "correctAnswer", correctAnswer);
                        ReflectionTestUtils.setField(question, "explanation", explanation);
                        return question;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourseQuestion 테스트 객체 생성 실패", e);
                }
        }

        private UserReviewItem createUserReviewItem(Long userId, Long contentId) {
                return UserReviewItem.builder()
                                .id(1L)
                                .userId(userId)
                                .contentType(ReviewContentType.COURSE_QUESTION)
                                .contentId(contentId)
                                .stability(2.5)
                                .difficulty(5.0)
                                .scheduledMinutes(3 * 1440)
                                .nextReviewAt(LocalDateTime.now().plusDays(3))
                                .state(2) // STATE_REVIEW
                                .reps(5)
                                .lapses(0)
                                .build();
        }

        /**
         * ContinuousAnswerRequest 생성 헬퍼 메서드
         * (NoArgsConstructor만 있어서 ReflectionTestUtils 사용)
         */
        private ContinuousAnswerRequest createAnswerRequest(String userAnswer, Long responseTimeMs) {
                ContinuousAnswerRequest request = new ContinuousAnswerRequest();
                ReflectionTestUtils.setField(request, "userAnswer", userAnswer);
                ReflectionTestUtils.setField(request, "responseTimeMs", responseTimeMs);
                return request;
        }
}
