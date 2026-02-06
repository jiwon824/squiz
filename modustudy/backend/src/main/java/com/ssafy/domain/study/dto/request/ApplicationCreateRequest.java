package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCreateRequest {

    /**
     * 신청 메시지
     */
    @NotNull(message = "신청 메시지는 필수입니다")
    @Size(min = 10, max = 500, message = "신청 메시지는 10자 이상 500자 이하로 작성해주세요")
    private String message;
}
