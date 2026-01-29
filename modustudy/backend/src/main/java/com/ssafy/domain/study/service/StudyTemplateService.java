package com.ssafy.domain.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.CreateTemplateRequest;
import com.ssafy.domain.study.dto.request.TemplateRecommendRequest;
import com.ssafy.domain.study.dto.request.UpdateTemplateRequest;
import com.ssafy.domain.study.dto.response.StudyTemplateResponse;
import com.ssafy.domain.study.dto.response.TemplateRecommendResponse;
import com.ssafy.domain.study.entity.StudyTemplate;
import com.ssafy.domain.study.repository.StudyTemplateRepository;
import com.ssafy.domain.user.entity.Profile;
import com.ssafy.domain.user.entity.UserSchedule;
import com.ssafy.domain.user.repository.ProfileRepository;
import com.ssafy.domain.user.repository.UserScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 스터디 템플릿 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTemplateService {

    private final StudyTemplateRepository studyTemplateRepository;
    private final ProfileRepository profileRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai-server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    /**
     * 템플릿 생성
     */
    @Transactional
    public StudyTemplateResponse createTemplate(CreateTemplateRequest request, Long userId) {
        log.info("템플릿 생성 시작 - userId: {}, name: {}", userId, request.getName());

        // 이름 중복 체크
        if (studyTemplateRepository.existsByUserIdAndName(userId, request.getName())) {
            log.warn("템플릿 이름 중복 - userId: {}, name: {}", userId, request.getName());
            throw new IllegalStateException("이미 같은 이름의 템플릿이 존재합니다.");
        }

        // Entity 생성
        StudyTemplate template = StudyTemplate.builder()
                .userId(userId)
                .name(request.getName())
                .intro(request.getIntro())
                .isSystem(false)
                .templateType(request.getTemplateType())
                .topic(request.getTopic())
                .format(request.getFormat())
                .meetingType(request.getMeetingType())
                .description(request.getDescription())
                .textbook(request.getTextbook())
                .goal(request.getGoal())
                .difficulty(request.getDifficulty())
                .prerequisites(request.getPrerequisites())
                .processDetail(request.getProcessDetail())
                .penaltyPolicy(request.getPenaltyPolicy())
                .build();

        StudyTemplate saved = studyTemplateRepository.save(template);
        log.info("템플릿 생성 완료 - templateId: {}", saved.getId());

        return StudyTemplateResponse.from(saved);
    }

    /**
     * 내 템플릿 목록 조회
     */
    public List<StudyTemplateResponse> getMyTemplates(Long userId) {
        log.info("내 템플릿 목록 조회 - userId: {}", userId);

        List<StudyTemplate> templates = studyTemplateRepository.findByUserId(userId);

        // 최신순 정렬 - 새로운 리스트로 변환 후 정렬
        return templates.stream()
                .sorted(Comparator.comparing(StudyTemplate::getCreatedAt).reversed())
                .map(StudyTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 시스템 템플릿 목록 조회
     */
    public List<StudyTemplateResponse> getSystemTemplates(String templateType) {
        log.info("시스템 템플릿 조회 - templateType: {}", templateType);

        List<StudyTemplate> templates;

        if (templateType != null && !templateType.isEmpty()) {
            templates = studyTemplateRepository.findByIsSystemTrueAndTemplateType(templateType);
        } else {
            templates = studyTemplateRepository.findByIsSystemTrue();
        }

        // 최신순 정렬 - 새로운 리스트로 변환 후 정렬
        return templates.stream()
                .sorted(Comparator.comparing(StudyTemplate::getCreatedAt).reversed())
                .map(StudyTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 템플릿 상세 조회
     */
    public StudyTemplateResponse getTemplate(Long templateId, Long userId) {
        log.info("템플릿 상세 조회 - templateId: {}, userId: {}", templateId, userId);

        StudyTemplate template = studyTemplateRepository.findById(templateId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 템플릿 - templateId: {}", templateId);
                    return new IllegalArgumentException("존재하지 않는 템플릿입니다.");
                });

        // 시스템 템플릿이거나 본인 템플릿인 경우만 조회 가능
        if (!template.isSystem() && !template.getUserId().equals(userId)) {
            log.warn("템플릿 조회 권한 없음 - templateId: {}, userId: {}", templateId, userId);
            throw new IllegalStateException("템플릿 조회 권한이 없습니다.");
        }

        log.info("템플릿 조회 완료 - templateId: {}", templateId);
        return StudyTemplateResponse.from(template);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public StudyTemplateResponse updateTemplate(Long templateId, UpdateTemplateRequest request, Long userId) {
        log.info("템플릿 수정 시작 - templateId: {}, userId: {}", templateId, userId);

        // 권한 체크 (본인 템플릿만 수정 가능)
        StudyTemplate template = studyTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> {
                    log.error("템플릿을 찾을 수 없거나 권한 없음 - templateId: {}, userId: {}", templateId, userId);
                    return new IllegalArgumentException("템플릿을 찾을 수 없거나 수정 권한이 없습니다.");
                });

        // 이름 변경 시 중복 체크
        if (request.getName() != null && !request.getName().equals(template.getName())) {
            if (studyTemplateRepository.existsByUserIdAndName(userId, request.getName())) {
                log.warn("템플릿 이름 중복 - userId: {}, name: {}", userId, request.getName());
                throw new IllegalStateException("이미 같은 이름의 템플릿이 존재합니다.");
            }
            template.setName(request.getName());
        }

        // 필드 업데이트 (null이 아닌 경우만)
        if (request.getIntro() != null) template.setIntro(request.getIntro());
        if (request.getTemplateType() != null) template.setTemplateType(request.getTemplateType());
        if (request.getTopic() != null) template.setTopic(request.getTopic());
        if (request.getFormat() != null) template.setFormat(request.getFormat());
        if (request.getMeetingType() != null) template.setMeetingType(request.getMeetingType());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getTextbook() != null) template.setTextbook(request.getTextbook());
        if (request.getGoal() != null) template.setGoal(request.getGoal());
        if (request.getDifficulty() != null) template.setDifficulty(request.getDifficulty());
        if (request.getPrerequisites() != null) template.setPrerequisites(request.getPrerequisites());
        if (request.getProcessDetail() != null) template.setProcessDetail(request.getProcessDetail());
        if (request.getPenaltyPolicy() != null) template.setPenaltyPolicy(request.getPenaltyPolicy());

        StudyTemplate updated = studyTemplateRepository.save(template);
        log.info("템플릿 수정 완료 - templateId: {}", templateId);

        return StudyTemplateResponse.from(updated);
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long templateId, Long userId) {
        log.info("템플릿 삭제 시작 - templateId: {}, userId: {}", templateId, userId);

        // 권한 체크 (본인 템플릿만 삭제 가능)
        StudyTemplate template = studyTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> {
                    log.error("템플릿을 찾을 수 없거나 권한 없음 - templateId: {}, userId: {}", templateId, userId);
                    return new IllegalArgumentException("템플릿을 찾을 수 없거나 삭제 권한이 없습니다.");
                });

        studyTemplateRepository.delete(template);
        log.info("템플릿 삭제 완료 - templateId: {}", templateId);
    }

    /**
     * AI 템플릿 추천
     * AI 서버에 사용자 정보를 전달하고 추천 결과를 반환
     */
    public TemplateRecommendResponse recommendTemplate(TemplateRecommendRequest request, Long userId) {
        log.info("AI 템플릿 추천 시작 - userId: {}", userId);

        // AI 서버 요청 바디 구성
        Map<String, Object> aiRequest = new HashMap<>();

        // 사용자 기술 스택 조회
        List<String> userTech = List.of();
        try {
            Profile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile != null && profile.getTech() != null) {
                userTech = objectMapper.readValue(profile.getTech(), List.class);
            }
        } catch (Exception e) {
            log.warn("기술 스택 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
        }
        aiRequest.put("user_tech", userTech);

        // 사용자 가용 스케줄 조회
        Map<String, Map<String, String>> userSchedule = new LinkedHashMap<>();
        List<UserSchedule> schedules = userScheduleRepository.findByUserIdAndIsAvailableTrue(userId);
        for (UserSchedule s : schedules) {
            Map<String, String> timeInfo = new HashMap<>();
            timeInfo.put("start", s.getStartTime().toString());
            timeInfo.put("end", s.getEndTime().toString());
            userSchedule.put(s.getDayOfWeek().name(), timeInfo);
        }
        aiRequest.put("user_schedule", userSchedule);
        if (request.getStudyType() != null) {
            aiRequest.put("study_type", request.getStudyType());
        }
        if (request.getDifficultyPreference() != null) {
            aiRequest.put("difficulty_preference", request.getDifficultyPreference());
        }
        // 새로 추가: 사용자 입력 주제, 선호 기간
        if (request.getTopicInput() != null) {
            aiRequest.put("topic_input", request.getTopicInput());
        }
        if (request.getDurationWeeks() != null) {
            aiRequest.put("duration_weeks", request.getDurationWeeks());
        }
        // 총 회차 (요일수 × 주수)
        if (request.getTotalSessions() != null) {
            aiRequest.put("total_sessions", request.getTotalSessions());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    aiServerBaseUrl + "/api/recommend-template", entity, String.class);
            String responseBody = responseEntity.getBody();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            TemplateRecommendResponse response = TemplateRecommendResponse.builder()
                    .templateType((String) result.get("template_type"))
                    .topic((String) result.get("topic"))
                    .format((String) result.get("format"))
                    .difficulty((String) result.get("difficulty"))
                    .goal((String) result.get("goal"))
                    .textbook((String) result.get("textbook"))
                    .scheduleSuggestion(result.containsKey("schedule_suggestion")
                            ? (Map<String, Object>) result.get("schedule_suggestion") : null)
                    .reason((String) result.get("reason"))
                    .tokensUsed(result.containsKey("tokens_used")
                            ? ((Number) result.get("tokens_used")).intValue() : 0)
                    // 새로 추가된 필드들
                    .name((String) result.get("name"))
                    .intro((String) result.get("intro"))
                    .description((String) result.get("description"))
                    .prerequisites((String) result.get("prerequisites"))
                    .processDetail((String) result.get("process_detail"))
                    .curriculum(result.containsKey("curriculum")
                            ? (List<Map<String, Object>>) result.get("curriculum") : null)
                    .build();

            log.info("AI 템플릿 추천 완료 - type: {}, topic: {}",
                    response.getTemplateType(), response.getTopic());

            return response;

        } catch (Exception e) {
            log.error("AI 서버 호출 실패 - userId: {}, error: {}", userId, e.getMessage());
            throw new RuntimeException("AI 추천 서비스를 일시적으로 사용할 수 없습니다.", e);
        }
    }

    /**
     * AI 템플릿 추천 (스트리밍)
     * AI 서버의 SSE 스트림을 받아서 클라이언트로 중계
     */
    public void streamRecommendTemplate(
            String topicInput,
            Integer durationWeeks,
            Integer totalSessions,
            Long userId,
            SseEmitter emitter) {

        log.info("스트리밍 템플릿 추천 시작 - userId: {}, topic: {}", userId, topicInput);

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // AI 서버 요청 바디 구성
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("topic_input", topicInput != null ? topicInput : "IT 스터디");
            aiRequest.put("duration_weeks", durationWeeks != null ? durationWeeks : 4);
            aiRequest.put("total_sessions", totalSessions != null ? totalSessions : durationWeeks);

            // 사용자 기술 스택 조회
            List<String> userTech = new ArrayList<>();
            try {
                Profile profile = profileRepository.findByUserId(userId).orElse(null);
                if (profile != null && profile.getTech() != null) {
                    userTech = objectMapper.readValue(profile.getTech(), List.class);
                }
            } catch (Exception e) {
                log.warn("기술 스택 조회 실패 - userId: {}", userId);
            }
            aiRequest.put("user_tech", userTech);

            // 사용자 가용 스케줄 조회
            Map<String, Map<String, String>> userSchedule = new LinkedHashMap<>();
            List<UserSchedule> schedules = userScheduleRepository.findByUserIdAndIsAvailableTrue(userId);
            for (UserSchedule s : schedules) {
                Map<String, String> timeInfo = new HashMap<>();
                timeInfo.put("start", s.getStartTime().toString());
                timeInfo.put("end", s.getEndTime().toString());
                userSchedule.put(s.getDayOfWeek().name(), timeInfo);
            }
            aiRequest.put("user_schedule", userSchedule);

            // AI 서버 스트리밍 엔드포인트 호출
            String requestJson = objectMapper.writeValueAsString(aiRequest);
            URL url = new URL(aiServerBaseUrl + "/api/recommend-template/stream");

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);  // 연결 타임아웃 10초
            connection.setReadTimeout(120000);    // 읽기 타임아웃 2분

            // 요청 바디 전송
            try (var os = connection.getOutputStream()) {
                byte[] input = requestJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 응답 스트림 읽기
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.error("AI 서버 응답 오류: {}", responseCode);
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"AI 서버 응답 오류: " + responseCode + "\"}"));
                emitter.complete();
                return;
            }

            reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 형식 파싱: "event: xxx" 또는 "data: xxx"
                if (line.startsWith("event: ")) {
                    String eventName = line.substring(7).trim();
                    String dataLine = reader.readLine();  // 다음 줄이 data

                    if (dataLine != null && dataLine.startsWith("data: ")) {
                        String data = dataLine.substring(6);

                        // 클라이언트로 중계
                        emitter.send(SseEmitter.event()
                                .name(eventName)
                                .data(data));

                        // complete 이벤트면 종료
                        if ("complete".equals(eventName) || "error".equals(eventName)) {
                            break;
                        }
                    }
                } else if (line.startsWith("data: ")) {
                    // event 없이 data만 있는 경우
                    String data = line.substring(6);
                    emitter.send(SseEmitter.event().data(data));
                }
                // 빈 줄은 건너뜀
            }

            emitter.complete();
            log.info("스트리밍 템플릿 추천 완료 - userId: {}", userId);

        } catch (IOException e) {
            log.error("스트리밍 중 IO 오류 - userId: {}, error: {}", userId, e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"" + e.getMessage() + "\"}"));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);

        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}