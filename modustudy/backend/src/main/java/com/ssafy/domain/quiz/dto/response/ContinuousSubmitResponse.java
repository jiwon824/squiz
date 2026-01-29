package com.ssafy.domain.quiz.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Continuous Learning 모드 - Submit & Next 통합 응답 DTO.
 *
 * <p>답변 제출 결과와 다음 문제를 한 번에 반환한다.</p>
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
     * 다음 문제 (없으면 null - 섹션 완료)
     */
    private NextQuestion nextQuestion;

    /**
     * 섹션 완료 여부 (다음 문제가 없으면 true)
     */
    private boolean sectionCompleted;

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
