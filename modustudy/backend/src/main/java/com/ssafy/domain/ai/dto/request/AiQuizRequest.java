package com.ssafy.domain.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 퀴즈 생성 요청")
public class AiQuizRequest {

    @NotBlank
    @Schema(description = "퀴즈 생성 대상 요약 텍스트", example = "Docker 기초: 컨테이너와 이미지 개념...")
    private String summary;

    @Builder.Default
    @Min(1) @Max(20)
    @Schema(description = "생성할 문제 수", example = "5")
    private int numQuestions = 5;

    @Schema(description = "사용자 취약 분야 목록", example = "[\"Docker 네트워크\", \"볼륨 마운트\"]")
    private List<String> userWeakPoints;

    @Schema(description = "최근 오답 기록")
    private List<RecentError> userRecentErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        @Schema(description = "틀린 주제", example = "Docker 네트워크")
        private String topic;
        @Schema(description = "오답 원인", example = "bridge와 host 모드 혼동")
        private String errorReason;
    }
}
