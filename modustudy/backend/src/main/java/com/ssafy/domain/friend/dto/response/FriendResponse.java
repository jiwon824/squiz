package com.ssafy.domain.friend.dto.response;

import com.ssafy.domain.friend.entity.Friendship;

import java.time.LocalDateTime;

/**
 * 친구 정보 응답 DTO
 */
 public record FriendResponse(
        Long friendshipId,
        Long userId,
        String nickname,
        String profileImage,
        Boolean isOnline,
        LocalDateTime lastSeenAt,
        LocalDateTime friendSince
        ) {
    public static FriendResponse from(Friendship friendship, Long myId) {
        boolean isRequester = friendship.getRequesterId().equals(myId);
        return new FriendResponse(
                friendship.getId(),
                isRequester ? friendship.getAddresseeId() : friendship.getRequesterId(),
                isRequester ? friendship.getAddresseeNickname() : friendship.getRequesterNickname(),
                isRequester ? friendship.getAddresseeProfileImage() : friendship.getRequesterProfileImage(),
                isRequester ? friendship.getAddresseeIsOnline() : friendship.getRequesterIsOnline(),
                isRequester ? friendship.getAddresseeLastSeenAt() : friendship.getRequesterLastSeenAt(),
                friendship.getUpdatedAt()
        );
    }
}
