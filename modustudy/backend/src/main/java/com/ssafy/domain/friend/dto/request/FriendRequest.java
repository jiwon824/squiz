package com.ssafy.domain.friend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 친구 요청 DTO
 */
 public record FriendRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId
        ) {
}
