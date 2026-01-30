package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.ContinuousAnswerResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.ContinuousQuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Continuous Learning 모드 서비스.
 *
 * <p>Sayvoca 스타일의 단일 문제 흐름 (Submit → Next)을 제공한다.</p>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>Attempt 레코드 생성 없음 — user_review_items 직접 업데이트</li>
 *   <li>Section Locking 없음 — 자유롭게 학습 가능</li>
 *   <li><b>확률적 가중치</b> 기반 랜덤 문제 선택 (엄격한 우선순위 아님)</li>
 *   <li>FSRS 기반 즉시 상태 갱신</li>
 *   <li><b>무한 루프</b> — 섹션 완료 개념 없음, 동일 문제 재출제 가능</li>
 * </ul>
 *
 * <h3>확률적 가중치 로직</h3>
 * <ul>
 *   <li>신규 문제: 가중치 10.0 (가장 높음)</li>
 *   <li>복습 필요 (Due): 가중치 5.0</li>
 *   <li>학습 완료: 가중치 1/(reps+1) (반복할수록 감소)</li>
 * </ul>
 * <p>모든 문제가 선택될 수 있으며, 100번 푼 문제도 다시 출제될 수 있음.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContinuousQuizService {

    private final ContinuousQuizRepository learningRepository;
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

        // 4. 정규화된 정답 (프론트엔드 표시용)
        String normalizedCorrectAnswer = normalizeCorrectAnswer(question.getCorrectAnswer());

        log.info("Continuous Learning 답변 처리 - userId: {}, questionId: {}, isCorrect: {}, " +
                        "stability: {:.2f}, nextReview: {}",
                userId, questionId, isCorrect,
                reviewItem.getStability(), reviewItem.getNextReviewAt());

        // 5. 결과 반환
        return ContinuousAnswerResponse.builder()
                .questionId(questionId)
                .isCorrect(isCorrect)
                .userAnswer(request.getUserAnswer())
                .correctAnswer(normalizedCorrectAnswer)
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
     * 다음 문제 조회 (확률적 가중치 기반 랜덤 선택).
     *
     * <p>신규/Due 문제가 높은 확률로 선택되지만, 모든 문제가 선택될 수 있음.</p>
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
     * <p>Single API Call로 처리:</p>
     * <ol>
     *   <li>현재 문제 답변 처리 (FSRS 업데이트)</li>
     *   <li>다음 문제 조회 (확률적 가중치 기반)</li>
     *   <li>통합 응답 반환</li>
     * </ol>
     *
     * <p><b>무한 루프:</b> 섹션 완료 개념 없음. 섹션에 문제가 있으면 항상 다음 문제 반환.</p>
     * <p>방금 푼 문제를 제외하되, 섹션에 문제가 1개뿐이면 같은 문제 재출제.</p>
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
        String normalizedCorrectAnswer = normalizeCorrectAnswer(currentQuestion.getCorrectAnswer());

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
                .scheduledDays(reviewItem.getScheduledDays())
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
            // 섹션에 문제가 없는 경우 (이론상 발생하지 않음)
            builder.nextQuestion(null);
        }

        log.info("Atomic Submit & Next - userId: {}, questionId: {}, isCorrect: {}, " +
                        "nextQuestionId: {}, stability: {:.2f}",
                userId, questionId, isCorrect,
                nextQuestionOpt.map(QuizCourseQuestion::getId).orElse(null),
                reviewItem.getStability());

        return builder.build();
    }

    // ══════════════════════════════════════════════════════
    //  Private
    // ══════════════════════════════════════════════════════

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 정답 여부를 판정한다.
     *
     * <p>객관식 문제의 경우:</p>
     * <ul>
     *   <li>userAnswer는 선택지 ID (A, B, C 등) - 프론트엔드에서 전송</li>
     *   <li>correctAnswer가 ID인 경우: 직접 비교</li>
     *   <li>correctAnswer가 텍스트인 경우: options에서 해당 ID의 텍스트를 찾아 비교</li>
     * </ul>
     *
     * <p>다중선택 문제의 경우:</p>
     * <ul>
     *   <li>userAnswer는 쉼표로 구분된 ID 목록 (예: "A,C")</li>
     *   <li>correctAnswer도 쉼표로 구분된 형식 (예: "A,C" 또는 "int,double")</li>
     *   <li>순서와 관계없이 집합 비교</li>
     * </ul>
     *
     * <p>단답형의 경우: 대소문자 무시, 공백 트림 후 비교</p>
     */
    private boolean checkAnswer(QuizCourseQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            log.info("[채점] questionId={} - 사용자 답변이 비어있음", question.getId());
            return false;
        }

        String rawCorrectAnswer = question.getCorrectAnswer().trim();
        String trimmedUserAnswer = userAnswer.trim();
        QuestionType questionType = question.getQuestionType();

        // 0. correct_answer가 JSON 형식인 경우 파싱 (예: "[\"B\"]" → "B")
        String normalizedCorrectAnswer = normalizeCorrectAnswer(rawCorrectAnswer);

        // 채점 추적 로그 (INFO 레벨로 변경하여 운영 환경에서도 확인 가능)
        log.info("[채점] questionId={}, questionType={}, userAnswer='{}', rawCorrect='{}', normalizedCorrect='{}', options='{}'",
                question.getId(), questionType, trimmedUserAnswer, rawCorrectAnswer, normalizedCorrectAnswer,
                question.getOptions() != null ? question.getOptions().substring(0, Math.min(100, question.getOptions().length())) : "null");

        // 1. 대소문자 무시 직접 비교 (단답형 또는 ID가 일치하는 경우)
        if (normalizedCorrectAnswer.equalsIgnoreCase(trimmedUserAnswer)) {
            log.info("[채점] questionId={} - 직접 비교 일치 → 정답", question.getId());
            return true;
        }

        // 2. 객관식 문제이고 options가 있는 경우, ID로 텍스트 찾아 비교
        if ((questionType == QuestionType.MULTIPLE_CHOICE ||
             questionType == QuestionType.MULTIPLE_CHOICE_MULTIPLE)
                && question.getOptions() != null && !question.getOptions().isBlank()) {
            boolean result = checkMultipleChoiceAnswer(question, trimmedUserAnswer, normalizedCorrectAnswer);
            log.info("[채점] questionId={} - 객관식 ID↔텍스트 비교 → {}", question.getId(), result ? "정답" : "오답");
            return result;
        }

        log.info("[채점] questionId={} - 매칭 실패 → 오답", question.getId());
        return false;
    }

    /**
     * correct_answer 필드를 정규화한다.
     *
     * <p>DB에 저장된 correct_answer 형식이 다양할 수 있음:</p>
     * <ul>
     *   <li>JSON 배열: ["B"] → "B"</li>
     *   <li>JSON 배열 (다중): ["A", "C"] → "A,C"</li>
     *   <li>JSON 문자열: "B" (따옴표 포함) → B</li>
     *   <li>일반 문자열: B → B (그대로)</li>
     * </ul>
     *
     * @param rawCorrectAnswer DB에서 가져온 원본 correct_answer
     * @return 정규화된 정답 문자열
     */
    private String normalizeCorrectAnswer(String rawCorrectAnswer) {
        if (rawCorrectAnswer == null || rawCorrectAnswer.isBlank()) {
            return "";
        }

        String trimmed = rawCorrectAnswer.trim();

        // JSON 배열 형식인 경우: ["B"] 또는 ["A", "C"]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                if (root.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < root.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(root.get(i).asText().trim());
                    }
                    String result = sb.toString();
                    log.debug("JSON 배열 파싱 성공: '{}' → '{}'", trimmed, result);
                    return result;
                }
            } catch (Exception e) {
                log.warn("correct_answer JSON 파싱 실패: '{}', error: {}", trimmed, e.getMessage());
            }
        }

        // JSON 문자열 형식인 경우: "B" (따옴표로 감싸진 경우)
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.isTextual()) {
                    String result = node.asText().trim();
                    log.debug("JSON 문자열 파싱 성공: '{}' → '{}'", trimmed, result);
                    return result;
                }
            } catch (Exception e) {
                // 파싱 실패 시 따옴표만 제거
                String result = trimmed.substring(1, trimmed.length() - 1).trim();
                log.debug("따옴표 제거: '{}' → '{}'", trimmed, result);
                return result;
            }
        }

        // 일반 문자열인 경우 그대로 반환
        return trimmed;
    }

    /**
     * 객관식 문제의 정답 여부를 판정한다.
     *
     * <p>다양한 데이터 형식을 지원:</p>
     * <ol>
     *   <li>correctAnswer가 옵션 ID인 경우 (예: "A", "A,B")</li>
     *   <li>correctAnswer가 옵션 텍스트인 경우 (예: "int", "int,double")</li>
     * </ol>
     */
    private boolean checkMultipleChoiceAnswer(QuizCourseQuestion question,
                                               String userAnswer,
                                               String correctAnswer) {
        QuestionType questionType = question.getQuestionType();

        // 다중선택의 경우 집합 비교
        if (questionType == QuestionType.MULTIPLE_CHOICE_MULTIPLE) {
            return checkMultipleSelectionAnswer(question, userAnswer, correctAnswer);
        }

        // 단일선택의 경우
        // userAnswer는 옵션 ID (예: "A")
        // correctAnswer가 ID인지 텍스트인지 확인

        // 먼저 직접 비교 (correctAnswer가 ID인 경우)
        if (correctAnswer.equalsIgnoreCase(userAnswer)) {
            log.info("[채점-단일선택] 직접 ID 비교 일치: userAnswer='{}' == correctAnswer='{}'",
                    userAnswer, correctAnswer);
            return true;
        }

        // correctAnswer가 텍스트일 수 있으므로, userAnswer(ID)에 해당하는 텍스트 찾아 비교
        String userOptionText = findOptionTextById(question.getOptions(), userAnswer);
        log.info("[채점-단일선택] ID→텍스트 변환: userAnswer(ID)='{}' → text='{}'",
                userAnswer, userOptionText);

        if (userOptionText != null && correctAnswer.equalsIgnoreCase(userOptionText)) {
            log.info("[채점-단일선택] 텍스트 비교 일치: optionText='{}' == correctAnswer='{}'",
                    userOptionText, correctAnswer);
            return true;
        }

        // correctAnswer가 ID일 수 있으므로, correctAnswer(ID)의 텍스트를 찾아 userAnswer(ID)의 텍스트와 비교
        String correctOptionText = findOptionTextById(question.getOptions(), correctAnswer);
        log.info("[채점-단일선택] correctAnswer(ID)='{}' → text='{}'",
                correctAnswer, correctOptionText);

        if (correctOptionText != null && userOptionText != null
                && correctOptionText.equalsIgnoreCase(userOptionText)) {
            log.info("[채점-단일선택] 양측 텍스트 비교 일치: userText='{}' == correctText='{}'",
                    userOptionText, correctOptionText);
            return true;
        }

        log.info("[채점-단일선택] 모든 비교 실패 - userAnswer='{}', correctAnswer='{}', userText='{}', correctText='{}'",
                userAnswer, correctAnswer, userOptionText, correctOptionText);
        return false;
    }

    /**
     * 다중선택 문제의 정답 여부를 판정한다.
     * 순서와 관계없이 선택한 옵션들이 정답 옵션들과 일치하는지 확인.
     */
    private boolean checkMultipleSelectionAnswer(QuizCourseQuestion question,
                                                  String userAnswer,
                                                  String correctAnswer) {
        // 쉼표로 구분하여 집합으로 변환
        Set<String> userAnswers = Arrays.stream(userAnswer.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> correctAnswers = Arrays.stream(correctAnswer.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // 1. 직접 비교 (둘 다 ID이거나 둘 다 텍스트인 경우)
        if (userAnswers.equals(correctAnswers)) {
            return true;
        }

        // 2. userAnswers는 ID, correctAnswers는 텍스트일 수 있음
        // userAnswer의 각 ID를 텍스트로 변환하여 비교
        Set<String> userTexts = userAnswers.stream()
                .map(id -> {
                    String text = findOptionTextById(question.getOptions(), id);
                    return text != null ? text.toLowerCase() : id;
                })
                .collect(Collectors.toSet());

        if (userTexts.equals(correctAnswers)) {
            log.debug("다중선택 정답 일치: userTexts={}, correctAnswers={}", userTexts, correctAnswers);
            return true;
        }

        // 3. correctAnswers도 ID일 수 있으므로 텍스트로 변환하여 비교
        Set<String> correctTexts = correctAnswers.stream()
                .map(id -> {
                    String text = findOptionTextById(question.getOptions(), id);
                    return text != null ? text.toLowerCase() : id;
                })
                .collect(Collectors.toSet());

        return userTexts.equals(correctTexts);
    }

    /**
     * options JSON에서 특정 ID의 텍스트를 찾는다.
     *
     * <p>지원하는 JSON 형식:</p>
     * <ul>
     *   <li>객체 배열: [{"id": "A", "text": "int"}, {"id": "B", "text": "integer"}]</li>
     *   <li>문자열 배열: ["int", "integer"] (ID는 인덱스 기반 A, B, C로 생성)</li>
     * </ul>
     *
     * @param optionsJson options JSON 문자열
     * @param id 찾을 ID (A, B, C 등)
     * @return 해당 ID의 텍스트, 없으면 null
     */
    private String findOptionTextById(String optionsJson, String id) {
        if (optionsJson == null || optionsJson.isBlank() || id == null) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(optionsJson);

            if (!root.isArray()) {
                return null;
            }

            int index = 0;
            for (JsonNode node : root) {
                String optionId;
                String optionText;

                if (node.isObject()) {
                    // 형식: [{"id": "A", "text": "int"}, ...]
                    optionId = node.has("id") ? node.get("id").asText() : generateIdFromIndex(index);
                    optionText = node.has("text") ? node.get("text").asText() : "";
                } else if (node.isTextual()) {
                    // 형식: ["int", "integer", ...]
                    optionId = generateIdFromIndex(index);
                    optionText = node.asText();
                } else {
                    index++;
                    continue;
                }

                if (optionId.equalsIgnoreCase(id)) {
                    return optionText;
                }
                index++;
            }
        } catch (Exception e) {
            log.warn("옵션 JSON 파싱 실패: {}, error: {}", optionsJson, e.getMessage());
        }

        return null;
    }

    /**
     * 인덱스를 기반으로 옵션 ID를 생성한다 (0 -> "A", 1 -> "B", ...).
     */
    private String generateIdFromIndex(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
