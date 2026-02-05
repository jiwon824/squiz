package com.ssafy.domain.quiz.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.domain.gamification.entity.Badge;
import com.ssafy.domain.gamification.event.QuizSolvedEvent;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.BadgeInfo;
import com.ssafy.domain.quiz.dto.response.ContinuousAnswerResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.dto.response.MyProgressDto;
import com.ssafy.domain.quiz.dto.response.QuizCourseDetailResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseListItem;
import com.ssafy.domain.quiz.dto.response.QuizCourseListResponse;
import com.ssafy.domain.quiz.dto.response.SectionSummary;
import com.ssafy.domain.quiz.dto.response.SectionWithProgressDto;
import com.ssafy.domain.quiz.dto.response.CourseQuizStatDto;
import com.ssafy.domain.quiz.dto.response.SectionsWithProgressResponse;
import com.ssafy.domain.quiz.entity.QuizCourse;
import com.ssafy.domain.quiz.repository.CourseQuizStatProjection;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserCourseProgress;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.repository.ContinuousQuizRepository;
import com.ssafy.domain.quiz.repository.QuizCourseRepository;
import com.ssafy.domain.quiz.repository.UserCourseProgressRepository;
import com.ssafy.domain.quiz.repository.UserReviewItemRepository;
import com.ssafy.domain.quiz.util.QuizGradingUtils;
import com.ssafy.domain.quiz.dto.response.WeakConceptDto;
import lombok.RequiredArgsConstructor;
import java.util.Comparator;
import java.util.Set;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 연속 학습 모드 통합 서비스.
 *
 * <p>
 * 코스 조회 + Sayvoca 스타일의 단일 문제 흐름 (Submit → Next)을 제공한다.
 * </p>
 *
 * <h3>코스 조회</h3>
 * <ul>
 * <li>코스 목록 / 상세 조회</li>
 * <li>섹션 + 진행 상황 조회 (UserCourseProgress 기반)</li>
 * </ul>
 *
 * <h3>연속 학습</h3>
 * <ul>
 * <li>Attempt 레코드 생성 없음 — user_review_items 직접 업데이트</li>
 * <li>Section Locking 없음 — 자유롭게 학습 가능</li>
 * <li><b>확률적 가중치</b> 기반 랜덤 문제 선택 (엄격한 우선순위 아님)</li>
 * <li>FSRS 기반 즉시 상태 갱신</li>
 * <li><b>무한 루프</b> — 섹션 완료 개념 없음, 동일 문제 재출제 가능</li>
 * </ul>
 *
 * <h3>확률적 가중치 로직</h3>
 * <ul>
 * <li>신규 문제: 가중치 10.0 (가장 높음)</li>
 * <li>복습 필요 (Due): 가중치 5.0</li>
 * <li>학습 완료: 가중치 1/(reps+1) (반복할수록 감소)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContinuousQuizService {

        private final ContinuousQuizRepository learningRepository;
        private final FsrsService fsrsService;
        private final QuizCourseRepository quizCourseRepository;
        private final UserCourseProgressRepository userCourseProgressRepository;
        private final BadgeRepository badgeRepository;
        private final UserReviewItemRepository reviewItemRepository;
        private final ApplicationEventPublisher eventPublisher;

        // ==================== 코스 조회 ====================

        /**
         * 활성 코스 목록을 반환한다.
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
         */
        public QuizCourseDetailResponse getCourseDetail(Long courseId) {
                QuizCourse course = quizCourseRepository.findByIdWithSections(courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz course not found"));

                if (!Boolean.TRUE.equals(course.getIsActive())) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz course not found");
                }

                BadgeInfo badgeInfo = null;
                String badgeCode = course.getBadgeCode();
                if (badgeCode != null) {
                        badgeInfo = badgeRepository.findByCode(badgeCode)
                                        .map(badge -> new BadgeInfo(badge.getCode(), badge.getName(),
                                                        badge.getDescription()))
                                        .orElseGet(() -> new BadgeInfo(badgeCode, null, null));
                }

                List<SectionSummary> sections = course.getSections().stream()
                                .map(this::toSectionSummary)
                                .toList();

                return new QuizCourseDetailResponse(
                                course.getId(),
                                course.getCode(),
                                course.getName(),
                                course.getDescription(),
                                course.getTotalSections(),
                                badgeInfo,
                                sections);
        }

        /**
         * 섹션 목록과 사용자 진행 상황을 조회한다.
         *
         * UserCourseProgress.currentSection 기반으로 해금 여부와 통과 여부를 결정한다.
         *
         * <h3>해금 규칙</h3>
         * <ul>
         * <li>섹션 N: currentSection >= N 이면 해금</li>
         * <li>섹션 N: currentSection > N 이면 통과</li>
         * </ul>
         */
        public SectionsWithProgressResponse getSectionsWithProgress(Long courseId, Long userId) {
                // 1. 코스 + 섹션 조회 (fetch join)
                QuizCourse course = quizCourseRepository.findByIdWithSections(courseId)
                                .orElseThrow(NotFoundException::course);

                if (!Boolean.TRUE.equals(course.getIsActive())) {
                        throw NotFoundException.course();
                }

                List<QuizCourseSection> sections = course.getSections();

                // 2. UserCourseProgress에서 currentSection 조회
                Optional<UserCourseProgress> progressOpt = userCourseProgressRepository.findByUserIdAndCourseId(userId,
                                courseId);
                int currentSection = progressOpt
                                .map(UserCourseProgress::getCurrentSection)
                                .orElse(1); // 진행 기록 없으면 첫 번째 섹션만 해금

                // 3. 섹션 DTO 목록 생성 (UserCourseProgress 기반)
                List<SectionWithProgressDto> sectionDtos = sections.stream()
                                .map(section -> {
                                        int sn = section.getSectionNumber();
                                        boolean isUnlocked = sn <= currentSection;
                                        boolean isPassed = sn < currentSection;
                                        return new SectionWithProgressDto(
                                                        sn,
                                                        section.getName(),
                                                        section.getTotalQuestions(),
                                                        section.getPassScore(),
                                                        isUnlocked,
                                                        isPassed,
                                                        null // bestScore — Attempt 데이터 소스 제거
                                        );
                                })
                                .toList();

                // 4. 전체 진행 상황 계산
                MyProgressDto myProgress = buildMyProgress(sectionDtos);

                return new SectionsWithProgressResponse(courseId, course.getName(), myProgress, sectionDtos);
        }

        /**
         * 사용자의 취약 개념(섹션)을 분석하여 반환한다.
         *
         * <p>
         * 분석 로직:
         * 1. 사용자의 모든 COURSE_QUESTION 복습 항목 조회
         * 2. 섹션별로 그룹화
         * 3. 섹션별 평균 안정도(Stability) 산출
         * 4. 취약도 점수 계산: 100 * (0.9 ^ AverageStability)
         * 5. 점수 내림차순 정렬 후 상위 limit개 반환
         * </p>
         */
        public List<WeakConceptDto> getWeakConcepts(Long userId, int limit) {
                // 1. 사용자의 모든 Course Question 리뷰 아이템 조회
                List<UserReviewItem> reviewItems = reviewItemRepository.findAllByUserIdAndContentType(userId,
                                ReviewContentType.COURSE_QUESTION);

                if (reviewItems.isEmpty()) {
                        return List.of();
                }

                // 2. 관련된 문제 정보 조회 (N+1 방지 위해 한 번에 조회)
                Set<Long> questionIds = reviewItems.stream()
                                .map(UserReviewItem::getContentId)
                                .collect(Collectors.toSet());

                Map<Long, QuizCourseQuestion> questionMap = learningRepository.findAllById(questionIds).stream()
                                .collect(Collectors.toMap(QuizCourseQuestion::getId, q -> q));

                // 3. 섹션별로 아이템 그룹화
                // Key: Section Object (equals/hashCode 주의, 여기서는 ID 기반 식별을 위해 별도 Key 사용 안하고
                // 스트림 내에서 처리)
                // Grouping by QuizCourseSection directly might be tricky if equals() isn't
                // strictly
                // on ID.
                // So we group by QuizCourseSection entity itself (assuming safe equals/hashCode
                // OR
                // use ID pair)
                Map<QuizCourseSection, List<UserReviewItem>> itemsBySection = reviewItems.stream()
                                .filter(item -> questionMap.containsKey(item.getContentId()))
                                .collect(Collectors
                                                .groupingBy(item -> questionMap.get(item.getContentId()).getSection()));

                // 4. 섹션별 취약도 점수 계산 및 정렬
                return itemsBySection.entrySet().stream()
                                .map(entry -> {
                                        QuizCourseSection section = entry.getKey();
                                        List<UserReviewItem> items = entry.getValue();

                                        double avgStability = items.stream()
                                                        .mapToDouble(UserReviewItem::getStability)
                                                        .average()
                                                        .orElse(0.0);

                                        // 취약도 점수 공식: 100 * (0.9 ^ stability)
                                        // stability가 낮을수록(0에 가까울수록) 점수가 100에 가까워짐
                                        double weaknessScore = 100.0 * Math.pow(0.9, avgStability);

                                        return WeakConceptDto.builder()
                                                        .courseId(section.getQuizCourseId())
                                                        .courseName(section.getCourse().getName())
                                                        .sectionNumber(section.getSectionNumber())
                                                        .sectionName(section.getName())
                                                        .weaknessScore(weaknessScore)
                                                        .build();
                                })
                                .sorted(Comparator.comparingDouble(WeakConceptDto::weaknessScore).reversed()) // 점수 높은
                                                                                                              // 순(취약한
                                                                                                              // 순)
                                .limit(limit)
                                .toList();
        }

        /**
         * 사용자별 코스 통계 조회.
         *
         * @param userId 사용자 ID
         * @return 코스별 통계 (attempted, correct)
         */
        public List<CourseQuizStatDto> getCourseStats(Long userId) {
                // Native Query Projection 조회
                List<CourseQuizStatProjection> projections = quizCourseRepository.findCourseStats(userId);

                // DTO 변환
                return projections.stream()
                                .map(p -> CourseQuizStatDto.builder()
                                                .courseName(p.getCourseName())
                                                .courseCode(p.getCourseCode())
                                                .attemptedCount(p.getAttemptedCount())
                                                .correctCount(p.getCorrectCount())
                                                .build())
                                .toList();
        }

        // ==================== 연속 학습 ====================

        /**
         * 답변 제출 및 FSRS 상태 즉시 업데이트.
         *
         * <p>
         * Attempt 레코드를 생성하지 않고, user_review_items 테이블만 직접 업데이트한다.
         * </p>
         *
         * @param userId     사용자 ID
         * @param questionId 문제 ID
         * @param request    답변 요청 (userAnswer, responseTimeMs)
         * @return 답변 결과 및 FSRS 갱신 정보
         * @throws BusinessException 문제가 존재하지 않는 경우
         */
        @Transactional
        public ContinuousAnswerResponse submitAnswer(Long userId, Long questionId,
                        ContinuousAnswerRequest request) {
                // 1. 문제 조회
                QuizCourseQuestion question = learningRepository.findById(questionId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                                                "문제를 찾을 수 없습니다. id=" + questionId));

                // 2. 정답 여부 판정 (서술형의 경우 키워드 기반 채점)
                boolean isCorrect = QuizGradingUtils.grade(
                                request.getUserAnswer(),
                                question.getCorrectAnswer(),
                                question.getQuestionType(),
                                question.getOptions(),
                                question.getKeywords());

                // 3. FSRS 상태 즉시 갱신 (Attempt 없이 직접 업데이트)
                UserReviewItem reviewItem = fsrsService.processReviewResult(
                                userId,
                                ReviewContentType.COURSE_QUESTION,
                                questionId,
                                isCorrect,
                                request.getResponseTimeMs());

                // 4. 정규화된 정답 (프론트엔드 표시용)
                String normalizedCorrectAnswer = QuizGradingUtils.normalizeCorrectAnswer(question.getCorrectAnswer());

                log.info("Continuous Learning 답변 처리 - userId: {}, questionId: {}, isCorrect: {}, " +
                                "stability: {:.2f}, nextReview: {}",
                                userId, questionId, isCorrect,
                                reviewItem.getStability(), reviewItem.getNextReviewAt());

                // 5. 게이미피케이션 이벤트 발행
                publishQuizSolvedEvent(userId, questionId, question.getQuestionText(), isCorrect);

                // 6. 결과 반환
                return ContinuousAnswerResponse.builder()
                                .questionId(questionId)
                                .isCorrect(isCorrect)
                                .userAnswer(request.getUserAnswer())
                                .correctAnswer(normalizedCorrectAnswer)
                                .explanation(question.getExplanation())
                                // FSRS 갱신 정보
                                .stability(reviewItem.getStability())
                                .difficulty(reviewItem.getDifficulty())
                                .scheduledMinutes(reviewItem.getScheduledMinutes())
                                .nextReviewAt(reviewItem.getNextReviewAt())
                                .state(reviewItem.getState())
                                .reps(reviewItem.getReps())
                                .lapses(reviewItem.getLapses())
                                .build();
        }

        /**
         * 다음 문제 조회 (확률적 가중치 기반 랜덤 선택).
         *
         * @param userId        사용자 ID
         * @param courseId      코스 ID
         * @param sectionNumber 섹션 번호
         * @return 다음 문제 (섹션에 문제가 없으면 예외)
         */
        public ContinuousQuestionResponse getNextQuestion(Long userId, Long courseId,
                        Integer sectionNumber) {
                QuizCourseQuestion question = learningRepository
                                .findNextQuestionProbabilisticNoExclude(courseId, sectionNumber, userId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND, "NO_QUESTIONS_AVAILABLE",
                                                "해당 섹션에 문제가 없습니다."));

                return ContinuousQuestionResponse.from(question);
        }

        /**
         * Atomic Submit & Fetch Next - 답변 제출 후 다음 문제까지 한 번에 반환.
         *
         * @param userId     사용자 ID
         * @param questionId 현재 문제 ID
         * @param request    답변 요청 (userAnswer, responseTimeMs)
         * @return 답변 결과 + 다음 문제 (섹션에 문제가 없을 때만 nextQuestion = null)
         */
        @Transactional
        public ContinuousSubmitResponse processAnswerAndGetNext(Long userId, Long questionId,
                        ContinuousAnswerRequest request) {
                // 1. 현재 문제 조회
                QuizCourseQuestion currentQuestion = learningRepository.findById(questionId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                                                "문제를 찾을 수 없습니다. id=" + questionId));

                // 2. 정답 여부 판정 (서술형의 경우 키워드 기반 채점)
                boolean isCorrect = QuizGradingUtils.grade(
                                request.getUserAnswer(),
                                currentQuestion.getCorrectAnswer(),
                                currentQuestion.getQuestionType(),
                                currentQuestion.getOptions(),
                                currentQuestion.getKeywords());

                // 3. FSRS 상태 즉시 갱신
                UserReviewItem reviewItem = fsrsService.processReviewResult(
                                userId,
                                ReviewContentType.COURSE_QUESTION,
                                questionId,
                                isCorrect,
                                request.getResponseTimeMs());

                // 4. 다음 문제 조회 (확률적 가중치 기반, 현재 문제 제외 시도)
                Long courseId = currentQuestion.getSection().getQuizCourseId();
                Integer sectionNumber = currentQuestion.getSection().getSectionNumber();

                // 현재 문제 제외하고 조회
                Optional<QuizCourseQuestion> nextQuestionOpt = learningRepository
                                .findNextQuestionProbabilistic(courseId, sectionNumber, userId, questionId);

                // 섹션에 문제가 1개뿐이면 같은 문제 재출제 (무한 루프 지원)
                if (nextQuestionOpt.isEmpty()) {
                        nextQuestionOpt = learningRepository
                                        .findNextQuestionProbabilisticNoExclude(courseId, sectionNumber, userId);
                }

                // 5. 정규화된 정답 (프론트엔드 표시용)
                String normalizedCorrectAnswer = QuizGradingUtils
                                .normalizeCorrectAnswer(currentQuestion.getCorrectAnswer());

                // 6. 통합 응답 빌드
                ContinuousSubmitResponse.ContinuousSubmitResponseBuilder builder = ContinuousSubmitResponse.builder()
                                // 답변 결과
                                .submittedQuestionId(questionId)
                                .isCorrect(isCorrect)
                                .userAnswer(request.getUserAnswer())
                                .correctAnswer(normalizedCorrectAnswer)
                                .explanation(currentQuestion.getExplanation())
                                // FSRS 갱신 정보
                                .stability(reviewItem.getStability())
                                .difficulty(reviewItem.getDifficulty())
                                .scheduledMinutes(reviewItem.getScheduledMinutes())
                                .nextReviewAt(reviewItem.getNextReviewAt())
                                .state(reviewItem.getState())
                                .reps(reviewItem.getReps())
                                .lapses(reviewItem.getLapses());

                // 다음 문제 설정 (섹션에 문제가 있으면 항상 존재)
                if (nextQuestionOpt.isPresent()) {
                        QuizCourseQuestion next = nextQuestionOpt.get();
                        builder.nextQuestion(ContinuousSubmitResponse.NextQuestion.builder()
                                        .questionId(next.getId())
                                        .questionNumber(next.getQuestionNumber())
                                        .questionText(next.getQuestionText())
                                        .questionType(next.getQuestionType().name())
                                        .options(next.getOptions())
                                        .courseId(next.getSection().getQuizCourseId())
                                        .sectionNumber(next.getSection().getSectionNumber())
                                        .build());
                } else {
                        builder.nextQuestion(null);
                }

                // 7. 게이미피케이션 이벤트 발행
                publishQuizSolvedEvent(userId, questionId, currentQuestion.getQuestionText(), isCorrect);

                log.info("Atomic Submit & Next - userId: {}, questionId: {}, isCorrect: {}, " +
                                "nextQuestionId: {}, stability: {:.2f}",
                                userId, questionId, isCorrect,
                                nextQuestionOpt.map(QuizCourseQuestion::getId).orElse(null),
                                reviewItem.getStability());

                return builder.build();
        }

        // ==================== Private Methods ====================

        /**
         * 퀴즈 풀이 이벤트 발행 (게이미피케이션 경험치 적립용)
         */
        private void publishQuizSolvedEvent(Long userId, Long questionId, String questionText, boolean isCorrect) {
                try {
                        eventPublisher.publishEvent(new QuizSolvedEvent(
                                        this, userId, questionId, questionText, isCorrect, LocalDate.now()));
                        log.debug("QuizSolvedEvent 발행 - userId: {}, questionId: {}, isCorrect: {}",
                                        userId, questionId, isCorrect);
                } catch (Exception e) {
                        log.warn("QuizSolvedEvent 발행 실패 - userId: {}, questionId: {}, error: {}",
                                        userId, questionId, e.getMessage());
                }
        }

        private SectionSummary toSectionSummary(QuizCourseSection section) {
                return new SectionSummary(
                                section.getSectionNumber(),
                                section.getName(),
                                section.getDescription(),
                                section.getTotalQuestions(),
                                section.getPassScore());
        }

        private MyProgressDto buildMyProgress(List<SectionWithProgressDto> sections) {
                int completedSections = (int) sections.stream()
                                .filter(SectionWithProgressDto::isPassed)
                                .count();

                int currentSection = sections.stream()
                                .filter(s -> s.isUnlocked() && !s.isPassed())
                                .map(SectionWithProgressDto::sectionNumber)
                                .findFirst()
                                .orElse(sections.isEmpty() ? 1 : sections.size());

                boolean isCompleted = !sections.isEmpty() && completedSections == sections.size();

                return new MyProgressDto(currentSection, completedSections, isCompleted);
        }
}
