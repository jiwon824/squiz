package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyRankingResponse {
    private List<RankingInfo> rankings;
    private Integer myRank;
    private Integer totalMembers;

    @Getter
    @Builder
    public static class RankingInfo {
        private Integer rank;
        private UserInfo user;
        private Integer activityDays;
        private Double attendanceRate;
        private Boolean isMe;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
        private String profileImage;
        private Integer level;
        private String levelName;
    }
}