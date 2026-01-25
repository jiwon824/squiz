package com.ssafy.domain.friend.dto.response;

import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.entity.FriendshipStatus;

import java.time.LocalDateTime;

/**
 * 친구 요청 정보 응답 DTO
 */
public record FriendRequestResponse(
        Long requestId,
        Long userId,
        String nickname,
        String profileImage,
        Boolean isOnline,
        FriendshipStatus status,
        LocalDateTime createdAt
) {
    /**
     * 받은 친구 요청 (requester 정보 반환)
     */
    public static FriendRequestResponse fromReceived(Friendship friendship) {
        return new FriendRequestResponse(
                friendship.getId(),
                friendship.getRequesterId(),
                friendship.getRequesterNickname(),
                friendship.getRequesterProfileImage(),
                friendship.getRequesterIsOnline(),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }

    /**
     * 보낸 친구 요청 (addressee 정보 반환)
     */
    public static FriendRequestResponse fromSent(Friendship friendship) {
        return new FriendRequestResponse(
                friendship.getId(),
                friendship.getAddresseeId(),
                friendship.getAddresseeNickname(),
                friendship.getAddresseeProfileImage(),
                friendship.getAddresseeIsOnline(),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }
}
