package com.ssafy.domain.quiz.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 코스별 취약점 분석을 위한 통계 응답 DTO (totalReps, totalLapses 포함)
 */
public record ReviewCourseWeaknessResponse(
        @Schema(description = "코스별 취약점 통계 리스트") List<CourseWeaknessStatDto> courseWeaknessStats) {

    public record CourseWeaknessStatDto(
            @Schema(description = "코스 ID", example = "1") Long courseId,
            @Schema(description = "코스 이름", example = "Java 기초") String courseName,
            @Schema(description = "총 복습 횟수", example = "150") long totalReps,
            @Schema(description = "총 오답 횟수", example = "10") long totalLapses) {

        public static CourseWeaknessStatDto from(Long courseId, String courseName, long totalReps, long totalLapses) {
            return new CourseWeaknessStatDto(courseId, courseName, totalReps, totalLapses);
        }
    }

    public static ReviewCourseWeaknessResponse from(List<CourseWeaknessStatDto> courseWeaknessStats) {
        return new ReviewCourseWeaknessResponse(courseWeaknessStats);
    }
}
