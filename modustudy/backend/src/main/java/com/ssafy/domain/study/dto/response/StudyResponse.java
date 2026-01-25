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
    private Long leaderId;
    private String leaderName;
    private String leaderNickname;
    private String name;
    private String description;
    private String topic;
    private String format;
    private StudyType studyType;
    private MeetingType meetingType;
    private Long regionId;
    private String locationDetail;
    private String scheduleSummary;
    private String scheduleDays;
    private LocalTime scheduleTime;
    private Integer maxMembers;
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

    /**
     * Entity를 DTO로 변환 (기본)
     */
    public static StudyResponse from(Study study) {
        return StudyResponse.builder()
                .id(study.getId())
                .leaderId(study.getLeaderId())
                .name(study.getName())
                .description(study.getDescription())
                .topic(study.getTopic())
                .format(study.getFormat())
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
                .build();
    }

    /**
     * Entity를 DTO로 변환 (스터디장 정보 포함)
     */
    public static StudyResponse from(Study study, User leader) {
        return StudyResponse.builder()
                .id(study.getId())
                .leaderId(study.getLeaderId())
                .leaderName(leader.getName())          // ⭐ 스터디장 이름
                .leaderNickname(leader.getNickname())  // ⭐ 스터디장 닉네임
                .name(study.getName())
                .description(study.getDescription())
                .topic(study.getTopic())
                .format(study.getFormat())
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
                .build();
    }
}