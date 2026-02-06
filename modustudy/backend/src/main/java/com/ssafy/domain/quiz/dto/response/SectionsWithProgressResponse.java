package com.ssafy.domain.quiz.dto.response;

import java.util.List;

/**
 * 섹션 목록 응답 DTO (진행 상황 포함).
 *
 * GET /api/v1/quiz-courses/{courseId}/sections 응답의 data 영역에 매핑된다.
 * 인증된 사용자만 접근 가능하며, 사용자별 진행 상황을 포함한다.
 */
 public record SectionsWithProgressResponse(
        Long courseId,
        String courseName,
        MyProgressDto myProgress,
        List<SectionWithProgressDto> sections) {
}
