package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.user.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyResponse {

    private Long id;
    private String name;
    private String intro;
    private String description;

    // ========== 카테고리 정보 ==========
    private TopicInfo topic;
    private FormatInfo format;
    // ==================================

    private StudyType studyType;
    private MeetingType meetingType;
    private Long regionId;
    private String locationDetail;
    private String scheduleSummary;
    private String scheduleDays;
    private LocalTime scheduleTime;
    private Integer maxMembers;
    private Integer currentMembers;  // 현재 참여 인원
    private Boolean isPublic;
    private Status status;
    private PenaltyPolicy penaltyPolicy;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalSessions;
    private LocalDate recruitStartDate;
    private LocalDate recruitEndDate;
    private Integer extensionCount;
    private String textbook;
    private String goal;
    private Difficulty difficulty;
    private String prerequisites;
    private String processDetail;
    private String targetOrgType;
    private Map<String, Object> targetOrgCriteria;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 스터디장 정보
    private LeaderInfo leader;

    // 현재 사용자가 스터디장인지 여부
    private Boolean isLeader;

    // ========== 내부 DTO 클래스 ==========

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopicInfo {
        private Long id;
        private String name;
        private String icon;
        private ParentTopicInfo parent;  // 대분류 정보

        public static TopicInfo from(Topic topic) {
            if (topic == null) return null;

            return TopicInfo.builder()
                    .id(topic.getId())
                    .name(topic.getName())
                    .icon(topic.getIcon())
                    .parent(topic.getParent() != null ? ParentTopicInfo.from(topic.getParent()) : null)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParentTopicInfo {
        private Long id;
        private String name;
        private String icon;

        public static ParentTopicInfo from(Topic topic) {
            if (topic == null) return null;

            return ParentTopicInfo.builder()
                    .id(topic.getId())
                    .name(topic.getName())
                    .icon(topic.getIcon())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FormatInfo {
        private Long id;
        private String name;
        private String description;
        private String icon;

        public static FormatInfo from(Format format) {
            if (format == null) return null;

            return FormatInfo.builder()
                    .id(format.getId())
                    .name(format.getName())
                    .description(format.getDescription())
                    .icon(format.getIcon())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaderInfo {
        private Long id;
        private String nickname;
        private String profileImage;

        public static LeaderInfo from(User user) {
            if (user == null) return null;

            return LeaderInfo.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .build();
        }
    }

    // ========== 변환 메서드 ==========

    /**
     * Study Entity → Response (스터디장 정보 포함)
     */
    public static StudyResponse from(Study study, User leader) {
        return StudyResponse.builder()
                .id(study.getId())
                .name(study.getName())
                .intro(study.getIntro())
                .description(study.getDescription())
                .topic(TopicInfo.from(study.getTopic()))
                .format(FormatInfo.from(study.getFormat()))
                .studyType(study.getStudyType())
                .meetingType(study.getMeetingType())
                .regionId(study.getRegionId())
                .locationDetail(study.getLocationDetail())
                .scheduleSummary(study.getScheduleSummary())
                .scheduleDays(study.getScheduleDays())
                .scheduleTime(study.getScheduleTime())
                .maxMembers(study.getMaxMembers())
                .isPublic(study.getIsPublic())
                .status(study.getStatus())
                .penaltyPolicy(study.getPenaltyPolicy())
                .startDate(study.getStartDate())
                .endDate(study.getEndDate())
                .totalSessions(study.getTotalSessions())
                .recruitStartDate(study.getRecruitStartDate())
                .recruitEndDate(study.getRecruitEndDate())
                .extensionCount(study.getExtensionCount())
                .textbook(study.getTextbook())
                .goal(study.getGoal())
                .difficulty(study.getDifficulty())
                .prerequisites(study.getPrerequisites())
                .processDetail(study.getProcessDetail())
                .targetOrgType(study.getTargetOrgType())
                .targetOrgCriteria(study.getTargetOrgCriteria())
                .createdAt(study.getCreatedAt())
                .updatedAt(study.getUpdatedAt())
                .leader(LeaderInfo.from(leader))
                .build();
    }

    /**
     * Study Entity → Response (스터디장 정보 없이)
     */
    public static StudyResponse from(Study study) {
        return from(study, null);
    }

    /**
     * Study Entity → Response (스터디장 정보 + 현재 멤버 수 포함)
     */
    public static StudyResponse from(Study study, User leader, Integer currentMembers) {
        StudyResponse response = from(study, leader);
        response.setCurrentMembers(currentMembers);
        return response;
    }
}
