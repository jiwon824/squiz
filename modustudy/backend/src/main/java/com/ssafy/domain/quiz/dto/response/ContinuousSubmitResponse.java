package com.ssafy.domain.quiz.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Continuous Learning 모드 - Submit & Next 통합 응답 DTO.
 *
 * <p>답변 제출 결과와 다음 문제를 한 번에 반환한다.</p>
 *
 * <p><b>무한 루프 지원:</b> 섹션 완료 개념 없음. nextQuestion은 섹션에 문제가 있으면 항상 존재.</p>
 */
@Getter
@Builder
public class ContinuousSubmitResponse {

    // ══════════════════════════════════════════════════════
    //  답변 결과 (Submission Result)
    // ══════════════════════════════════════════════════════

    private Long submittedQuestionId;
    private boolean isCorrect;
    private String userAnswer;
    private String correctAnswer;
    private String explanation;

    // ── FSRS 갱신 정보 ──

    private Double stability;
    private Double difficulty;
    private Integer scheduledDays;
    private LocalDateTime nextReviewAt;
    private Integer state;
    private Integer reps;
    private Integer lapses;

    // ══════════════════════════════════════════════════════
    //  다음 문제 (Next Question)
    // ══════════════════════════════════════════════════════

    /**
     * 다음 문제.
     *
     * <p>무한 루프 지원으로 섹션에 문제가 있으면 항상 존재.
     * 섹션에 문제가 없는 경우에만 null (이론상 발생하지 않음).</p>
     */
    private NextQuestion nextQuestion;

    @Getter
    @Builder
    public static class NextQuestion {
        private Long questionId;
        private Integer questionNumber;
        private String questionText;
        private String questionType;
        private String options;
        private Long courseId;
        private Integer sectionNumber;
    }
}
