package com.ssafy.domain.quiz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Continuous Learning 모드 답변 제출 요청 DTO.
 */
 @Getter
 @NoArgsConstructor
 public class ContinuousAnswerRequest {

    /**
     * 사용자 답변
     */
    @NotNull(message = "답변을 입력해주세요.")
    private String userAnswer;

    /**
     * 응답 시간 (밀리초)
     * <p>
     * FSRS Rating 자동 산출에 사용:
     * <ul>
     *   <li>≤ 2초: Easy</li>
     *   <li>2초 ~ 5초: Good</li>
     *   <li>> 5초: Hard</li>
     * </ul>
     */
    @NotNull(message = "응답 시간을 입력해주세요.")
    private Long responseTimeMs;
}
