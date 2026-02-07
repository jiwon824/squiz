package com.ssafy.domain.gamification.dto.response;

import com.ssafy.domain.gamification.entity.Penalty;
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
        private String code;              // ⭐ 추가: THREE_DAY_QUIT 등
        private String name;              // 작심삼일 등
        private String description;
        private String icon;
        private String grantCondition;    // ⭐ 추가
        private String removalCondition;
        private Integer removalProgress;
        private Integer removalRequired;
        private Long studyId;
        private String studyName;
        private Boolean isActive;
        private LocalDateTime grantedAt;

        // ⭐ 수정: Penalty 엔티티 사용
        public static PenaltyInfo from(UserPenalty userPenalty) {
            Penalty penalty = userPenalty.getPenalty();
            return PenaltyInfo.builder()
                    .id(userPenalty.getId())
                    .code(penalty.getCode())
                    .name(penalty.getName())
                    .description(penalty.getDescription())
                    .icon(penalty.getIcon())
                    .grantCondition(penalty.getGrantCondition())
                    .removalCondition(penalty.getRemovalCondition())
                    .removalProgress(userPenalty.getRemovalProgress())
                    .removalRequired(penalty.getRemovalRequired())
                    .studyId(userPenalty.getStudy() != null ? userPenalty.getStudy().getId() : null)
                    .studyName(userPenalty.getStudy() != null ? userPenalty.getStudy().getName() : null)
                    .isActive(userPenalty.getIsActive())
                    .grantedAt(userPenalty.getGrantedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RemovedPenalty {
        private Long id;
        private String code;              // ⭐ 추가
        private String name;
        private String description;
        private String icon;
        private LocalDateTime grantedAt;
        private LocalDateTime removedAt;
        private Long studyId;
        private String studyName;

        // ⭐ 수정: Penalty 엔티티 사용
        public static RemovedPenalty from(UserPenalty userPenalty) {
            Penalty penalty = userPenalty.getPenalty();
            return RemovedPenalty.builder()
                    .id(userPenalty.getId())
                    .code(penalty.getCode())
                    .name(penalty.getName())
                    .description(penalty.getDescription())
                    .icon(penalty.getIcon())
                    .grantedAt(userPenalty.getGrantedAt())
                    .removedAt(userPenalty.getRemovedAt())
                    .studyId(userPenalty.getStudy() != null ? userPenalty.getStudy().getId() : null)
                    .studyName(userPenalty.getStudy() != null ? userPenalty.getStudy().getName() : null)
                    .build();
        }
    }
}
