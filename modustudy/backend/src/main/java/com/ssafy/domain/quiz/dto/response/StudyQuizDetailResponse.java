package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.StudyQuiz;
import com.ssafy.domain.quiz.entity.StudyQuizQuestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스터디 퀴즈 상세 응답 DTO
 */
 @Getter
 @Builder
 public class StudyQuizDetailResponse {

    private Long id;
    private Long studyId;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String status;
    private LocalDateTime createdAt;
    private List<QuestionDto> questions;

    @Getter
    @Builder
    public static class QuestionDto {
        private Long id;
        private String questionText;
        private String questionType;
        private String options;         // JSON string
        private String correctAnswer;
        private String answerKeywords;  // JSON string (서술형 채점용 키워드)
        private String explanation;
    }

    public static StudyQuizDetailResponse from(StudyQuiz quiz) {
        List<QuestionDto> questionDtos = quiz.getQuestions().stream()
                .map(StudyQuizDetailResponse::toQuestionDto)
                .toList();

        return StudyQuizDetailResponse.builder()
                .id(quiz.getId())
                .studyId(quiz.getStudyId())
                .title(quiz.getTitle())
                .sourceType(quiz.getSourceType().name())
                .sourceId(quiz.getSourceId())
                .status(quiz.getStatus().name())
                .createdAt(quiz.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private static QuestionDto toQuestionDto(StudyQuizQuestion q) {
        return QuestionDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType().name())
                .options(q.getOptions())
                .correctAnswer(q.getCorrectAnswer())
                .answerKeywords(q.getAnswerKeywords())
                .explanation(q.getExplanation())
                .build();
    }
}
