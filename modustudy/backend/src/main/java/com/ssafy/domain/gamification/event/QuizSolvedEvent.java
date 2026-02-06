package com.ssafy.domain.gamification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class QuizSolvedEvent extends ApplicationEvent {
    private final Long userId;
    private final Long quizId;
    private final String quizTitle;
    private final boolean isCorrect;
    private final LocalDate solvedDate;

    public QuizSolvedEvent(Object source, Long userId, Long quizId, String quizTitle, boolean isCorrect, LocalDate solvedDate) {
        super(source);
        this.userId = userId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.isCorrect = isCorrect;
        this.solvedDate = solvedDate;
    }
}
