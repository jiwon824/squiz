package com.ssafy.domain.quiz.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.ContinuousAnswerResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.repository.ContinuousLearningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Continuous Learning 모드 서비스.
 *
 * <p>Sayvoca 스타일의 단일 문제 흐름 (Submit → Next)을 제공한다.</p>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>Attempt 레코드 생성 없음 — user_review_items 직접 업데이트</li>
 *   <li>Section Locking 없음 — 자유롭게 학습 가능</li>
 *   <li>가중치 기반 랜덤 문제 선택</li>
 *   <li>FSRS 기반 즉시 상태 갱신</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContinuousLearningService {

    private final ContinuousLearningRepository learningRepository;
    private final FsrsService fsrsService;

    /**
     * 답변 제출 및 FSRS 상태 즉시 업데이트.
     *
     * <p>Attempt 레코드를 생성하지 않고, user_review_items 테이블만 직접 업데이트한다.</p>
     *
     * <h4>처리 흐름</h4>
     * <ol>
     *   <li>문제 존재 여부 검증</li>
     *   <li>정답 여부 판정</li>
     *   <li>FsrsService.processReview()로 FSRS 상태 갱신
     *       <ul>
     *         <li>Stability (기억 안정성)</li>
     *         <li>Difficulty (난이도)</li>
     *         <li>Interval (다음 복습 간격)</li>
     *         <li>State (학습 상태)</li>
     *       </ul>
     *   </li>
     *   <li>결과 반환 (정답 여부, 정답, 해설, FSRS 갱신 정보)</li>
     * </ol>
     *
     * @param userId    사용자 ID
     * @param questionId 문제 ID
     * @param request   답변 요청 (userAnswer, responseTimeMs)
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

        // 2. 정답 여부 판정
        boolean isCorrect = checkAnswer(question, request.getUserAnswer());

        // 3. FSRS 상태 즉시 갱신 (Attempt 없이 직접 업데이트)
        UserReviewItem reviewItem = fsrsService.processReview(
                userId,
                ReviewContentType.COURSE_QUESTION,
                questionId,
                isCorrect,
                request.getResponseTimeMs()
        );

        log.info("Continuous Learning 답변 처리 - userId: {}, questionId: {}, isCorrect: {}, " +
                        "stability: {:.2f}, nextReview: {}",
                userId, questionId, isCorrect,
                reviewItem.getStability(), reviewItem.getNextReviewAt());

        // 4. 결과 반환
        return ContinuousAnswerResponse.builder()
                .questionId(questionId)
                .isCorrect(isCorrect)
                .userAnswer(request.getUserAnswer())
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                // FSRS 갱신 정보
                .stability(reviewItem.getStability())
                .difficulty(reviewItem.getDifficulty())
                .scheduledDays(reviewItem.getScheduledDays())
                .nextReviewAt(reviewItem.getNextReviewAt())
                .state(reviewItem.getState())
                .reps(reviewItem.getReps())
                .lapses(reviewItem.getLapses())
                .build();
    }

    /**
     * 다음 문제 조회 (가중치 기반 랜덤 선택).
     *
     * @param userId        사용자 ID
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @return 다음 문제 (없으면 예외)
     */
    public ContinuousQuestionResponse getNextQuestion(Long userId, Long courseId,
                                                       Integer sectionNumber) {
        QuizCourseQuestion question = learningRepository
                .findNextQuestionWeightedRandom(courseId, sectionNumber, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NO_QUESTIONS_AVAILABLE",
                        "해당 섹션에 문제가 없습니다."));

        return ContinuousQuestionResponse.from(question);
    }

    /**
     * Atomic Submit & Fetch Next - 답변 제출 후 다음 문제까지 한 번에 반환.
     *
     * <p>Single API Call로 처리:</p>
     * <ol>
     *   <li>현재 문제 답변 처리 (FSRS 업데이트)</li>
     *   <li>다음 문제 조회 (Priority 기반: New > Due > Random)</li>
     *   <li>통합 응답 반환</li>
     * </ol>
     *
     * @param userId     사용자 ID
     * @param questionId 현재 문제 ID
     * @param request    답변 요청 (userAnswer, responseTimeMs)
     * @return 답변 결과 + 다음 문제 (섹션 완료 시 nextQuestion = null)
     */
    @Transactional
    public ContinuousSubmitResponse processAnswerAndGetNext(Long userId, Long questionId,
                                                             ContinuousAnswerRequest request) {
        // 1. 현재 문제 조회
        QuizCourseQuestion currentQuestion = learningRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                        "문제를 찾을 수 없습니다. id=" + questionId));

        // 2. 정답 여부 판정
        boolean isCorrect = checkAnswer(currentQuestion, request.getUserAnswer());

        // 3. FSRS 상태 즉시 갱신
        UserReviewItem reviewItem = fsrsService.processReview(
                userId,
                ReviewContentType.COURSE_QUESTION,
                questionId,
                isCorrect,
                request.getResponseTimeMs()
        );

        // 4. 다음 문제 조회 (Priority 기반, 현재 문제 제외)
        Long courseId = currentQuestion.getSection().getQuizCourseId();
        Integer sectionNumber = currentQuestion.getSection().getSectionNumber();

        Optional<QuizCourseQuestion> nextQuestionOpt = learningRepository
                .findNextQuestionByPriority(courseId, sectionNumber, userId, questionId);

        // 5. 통합 응답 빌드
        ContinuousSubmitResponse.ContinuousSubmitResponseBuilder builder = ContinuousSubmitResponse.builder()
                // 답변 결과
                .submittedQuestionId(questionId)
                .isCorrect(isCorrect)
                .userAnswer(request.getUserAnswer())
                .correctAnswer(currentQuestion.getCorrectAnswer())
                .explanation(currentQuestion.getExplanation())
                // FSRS 갱신 정보
                .stability(reviewItem.getStability())
                .difficulty(reviewItem.getDifficulty())
                .scheduledDays(reviewItem.getScheduledDays())
                .nextReviewAt(reviewItem.getNextReviewAt())
                .state(reviewItem.getState())
                .reps(reviewItem.getReps())
                .lapses(reviewItem.getLapses());

        // 다음 문제 설정
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
                            .build())
                    .sectionCompleted(false);
        } else {
            builder.nextQuestion(null)
                    .sectionCompleted(true);
        }

        log.info("Atomic Submit & Next - userId: {}, questionId: {}, isCorrect: {}, " +
                        "hasNext: {}, stability: {:.2f}",
                userId, questionId, isCorrect, nextQuestionOpt.isPresent(),
                reviewItem.getStability());

        return builder.build();
    }

    // ══════════════════════════════════════════════════════
    //  Private
    // ══════════════════════════════════════════════════════

    /**
     * 정답 여부를 판정한다.
     * 대소문자 무시, 공백 트림 후 비교.
     */
    private boolean checkAnswer(QuizCourseQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        String correct = question.getCorrectAnswer().trim().toLowerCase();
        String user = userAnswer.trim().toLowerCase();
        return correct.equals(user);
    }
}
