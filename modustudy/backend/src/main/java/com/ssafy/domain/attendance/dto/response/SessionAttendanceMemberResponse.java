package com.ssafy.domain.attendance.dto.response;

import com.ssafy.domain.attendance.entity.Attendance;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

/**
 * 세션 출석 멤버 정보 응답 DTO (스터디장 화면용)
 */
 @Schema(description = "세션 출석 멤버 응답")
 public record SessionAttendanceMemberResponse(
        @Schema(description = "사용자 ID", example = "100")
        Long userId,
        @Schema(description = "닉네임", example = "홍길동")
        String nickname,
        @Schema(description = "프로필 이미지 URL")
        String profileImage,
        @Schema(description = "출석 상태", example = "PRESENT")
        String status,
        @Schema(description = "출석 체크 시각", example = "2024-01-15T14:30:00")
        String checkedAt
        ) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Attendance 엔티티에서 변환
     */
    public static SessionAttendanceMemberResponse from(Attendance attendance) {
        return new SessionAttendanceMemberResponse(
                attendance.getUser() != null ? attendance.getUser().getId() : null,
                attendance.getUser() != null ? attendance.getUser().getNickname() : null,
                attendance.getUser() != null ? attendance.getUser().getProfileImage() : null,
                attendance.getStatus() != null ? attendance.getStatus().name() : "PENDING",
                attendance.getCheckedAt() != null ? attendance.getCheckedAt().format(FORMATTER) : null
        );
    }

    /**
     * 출석 전 멤버용 생성자 (User 정보만)
     */
    public static SessionAttendanceMemberResponse pending(Long userId, String nickname, String profileImage) {
        return new SessionAttendanceMemberResponse(
                userId,
                nickname,
                profileImage,
                "PENDING",
                null
        );
    }
}
