package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_contest_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizContestState {

    @Id
    @Column(name = "contest_id")
    private Long contestId;

    @Column(name = "current_question_pool_id")
    private Long currentQuestionPoolId;

    @Column(name = "current_question_started_at")
    private LocalDateTime currentQuestionStartedAt;

    @Column(name = "is_showing_results")
    private Boolean isShowingResults = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase")
    private ContestPhase phase = ContestPhase.WAITING;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ContestPhase {
        WAITING, QUESTION, RESULT, ENDED
    }
}
