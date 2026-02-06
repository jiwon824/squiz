package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 퀴즈 문제 항목 DTO.
 *
 * @param questionNumber 문제 순서 번호
 * @param questionText 문제 내용
 * @param questionType 문제 유형 (MULTIPLE_CHOICE, SHORT_ANSWER)
 * @param options 객관식 보기 목록 (단답형인 경우 null)
 */
 @Schema(description = "퀴즈 문제 항목")
 public record QuestionItem(
        @Schema(description = "문제 순서 번호", example = "1")
        Integer questionNumber,

        @Schema(description = "문제 내용", example = "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?")
        String questionText,

        @Schema(description = "문제 유형", example = "MULTIPLE_CHOICE")
        QuestionType questionType,

        @Schema(description = "객관식 보기 목록")
        List<OptionItem> options
        ) {
}
