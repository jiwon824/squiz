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
@Schema(description = "AI 콘텐츠 검증 요청")
public class AiVerifyRequest {

    @NotBlank
    @Schema(description = "검증할 요약 텍스트", example = "Docker에서 이미지는 실행 중인 컨테이너를 말하며...")
    private String summary;

    @Schema(description = "요약본의 원래 주제", example = "Docker 기초")
    private String originalTopic;
}
