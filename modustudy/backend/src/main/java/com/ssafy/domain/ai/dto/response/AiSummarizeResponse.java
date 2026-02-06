package com.ssafy.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 회의록 요약 응답")
public class AiSummarizeResponse {

    @Schema(description = "요약 결과")
    private String summary;

    @Schema(description = "사용된 토큰 수")
    private int tokensUsed;
}
