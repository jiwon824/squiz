package com.ssafy.domain.quiz.dto.response;

/**
 * 진행 상황이 포함된 섹션 정보 DTO.
 *
 * GET /api/v1/quiz-courses/{courseId}/sections 응답의 sections 배열 요소로 사용된다.
 */
public record SectionWithProgressDto(
        Integer sectionNumber,
        String name,
        Integer totalQuestions,
        Integer passScore,
        Boolean isUnlocked, // 해금 여부
        Boolean isPassed, // 통과 여부
        Integer bestScore, // 최고 점수 (null 가능)
        Integer attemptCount, // 시도 횟수
        Long inProgressAttemptId // 진행 중인 시도 ID (null 가능)
) {
    /**
     * 기본 섹션 정보로부터 진행 상황이 없는 DTO를 생성한다.
     *
     * @param sectionNumber  섹션 번호
     * @param name           섹션 이름
     * @param totalQuestions 총 문제 수
     * @param passScore      통과 점수
     * @param isUnlocked     해금 여부
     * @return 진행 상황이 없는 섹션 DTO
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
                false, // isPassed
                null, // bestScore
                0, // attemptCount
                null // inProgressAttemptId
        );
    }
}
