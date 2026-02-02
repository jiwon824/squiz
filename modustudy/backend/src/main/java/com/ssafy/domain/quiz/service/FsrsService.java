package com.ssafy.domain.quiz.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.quiz.dto.response.ReviewHistoryResponse;
import com.ssafy.domain.quiz.dto.response.ReviewStatsResponse;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewLog;
import com.ssafy.domain.quiz.repository.UserReviewItemRepository;
import com.ssafy.domain.quiz.repository.UserReviewLogRepository;
import com.ssafy.domain.quiz.util.QuizGradingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.quiz.dto.response.OptionItem;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse.QuestionDetail;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse.ReviewItemDto;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.repository.ContinuousQuizRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FSRS v14 기반 간격 반복 복습 서비스
 * <p>
 * MalBoka 방식: 사용자가 난이도 버튼을 직접 선택하지 않고,
 * 정답 여부(is_correct)와 응답 시간(response_time_ms)으로 Rating을 자동 산출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FsrsService {

    private final UserReviewItemRepository reviewItemRepository;
    private final UserReviewLogRepository reviewLogRepository;

    private final ContinuousQuizRepository continuousQuizRepository;
    private final ObjectMapper objectMapper;

    // ── Rating 자동 산출 ──

    /**
     * 정답 여부와 응답 시간으로 FSRS Rating(1~4)을 자동 산출한다.
     * <ul>
     * <li>오답 → 1 (Again)</li>
     * <li>정답 &amp; 응답 시간 &gt; 5초 → 2 (Hard)</li>
     * <li>정답 &amp; 2초 &lt; 응답 시간 ≤ 5초 → 3 (Good)</li>
     * <li>정답 &amp; 응답 시간 ≤ 2초 → 4 (Easy)</li>
     * </ul>
     */
    public int calculateRating(boolean isCorrect, long responseTimeMs) {
        if (!isCorrect) {
            return FsrsConstants.RATING_AGAIN;
        }
        if (responseTimeMs > FsrsConstants.HARD_THRESHOLD_MS) {
            return FsrsConstants.RATING_HARD;
        }
        if (responseTimeMs > FsrsConstants.EASY_THRESHOLD_MS) {
            return FsrsConstants.RATING_GOOD;
        }
        return FsrsConstants.RATING_EASY;
    }

    // ── FSRS 상태 갱신 ──

    /**
     * FSRS v14 알고리즘으로 복습 항목의 상태를 갱신한다.
     * <p>
     * 새 카드(state=0)는 초기 안정성/난이도를 설정하고,
     * 기존 카드는 Retrievability 기반으로 안정성/난이도를 재계산한다.
     */
    public void updateFsrsState(UserReviewItem item, int rating) {
        if (item.getState() == FsrsConstants.STATE_NEW) {
            initializeNewCard(item, rating);
        } else {
            updateExistingCard(item, rating);
        }

        // 다음 복습 간격 계산
        int scheduledDays = calculateScheduledDays(item.getStability());
        item.setScheduledDays(scheduledDays);
        item.setReps(item.getReps() + 1);

        LocalDateTime now = LocalDateTime.now();
        item.setLastReviewedAt(now);
        item.setNextReviewAt(now.plusDays(scheduledDays));
    }

    // ── 복습 처리 (Upsert + Log) ──

    /**
     * 복습 결과를 처리하고 FSRS 상태를 업데이트한다. (서버 사이드 채점 포함)
     *
     * @param userId         사용자 ID
     * @param contentType    콘텐츠 유형
     * @param contentId      콘텐츠 ID
     * @param userAnswer     사용자 답안
     * @param responseTimeMs 응답 시간 (밀리초)
     * @return 업데이트된 UserReviewItem
     */
    @Transactional
    public UserReviewItem processReview(Long userId, ReviewContentType contentType,
            Long contentId, String userAnswer, long responseTimeMs) {

        boolean isCorrect = false;

        // 1. 정답 조회 및 채점
        if (contentType == ReviewContentType.COURSE_QUESTION) {
            // 문제 조회
            QuizCourseQuestion question = continuousQuizRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + contentId));

            String correctAnswer = question.getCorrectAnswer();
            isCorrect = QuizGradingUtils.grade(userAnswer, correctAnswer, question.getQuestionType(),
                    question.getOptions(), question.getKeywords());

        } else if (contentType == ReviewContentType.STUDY_QUESTION) {
            // TODO: 스터디 퀴즈(GPT 생성)의 경우 정답 저장 방식에 따라 처리 필요
            // 현재는 임시로 정답 처리 (또는 별도 조회 로직 구현)
            log.warn("Study question grading not fully implemented yet. Assuming correct.");
            isCorrect = true;
        }

        // 2. FSRS 로직 위임
        return processReviewResult(userId, contentType, contentId, isCorrect, responseTimeMs);
    }

    /**
     * 복습 결과(정답 여부)를 기반으로 FSRS 상태를 업데이트한다.
     * <ol>
     * <li>UserReviewItem을 조회하거나 신규 생성 (Upsert)</li>
     * <li>FSRS v14 알고리즘으로 상태 갱신</li>
     * <li>UserReviewLog에 이력 기록</li>
     * </ol>
     */
    @Transactional
    public UserReviewItem processReviewResult(Long userId, ReviewContentType contentType,
            Long contentId, boolean isCorrect, long responseTimeMs) {

        int rating = calculateRating(isCorrect, responseTimeMs);

        // Upsert: 기존 항목 조회 또는 신규 생성
        UserReviewItem item = reviewItemRepository
                .findByUserIdAndContentTypeAndContentId(userId, contentType, contentId)
                .orElseGet(() -> UserReviewItem.builder()
                        .userId(userId)
                        .contentType(contentType)
                        .contentId(contentId)
                        .build());

        // FSRS 상태 갱신
        updateFsrsState(item, rating);
        item.setLastResponseTimeMs(responseTimeMs);

        // 저장 (신규: INSERT / 기존: dirty checking UPDATE)
        reviewItemRepository.save(item);

        // 복습 이력 기록
        UserReviewLog reviewLog = UserReviewLog.builder()
                .reviewItem(item)
                .isCorrect(isCorrect)
                .responseTimeMs(responseTimeMs)
                .stability(item.getStability())
                .difficulty(item.getDifficulty())
                .build();
        reviewLogRepository.save(reviewLog);

        log.info("복습 처리 완료 - userId: {}, contentType: {}, contentId: {}, rating: {}, " +
                "stability: {}, nextReview: {}",
                userId, contentType, contentId, rating,
                String.format("%.2f", item.getStability()), item.getNextReviewAt());

        return item;
    }

    /**
     * 특정 사용자의 복습 예정 항목을 조회하고, 문제 정보(지문, 보기, 정답 등)를 함께 반환한다.
     */
    public List<ReviewItemDto> getTodayReviewsWithQuestions(Long userId) {
        // 1. 복습 예정 항목 조회
        List<UserReviewItem> dueItems = reviewItemRepository.findDueItems(userId, LocalDateTime.now());

        // (테스트용) 내일 이 시간까지 포함해서 조회
        // List<UserReviewItem> dueItems = reviewItemRepository.findDueItems(userId,
        // LocalDateTime.now().plusDays(1));
        return enrichReviewItems(dueItems);
    }

    /**
     * 특정 사용자의 오답 노트(많이 틀린 문제)를 조회하고, 문제 정보와 함께 반환한다.
     */
    public List<ReviewItemDto> getWrongAnswersWithQuestions(Long userId) {
        // 1. 오답 노트 조회
        List<UserReviewItem> wrongItems = reviewItemRepository.findWrongAnswers(userId);
        return enrichReviewItems(wrongItems);
    }

    private List<ReviewItemDto> enrichReviewItems(List<UserReviewItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        // 2. COURSE_QUESTION ID 수집
        List<Long> courseQuestionIds = items.stream()
                .filter(item -> item.getContentType() == ReviewContentType.COURSE_QUESTION)
                .map(UserReviewItem::getContentId)
                .toList();

        // 3. 문제 일괄 조회
        Map<Long, QuizCourseQuestion> questionMap = continuousQuizRepository.findAllById(courseQuestionIds).stream()
                .collect(Collectors.toMap(QuizCourseQuestion::getId, Function.identity()));

        // 4. DTO 조립
        return items.stream().map(item -> {
            QuestionDetail questionDetail = null;

            if (item.getContentType() == ReviewContentType.COURSE_QUESTION) {
                QuizCourseQuestion q = questionMap.get(item.getContentId());
                if (q != null) {
                    List<OptionItem> options = parseOptions(q.getOptions());
                    String category = q.getSection().getCourse().getName(); // 코스명을 카테고리로 사용

                    questionDetail = new QuestionDetail(
                            q.getQuestionNumber(),
                            q.getQuestionText(),
                            q.getQuestionType(),
                            options,
                            q.getCorrectAnswer(),
                            q.getExplanation(),
                            category,
                            item.getLastReviewedAt());
                }
            }
            // TODO: STUDY_QUESTION 처리 로직 추가 가능

            return ReviewItemDto.from(item, questionDetail);
        }).toList();
    }

    /**
     * 특정 사용자의 복습 예정 항목을 조회한다. (nextReviewAt ≤ 현재 시각)
     */
    public List<UserReviewItem> getDueItems(Long userId) {
        return reviewItemRepository.findDueItems(userId, LocalDateTime.now());
    }

    private List<OptionItem> parseOptions(String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<OptionItem>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("옵션 파싱 실패: {}", json, e);
            return List.of();
        }
    }

    // ── 복습 통계 조회 ──

    /**
     * 사용자의 전체 복습 통계를 조회한다.
     * <p>
     * 상태별 항목 수, 평균 안정성, 총 복습/오답 횟수, 숙련도 등을 집계한다.
     *
     * @param userId 사용자 ID
     * @return 복습 통계 응답 DTO
     */
    public ReviewStatsResponse getStats(Long userId) {
        List<UserReviewItem> allItems = reviewItemRepository.findAllByUserId(userId);
        long dueCount = reviewItemRepository
                .countByUserIdAndNextReviewAtBefore(userId, LocalDateTime.now());

        return ReviewStatsResponse.from(allItems, (int) dueCount);
    }

    // ── 복습 항목 이력 조회 ──

    /**
     * 특정 복습 항목의 상세 정보와 최근 복습 이력을 조회한다.
     * <p>
     * 본인의 복습 항목만 조회할 수 있으며, 타인의 항목 접근 시 403을 반환한다.
     *
     * @param userId       사용자 ID
     * @param reviewItemId 복습 항목 ID
     * @return 복습 항목 상세 및 최근 10건 이력
     * @throws BusinessException 항목 미존재(404) 또는 접근 권한 없음(403)
     */
    public ReviewHistoryResponse getReviewHistory(Long userId, Long reviewItemId) {
        UserReviewItem item = reviewItemRepository.findById(reviewItemId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "REVIEW_ITEM_NOT_FOUND",
                        "복습 항목을 찾을 수 없습니다. id=" + reviewItemId));

        // 본인 소유 검증
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED",
                    "해당 복습 항목에 접근 권한이 없습니다.");
        }

        // 최근 10건 복습 이력 조회
        List<UserReviewLog> logs = reviewLogRepository
                .findTop10ByReviewItemIdOrderByReviewedAtDesc(reviewItemId);

        return ReviewHistoryResponse.from(item, logs);
    }

    // ══════════════════════════════════════════════════════
    // Private — FSRS v14 핵심 수식
    // ══════════════════════════════════════════════════════

    /**
     * 새 카드(state=NEW)의 초기 안정성/난이도를 설정한다.
     * 
     * <pre>
     * S0(G) = w[G - 1]
     * D0(G) = w[4] - exp(w[5] * (G - 1)) + 1
     * </pre>
     */
    private void initializeNewCard(UserReviewItem item, int rating) {
        double[] w = FsrsConstants.W;

        double stability = w[rating - 1];
        double difficulty = w[4] - Math.exp(w[5] * (rating - 1)) + 1;
        difficulty = clampDifficulty(difficulty);

        item.setStability(stability);
        item.setDifficulty(difficulty);
        item.setElapsedDays(0);
        item.setLastElapsedDays(0);

        // 상태 전이
        if (rating == FsrsConstants.RATING_AGAIN) {
            item.setState(FsrsConstants.STATE_LEARNING);
            item.setLapses(item.getLapses() + 1);
        } else {
            item.setState(FsrsConstants.STATE_REVIEW);
        }
    }

    /**
     * 기존 카드(Learning/Review/Relearning)의 FSRS 상태를 갱신한다.
     * 
     * <pre>
     * Retrievability:  R = (1 + FACTOR * t / S) ^ DECAY
     * Difficulty:      D' = D - w[6] * (G - 3)  →  mean reversion
     * Stability(정답): S' = S * (e^w[8] * (11-D) * S^(-w[9]) * (e^(w[10]*(1-R)) - 1) * penalty/bonus + 1)
     * Stability(오답): S' = w[11] * D^(-w[12]) * (S+1)^w[13] * e^(w[14]*(1-R))
     * </pre>
     */
    private void updateExistingCard(UserReviewItem item, int rating) {
        double[] w = FsrsConstants.W;
        double S = item.getStability();
        double D = item.getDifficulty();
        int elapsedDays = calculateElapsedDays(item);

        item.setLastElapsedDays(item.getElapsedDays());
        item.setElapsedDays(elapsedDays);

        // Retrievability
        double R = calculateRetrievability(elapsedDays, S);
        item.setRetrievability(R);

        // 난이도 갱신
        double newD = updateDifficulty(D, rating);
        item.setDifficulty(newD);

        // 안정성 갱신
        double newS;
        if (rating == FsrsConstants.RATING_AGAIN) {
            // 오답: 안정성 감소, Relearning 상태로 전이
            newS = w[11]
                    * Math.pow(newD, -w[12])
                    * (Math.pow(S + 1, w[13]) - 1)
                    * Math.exp(w[14] * (1 - R));
            item.setLapses(item.getLapses() + 1);
            item.setState(FsrsConstants.STATE_RELEARNING);
        } else {
            // 정답: 안정성 증가
            double hardPenalty = (rating == FsrsConstants.RATING_HARD) ? w[15] : 1.0;
            double easyBonus = (rating == FsrsConstants.RATING_EASY) ? w[16] : 1.0;

            newS = S * (Math.exp(w[8])
                    * (11 - newD)
                    * Math.pow(S, -w[9])
                    * (Math.exp(w[10] * (1 - R)) - 1)
                    * hardPenalty
                    * easyBonus
                    + 1);
            item.setState(FsrsConstants.STATE_REVIEW);
        }

        item.setStability(Math.max(0.01, newS));
    }

    /**
     * 난이도 갱신 — Mean Reversion 적용
     * 
     * <pre>
     * D0(G) = w[4] - exp(w[5] * (G - 1)) + 1
     * D'    = D - w[6] * (G - 3)
     * D_new = w[7] * D0(G) + (1 - w[7]) * D'
     * </pre>
     */
    private double updateDifficulty(double D, int rating) {
        double[] w = FsrsConstants.W;

        double d0 = w[4] - Math.exp(w[5] * (rating - 1)) + 1;
        double deltaD = -w[6] * (rating - 3);
        double dPrime = D + deltaD;

        double newD = w[7] * d0 + (1 - w[7]) * dPrime;
        return clampDifficulty(newD);
    }

    /**
     * Retrievability 계산 (Power Forgetting Curve)
     * 
     * <pre>
     * R(t, S) = (1 + FACTOR * t / S) ^ DECAY
     * </pre>
     */
    private double calculateRetrievability(int elapsedDays, double stability) {
        if (stability <= 0) {
            return 0.0;
        }
        return Math.pow(
                1 + FsrsConstants.FACTOR * elapsedDays / stability,
                FsrsConstants.DECAY);
    }

    /**
     * 안정성(Stability)으로부터 다음 복습 간격(일)을 산출한다.
     * 
     * <pre>
     * interval = S / FACTOR * (desired_retention ^ (1 / DECAY) - 1)
     * </pre>
     * 
     * DESIRED_RETENTION = 0.9, DECAY = -0.5 일 때 interval ≈ S (설계 의도)
     */
    private int calculateScheduledDays(double stability) {
        double interval = stability / FsrsConstants.FACTOR
                * (Math.pow(FsrsConstants.DESIRED_RETENTION, 1.0 / FsrsConstants.DECAY) - 1);
        return Math.max(1, (int) Math.round(interval));
    }

    /**
     * 마지막 복습 이후 경과일 계산
     */
    private int calculateElapsedDays(UserReviewItem item) {
        if (item.getLastReviewedAt() == null) {
            return 0;
        }
        long days = Duration.between(item.getLastReviewedAt(), LocalDateTime.now()).toDays();
        return Math.max(0, (int) days);
    }

    /**
     * 난이도를 [1, 10] 범위로 클램핑
     */
    private double clampDifficulty(double d) {
        return Math.max(1.0, Math.min(10.0, d));
    }
}
