package com.ssafy.domain.dm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DM 전송 요청 DTO
 */
 public record DirectMessageRequest(
        @NotNull(message = "수신자 ID는 필수입니다.")
        Long receiverId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 2000, message = "메시지는 2000자 이하여야 합니다.")
        String content
        ) {
}
