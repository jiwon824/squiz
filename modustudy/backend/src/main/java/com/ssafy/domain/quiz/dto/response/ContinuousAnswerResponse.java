package com.ssafy.domain.quiz.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Continuous Learning 모드 답변 결과 응답 DTO.
 *
 * <p>정답 여부, 해설, FSRS 갱신 정보를 포함한다.</p>
 */
@Getter
@Builder
public class ContinuousAnswerResponse {

    // ── 답변 결과 ──

    private Long questionId;
    @JsonProperty("correct")
    private boolean isCorrect;
    private String userAnswer;
    private String correctAnswer;
    private String explanation;

    // ── FSRS 갱신 정보 ──

    /**
     * 기억 안정성 (S)
     * <p>높을수록 오래 기억함</p>
     */
    private Double stability;

    /**
     * 난이도 (D, 1~10)
     * <p>높을수록 어려운 문제</p>
     */
    private Double difficulty;

    /**
     * 다음 복습까지의 간격 (일)
     */
    private Integer scheduledDays;

    /**
     * 다음 복습 예정일
     */
    private LocalDateTime nextReviewAt;

    /**
     * 학습 상태
     * <ul>
     *   <li>0: New (새 문제)</li>
     *   <li>1: Learning (학습 중)</li>
     *   <li>2: Review (복습 단계)</li>
     *   <li>3: Relearning (재학습 중)</li>
     * </ul>
     */
    private Integer state;

    /**
     * 총 복습 횟수
     */
    private Integer reps;

    /**
     * 틀린 횟수 (Lapses)
     */
    private Integer lapses;
}
