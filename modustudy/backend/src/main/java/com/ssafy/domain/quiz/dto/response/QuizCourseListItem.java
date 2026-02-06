package com.ssafy.domain.quiz.dto.response;

/**
 * 코스 목록의 단일 아이템 응답 DTO.
 *
 * 코스 목록 API 응답 내 courses 배열 요소로 사용된다.
 *
 */
 public record QuizCourseListItem(
        Long id,
        String code,
        String name,
        String description,
        Integer totalSections,
        String badgeCode,
        String badgeName
        ) {
}
