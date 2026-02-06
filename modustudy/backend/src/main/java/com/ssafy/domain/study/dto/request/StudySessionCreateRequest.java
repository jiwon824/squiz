package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudySessionCreateRequest {

    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요")
    private String title;

    private String description;

    @NotNull(message = "예정 일시는 필수입니다")
    private LocalDateTime scheduledAt;

    @Positive(message = "진행 시간은 양수여야 합니다")
    private Integer durationMinutes;

    @Size(max = 200, message = "장소는 200자 이내로 입력해주세요")
    private String location;

    private Boolean isOnline;
}
