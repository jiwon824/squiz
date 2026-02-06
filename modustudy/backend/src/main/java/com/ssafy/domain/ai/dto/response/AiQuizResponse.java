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
@Schema(description = "AI 퀴즈 생성 응답")
public class AiQuizResponse {

    @Schema(description = "생성된 퀴즈 목록")
    private List<QuizItem> questions;

    @Schema(description = "퀴즈 소스", example = "gemini")
    private String source;

    @Schema(description = "원본 응답 텍스트 (파싱 실패 시)")
    private String rawResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizItem {
        @Schema(description = "문제 유형", example = "multiple_choice")
        private String type;

        @Schema(description = "문제 내용")
        private String question;

        @Schema(description = "선택지 (객관식)")
        private List<String> options;

        @Schema(description = "정답")
        private String answer;

        @Schema(description = "해설")
        private String explanation;
    }
}
