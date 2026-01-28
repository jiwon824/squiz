package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.SectionLockedException;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.request.SaveAnswerRequest;
import com.ssafy.domain.quiz.dto.response.*;
import com.ssafy.domain.quiz.entity.*;
import com.ssafy.domain.quiz.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 퀴즈 섹션 시도 서비스.
 *
 * 섹션 문제 풀이와 관련된 비즈니스 로직을 처리한다.
 * - 시도 시작/재개 (문제 셔플)
 * - 임시 저장
 * - 제출 및 채점
 * - 배지 수여
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSectionAttemptService {

    private final UserRepository userRepository;
    private final QuizCourseSectionRepository sectionRepository;
    private final QuizCourseRepository courseRepository;
    private final UserSectionAttemptRepository attemptRepository;
    private final UserSectionAttemptQuestionRepository attemptQuestionRepository;
    private final UserCourseProgressRepository progressRepository;
    private final BadgeRepository badgeRepository;
    private final ObjectMapper objectMapper;

    /**
     * 시도당 출제할 문제 수.
     * 섹션의 총 문제 수가 이보다 적으면 전체 출제.
     */
    @Value("${quiz.course.questions-per-attempt:30}")
    private int questionsPerAttempt;

    /**
     * Optimistic Lock 충돌 시 재시도 횟수.
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * 섹션 시도를 시작하거나 재개한다.
     *
     * <h3>처리 흐름</h3>
     * <ol>
     * <li>섹션 해금 여부 확인</li>
     * <li>진행 중인 시도가 있으면 재개 (기존 순서 유지)</li>
     * <li>없으면 새 시도 생성 (문제 셔플)</li>
     * </ol>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userId        사용자 ID
     * @return 시도 응답 (문제 목록 포함)
     */
    @Transactional
    public SectionAttemptResponse startOrResumeAttempt(Long courseId, Integer sectionNumber, Long userId) {
        // 1. 섹션 조회 (코스 포함)
        QuizCourseSection section = sectionRepository
                .findByIdWithCourseAndQuestions(courseId, sectionNumber)
                .orElseThrow(NotFoundException::section);

        // 2. 코스 활성화 상태 확인
        QuizCourse course = section.getCourse();
        if (!Boolean.TRUE.equals(course.getIsActive())) {
            throw NotFoundException.course();
        }

        // 3. 섹션 해금 여부 확인
        if (!isSectionUnlocked(userId, courseId, sectionNumber)) {
            throw new SectionLockedException();
        }

        // 4. 진행 중인 시도 확인 (중복 시도 방지 로직 추가)
        List<UserSectionAttempt> attempts = attemptRepository
                .findAllInProgressAttemptsWithQuestions(userId, courseId, sectionNumber);

        UserSectionAttempt attempt;
        if (attempts.isEmpty()) {
            // 새 시도 생성
            User user = userRepository.getReferenceById(userId);
            attempt = createNewAttempt(user, section);
        } else {
            // Self-healing: 가장 진행도가 높은 시도 선택
            // - 1차 기준: 답변한 문제 수가 가장 많은 시도
            // - 2차 기준 (동점): 가장 최근 생성된 시도
            //
            // [이전 버그 수정]
            // 기존 로직은 단순히 createdAt DESC로 가장 최근 시도를 선택했음.
            // 문제: Race condition으로 빈 시도가 생성되면, 답변이 저장된 기존 시도가 abandon 처리됨.
            // 해결: 답변 수 기준으로 선택하여 사용자 데이터 유실 방지.
            attempt = selectBestAttempt(attempts);

            // 선택되지 않은 중복 시도들을 포기 처리 (Self-healing)
            if (attempts.size() > 1) {
                final Long selectedAttemptId = attempt.getId();
                log.warn("Multiple IN_PROGRESS attempts found for user {} in section {}. " +
                        "Selected attempt {} with most progress. Cleaning up {} duplicates.",
                        userId, sectionNumber, selectedAttemptId, attempts.size() - 1);

                attempts.stream()
                        .filter(a -> !a.getId().equals(selectedAttemptId))
                        .forEach(redundant -> {
                            log.debug("Abandoning redundant attempt {}", redundant.getId());
                            redundant.abandon();
                            // Dirty checking으로 자동 저장됨
                        });
            }
        }

        return buildAttemptResponse(attempt, section);
    }

    /**
     * 단일 답안을 임시 저장한다 (Optimistic Lock 재시도 포함).
     *
     * <p>
     * 사용자가 문제를 풀고 "다음" 버튼을 누를 때마다 호출되어
     * 실시간으로 답안을 저장한다. 이를 통해 브라우저 충돌이나
     * 네트워크 끊김 시에도 데이터 유실을 방지한다.
     * </p>
     *
     * <h3>Optimistic Locking 재시도 전략</h3>
     * <p>
     * 동시 요청으로 인한 {@link ObjectOptimisticLockingFailureException} 발생 시
     * 최대 {@value #MAX_RETRY_ATTEMPTS}회까지 자동 재시도한다.
     * 재시도 시 최신 데이터를 다시 조회하여 충돌을 해결한다.
     * </p>
     *
     * @param attemptId 시도 ID
     * @param request   저장할 단일 답안
     * @param userId    사용자 ID
     * @throws ObjectOptimisticLockingFailureException 최대 재시도 횟수 초과 시
     */
    @Transactional
    public void saveAnswer(Long attemptId, SaveAnswerRequest request, Long userId) {
        int retryCount = 0;
        ObjectOptimisticLockingFailureException lastException = null;

        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                doSaveAnswer(attemptId, request, userId);
                return; // 성공 시 즉시 반환
            } catch (ObjectOptimisticLockingFailureException e) {
                lastException = e;
                retryCount++;
                log.warn("Optimistic lock conflict on saveAnswer (attempt {}/{}). " +
                        "attemptId={}, questionId={}",
                        retryCount, MAX_RETRY_ATTEMPTS,
                        attemptId, request.answer().questionId());

                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    // 영속성 컨텍스트 클리어 후 재시도
                    // (실제로는 트랜잭션이 롤백되어 자동 클리어됨)
                    try {
                        Thread.sleep(50L * retryCount); // 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        // 최대 재시도 초과 - 프론트엔드에서 처리하도록 예외 전파
        log.error("Max retry attempts exceeded for saveAnswer. attemptId={}, questionId={}",
                attemptId, request.answer().questionId());
        throw lastException;
    }

    /**
     * 실제 답안 저장 로직 (내부용).
     *
     * @throws ObjectOptimisticLockingFailureException 동시 수정 충돌 시
     */
    private void doSaveAnswer(Long attemptId, SaveAnswerRequest request, Long userId) {
        // 최신 데이터 조회 (Version 포함)
        UserSectionAttemptQuestion aq = attemptQuestionRepository
                .findByAttemptIdAndQuestionId(attemptId, request.answer().questionId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "QUESTION_NOT_FOUND",
                        "해당 문제를 찾을 수 없습니다."));

        // 본인 시도인지 확인
        if (!aq.getAttempt().getUser().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NOT_ATTEMPT_OWNER",
                    "본인의 시도만 수정할 수 있습니다.");
        }

        // 진행 중인 시도인지 확인
        if (!aq.getAttempt().isInProgress()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ATTEMPT_ALREADY_COMPLETED",
                    "완료된 시도는 수정할 수 없습니다.");
        }

        // 답안 저장 (Version 자동 증가)
        aq.saveAnswer(request.answer().answer());
        attemptQuestionRepository.save(aq);
    }

    /**
     * 시도를 제출하고 채점한다.
     *
     * @param attemptId 시도 ID
     * @param userId    사용자 ID
     * @return 채점 결과
     */
    @Transactional
    public AttemptResultResponse submitAttempt(Long attemptId, Long userId) {
        UserSectionAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(NotFoundException::attempt);

        // 본인 시도인지 확인
        if (!attempt.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NOT_ATTEMPT_OWNER",
                    "본인의 시도만 제출할 수 있습니다.");
        }

        // 진행 중인 시도인지 확인
        if (!attempt.isInProgress()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ATTEMPT_ALREADY_COMPLETED",
                    "이미 완료된 시도입니다.");
        }

        // 1. 문제 목록 조회 (질문 정보 포함)
        // 이 시점에서 question 정보가 필요하므로 fetch join된 쿼리 사용
        List<UserSectionAttemptQuestion> attemptQuestions = attemptQuestionRepository
                .findByAttemptIdWithQuestionOrderByOrderIndex(attemptId);

        // 2. 각 문제 채점
        for (UserSectionAttemptQuestion aq : attemptQuestions) {
            // UserSectionAttemptQuestion 엔티티의 비즈니스 메서드 호출
            // 내부적으로 userAnswer와 question.correctAnswer 비교
            aq.grade(aq.getQuestion().getCorrectAnswer());
        }

        // 3. 정답 개수 계산
        int correctCount = (int) attemptQuestions.stream()
                .filter(UserSectionAttemptQuestion::getIsCorrect)
                .count();

        // 4. 시도 완료 처리
        QuizCourseSection section = attempt.getSection();
        attempt.complete(correctCount, section.getPassScore());
        // JPA Dirty Checking으로 변경사항 자동 저장

        // 통과 시 다음 섹션 해금
        boolean isNextSectionUnlocked = false;
        BadgeInfo earnedBadge = null;

        if (attempt.getIsPassed()) {
            isNextSectionUnlocked = updateProgress(
                    userId,
                    section.getCourse().getId(),
                    section.getSectionNumber());

            // 코스 완료 시 배지 수여
            if (isCoursCompleted(userId, section.getCourse())) {
                earnedBadge = awardCourseBadge(userId, section.getCourse());
            }
        }

        return buildResultResponse(attempt, attemptQuestions, isNextSectionUnlocked, earnedBadge);
    }

    /**
     * 시도를 포기한다.
     *
     * @param attemptId 시도 ID
     * @param userId    사용자 ID
     */
    @Transactional
    public void abandonAttempt(Long attemptId, Long userId) {
        UserSectionAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(NotFoundException::attempt);

        if (!attempt.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NOT_ATTEMPT_OWNER",
                    "본인의 시도만 포기할 수 있습니다.");
        }

        if (!attempt.isInProgress()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ATTEMPT_ALREADY_COMPLETED",
                    "이미 완료된 시도입니다.");
        }

        attempt.abandon();
        // JPA Dirty Checking으로 변경사항 자동 저장
    }

    // ========== Private Methods ==========

    /**
     * 여러 시도 중 가장 진행도가 높은 시도를 선택한다.
     *
     * <h3>선택 기준 (우선순위)</h3>
     * <ol>
     *   <li><b>답변 수</b>: 답변한 문제가 가장 많은 시도</li>
     *   <li><b>생성 시각</b>: 동점일 경우 가장 최근 생성된 시도</li>
     * </ol>
     *
     * <h3>구현 Trade-off: Stream.max() vs Manual Loop</h3>
     * <ul>
     *   <li><b>Stream.max()</b>: 선언적 코드로 가독성 우수. O(n) 시간 복잡도.</li>
     *   <li><b>Manual Loop</b>: 약간 빠를 수 있으나 (Stream 오버헤드 없음),
     *       중복 시도 수가 적어 (보통 1~2개) 성능 차이 무시 가능.</li>
     * </ul>
     * <p>
     * {@code @Transactional} 컨텍스트에서 DB I/O가 주요 병목이므로,
     * 인메모리 컬렉션 순회의 미세한 성능 차이는 무의미함.
     * 가독성과 유지보수성을 위해 Stream API 채택.
     * </p>
     *
     * @param attempts 진행 중인 시도 목록 (비어있지 않음을 전제)
     * @return 가장 진행도가 높은 시도
     */
    private UserSectionAttempt selectBestAttempt(List<UserSectionAttempt> attempts) {
        return attempts.stream()
                .max(Comparator
                        // 1차: 답변한 문제 수 (내림차순)
                        .comparingLong((UserSectionAttempt a) -> a.getAttemptQuestions().stream()
                                .filter(UserSectionAttemptQuestion::isAnswered)
                                .count())
                        // 2차: 생성 시각 (최신 우선)
                        .thenComparing(UserSectionAttempt::getCreatedAt))
                .orElse(attempts.get(0)); // Fallback (논리적으로 도달 불가)
    }

    /**
     * 섹션 해금 여부를 확인한다.
     */
    private boolean isSectionUnlocked(Long userId, Long courseId, Integer sectionNumber) {
        if (sectionNumber == 1) {
            return true;
        }

        return progressRepository.findByUserIdAndCourseId(userId, courseId)
                .map(progress -> progress.getCurrentSection() >= sectionNumber)
                .orElse(false);
    }

    /**
     * 새 시도를 생성한다.
     * 문제를 셔플하여 할당한다.
     */
    private UserSectionAttempt createNewAttempt(User user, QuizCourseSection section) {
        List<QuizCourseQuestion> allQuestions = section.getQuestions();

        // 출제할 문제 수 결정
        int questionCount = Math.min(questionsPerAttempt, allQuestions.size());

        // 문제 셔플
        List<QuizCourseQuestion> shuffledQuestions = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffledQuestions);
        List<QuizCourseQuestion> selectedQuestions = shuffledQuestions.subList(0, questionCount);

        // 시도 생성
        UserSectionAttempt attempt = UserSectionAttempt.builder()
                .user(user)
                .section(section)
                .totalQuestions(questionCount)
                .build();

        // 문제 할당 (order_index 부여)
        IntStream.range(0, selectedQuestions.size()).forEach(i -> {
            UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                    .question(selectedQuestions.get(i))
                    .orderIndex(i + 1) // 1부터 시작
                    .build();
            attempt.addAttemptQuestion(aq);
        });

        return attemptRepository.save(attempt);
    }

    /**
     * 진행 상황을 업데이트한다.
     *
     * @return 다음 섹션이 새로 해금되었는지 여부
     */
    private boolean updateProgress(Long userId, Long courseId, Integer completedSectionNumber) {
        Optional<UserCourseProgress> existingProgress = progressRepository
                .findByUserIdAndCourseId(userId, courseId);

        if (existingProgress.isPresent()) {
            UserCourseProgress progress = existingProgress.get();
            if (progress.getCurrentSection() <= completedSectionNumber) {
                progress.advanceToSection(completedSectionNumber + 1);
                progressRepository.save(progress); // ✅ 명시적으로 save 추가
                return true;
            }
            return false;
        } else {
            // 첫 섹션 통과 시 진행 기록 생성
            User user = userRepository.getReferenceById(userId);
            QuizCourse course = courseRepository.getReferenceById(courseId);

            UserCourseProgress progress = UserCourseProgress.builder()
                    .userId(userId) // @Id 필드 직접 설정
                    .courseId(courseId) // @Id 필드 직접 설정
                    .user(user) // 연관관계 (조회용)
                    .course(course) // 연관관계 (조회용)
                    .currentSection(completedSectionNumber + 1)
                    .completedSections(1) // 완료된 섹션 수 초기화
                    .isCompleted(false) // 완료 여부 초기화
                    .build();
            progressRepository.save(progress);
            return true;
        }
    }

    /**
     * 코스 완료 여부를 확인한다.
     */
    private boolean isCoursCompleted(Long userId, QuizCourse course) {
        int passedSections = attemptRepository.countPassedSections(userId, course.getId());
        return passedSections >= course.getTotalSections();
    }

    /**
     * 코스 완료 배지를 수여한다.
     */
    private BadgeInfo awardCourseBadge(Long userId, QuizCourse course) {
        String badgeCode = course.getBadgeCode();
        if (badgeCode == null) {
            return null;
        }

        return badgeRepository.findByCode(badgeCode)
                .map(badge -> {
                    // TODO: 실제 배지 수여 로직 (UserBadge 테이블에 저장)
                    log.info("Badge awarded: userId={}, badgeCode={}", userId, badgeCode);
                    return new BadgeInfo(badge.getCode(), badge.getName(), badge.getDescription());
                })
                .orElse(null);
    }

    /**
     * 시도 응답을 생성한다.
     */
    private SectionAttemptResponse buildAttemptResponse(UserSectionAttempt attempt, QuizCourseSection section) {
        List<AttemptQuestionItem> questions = attempt.getAttemptQuestions().stream()
                .map(aq -> new AttemptQuestionItem(
                        aq.getOrderIndex(),
                        aq.getQuestion().getId(),
                        aq.getQuestion().getQuestionText(),
                        aq.getQuestion().getQuestionType(),
                        parseOptions(aq.getQuestion().getOptions()),
                        aq.getUserAnswer()))
                .toList();

        int answeredCount = (int) attempt.getAttemptQuestions().stream()
                .filter(UserSectionAttemptQuestion::isAnswered)
                .count();

        return new SectionAttemptResponse(
                attempt.getId(),
                section.getSectionNumber(),
                section.getName(),
                attempt.getStatus(),
                attempt.getTotalQuestions(),
                answeredCount,
                section.getPassScore(),
                attempt.getCreatedAt(),
                questions);
    }

    /**
     * 채점 결과 응답을 생성한다.
     */
    private AttemptResultResponse buildResultResponse(
            UserSectionAttempt attempt,
            List<UserSectionAttemptQuestion> results,
            boolean isNextSectionUnlocked,
            BadgeInfo earnedBadge) {
        List<AttemptResultResponse.QuestionResultItem> resultItems = results.stream()
                .map(aq -> new AttemptResultResponse.QuestionResultItem(
                        aq.getOrderIndex(),
                        aq.getQuestion().getId(),
                        aq.getUserAnswer(),
                        aq.getQuestion().getCorrectAnswer(),
                        aq.getIsCorrect(),
                        aq.getQuestion().getExplanation()))
                .toList();

        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getScore(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                attempt.getSection().getPassScore(),
                attempt.getIsPassed(),
                isNextSectionUnlocked,
                earnedBadge,
                resultItems);
    }

    /**
     * JSON 형태의 보기를 파싱한다.
     */
    private List<OptionItem> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, String>> optionMaps = objectMapper.readValue(
                    optionsJson,
                    new TypeReference<List<Map<String, String>>>() {
                    });

            return optionMaps.stream()
                    .map(map -> new OptionItem(map.get("id"), map.get("text")))
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse options JSON: {}", optionsJson, e);
            return Collections.emptyList();
        }
    }
}
