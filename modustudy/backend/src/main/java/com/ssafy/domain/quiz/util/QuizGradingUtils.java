package com.ssafy.domain.quiz.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 퀴즈 채점 로직을 담당하는 유틸리티 클래스.
 *
 * <p>
 * ContinuousQuizService(연속 학습)와 FsrsService(FSRS 복습) 등
 * 여러 도메인에서 동일한 채점 기준을 적용하기 위해 분리됨.
 * </p>
 */
@Slf4j
public class QuizGradingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 답안을 채점한다 (옵션 정보 없이 - 단순 비교).
     */
    public static boolean grade(String userAnswer, String correctAnswer, QuestionType questionType) {
        return grade(userAnswer, correctAnswer, questionType, null, null);
    }

    /**
     * 답안을 채점한다 (옵션 정보 포함 - ID/텍스트 매핑 지원).
     *
     * @param userAnswer    사용자가 제출한 답안
     * @param correctAnswer 정답
     * @param questionType  문제 유형
     * @param optionsJson   보기 옵션 JSON (객관식 ID 매핑용)
     * @return 정답 여부 (true/false)
     */
    public static boolean grade(String userAnswer, String correctAnswer, QuestionType questionType,
            String optionsJson) {
        return grade(userAnswer, correctAnswer, questionType, optionsJson, null);
    }

    /**
     * 답안을 채점한다 (키워드 기반 서술형 채점 지원).
     *
     * @param userAnswer    사용자가 제출한 답안
     * @param correctAnswer 정답
     * @param questionType  문제 유형
     * @param optionsJson   보기 옵션 JSON (객관식 ID 매핑용)
     * @param keywordsJson  서술형 채점용 키워드 JSON 배열 (예: ["키워드1", "키워드2"])
     * @return 정답 여부 (true/false)
     */
    public static boolean grade(String userAnswer, String correctAnswer, QuestionType questionType,
            String optionsJson, String keywordsJson) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        if (correctAnswer == null) {
            return false;
        }

        String trimmedUserAnswer = userAnswer.trim();
        String normalizedCorrectAnswer = normalizeCorrectAnswer(correctAnswer);

        // 1. 대소문자 무시 직접 비교 (단답형 또는 ID가 일치하는 경우)
        if (normalizedCorrectAnswer.equalsIgnoreCase(trimmedUserAnswer)) {
            return true;
        }

        // 2. 서술형 문제 채점
        if (questionType == QuestionType.SHORT_ANSWER) {
            // 2-1. 키워드가 있는 경우, 키워드 기반 채점
            if (keywordsJson != null && !keywordsJson.isBlank()) {
                return checkShortAnswerWithKeywords(trimmedUserAnswer, keywordsJson);
            }
            // 2-2. 키워드가 없는 경우, 정답이 사용자 답변에 포함되어 있으면 정답
            String normalizedUserAnswer = trimmedUserAnswer.toLowerCase().replaceAll("\\s+", " ");
            String normalizedCorrect = normalizedCorrectAnswer.toLowerCase().replaceAll("\\s+", " ");
            if (normalizedUserAnswer.contains(normalizedCorrect)) {
                log.debug("서술형 포함 채점: '{}' 포함 '{}' → 정답", normalizedCorrect, normalizedUserAnswer);
                return true;
            }
        }

        // 3. 객관식 문제이고 options가 있는 경우, ID로 텍스트 찾아 비교
        if ((questionType == QuestionType.MULTIPLE_CHOICE ||
                questionType == QuestionType.MULTIPLE_CHOICE_MULTIPLE)
                && optionsJson != null && !optionsJson.isBlank()) {
            return checkMultipleChoiceAnswer(questionType, trimmedUserAnswer, normalizedCorrectAnswer, optionsJson);
        }

        return false;
    }

    /**
     * 서술형 답안을 키워드 기반으로 채점한다.
     * 모든 키워드가 사용자 답변에 포함되어 있어야 정답 처리.
     *
     * @param userAnswer   사용자 답변
     * @param keywordsJson 키워드 JSON 배열
     * @return 정답 여부
     */
    private static boolean checkShortAnswerWithKeywords(String userAnswer, String keywordsJson) {
        try {
            JsonNode root = objectMapper.readTree(keywordsJson);
            if (!root.isArray() || root.isEmpty()) {
                return false;
            }

            String normalizedUserAnswer = userAnswer.toLowerCase().replaceAll("\\s+", " ");

            for (JsonNode keywordNode : root) {
                String keyword = keywordNode.asText().toLowerCase().trim();
                if (keyword.isEmpty()) {
                    continue;
                }
                // 키워드가 사용자 답변에 포함되어 있는지 확인
                if (!normalizedUserAnswer.contains(keyword)) {
                    log.debug("키워드 '{}' 미포함, 오답 처리", keyword);
                    return false;
                }
            }

            log.debug("모든 키워드 포함, 정답 처리");
            return true;

        } catch (Exception e) {
            log.warn("키워드 JSON 파싱 실패: {}", keywordsJson, e);
            return false;
        }
    }

    private static boolean checkMultipleChoiceAnswer(QuestionType questionType,
            String userAnswer,
            String correctAnswer,
            String optionsJson) {
        // 다중선택의 경우 집합 비교
        if (questionType == QuestionType.MULTIPLE_CHOICE_MULTIPLE) {
            return checkMultipleSelectionAnswer(userAnswer, correctAnswer, optionsJson);
        }

        // 단일선택의 경우
        // userAnswer는 옵션 ID (예: "A")
        // correctAnswer가 ID인지 텍스트인지 확인

        // 먼저 직접 비교 (correctAnswer가 ID인 경우)
        if (correctAnswer.equalsIgnoreCase(userAnswer)) {
            return true;
        }

        // correctAnswer가 텍스트일 수 있으므로, userAnswer(ID)에 해당하는 텍스트 찾아 비교
        String userOptionText = findOptionTextById(optionsJson, userAnswer);

        if (userOptionText != null && correctAnswer.equalsIgnoreCase(userOptionText)) {
            return true;
        }

        // correctAnswer가 ID일 수 있으므로, correctAnswer(ID)의 텍스트를 찾아 userAnswer(ID)의 텍스트와 비교
        String correctOptionText = findOptionTextById(optionsJson, correctAnswer);

        if (correctOptionText != null && userOptionText != null
                && correctOptionText.equalsIgnoreCase(userOptionText)) {
            return true;
        }

        return false;
    }

    private static boolean checkMultipleSelectionAnswer(String userAnswer,
            String correctAnswer,
            String optionsJson) {
        // 쉼표로 구분하여 집합으로 변환
        Set<String> userAnswers = parseAnswerToSet(userAnswer);
        Set<String> correctAnswers = parseAnswerToSet(correctAnswer);

        // 1. 직접 비교 (둘 다 ID이거나 둘 다 텍스트인 경우)
        if (userAnswers.equals(correctAnswers)) {
            return true;
        }

        // 2. userAnswers는 ID, correctAnswers는 텍스트일 수 있음
        // userAnswer의 각 ID를 텍스트로 변환하여 비교
        Set<String> userTexts = userAnswers.stream()
                .map(id -> {
                    String text = findOptionTextById(optionsJson, id);
                    return text != null ? text.toLowerCase() : id;
                })
                .collect(Collectors.toSet());

        if (userTexts.equals(correctAnswers)) {
            return true;
        }

        // 3. correctAnswers도 ID일 수 있으므로 텍스트로 변환하여 비교
        Set<String> correctTexts = correctAnswers.stream()
                .map(id -> {
                    String text = findOptionTextById(optionsJson, id);
                    return text != null ? text.toLowerCase() : id;
                })
                .collect(Collectors.toSet());

        return userTexts.equals(correctTexts);
    }

    /**
     * correct_answer 필드를 정규화한다.
     */
    public static String normalizeCorrectAnswer(String rawCorrectAnswer) {
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
                        if (i > 0)
                            sb.append(",");
                        sb.append(root.get(i).asText().trim());
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                log.warn("correct_answer JSON parsing failed: {}", trimmed);
            }
        }

        // JSON 문자열 형식인 경우: "B" (따옴표로 감싸진 경우)
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.isTextual()) {
                    return node.asText().trim();
                }
            } catch (Exception e) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }

        return trimmed;
    }

    /**
     * options JSON에서 특정 ID의 텍스트를 찾는다.
     */
    private static String findOptionTextById(String optionsJson, String id) {
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
            log.warn("Options JSON parsing failed: {}", optionsJson);
        }

        return null;
    }

    private static String generateIdFromIndex(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private static Set<String> parseAnswerToSet(String answer) {
        if (answer == null || answer.isBlank()) {
            return Collections.emptySet();
        }

        // Remove JSON brackets and quotes to handle ["A", "B"] or "A, B"
        String cleaned = answer.replaceAll("[\\[\\]\"]", "");

        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase) // Lowercase for internal set comparison
                .collect(Collectors.toSet());
    }
}
