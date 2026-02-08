package com.ssafy.domain.friend.dto.response;

import com.ssafy.domain.user.entity.User;

/**
 * 사용자 검색 결과 응답 DTO
 */
 public record UserSearchResponse(
        Long userId,
        String nickname,
        String profileImage,
        Boolean isOnline,
        FriendStatus friendStatus
        ) {
    public enum FriendStatus {
        NONE,            // 관계 없음
        PENDING_SENT,    // 내가 요청 보냄
        PENDING_RECEIVED,// 상대가 요청 보냄
        FRIEND,          // 이미 친구
        BLOCKED          // 내가 차단함
    }

    public static UserSearchResponse from(User user, FriendStatus friendStatus) {
        return new UserSearchResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getIsOnline(),
                friendStatus
        );
    }
}
