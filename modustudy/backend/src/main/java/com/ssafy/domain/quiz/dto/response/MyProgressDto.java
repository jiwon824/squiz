package com.ssafy.domain.quiz.dto.response;

/**
 * 사용자 진행 상황 DTO.
 *
 * GET /api/v1/quiz-courses/{courseId}/sections 응답의 myProgress 영역에 매핑된다.
 */
 public record MyProgressDto(
        Integer currentSection, // 현재 진행 중인 섹션 번호
        Integer completedSections, // 완료된 섹션 수
        Boolean isCompleted // 코스 전체 완료 여부
        ) {
    public static MyProgressDto empty() {
        return new MyProgressDto(1, 0, false);
    }
}
