package com.ssafy.domain.friend.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 친구 관계 도메인
 * - PENDING: 친구 요청 보냄 (requester → addressee)
 * - ACCEPTED: 친구 수락됨 (양방향 친구)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {
    private Long id;
    private Long requesterId;
    private Long addresseeId;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 조인된 사용자 정보 (SELECT 시 사용)
    private String requesterNickname;
    private String requesterProfileImage;
    private Boolean requesterIsOnline;
    private LocalDateTime requesterLastSeenAt;

    private String addresseeNickname;
    private String addresseeProfileImage;
    private Boolean addresseeIsOnline;
    private LocalDateTime addresseeLastSeenAt;
}
