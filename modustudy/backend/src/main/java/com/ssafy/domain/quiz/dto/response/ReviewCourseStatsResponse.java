package com.ssafy.domain.quiz.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 코스별 정답 통계 응답 DTO.
 *
 * @param totalSolvedCount 전체 맞춘 문제 수 (중복 제외)
 * @param courseStats      코스별 상세 통계 리스트
 */
 @Schema(description = "코스별 정답 통계 응답")
 public record ReviewCourseStatsResponse(

        @Schema(description = "전체 맞춘 문제 수 (중복 제외)", example = "45") long totalSolvedCount,

        @Schema(description = "코스별 상세 통계 리스트") List<CourseStatDto> courseStats) {

    /**
     * 코스별 통계 DTO.
     *
     * @param courseId       코스 ID
     * @param courseName     코스 이름
     * @param totalQuestions 코스 전체 문제 수
     * @param solvedCount    맞춘 문제 수 (최소 1회 이상 정답)
     */
    public record CourseStatDto(

            @Schema(description = "코스 ID", example = "1") Long courseId,

            @Schema(description = "코스 이름", example = "Java 기초") String courseName,

            @Schema(description = "코스 전체 문제 수", example = "50") long totalQuestions,

            @Schema(description = "맞춘 문제 수 (최소 1회 이상 정답)", example = "30") long solvedCount) {
        /**
         * 코스별 통계 DTO 생성 팩토리 메서드.
         */
        public static CourseStatDto from(Long courseId, String courseName,
                long totalQuestions, long solvedCount) {
            return new CourseStatDto(courseId, courseName, totalQuestions, solvedCount);
        }
    }

    /**
     * 코스별 정답 통계 응답 생성 팩토리 메서드.
     */
    public static ReviewCourseStatsResponse from(long totalSolvedCount,
            List<CourseStatDto> courseStats) {
        return new ReviewCourseStatsResponse(totalSolvedCount, courseStats);
    }
}
