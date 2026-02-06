package com.ssafy.domain.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 세션 출석 현황 정보 응답 DTO (스터디장 화면용)
 */
 @Schema(description = "세션 출석 현황 응답")
 public record SessionAttendanceInfoResponse(
        @Schema(description = "세션 ID", example = "10")
        Long sessionId,
        @Schema(description = "세션 제목", example = "알고리즘 스터디 1회차")
        String sessionTitle,
        @Schema(description = "전체 스터디원 수", example = "5")
        int totalMembers,
        @Schema(description = "출석 완료 인원", example = "3")
        int presentCount,
        @Schema(description = "멤버별 출석 현황")
        List<SessionAttendanceMemberResponse> members
        ) {
    public static SessionAttendanceInfoResponse of(
            Long sessionId,
            String sessionTitle,
            int totalMembers,
            int presentCount,
            List<SessionAttendanceMemberResponse> members
    ) {
        return new SessionAttendanceInfoResponse(
                sessionId,
                sessionTitle,
                totalMembers,
                presentCount,
                members
        );
    }
}
