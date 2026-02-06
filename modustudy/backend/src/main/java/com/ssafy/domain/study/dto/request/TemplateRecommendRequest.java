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

    @Schema(description = "사용자가 입력한 스터디 주제", example = "React 심화 학습")
    private String topicInput;

    @Schema(description = "선호 스터디 기간 (주)", example = "4")
    private Integer durationWeeks;

    @Schema(description = "총 회차 (요일수 × 주수)", example = "8")
    private Integer totalSessions;
}
