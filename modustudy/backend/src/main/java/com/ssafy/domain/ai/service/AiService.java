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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Path;
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

    // ===== 미팅 음성 처리 =====

    /**
     * 미팅 전체 처리 작업 등록 (비동기)
     * - 전체 음성 STT + 요약 + 키워드
     * - 화자별 STT + 액션아이템 추천
     * - 복습 퀴즈 생성
     *
     * @param mixedAudioPath 전체 음성 파일 경로
     * @param individualAudioPaths 화자별 음성 파일 경로 목록 (userId -> filePath)
     * @param generateQuiz 퀴즈 생성 여부
     * @return job_id (비동기 작업 ID)
     */
    public String processMeetingAsync(Path mixedAudioPath, Map<Long, Path> individualAudioPaths, boolean generateQuiz) {
        log.info("미팅 처리 요청 - mixedAudio: {}, individualCount: {}", mixedAudioPath, individualAudioPaths.size());

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 전체 음성
            body.add("mixed_audio", new FileSystemResource(mixedAudioPath.toFile()));

            // 화자별 음성 및 user_ids
            StringBuilder userIds = new StringBuilder();
            for (Map.Entry<Long, Path> entry : individualAudioPaths.entrySet()) {
                body.add("individual_audios", new FileSystemResource(entry.getValue().toFile()));
                if (userIds.length() > 0) userIds.append(",");
                userIds.append(entry.getKey());
            }
            body.add("user_ids", userIds.toString());
            body.add("generate_quiz", generateQuiz);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiServerBaseUrl + "/api/process-meeting-full", entity, String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            String jobId = (String) result.get("job_id");

            log.info("미팅 처리 작업 등록 완료 - jobId: {}", jobId);
            return jobId;

        } catch (Exception e) {
            log.error("미팅 처리 작업 등록 실패: {}", e.getMessage());
            throw new RuntimeException("미팅 처리 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * 실시간 STT transcript 기반 요약 작업 등록 (비동기)
     * 미팅 중 실시간으로 수집된 STT 결과를 바로 Claude API로 요약
     * STT 단계를 건너뛰어 처리 시간 단축
     *
     * @param transcript 실시간 STT로 수집된 transcript (화자: 텍스트 형식)
     * @param speakerIds 화자 ID 목록 (액션아이템 추천용)
     * @param generateQuiz 퀴즈 생성 여부
     * @return job_id (비동기 작업 ID)
     */
    public String summarizeTranscriptAsync(String transcript, List<Long> speakerIds, boolean generateQuiz) {
        log.info("[AI 요약 요청] transcript 길이: {}, 화자 수: {}, 퀴즈생성: {}", transcript.length(), speakerIds.size(), generateQuiz);
        log.info("[AI 요약 요청] transcript 미리보기: {}", transcript.length() > 200 ? transcript.substring(0, 200) + "..." : transcript);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("transcript", transcript);
            body.put("speaker_ids", speakerIds);
            body.put("generate_quiz", generateQuiz);

            log.info("[AI 요약 요청] AI 서버 호출 시작 - url: {}/api/summarize-transcript", aiServerBaseUrl);
            String responseBody = callAiServer("/api/summarize-transcript", body);
            log.info("[AI 요약 요청] AI 서버 응답 수신 - 길이: {}", responseBody != null ? responseBody.length() : 0);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
            String status = (String) result.get("status");
            log.info("[AI 요약 요청] 응답 상태: {}, 전체 응답: {}", status, result);

            if ("error".equals(status)) {
                String message = (String) result.get("message");
                throw new RuntimeException("Transcript 요약 실패: " + message);
            }

            String jobId = (String) result.get("job_id");
            log.info("[AI 요약 요청] 작업 등록 완료 - jobId: {}", jobId);
            return jobId;

        } catch (Exception e) {
            log.error("[AI 요약 요청] 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Transcript 요약 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * 미팅 처리 작업 상태 조회
     */
    public MeetingProcessResult getMeetingProcessResult(String jobId) {
        log.info("[AI 결과 조회] jobId: {}, aiServerUrl: {}", jobId, aiServerBaseUrl);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    aiServerBaseUrl + "/api/jobs/" + jobId, String.class);

            log.info("[AI 결과 조회] 응답 수신 - statusCode: {}, bodyLength: {}",
                    response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            String status = (String) result.get("status");
            log.info("[AI 결과 조회] 작업 상태: {}", status);

            MeetingProcessResult processResult = new MeetingProcessResult();
            processResult.setStatus(status);

            if ("completed".equals(status)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("result");
                if (data != null) {
                    processResult.setTranscript((String) data.get("transcript"));
                    processResult.setSummary((String) data.get("summary"));

                    @SuppressWarnings("unchecked")
                    List<String> keywords = (List<String>) data.get("keywords");
                    processResult.setKeywords(keywords != null ? keywords : List.of());

                    @SuppressWarnings("unchecked")
                    List<String> highlights = (List<String>) data.get("highlights");
                    processResult.setHighlights(highlights != null ? highlights : List.of());

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> actionItems = (List<Map<String, Object>>) data.get("action_items");
                    if (actionItems != null) {
                        List<MeetingProcessResult.ActionItem> items = new ArrayList<>();
                        for (Map<String, Object> item : actionItems) {
                            MeetingProcessResult.ActionItem actionItem = new MeetingProcessResult.ActionItem();
                            // user_id가 null일 수 있음 (전체 액션아이템)
                            Object userIdObj = item.get("user_id");
                            actionItem.setUserId(userIdObj != null ? ((Number) userIdObj).longValue() : null);
                            actionItem.setContent((String) item.get("content"));
                            items.add(actionItem);
                        }
                        processResult.setActionItems(items);
                    }

                    // 퀴즈 파싱
                    Object quizData = data.get("quiz");
                    if (quizData != null) {
                        processResult.setQuizRaw(quizData.toString());
                    }
                }
            } else if ("failed".equals(status)) {
                processResult.setError((String) result.get("error"));
            }

            return processResult;

        } catch (Exception e) {
            log.error("미팅 처리 결과 조회 실패: {}", e.getMessage());
            throw new RuntimeException("미팅 처리 결과 조회에 실패했습니다.", e);
        }
    }

    /**
     * 미팅 처리 결과 DTO
     */
    @lombok.Data
    public static class MeetingProcessResult {
        private String status;  // pending, processing, completed, failed
        private String transcript;
        private String summary;
        private List<String> keywords = new ArrayList<>();
        private List<String> highlights = new ArrayList<>();
        private List<ActionItem> actionItems = new ArrayList<>();
        private String quizRaw;
        private String error;

        @lombok.Data
        public static class ActionItem {
            private Long userId;
            private String content;
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
