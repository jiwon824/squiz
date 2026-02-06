package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationProcessRequest {

    /**
     * 거절 사유 (거절 시에만 필요)
     */
    private String rejectedReason;
}
