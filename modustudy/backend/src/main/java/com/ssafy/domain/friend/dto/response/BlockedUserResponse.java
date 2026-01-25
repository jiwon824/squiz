package com.ssafy.domain.friend.dto.response;

import com.ssafy.domain.friend.entity.UserBlock;

import java.time.LocalDateTime;

/**
 * 차단된 사용자 응답 DTO
 */
public record BlockedUserResponse(
        Long blockId,
        Long userId,
        String nickname,
        String profileImage,
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock block) {
        return new BlockedUserResponse(
                block.getId(),
                block.getBlockedId(),
                block.getBlockedNickname(),
                block.getBlockedProfileImage(),
                block.getCreatedAt()
        );
    }
}
