package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.StudyQuiz;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 스터디 퀴즈 목록 응답 DTO
 */
@Getter
@Builder
public class StudyQuizListResponse {

    private Long id;
    private String title;
    private String sourceType;
    private Long sourceId;
    private int questionCount;
    private String status;
    private LocalDateTime createdAt;

    public static StudyQuizListResponse from(StudyQuiz quiz) {
        return StudyQuizListResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .sourceType(quiz.getSourceType().name())
                .sourceId(quiz.getSourceId())
                .questionCount(quiz.getQuestions().size())
                .status(quiz.getStatus().name())
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
