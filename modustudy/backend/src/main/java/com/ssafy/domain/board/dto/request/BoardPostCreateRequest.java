package com.ssafy.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.ssafy.domain.study.entity.MeetingType;
import jakarta.validation.constraints.NotNull;

public record BoardPostCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String recruitmentField,
        @NotNull MeetingType meetingType,
        @NotNull Integer targetMembers
        ) {
}
