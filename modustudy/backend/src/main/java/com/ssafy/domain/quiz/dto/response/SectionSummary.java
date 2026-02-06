package com.ssafy.domain.quiz.dto.response;

/**
 * 섹션 요약 정보 DTO.
 *
 * 코스 상세 응답 내 sections 배열 요소로 사용된다.
 */
 public record SectionSummary(
        Integer sectionNumber,
        String name,
        String description,
        Integer totalQuestions,
        Integer passScore
        ) {
}
