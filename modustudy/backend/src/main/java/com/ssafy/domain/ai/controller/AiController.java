package com.ssafy.domain.ai.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.ai.dto.request.AiQuizRequest;
import com.ssafy.domain.ai.dto.request.AiSummarizeRequest;
import com.ssafy.domain.ai.dto.request.AiVerifyRequest;
import com.ssafy.domain.ai.dto.response.AiQuizResponse;
import com.ssafy.domain.ai.dto.response.AiSummarizeResponse;
import com.ssafy.domain.ai.dto.response.AiVerifyResponse;
import com.ssafy.domain.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI 요약/퀴즈/검증 API")
public class AiController {

    private final AiService aiService;

    @PostMapping("/summarize")
    @Operation(summary = "회의록 요약", description = "회의록 텍스트를 AI로 요약합니다 (로컬 8B 모델)")
    public ResponseEntity<ApiResponse<AiSummarizeResponse>> summarize(
            @Valid @RequestBody AiSummarizeRequest request
    ) {
        AiSummarizeResponse response = aiService.summarize(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/quiz")
    @Operation(summary = "복습 퀴즈 생성", description = "스터디 요약 기반 퀴즈를 생성합니다 (Gemini Flash). 학습 이력 기반 개인화 지원.")
    public ResponseEntity<ApiResponse<AiQuizResponse>> generateQuiz(
            @Valid @RequestBody AiQuizRequest request
    ) {
        AiQuizResponse response = aiService.generateQuiz(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify")
    @Operation(summary = "콘텐츠 사실 검증", description = "스터디원 요약본의 기술적 오류를 감지합니다 (Gemini Flash)")
    public ResponseEntity<ApiResponse<AiVerifyResponse>> verifyContent(
            @Valid @RequestBody AiVerifyRequest request
    ) {
        AiVerifyResponse response = aiService.verifyContent(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    @Operation(summary = "AI 서버 상태 확인", description = "AI 추론 서버의 상태를 확인합니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = aiService.healthCheck();
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
