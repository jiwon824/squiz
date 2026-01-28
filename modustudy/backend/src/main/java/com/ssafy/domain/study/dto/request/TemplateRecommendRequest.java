package com.ssafy.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI 템플릿 추천 요청")
public class TemplateRecommendRequest {

    @Schema(description = "희망 스터디 유형 (ALGORITHM, CS, PROJECT 등)", example = "ALGORITHM")
    private String studyType;

    @Schema(description = "희망 난이도", example = "INTERMEDIATE")
    private String difficultyPreference;
}
