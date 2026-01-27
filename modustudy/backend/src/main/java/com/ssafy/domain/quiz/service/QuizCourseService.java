package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.SectionLockedException;
import com.ssafy.domain.gamification.entity.Badge;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.response.BadgeInfo;
import com.ssafy.domain.quiz.dto.response.MyProgressDto;
import com.ssafy.domain.quiz.dto.response.OptionItem;
import com.ssafy.domain.quiz.dto.response.QuestionItem;
import com.ssafy.domain.quiz.dto.response.QuizCourseDetailResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseListItem;
import com.ssafy.domain.quiz.dto.response.QuizCourseListResponse;
import com.ssafy.domain.quiz.dto.response.SectionQuestionsResponse;
import com.ssafy.domain.quiz.dto.response.SectionSummary;
import com.ssafy.domain.quiz.dto.response.SectionWithProgressDto;
import com.ssafy.domain.quiz.dto.response.SectionsWithProgressResponse;
import com.ssafy.domain.quiz.entity.QuizCourse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.UserSectionAttempt;
import com.ssafy.domain.quiz.repository.QuizCourseRepository;
import com.ssafy.domain.quiz.repository.QuizCourseSectionRepository;
import com.ssafy.domain.quiz.repository.UserCourseProgressRepository;
import com.ssafy.domain.quiz.repository.UserSectionAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * QuizCourseService - 조회(읽기) 중심
 * 퀴즈 코스 조회 비즈니스 로직.
 *
 * 호출자: {@link com.ssafy.domain.quiz.controller.QuizCourseController}
 *
 * 책임:
 * - 활성 코스 목록 조회
 * - 코스에 연결된 배지 코드로 배지 이름 조합
 * - 섹션 문제 조회 (잠금 상태 확인)
 * - 섹션 목록 조회 (진행 상황 포함)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizCourseService {

        private final QuizCourseRepository quizCourseRepository;
        private final QuizCourseSectionRepository quizCourseSectionRepository;
        private final UserCourseProgressRepository userCourseProgressRepository;
        private final UserSectionAttemptRepository userSectionAttemptRepository;
        private final BadgeRepository badgeRepository;
        private final ObjectMapper objectMapper;

        /**
         * 활성 코스 목록을 반환한다.
         *
         * 정렬 기준: sortOrder 오름차순, 동일 시 id 오름차순.
         * 배지 코드는 코스에 저장된 값을 사용하고, 배지명은 Badge 테이블에서 조회해 합성한다.
         *
         * @return 코스 목록 응답 DTO
         */
        public QuizCourseListResponse getCourseList() {
                List<QuizCourse> courses = quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc();
                List<String> badgeCodes = courses.stream()
                                .map(QuizCourse::getBadgeCode)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();

                Map<String, String> badgeNameByCode = badgeCodes.isEmpty()
                                ? Map.of()
                                : badgeRepository.findByCodeIn(badgeCodes).stream()
                                                .collect(Collectors.toMap(Badge::getCode, Badge::getName,
                                                                (first, ignored) -> first));

                List<QuizCourseListItem> items = courses.stream()
                                .map(course -> new QuizCourseListItem(
                                                course.getId(),
                                                course.getCode(),
                                                course.getName(),
                                                course.getDescription(),
                                                course.getTotalSections(),
                                                course.getBadgeCode(),
                                                course.getBadgeCode() == null ? null
                                                                : badgeNameByCode.get(course.getBadgeCode())))
                                .toList();

                return new QuizCourseListResponse(items);
        }

        /**
         * 코스 상세 정보를 반환한다.
         *
         * 코스 정보 + 섹션 목록 + 배지 정보를 조합한다.
         *
         * @param courseId 코스 ID
         * @return 코스 상세 응답 DTO
         */
        public QuizCourseDetailResponse getCourseDetail(Long courseId) {
                // 코스 + 섹션을 fetch join으로 조회 findByIdWithSections
                // 코스가 없으면 404로 응답 .orElseThrow
                QuizCourse course = quizCourseRepository.findByIdWithSections(courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz course not found"));

                // 비활성 코스는 공개하지 않으므로 404로 처리
                if (!Boolean.TRUE.equals(course.getIsActive())) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz course not found");
                }

                BadgeInfo badgeInfo = null; // 응답에 넣을 배지 정보를 준비(기본값은 null).
                String badgeCode = course.getBadgeCode(); // 코스에 매핑된 배지 코드가 있는지 확인

                // 배지 코드가 있으면 배지 테이블에서 상세 조회
                if (badgeCode != null) {
                        // code/name/description으로 DTO 생성 .map(람다)
                        // 배지가 없으면 code만 넣고 나머지는 null로 둔다 .orElseGet(람다)
                        badgeInfo = badgeRepository.findByCode(badgeCode)
                                        .map(badge -> new BadgeInfo(badge.getCode(), badge.getName(),
                                                        badge.getDescription()))
                                        .orElseGet(() -> new BadgeInfo(badgeCode, null, null));
                }

                // 섹션 엔티티 목록을 응답용 요약 DTO 목록으로 변환
                // 섹션 하나를 SectionSummary로 매핑 map()
                // List로 수집 .toList()
                List<SectionSummary> sections = course.getSections().stream()
                                .map(section -> toSectionSummary(section))
                                .toList();

                // 최종 응답 DTO를 생성해 반환
                return new QuizCourseDetailResponse(
                                course.getId(),
                                course.getCode(),
                                course.getName(),
                                course.getDescription(),
                                course.getTotalSections(),
                                badgeInfo,
                                sections);
        }

        private SectionSummary toSectionSummary(QuizCourseSection section) {
                return new SectionSummary(
                                section.getSectionNumber(),
                                section.getName(),
                                section.getDescription(),
                                section.getTotalQuestions(),
                                section.getPassScore());
        }

        /**
         * 섹션 목록과 사용자 진행 상황을 조회한다.
         *
         * <p>
         * 인증된 사용자의 섹션별 시도 기록을 조회하여 해금 상태, 통과 여부,
         * 최고 점수, 시도 횟수를 포함한 응답을 반환한다.
         * </p>
         *
         * <h3>해금 규칙</h3>
         * <ul>
         * <li>섹션 1: 항상 해금</li>
         * <li>섹션 N (N > 1): 이전 섹션(N-1)을 통과한 경우 해금</li>
         * </ul>
         *
         * @param courseId 코스 ID
         * @param userId   사용자 ID
         * @return 진행 상황이 포함된 섹션 목록 응답
         * @throws NotFoundException 코스를 찾을 수 없는 경우
         */
        public SectionsWithProgressResponse getSectionsWithProgress(Long courseId, Long userId) {
                // 1. 코스 + 섹션 조회 (fetch join)
                QuizCourse course = quizCourseRepository.findByIdWithSections(courseId)
                                .orElseThrow(NotFoundException::course);

                // 비활성 코스는 공개하지 않음
                if (!Boolean.TRUE.equals(course.getIsActive())) {
                        throw NotFoundException.course();
                }

                List<QuizCourseSection> sections = course.getSections();

                // 2. 사용자의 제출된 시도 기록 조회
                List<UserSectionAttempt> attempts = userSectionAttemptRepository
                                .findCompletedAttemptsByUserIdAndCourseId(userId, courseId);

                // 3. 섹션별 시도 요약 정보 구성 (Map 기반 O(n) 조회)
                Map<Integer, SectionAttemptSummary> summaryMap = buildAttemptSummaryMap(attempts);

                // 4. 섹션 DTO 목록 생성 (해금 로직 적용)
                List<SectionWithProgressDto> sectionDtos = buildSectionWithProgressDtos(sections, summaryMap);

                // 5. 전체 진행 상황 계산
                MyProgressDto myProgress = buildMyProgress(sectionDtos);

                return new SectionsWithProgressResponse(
                                courseId,
                                course.getName(),
                                myProgress,
                                sectionDtos);
        }

        /**
         * 섹션별 시도 요약 정보를 Map으로 구성한다.
         * 동일 섹션에 여러 시도가 있을 경우 최고 점수와 통과 여부를 집계한다.
         */
        private Map<Integer, SectionAttemptSummary> buildAttemptSummaryMap(List<UserSectionAttempt> attempts) {
                Map<Integer, SectionAttemptSummary> map = new java.util.HashMap<>();

                for (UserSectionAttempt attempt : attempts) {
                        Integer sectionNumber = attempt.getSection().getSectionNumber();
                        SectionAttemptSummary existing = map.get(sectionNumber);

                        if (existing == null) {
                                map.put(sectionNumber, new SectionAttemptSummary(
                                                attempt.getScore(),
                                                Boolean.TRUE.equals(attempt.getIsPassed()),
                                                1));
                        } else {
                                // 최고 점수 갱신 및 통과 여부 OR 연산
                                map.put(sectionNumber, new SectionAttemptSummary(
                                                Math.max(existing.bestScore(),
                                                                attempt.getScore() != null ? attempt.getScore() : 0),
                                                existing.isPassed() || Boolean.TRUE.equals(attempt.getIsPassed()),
                                                existing.attemptCount() + 1));
                        }
                }

                return map;
        }

        /**
         * 섹션 DTO 목록을 생성한다 (해금 로직 적용).
         * 규칙: 이전 섹션을 통과해야 다음 섹션이 해금됨.
         */
        private List<SectionWithProgressDto> buildSectionWithProgressDtos(
                        List<QuizCourseSection> sections,
                        Map<Integer, SectionAttemptSummary> summaryMap) {

                List<SectionWithProgressDto> result = new java.util.ArrayList<>();
                boolean previousPassed = true; // 첫 번째 섹션은 항상 해금

                for (QuizCourseSection section : sections) {
                        SectionAttemptSummary summary = summaryMap.get(section.getSectionNumber());

                        boolean isUnlocked = previousPassed;
                        boolean isPassed = summary != null && summary.isPassed();
                        Integer bestScore = summary != null ? summary.bestScore() : null;
                        int attemptCount = summary != null ? summary.attemptCount() : 0;

                        result.add(new SectionWithProgressDto(
                                        section.getSectionNumber(),
                                        section.getName(),
                                        section.getTotalQuestions(),
                                        section.getPassScore(),
                                        isUnlocked,
                                        isPassed,
                                        bestScore,
                                        attemptCount));

                        // 다음 섹션 해금 조건: 현재 섹션 통과
                        previousPassed = isPassed;
                }

                return result;
        }

        /**
         * 전체 진행 상황을 계산한다.
         */
        private MyProgressDto buildMyProgress(List<SectionWithProgressDto> sections) {
                int completedSections = (int) sections.stream()
                                .filter(SectionWithProgressDto::isPassed)
                                .count();

                // 현재 진행 중인 섹션: 해금되었지만 통과하지 않은 첫 번째 섹션
                int currentSection = sections.stream()
                                .filter(s -> s.isUnlocked() && !s.isPassed())
                                .map(SectionWithProgressDto::sectionNumber)
                                .findFirst()
                                .orElse(sections.isEmpty() ? 1 : sections.size()); // 모두 완료 시 마지막 섹션

                boolean isCompleted = !sections.isEmpty() && completedSections == sections.size();

                return new MyProgressDto(currentSection, completedSections, isCompleted);
        }

        /**
         * 섹션별 시도 요약 정보를 담는 내부 record.
         */
        private record SectionAttemptSummary(Integer bestScore, boolean isPassed, int attemptCount) {
        }

        /**
         * 섹션 문제 목록을 조회한다.
         *
         * <p>
         * 퀴즈 코스는 순차적 학습 시스템으로, 이전 섹션을 통과해야 다음 섹션이 해금된다.
         * 이 메서드는 사용자가 특정 섹션의 문제를 풀기 위해 호출하며,
         * 해금 여부를 검증한 후 문제 목록을 반환한다.
         * </p>
         *
         * <h3>해금 규칙</h3>
         * <ul>
         * <li>섹션 1: 항상 해금 (코스 시작점)</li>
         * <li>섹션 N (N > 1): 사용자의 currentSection이 N 이상이면 해금</li>
         * </ul>
         *
         * <h3>처리 흐름</h3>
         * <ol>
         * <li>섹션 존재 여부 확인 (문제 목록 fetch join)</li>
         * <li>코스 활성화 상태 확인</li>
         * <li>섹션 해금 여부 확인</li>
         * <li>문제 목록을 응답 DTO로 변환</li>
         * </ol>
         *
         * @param courseId      코스 ID (URL path variable)
         * @param sectionNumber 섹션 번호 (URL path variable)
         * @param userId        인증된 사용자 ID (JWT에서 추출)
         * @return 섹션 정보 및 문제 목록을 담은 응답 DTO
         * @throws NotFoundException      코스 또는 섹션을 찾을 수 없거나 비활성 상태인 경우
         * @throws SectionLockedException 이전 섹션을 완료하지 않아 잠긴 섹션에 접근한 경우
         */
        public SectionQuestionsResponse getSectionQuestions(Long courseId, Integer sectionNumber, Long userId) {
                // 1. 섹션 조회: N+1 문제 방지를 위해 문제 목록을 fetch join으로 함께 조회
                // - courseId와 sectionNumber 모두 일치하는 섹션만 조회 (잘못된 코스-섹션 조합 방지)
                QuizCourseSection section = quizCourseSectionRepository
                                .findByIdWithCourseAndQuestions(courseId, sectionNumber)
                                .orElseThrow(NotFoundException::section);

                // 2. 코스 활성화 상태 확인: 비활성 코스는 관리자가 숨긴 것이므로 404 처리
                // - 예: 시즌 종료된 이벤트 코스, 준비 중인 신규 코스 등
                QuizCourse course = section.getCourse();
                if (!Boolean.TRUE.equals(course.getIsActive())) {
                        throw NotFoundException.course();
                }

                // 3. 섹션 해금 여부 확인: 순차 학습을 강제하기 위한 잠금 체크
                // - 해금되지 않은 섹션 접근 시 403 Forbidden 응답
                if (!isSectionUnlocked(userId, courseId, section.getSectionNumber())) {
                        throw new SectionLockedException();
                }

                // 4. 엔티티를 응답 DTO로 변환
                // - 정답(correctAnswer)과 해설(explanation)은 제외하여 클라이언트에 노출하지 않음
                // - 채점 시에만 정답을 사용하므로 보안상 필요한 조치
                List<QuestionItem> questions = section.getQuestions().stream()
                                .map(this::toQuestionItem)
                                .toList();

                return new SectionQuestionsResponse(
                                section.getSectionNumber(),
                                section.getName(),
                                section.getTotalQuestions(),
                                section.getPassScore(),
                                questions);
        }

        /**
         * 섹션이 해금되었는지 확인한다.
         *
         * <p>
         * 퀴즈 코스의 핵심 규칙인 순차 학습을 구현하는 메서드이다.
         * 사용자는 이전 섹션을 70% 이상 통과해야 다음 섹션이 해금된다.
         * </p>
         *
         * <h3>해금 판단 로직</h3>
         * <ul>
         * <li>섹션 1: 무조건 true 반환 (모든 사용자의 시작점)</li>
         * <li>섹션 N (N > 1): UserCourseProgress.currentSection >= N 이면 해금</li>
         * <li>진행 기록이 없는 경우: false 반환 (아직 코스를 시작하지 않음)</li>
         * </ul>
         *
         * <h3>currentSection의 의미</h3>
         * <p>
         * currentSection은 "현재 도전 가능한 가장 높은 섹션 번호"를 의미한다.
         * 예를 들어 currentSection=3이면 섹션 1, 2, 3 모두 접근 가능하다.
         * (이미 통과한 섹션도 복습을 위해 다시 풀 수 있음)
         * </p>
         *
         * @param userId        사용자 ID
         * @param courseId      코스 ID
         * @param sectionNumber 접근하려는 섹션 번호 (1부터 시작)
         * @return 해금 여부 (true: 접근 가능, false: 잠김)
         */
        private boolean isSectionUnlocked(Long userId, Long courseId, Integer sectionNumber) {
                // 첫 번째 섹션은 항상 해금: 코스의 진입점이므로 누구나 접근 가능
                if (sectionNumber == 1) {
                        return true;
                }

                // 사용자 진행 상황 조회 후 해금 여부 판단
                // - 진행 기록이 있으면: currentSection과 비교하여 해금 여부 결정
                // - 진행 기록이 없으면: 코스를 시작하지 않은 것이므로 false 반환
                return userCourseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                                .map(progress -> progress.getCurrentSection() >= sectionNumber)
                                .orElse(false);
        }

        /**
         * 문제 엔티티를 클라이언트 응답용 DTO로 변환한다.
         *
         * <p>
         * 보안상 정답(correctAnswer)과 해설(explanation)은 포함하지 않는다.
         * 이 정보들은 답안 제출 API에서 채점 후에만 클라이언트에 전달된다.
         * </p>
         *
         * @param question 문제 엔티티 (DB에서 조회한 원본 데이터)
         * @return 클라이언트에 전달할 문제 DTO (정답 제외)
         */
        private QuestionItem toQuestionItem(QuizCourseQuestion question) {
                // JSON 문자열로 저장된 보기를 OptionItem 목록으로 파싱
                List<OptionItem> options = parseOptions(question.getOptions());

                return new QuestionItem(
                                question.getQuestionNumber(), // 문제 순서 (1, 2, 3, ...)
                                question.getQuestionText(), // 문제 내용
                                question.getQuestionType(), // MULTIPLE_CHOICE 또는 SHORT_ANSWER
                                options // 객관식 보기 (단답형은 빈 리스트)
                );
        }

        /**
         * JSON 형태의 보기 문자열을 OptionItem 목록으로 파싱한다.
         *
         * <p>
         * DB에는 보기가 JSON 문자열로 저장되어 있다.
         * 예: [{"id": "A", "text": "int"}, {"id": "B", "text": "integer"}]
         * </p>
         *
         * <h3>예외 처리</h3>
         * <ul>
         * <li>null 또는 빈 문자열: 빈 리스트 반환 (단답형 문제)</li>
         * <li>잘못된 JSON 형식: 경고 로그 후 빈 리스트 반환 (서비스 중단 방지)</li>
         * </ul>
         *
         * @param optionsJson DB에 저장된 보기 JSON 문자열
         * @return 파싱된 보기 목록 (파싱 실패 시 빈 리스트)
         */
        private List<OptionItem> parseOptions(String optionsJson) {
                // null 또는 빈 문자열 체크: 단답형(SHORT_ANSWER) 문제는 보기가 없음
                if (optionsJson == null || optionsJson.isBlank()) {
                        return Collections.emptyList();
                }

                try {
                        // Jackson ObjectMapper를 사용하여 JSON 파싱
                        // TypeReference를 통해 제네릭 타입 정보를 런타임에 유지
                        List<Map<String, String>> optionMaps = objectMapper.readValue(
                                        optionsJson,
                                        new TypeReference<List<Map<String, String>>>() {
                                        });

                        // Map을 OptionItem DTO로 변환
                        // - id: 보기 식별자 (A, B, C, D 등)
                        // - text: 보기 내용
                        return optionMaps.stream()
                                        .map(map -> new OptionItem(map.get("id"), map.get("text")))
                                        .toList();
                } catch (JsonProcessingException e) {
                        // JSON 파싱 실패 시 서비스를 중단하지 않고 빈 리스트 반환
                        // - 데이터 오류가 있더라도 다른 문제는 정상 표시
                        // - 운영 중 문제 파악을 위해 경고 로그 기록
                        log.warn("Failed to parse options JSON: {}", optionsJson, e);
                        return Collections.emptyList();
                }
        }
}
