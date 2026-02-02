package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import lombok.Builder;
import lombok.Getter;

/**
 * Continuous Learning 모드 문제 응답 DTO.
 */
@Getter
@Builder
public class ContinuousQuestionResponse {

    private Long questionId;
    private Integer questionNumber;
    private String questionText;
    private QuestionType questionType;
    private String options;

    // 섹션 정보
    private Long courseId;
    private Integer sectionNumber;

    public static ContinuousQuestionResponse from(QuizCourseQuestion question) {
        return ContinuousQuestionResponse.builder()
                .questionId(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .options(question.getOptions())
                .courseId(question.getSection().getQuizCourseId())
                .sectionNumber(question.getSection().getSectionNumber())
                .build();
    }
}
