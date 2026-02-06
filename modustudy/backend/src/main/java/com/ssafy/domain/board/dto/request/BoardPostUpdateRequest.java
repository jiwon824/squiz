package com.ssafy.domain.board.dto.request;

import com.ssafy.domain.board.entity.RecruitmentStatus;
import com.ssafy.domain.study.entity.MeetingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoardPostUpdateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String recruitmentField,
        @NotNull MeetingType meetingType,
        @NotNull Integer targetMembers,
        @NotNull RecruitmentStatus recruitmentStatus
        ) {
}
