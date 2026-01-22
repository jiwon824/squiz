package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.SectionLockedException;
import com.ssafy.domain.gamification.entity.Badge;
import com.ssafy.domain.gamification.entity.BadgeCategory;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.response.BadgeInfo;
import com.ssafy.domain.quiz.dto.response.QuizCourseDetailResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseListItem;
import com.ssafy.domain.quiz.dto.response.QuizCourseListResponse;
import com.ssafy.domain.quiz.dto.response.SectionQuestionsResponse;
import com.ssafy.domain.quiz.dto.response.SectionSummary;
import com.ssafy.domain.quiz.entity.QuizCourse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.UserCourseProgress;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.QuizCourseRepository;
import com.ssafy.domain.quiz.repository.QuizCourseSectionRepository;
import com.ssafy.domain.quiz.repository.UserCourseProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * QuizCourseService 단위 테스트.
 *
 * 테스트 대상: {@link QuizCourseService#getCourseList()},
 * {@link QuizCourseService#getCourseDetail(Long)}
 */
@ExtendWith(MockitoExtension.class)
class QuizCourseServiceTest {

        @InjectMocks
        private QuizCourseService quizCourseService;

        @Mock
        private QuizCourseRepository quizCourseRepository;

        @Mock
        private QuizCourseSectionRepository quizCourseSectionRepository;

        @Mock
        private UserCourseProgressRepository userCourseProgressRepository;

        @Mock
        private BadgeRepository badgeRepository;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @Nested
        @DisplayName("getCourseList 메서드는")
        class GetCourseList {

                @Test
                @DisplayName("활성 코스가 없으면 빈 목록을 반환한다")
                void returnEmptyListWhenNoActiveCourses() {
                        // given
                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(Collections.emptyList());

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response).isNotNull();
                        assertThat(response.courses()).isEmpty();
                        verify(quizCourseRepository).findAllByIsActiveTrueOrderBySortOrderAscIdAsc();
                }

                @Test
                @DisplayName("코스가 있고 배지 코드가 있으면 배지 이름을 포함해 반환한다")
                void returnCoursesWithBadgeName() {
                        // given
                        QuizCourse javaCourse = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        Badge javaBadge = createBadge(1L, "JAVA_MASTER", "Java 마스터");

                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(javaCourse));
                        given(badgeRepository.findByCodeIn(List.of("JAVA_MASTER")))
                                        .willReturn(List.of(javaBadge));

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response.courses()).hasSize(1);
                        QuizCourseListItem item = response.courses().get(0);
                        assertThat(item.id()).isEqualTo(1L);
                        assertThat(item.code()).isEqualTo("JAVA");
                        assertThat(item.name()).isEqualTo("Java 마스터");
                        assertThat(item.description()).isEqualTo("Java 기초부터 고급까지");
                        assertThat(item.totalSections()).isEqualTo(5);
                        assertThat(item.badgeCode()).isEqualTo("JAVA_MASTER");
                        assertThat(item.badgeName()).isEqualTo("Java 마스터");
                }

                @Test
                @DisplayName("배지 코드가 null인 코스는 배지 이름도 null로 반환한다")
                void returnNullBadgeNameWhenBadgeCodeIsNull() {
                        // given
                        QuizCourse courseWithNoBadge = createQuizCourse(1L, "TEST", "테스트 코스",
                                        "테스트 설명", 3, null, 0);

                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(courseWithNoBadge));

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response.courses()).hasSize(1);
                        QuizCourseListItem item = response.courses().get(0);
                        assertThat(item.badgeCode()).isNull();
                        assertThat(item.badgeName()).isNull();
                }

                @Test
                @DisplayName("여러 코스가 정렬 순서대로 반환된다")
                void returnCoursesInSortOrder() {
                        // given
                        QuizCourse course1 = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 설명", 5, "JAVA_MASTER", 1);
                        QuizCourse course2 = createQuizCourse(2L, "PYTHON", "Python 기초",
                                        "Python 설명", 4, "PYTHON_MASTER", 2);
                        QuizCourse course3 = createQuizCourse(3L, "CS_BASIC", "CS 기초",
                                        "CS 설명", 6, "CS_MASTER", 3);

                        Badge javaBadge = createBadge(1L, "JAVA_MASTER", "Java 마스터");
                        Badge pythonBadge = createBadge(2L, "PYTHON_MASTER", "Python 마스터");
                        Badge csBadge = createBadge(3L, "CS_MASTER", "CS 마스터");

                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(course1, course2, course3));
                        given(badgeRepository.findByCodeIn(List.of("JAVA_MASTER", "PYTHON_MASTER", "CS_MASTER")))
                                        .willReturn(List.of(javaBadge, pythonBadge, csBadge));

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response.courses()).hasSize(3);
                        assertThat(response.courses().get(0).code()).isEqualTo("JAVA");
                        assertThat(response.courses().get(1).code()).isEqualTo("PYTHON");
                        assertThat(response.courses().get(2).code()).isEqualTo("CS_BASIC");
                }

                @Test
                @DisplayName("일부 코스만 배지 코드가 있으면 해당 코스만 배지 이름이 있다")
                void returnMixedBadgeNames() {
                        // given
                        QuizCourse courseWithBadge = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 설명", 5, "JAVA_MASTER", 1);
                        QuizCourse courseWithoutBadge = createQuizCourse(2L, "INTRO", "입문 코스",
                                        "입문 설명", 2, null, 2);

                        Badge javaBadge = createBadge(1L, "JAVA_MASTER", "Java 마스터");

                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(courseWithBadge, courseWithoutBadge));
                        given(badgeRepository.findByCodeIn(List.of("JAVA_MASTER")))
                                        .willReturn(List.of(javaBadge));

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response.courses()).hasSize(2);
                        assertThat(response.courses().get(0).badgeName()).isEqualTo("Java 마스터");
                        assertThat(response.courses().get(1).badgeName()).isNull();
                }

                @Test
                @DisplayName("동일한 배지 코드를 가진 코스가 여러 개여도 정상 처리된다")
                void handleDuplicateBadgeCodes() {
                        // given
                        QuizCourse course1 = createQuizCourse(1L, "JAVA_BASIC", "Java 기초",
                                        "Java 기초 설명", 3, "JAVA_MASTER", 1);
                        QuizCourse course2 = createQuizCourse(2L, "JAVA_ADVANCED", "Java 고급",
                                        "Java 고급 설명", 5, "JAVA_MASTER", 2);

                        Badge javaBadge = createBadge(1L, "JAVA_MASTER", "Java 마스터");

                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(course1, course2));
                        given(badgeRepository.findByCodeIn(List.of("JAVA_MASTER")))
                                        .willReturn(List.of(javaBadge));

                        // when
                        QuizCourseListResponse response = quizCourseService.getCourseList();

                        // then
                        assertThat(response.courses()).hasSize(2);
                        assertThat(response.courses().get(0).badgeName()).isEqualTo("Java 마스터");
                        assertThat(response.courses().get(1).badgeName()).isEqualTo("Java 마스터");
                }
        }

        @Nested
        @DisplayName("getCourseDetail 메서드는")
        class GetCourseDetail {

                @Test
                @DisplayName("코스 상세를 섹션과 배지 정보까지 포함해 반환한다")
                void returnCourseDetailWithSectionsAndBadge() {
                        // given
                        QuizCourse course = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);
                        QuizCourseSection section1 = createQuizCourseSection(1L, 1, "기본 문법",
                                        "변수, 자료형, 연산자", 10, 70);
                        QuizCourseSection section2 = createQuizCourseSection(2L, 2, "객체지향",
                                        "클래스, 상속, 다형성", 15, 70);
                        ReflectionTestUtils.setField(course, "sections", List.of(section1, section2));

                        Badge badge = createBadge(1L, "JAVA_MASTER", "Java 마스터");

                        given(quizCourseRepository.findByIdWithSections(1L))
                                        .willReturn(Optional.of(course));

                        // "JAVA_MASTER"라는 코드로 findByCode가 호출되면,
                        // 비어있지 않은 Optional<Badge> 객체를 반환하라고 설정
                        given(badgeRepository.findByCode("JAVA_MASTER"))
                                        .willReturn(Optional.of(badge));

                        // when
                        QuizCourseDetailResponse response = quizCourseService.getCourseDetail(1L);

                        // then
                        assertThat(response.id()).isEqualTo(1L);
                        assertThat(response.code()).isEqualTo("JAVA");
                        assertThat(response.name()).isEqualTo("Java 마스터");
                        assertThat(response.description()).isEqualTo("Java 기초부터 고급까지");
                        assertThat(response.totalSections()).isEqualTo(5);
                        assertThat(response.badge()).isEqualTo(new BadgeInfo("JAVA_MASTER", "Java 마스터", "테스트 배지 설명"));
                        assertThat(response.sections()).hasSize(2);
                        SectionSummary firstSection = response.sections().get(0);
                        assertThat(firstSection.sectionNumber()).isEqualTo(1);
                        assertThat(firstSection.name()).isEqualTo("기본 문법");
                        assertThat(firstSection.description()).isEqualTo("변수, 자료형, 연산자");
                        assertThat(firstSection.totalQuestions()).isEqualTo(10);
                        assertThat(firstSection.passScore()).isEqualTo(70);
                }

                @Test
                @DisplayName("코스가 없으면 404 예외를 던진다")
                void throwNotFoundWhenCourseMissing() {
                        // given
                        given(quizCourseRepository.findByIdWithSections(99L))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> quizCourseService.getCourseDetail(99L))
                                        .isInstanceOf(ResponseStatusException.class)
                                        .hasMessageContaining(String.valueOf(HttpStatus.NOT_FOUND.value()));
                }

                @Test
                @DisplayName("비활성 코스는 404 예외를 던진다")
                void throwNotFoundWhenCourseInactive() {
                        // given
                        QuizCourse course = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);
                        ReflectionTestUtils.setField(course, "isActive", false);

                        given(quizCourseRepository.findByIdWithSections(1L))
                                        .willReturn(Optional.of(course));

                        // when & then
                        assertThatThrownBy(() -> quizCourseService.getCourseDetail(1L))
                                        .isInstanceOf(ResponseStatusException.class)
                                        .hasMessageContaining(String.valueOf(HttpStatus.NOT_FOUND.value()));
                }

                @Test
                @DisplayName("배지 코드만 있고 배지가 없으면 코드만 반환한다")
                void returnBadgeCodeOnlyWhenBadgeMissing() {
                        // given
                        QuizCourse course = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);
                        ReflectionTestUtils.setField(course, "sections", Collections.emptyList());

                        given(quizCourseRepository.findByIdWithSections(1L))
                                        .willReturn(Optional.of(course));
                        given(badgeRepository.findByCode("JAVA_MASTER"))
                                        .willReturn(Optional.empty());

                        // when
                        QuizCourseDetailResponse response = quizCourseService.getCourseDetail(1L);

                        // then
                        assertThat(response.badge()).isEqualTo(new BadgeInfo("JAVA_MASTER", null, null));
                }

                @Test
                @DisplayName("섹션이 없는 코스도 빈 배열로 정상 반환한다")
                void returnEmptySectionsWhenNoSections() {
                        // given
                        QuizCourse course = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);
                        ReflectionTestUtils.setField(course, "sections", Collections.emptyList());

                        Badge badge = createBadge(1L, "JAVA_MASTER", "Java 마스터");

                        given(quizCourseRepository.findByIdWithSections(1L))
                                        .willReturn(Optional.of(course));
                        given(badgeRepository.findByCode("JAVA_MASTER"))
                                        .willReturn(Optional.of(badge));

                        // when
                        QuizCourseDetailResponse response = quizCourseService.getCourseDetail(1L);

                        // then
                        assertThat(response.sections()).isEmpty();
                }

                @Test
                @DisplayName("배지 코드가 없으면 badge는 null로 반환한다")
                void returnNullBadgeWhenNoBadgeCode() {
                        // given
                        QuizCourse course = createQuizCourse(1L, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, null, 0);
                        ReflectionTestUtils.setField(course, "sections", Collections.emptyList());

                        given(quizCourseRepository.findByIdWithSections(1L))
                                        .willReturn(Optional.of(course));

                        // when
                        QuizCourseDetailResponse response = quizCourseService.getCourseDetail(1L);

                        // then
                        assertThat(response.badge()).isNull();
                }
        }

        private QuizCourse createQuizCourse(Long id, String code, String name,
                        String description, Integer totalSections, String badgeCode, Integer sortOrder) {
                try {
                        Constructor<QuizCourse> constructor = QuizCourse.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourse course = constructor.newInstance();
                        ReflectionTestUtils.setField(course, "id", id);
                        ReflectionTestUtils.setField(course, "code", code);
                        ReflectionTestUtils.setField(course, "name", name);
                        ReflectionTestUtils.setField(course, "description", description);
                        ReflectionTestUtils.setField(course, "totalSections", totalSections);
                        ReflectionTestUtils.setField(course, "badgeCode", badgeCode);
                        ReflectionTestUtils.setField(course, "sortOrder", sortOrder);
                        ReflectionTestUtils.setField(course, "isActive", true);
                        return course;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourse 테스트 객체 생성 실패", e);
                }
        }

        private Badge createBadge(Long id, String code, String name) {
                try {
                        Constructor<Badge> constructor = Badge.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        Badge badge = constructor.newInstance();
                        ReflectionTestUtils.setField(badge, "id", id);
                        ReflectionTestUtils.setField(badge, "code", code);
                        ReflectionTestUtils.setField(badge, "name", name);
                        ReflectionTestUtils.setField(badge, "description", "테스트 배지 설명");
                        ReflectionTestUtils.setField(badge, "category", BadgeCategory.MASTER);
                        ReflectionTestUtils.setField(badge, "conditionType", "COURSE_COMPLETE");
                        ReflectionTestUtils.setField(badge, "conditionValue", 1);
                        return badge;
                } catch (Exception e) {
                        throw new RuntimeException("Badge 테스트 객체 생성 실패", e);
                }
        }

        private QuizCourseSection createQuizCourseSection(Long quizCourseId, Integer sectionNumber, String name,
                        String description, Integer totalQuestions, Integer passScore) {
                try {
                        Constructor<QuizCourseSection> constructor = QuizCourseSection.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourseSection section = constructor.newInstance();

                        // 이 부분 quizCourseId로 바꿨는데, id로 남아 있어서(BaseEntity 상속 받았다가 안 받도록 수정했었음) 테스트 오류가
                        // 났음
                        ReflectionTestUtils.setField(section, "quizCourseId", quizCourseId);
                        ReflectionTestUtils.setField(section, "sectionNumber", sectionNumber);
                        ReflectionTestUtils.setField(section, "name", name);
                        ReflectionTestUtils.setField(section, "description", description);
                        ReflectionTestUtils.setField(section, "totalQuestions", totalQuestions);
                        ReflectionTestUtils.setField(section, "passScore", passScore);
                        return section;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourseSection 테스트 객체 생성 실패", e);
                }
        }

        private QuizCourseSection createQuizCourseSectionWithCourseAndQuestions(
                        QuizCourse course, Integer sectionNumber, String name,
                        String description, Integer totalQuestions, Integer passScore,
                        List<QuizCourseQuestion> questions) {
                try {
                        Constructor<QuizCourseSection> constructor = QuizCourseSection.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        QuizCourseSection section = constructor.newInstance();

                        ReflectionTestUtils.setField(section, "quizCourseId", course.getId());
                        ReflectionTestUtils.setField(section, "sectionNumber", sectionNumber);
                        ReflectionTestUtils.setField(section, "name", name);
                        ReflectionTestUtils.setField(section, "description", description);
                        ReflectionTestUtils.setField(section, "totalQuestions", totalQuestions);
                        ReflectionTestUtils.setField(section, "passScore", passScore);
                        ReflectionTestUtils.setField(section, "course", course);
                        ReflectionTestUtils.setField(section, "questions", questions);
                        return section;
                } catch (Exception e) {
                        throw new RuntimeException("QuizCourseSection 테스트 객체 생성 실패", e);
                }
        }

        private QuizCourseQuestion createQuizCourseQuestion(Long id, Integer questionNumber, String questionText,
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

        private UserCourseProgress createUserCourseProgress(Long userId, Long courseId, Integer currentSection) {
                try {
                        Constructor<UserCourseProgress> constructor = UserCourseProgress.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        UserCourseProgress progress = constructor.newInstance();

                        ReflectionTestUtils.setField(progress, "userId", userId);
                        ReflectionTestUtils.setField(progress, "courseId", courseId);
                        ReflectionTestUtils.setField(progress, "currentSection", currentSection);
                        return progress;
                } catch (Exception e) {
                        throw new RuntimeException("UserCourseProgress 테스트 객체 생성 실패", e);
                }
        }

        @Nested
        @DisplayName("getSectionQuestions 메서드는")
        class GetSectionQuestions {

                @Test
                @DisplayName("해금된 섹션의 문제 목록을 반환한다")
                void returnQuestionsForUnlockedSection() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        String optionsJson = "[{\"id\":\"A\",\"text\":\"integer\"},{\"id\":\"B\",\"text\":\"int\"},{\"id\":\"C\",\"text\":\"num\"},{\"id\":\"D\",\"text\":\"number\"}]";
                        QuizCourseQuestion question1 = createQuizCourseQuestion(
                                        1L, 1, "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?",
                                        QuestionType.MULTIPLE_CHOICE, optionsJson, "B", "Java에서 정수형은 int 키워드를 사용합니다.");

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "기본 문법", "변수, 자료형, 연산자",
                                        10, 70, List.of(question1));

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));

                        // when
                        SectionQuestionsResponse response = quizCourseService.getSectionQuestions(courseId,
                                        sectionNumber,
                                        userId);

                        // then
                        assertThat(response.sectionNumber()).isEqualTo(1);
                        assertThat(response.sectionName()).isEqualTo("기본 문법");
                        assertThat(response.totalQuestions()).isEqualTo(10);
                        assertThat(response.passScore()).isEqualTo(70);
                        assertThat(response.questions()).hasSize(1);
                        assertThat(response.questions().get(0).questionNumber()).isEqualTo(1);
                        assertThat(response.questions().get(0).questionText())
                                        .isEqualTo("Java에서 정수형 변수를 선언할 때 사용하는 키워드는?");
                        assertThat(response.questions().get(0).questionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
                        assertThat(response.questions().get(0).options()).hasSize(4);
                }

                @Test
                @DisplayName("첫 번째 섹션은 항상 해금되어 접근 가능하다")
                void firstSectionAlwaysUnlocked() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "기본 문법", "변수, 자료형, 연산자",
                                        10, 70, Collections.emptyList());

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));

                        // when
                        SectionQuestionsResponse response = quizCourseService.getSectionQuestions(courseId,
                                        sectionNumber,
                                        userId);

                        // then
                        assertThat(response.sectionNumber()).isEqualTo(1);
                }

                @Test
                @DisplayName("잠긴 섹션에 접근하면 SectionLockedException을 던진다")
                void throwSectionLockedExceptionWhenLocked() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 3; // 3번째 섹션 접근 시도
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "컬렉션", "List, Map, Set",
                                        12, 70, Collections.emptyList());

                        UserCourseProgress progress = createUserCourseProgress(userId, courseId, 2); // 현재 2번 섹션까지만 해금

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));
                        given(userCourseProgressRepository.findByUserIdAndCourseId(userId, courseId))
                                        .willReturn(Optional.of(progress));

                        // when & then
                        assertThatThrownBy(() -> quizCourseService.getSectionQuestions(courseId, sectionNumber, userId))
                                        .isInstanceOf(SectionLockedException.class);
                }

                @Test
                @DisplayName("존재하지 않는 섹션에 접근하면 NotFoundException을 던진다")
                void throwNotFoundExceptionWhenSectionMissing() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 99;
                        Long userId = 100L;

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> quizCourseService.getSectionQuestions(courseId, sectionNumber, userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("비활성 코스의 섹션에 접근하면 NotFoundException을 던진다")
                void throwNotFoundExceptionWhenCourseInactive() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);
                        ReflectionTestUtils.setField(course, "isActive", false);

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "기본 문법", "변수, 자료형, 연산자",
                                        10, 70, Collections.emptyList());

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));

                        // when & then
                        assertThatThrownBy(() -> quizCourseService.getSectionQuestions(courseId, sectionNumber, userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("단답형 문제는 빈 옵션 목록을 반환한다")
                void returnEmptyOptionsForShortAnswerQuestion() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        QuizCourseQuestion question = createQuizCourseQuestion(
                                        1L, 1, "Java의 기본 정수형 자료형은?",
                                        QuestionType.SHORT_ANSWER, null, "int", "Java의 기본 정수형은 int입니다.");

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "기본 문법", "변수, 자료형, 연산자",
                                        1, 70, List.of(question));

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));

                        // when
                        SectionQuestionsResponse response = quizCourseService.getSectionQuestions(courseId,
                                        sectionNumber,
                                        userId);

                        // then
                        assertThat(response.questions()).hasSize(1);
                        assertThat(response.questions().get(0).questionType()).isEqualTo(QuestionType.SHORT_ANSWER);
                        assertThat(response.questions().get(0).options()).isEmpty();
                }

                @Test
                @DisplayName("해금된 이전 섹션도 다시 접근 가능하다")
                void previousUnlockedSectionAccessible() {
                        // given
                        Long courseId = 1L;
                        Integer sectionNumber = 2; // 2번째 섹션 접근
                        Long userId = 100L;

                        QuizCourse course = createQuizCourse(courseId, "JAVA", "Java 마스터",
                                        "Java 기초부터 고급까지", 5, "JAVA_MASTER", 0);

                        QuizCourseSection section = createQuizCourseSectionWithCourseAndQuestions(
                                        course, sectionNumber, "객체지향", "클래스, 상속, 다형성",
                                        15, 70, Collections.emptyList());

                        UserCourseProgress progress = createUserCourseProgress(userId, courseId, 3); // 현재 3번 섹션까지 해금

                        given(quizCourseSectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section));
                        given(userCourseProgressRepository.findByUserIdAndCourseId(userId, courseId))
                                        .willReturn(Optional.of(progress));

                        // when
                        SectionQuestionsResponse response = quizCourseService.getSectionQuestions(courseId,
                                        sectionNumber,
                                        userId);

                        // then
                        assertThat(response.sectionNumber()).isEqualTo(2);
                        assertThat(response.sectionName()).isEqualTo("객체지향");
                }
        }
}
