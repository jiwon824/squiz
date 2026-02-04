package com.ssafy.domain.quiz.repository;

/**
 * 코스별 문제 통계 조회용 Projection 인터페이스.
 *
 * <p>
 * Native Query 결과를 타입 안전하게 매핑하기 위해 사용된다.
 * </p>
 */
public interface CourseQuestionStatsProjection {

    /**
     * 코스 ID
     */
    Long getCourseId();

    /**
     * 문제 수 (전체 또는 정답)
     */
    Long getQuestionCount();

    /**
     * 총 복습 횟수(reps) 합계
     */
    Long getSumReps();

    /**
     * 총 오답 횟수(lapses) 합계
     */
    Long getSumLapses();
}
