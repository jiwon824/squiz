package com.ssafy.domain.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * AiService 미팅 처리 기능 테스트
 */
 @ExtendWith(MockitoExtension.class)
 class AiServiceMeetingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiService aiService;

    @Test
    @DisplayName("미팅 처리 결과 조회 - 완료 상태")
    void getMeetingProcessResult_completed() throws Exception {
        // given
        String jobId = "test-job-id";
        String responseJson = """
            {
                "status": "completed",
                "result": {
                    "transcript": "회의 내용 전문",
                    "summary": "요약된 내용",
                    "keywords": ["키워드1", "키워드2"],
                    "action_items": [
                        {"user_id": 1, "content": "액션 아이템 1"},
                        {"user_id": 2, "content": "액션 아이템 2"}
                    ],
                    "quiz": "퀴즈 내용"
                }
            }
            """;

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "completed");
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("transcript", "회의 내용 전문");
        resultMap.put("summary", "요약된 내용");
        resultMap.put("keywords", java.util.List.of("키워드1", "키워드2"));
        resultMap.put("action_items", java.util.List.of(
            Map.of("user_id", 1, "content", "액션 아이템 1"),
            Map.of("user_id", 2, "content", "액션 아이템 2")
        ));
        resultMap.put("quiz", "퀴즈 내용");
        responseMap.put("result", resultMap);

        when(restTemplate.getForEntity(any(String.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));
        when(objectMapper.readValue(any(String.class), eq(Map.class)))
            .thenReturn(responseMap);

        // when
        AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        // then
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getTranscript()).isEqualTo("회의 내용 전문");
        assertThat(result.getSummary()).isEqualTo("요약된 내용");
        assertThat(result.getKeywords()).containsExactly("키워드1", "키워드2");
        assertThat(result.getActionItems()).hasSize(2);
        assertThat(result.getActionItems().get(0).getUserId()).isEqualTo(1L);
        assertThat(result.getActionItems().get(0).getContent()).isEqualTo("액션 아이템 1");
    }

    @Test
    @DisplayName("미팅 처리 결과 조회 - 처리 중 상태")
    void getMeetingProcessResult_processing() throws Exception {
        // given
        String jobId = "test-job-id";
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "processing");

        when(restTemplate.getForEntity(any(String.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{\"status\":\"processing\"}", HttpStatus.OK));
        when(objectMapper.readValue(any(String.class), eq(Map.class)))
            .thenReturn(responseMap);

        // when
        AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        // then
        assertThat(result.getStatus()).isEqualTo("processing");
        assertThat(result.getTranscript()).isNull();
    }

    @Test
    @DisplayName("미팅 처리 결과 조회 - 실패 상태")
    void getMeetingProcessResult_failed() throws Exception {
        // given
        String jobId = "test-job-id";
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "failed");
        responseMap.put("error", "Whisper 모델 로드 실패");

        when(restTemplate.getForEntity(any(String.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{\"status\":\"failed\",\"error\":\"Whisper 모델 로드 실패\"}", HttpStatus.OK));
        when(objectMapper.readValue(any(String.class), eq(Map.class)))
            .thenReturn(responseMap);

        // when
        AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        // then
        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getError()).isEqualTo("Whisper 모델 로드 실패");
    }

    @Test
    @DisplayName("MeetingProcessResult DTO 기본값 테스트")
    void meetingProcessResult_defaults() {
        // when
        AiService.MeetingProcessResult result = new AiService.MeetingProcessResult();

        // then
        assertThat(result.getKeywords()).isEmpty();
        assertThat(result.getActionItems()).isEmpty();
        assertThat(result.getStatus()).isNull();
    }
}
