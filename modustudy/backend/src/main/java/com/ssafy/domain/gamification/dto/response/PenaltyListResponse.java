package com.ssafy.domain.gamification.dto.response;

import com.ssafy.domain.gamification.entity.PenaltyType;
import com.ssafy.domain.gamification.entity.UserPenalty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PenaltyListResponse {
    private List<PenaltyInfo> activePenalties;
    private List<RemovedPenalty> removedPenalties;
    private Integer totalActive;
    private Integer totalRemoved;

    @Getter
    @Builder
    public static class PenaltyInfo {
        private Long id;
        private String penaltyType;
        private String name;
        private String description;
        private String icon;
        private LocalDateTime grantedAt;
        private Long studyId;
        private String studyName;
        private Boolean isActive;
        private String removalCondition;
        private Integer removalProgress;
        private Integer removalRequired;

        // ⭐ 추가: UserPenalty로부터 생성하는 메서드
        public static PenaltyInfo from(UserPenalty userPenalty) {
            PenaltyType type = userPenalty.getPenaltyType();
            return PenaltyInfo.builder()
                    .id(userPenalty.getId())
                    .penaltyType(type.name())
                    .name(type.getName())
                    .description(type.getDescription())
                    .icon(type.getIcon())
                    .grantedAt(userPenalty.getGrantedAt())
                    .studyId(userPenalty.getStudy() != null ? userPenalty.getStudy().getId() : null)
                    .studyName(userPenalty.getStudy() != null ? userPenalty.getStudy().getName() : null)
                    .isActive(userPenalty.getIsActive())
                    .removalCondition(type.getRemovalCondition())
                    .removalProgress(userPenalty.getRemovalProgress())
                    .removalRequired(type.getRemovalRequired())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RemovedPenalty {
        private Long id;
        private String penaltyType;
        private String name;
        private String description;
        private String icon;
        private LocalDateTime grantedAt;
        private LocalDateTime removedAt;
        private Long studyId;
        private String studyName;

        // ⭐ 추가: UserPenalty로부터 생성하는 메서드
        public static RemovedPenalty from(UserPenalty userPenalty) {
            PenaltyType type = userPenalty.getPenaltyType();
            return RemovedPenalty.builder()
                    .id(userPenalty.getId())
                    .penaltyType(type.name())
                    .name(type.getName())
                    .description(type.getDescription())
                    .icon(type.getIcon())
                    .grantedAt(userPenalty.getGrantedAt())
                    .removedAt(userPenalty.getRemovedAt())
                    .studyId(userPenalty.getStudy() != null ? userPenalty.getStudy().getId() : null)
                    .studyName(userPenalty.getStudy() != null ? userPenalty.getStudy().getName() : null)
                    .build();
        }
    }
}