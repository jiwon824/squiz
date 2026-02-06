package com.ssafy.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 콘텐츠 검증 응답")
public class AiVerifyResponse {

    @Schema(description = "오류 존재 여부")
    private boolean hasErrors;

    @Schema(description = "발견된 오류 목록")
    private List<ErrorItem> errors;

    @Schema(description = "검증 신뢰도", example = "high")
    private String confidence;

    @Schema(description = "검증 소스", example = "gemini")
    private String source;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorItem {
        @Schema(description = "틀린 부분 원문 인용")
        private String claim;

        @Schema(description = "올바른 설명")
        private String correction;

        @Schema(description = "심각도", example = "high")
        private String severity;
    }
}
