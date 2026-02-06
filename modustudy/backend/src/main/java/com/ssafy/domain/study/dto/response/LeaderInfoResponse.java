package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderInfoResponse {

    private Long userId;

    private String name;

    private String nickname;

    private String email;

    private String profileImage;

    private Float leaderRating;

    private Integer leaderReviewCount;

    private Integer currentLevel;

    private String levelName;

    public static LeaderInfoResponse from(User user) {
        return LeaderInfoResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .leaderRating(user.getLeaderRating())
                .leaderReviewCount(user.getLeaderReviewCount())
                .currentLevel(user.getCurrentLevel())
                .levelName(user.getLevelName())
                .build();
    }
}
