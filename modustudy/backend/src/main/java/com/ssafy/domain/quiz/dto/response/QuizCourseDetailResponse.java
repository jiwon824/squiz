package com.ssafy.domain.quiz.dto.response;

import java.util.List;

/**
 * 코스 상세 응답 DTO.
 *
 * GET /api/v1/quiz-courses/{courseId} 응답의 data 영역에 매핑된다.
 */
 public record QuizCourseDetailResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer totalSections,
        BadgeInfo badge,
        List<SectionSummary> sections
        ) {
}
