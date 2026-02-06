package com.ssafy.domain.friend.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 차단 도메인
 * - blocker가 blocked를 차단
 * - 차단된 사용자는 친구 요청, DM을 보낼 수 없음
 */
 @Getter
 @Setter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class UserBlock {
    private Long id;
    private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;

    // 조인된 사용자 정보
    private String blockedNickname;
    private String blockedProfileImage;
}
