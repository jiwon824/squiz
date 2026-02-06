package com.ssafy.domain.quiz.dto.response;

/**
 * 진행 상황이 포함된 섹션 정보 DTO.
 *
 * GET /api/v1/continuous-quiz/courses/{courseId}/sections 응답의 sections 배열 요소.
 * Attempt 의존성 제거 — UserCourseProgress 기반.
 */
 public record SectionWithProgressDto(
        Integer sectionNumber,
        String name,
        Integer totalQuestions,
        Integer passScore,
        Boolean isUnlocked,  // 해금 여부
        Boolean isPassed,    // 통과 여부
        Integer bestScore    // 최고 점수 (null 가능)
        ) {
    /**
     * 진행 상황이 없는 기본 섹션 DTO를 생성한다.
     */
    public static SectionWithProgressDto ofNoProgress(
            Integer sectionNumber,
            String name,
            Integer totalQuestions,
            Integer passScore,
            Boolean isUnlocked) {
        return new SectionWithProgressDto(
                sectionNumber,
                name,
                totalQuestions,
                passScore,
                isUnlocked,
                false,  // isPassed
                null    // bestScore
        );
    }
}
