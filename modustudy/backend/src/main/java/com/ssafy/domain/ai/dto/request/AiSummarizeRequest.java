package com.ssafy.domain.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 회의록 요약 요청")
public class AiSummarizeRequest {

    @NotBlank
    @Schema(description = "요약할 회의록 텍스트")
    private String transcript;

    @Builder.Default
    @Schema(description = "최대 토큰 수", example = "512")
    private int maxTokens = 512;
}
