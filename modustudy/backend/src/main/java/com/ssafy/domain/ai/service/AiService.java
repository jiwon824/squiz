package com.ssafy.domain.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.ai.dto.request.AiQuizRequest;
import com.ssafy.domain.ai.dto.request.AiSummarizeRequest;
import com.ssafy.domain.ai.dto.request.AiVerifyRequest;
import com.ssafy.domain.ai.dto.response.AiQuizResponse;
import com.ssafy.domain.ai.dto.response.AiSummarizeResponse;
import com.ssafy.domain.ai.dto.response.AiVerifyResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai-server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    /**
     * 회의록 요약 (로컬 AI 서버)
     */
    public AiSummarizeResponse summarize(AiSummarizeRequest request) {
        log.info("AI 요약 요청 - transcript length: {}", request.getTranscript().length());

        Map<String, Object> body = new HashMap<>();
        body.put("transcript", request.getTranscript());
        body.put("max_tokens", request.getMaxTokens());

        try {
            String responseBody = callAiServer("/api/summarize", body);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            AiSummarizeResponse response = AiSummarizeResponse.builder()
                    .summary((String) result.get("summary"))
                    .tokensUsed(result.containsKey("tokens_used")
                            ? ((Number) result.get("tokens_used")).intValue() : 0)
                    .build();

            log.info("AI 요약 완료 - summary length: {}", response.getSummary().length());
            return response;

        } catch (Exception e) {
            log.error("AI 요약 실패: {}", e.getMessage());
            throw new RuntimeException("AI 요약 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * 퀴즈 생성 (AI 서버 → Gemini Flash)
     */
    public AiQuizResponse generateQuiz(AiQuizRequest request) {
        log.info("AI 퀴즈 생성 요청 - questions: {}, weakPoints: {}",
                request.getNumQuestions(),
                request.getUserWeakPoints() != null ? request.getUserWeakPoints().size() : 0);

        Map<String, Object> body = new HashMap<>();
        body.put("summary", request.getSummary());
        body.put("num_questions", request.getNumQuestions());

        if (request.getUserWeakPoints() != null && !request.getUserWeakPoints().isEmpty()) {
            body.put("user_weak_points", request.getUserWeakPoints());
        }

        if (request.getUserRecentErrors() != null && !request.getUserRecentErrors().isEmpty()) {
            List<Map<String, String>> errors = new ArrayList<>();
            for (AiQuizRequest.RecentError error : request.getUserRecentErrors()) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("topic", error.getTopic());
                errorMap.put("error_reason", error.getErrorReason());
                errors.add(errorMap);
            }
            body.put("user_recent_errors", errors);
        }

        try {
            String responseBody = callAiServer("/api/quiz", body);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            String quizRaw = (String) result.get("quiz");
            String source = (String) result.getOrDefault("source", "claude");

            // JSON 배열 파싱 시도
            List<AiQuizResponse.QuizItem> questions = parseQuizJson(quizRaw);

            AiQuizResponse response = AiQuizResponse.builder()
                    .questions(questions)
                    .source(source)
                    .rawResponse(questions.isEmpty() ? quizRaw : null)
                    .build();

            log.info("AI 퀴즈 생성 완료 - questions: {}, source: {}", questions.size(), source);
            return response;

        } catch (Exception e) {
            log.error("AI 퀴즈 생성 실패: {}", e.getMessage());
            throw new RuntimeException("AI 퀴즈 생성 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * 콘텐츠 사실 검증 (AI 서버 → Gemini Flash)
     */
    public AiVerifyResponse verifyContent(AiVerifyRequest request) {
        log.info("AI 콘텐츠 검증 요청 - topic: {}", request.getOriginalTopic());

        Map<String, Object> body = new HashMap<>();
        body.put("summary", request.getSummary());
        if (request.getOriginalTopic() != null) {
            body.put("original_topic", request.getOriginalTopic());
        }

        try {
            String responseBody = callAiServer("/api/verify", body);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            boolean hasErrors = Boolean.TRUE.equals(result.get("has_errors"));
            String confidence = (String) result.getOrDefault("confidence", "medium");
            String source = (String) result.getOrDefault("source", "claude");

            // 오류 목록 파싱
            List<AiVerifyResponse.ErrorItem> errorItems = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> rawErrors = (List<Map<String, String>>) result.get("errors");
            if (rawErrors != null) {
                for (Map<String, String> rawError : rawErrors) {
                    errorItems.add(AiVerifyResponse.ErrorItem.builder()
                            .claim(rawError.get("claim"))
                            .correction(rawError.get("correction"))
                            .severity(rawError.getOrDefault("severity", "medium"))
                            .build());
                }
            }

            AiVerifyResponse response = AiVerifyResponse.builder()
                    .hasErrors(hasErrors)
                    .errors(errorItems)
                    .confidence(confidence)
                    .source(source)
                    .build();

            log.info("AI 콘텐츠 검증 완료 - hasErrors: {}, errorCount: {}", hasErrors, errorItems.size());
            return response;

        } catch (Exception e) {
            log.error("AI 콘텐츠 검증 실패: {}", e.getMessage());
            throw new RuntimeException("AI 검증 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * AI 서버 헬스체크
     */
    public Map<String, Object> healthCheck() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    aiServerBaseUrl + "/health", String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("AI 서버 헬스체크 실패: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }

    // ===== 내부 메서드 =====

    private String callAiServer(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                aiServerBaseUrl + path, entity, String.class);

        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<AiQuizResponse.QuizItem> parseQuizJson(String quizRaw) {
        List<AiQuizResponse.QuizItem> questions = new ArrayList<>();
        if (quizRaw == null || quizRaw.isBlank()) {
            return questions;
        }

        try {
            // markdown 코드블록 제거
            String jsonText = quizRaw;
            if (jsonText.contains("```json")) {
                jsonText = jsonText.split("```json")[1].split("```")[0].trim();
            } else if (jsonText.contains("```")) {
                jsonText = jsonText.split("```")[1].split("```")[0].trim();
            }

            List<Map<String, Object>> rawList = objectMapper.readValue(
                    jsonText, new TypeReference<>() {});

            for (Map<String, Object> item : rawList) {
                questions.add(AiQuizResponse.QuizItem.builder()
                        .type((String) item.get("type"))
                        .question((String) item.get("question"))
                        .options(item.get("options") instanceof List ? (List<String>) item.get("options") : null)
                        .answer((String) item.get("answer"))
                        .explanation((String) item.get("explanation"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("퀴즈 JSON 파싱 실패, rawResponse로 반환: {}", e.getMessage());
        }

        return questions;
    }
}
