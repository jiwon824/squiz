package com.ssafy.domain.study.dto.request;

import com.ssafy.domain.study.entity.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyUpdateRequest {

    @Size(max = 100, message = "스터디명은 100자 이내여야 합니다")
    private String name;

    @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
    private String description;

    // ========== 카테고리 변경: String → Long(ID) ==========
    private Long topicId;

    private Long formatId;
    // ====================================================

    private StudyType studyType;

    private MeetingType meetingType;

    private Long regionId;

    @Size(max = 200, message = "상세 장소는 200자 이내여야 합니다")
    private String locationDetail;

    @Size(max = 100, message = "일정 요약은 100자 이내여야 합니다")
    private String scheduleSummary;

    @Size(max = 50, message = "요일 정보는 50자 이내여야 합니다")
    private String scheduleDays;

    private LocalTime scheduleTime;

    @Min(value = 2, message = "최소 인원은 2명 이상이어야 합니다")
    @Max(value = 100, message = "최대 인원은 100명 이하여야 합니다")
    private Integer maxMembers;

    private Boolean isPublic;

    private PenaltyPolicy penaltyPolicy;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer totalSessions;

    private LocalDate recruitStartDate;

    private LocalDate recruitEndDate;

    @Size(max = 500, message = "교재는 500자 이내여야 합니다")
    private String textbook;

    @Size(max = 500, message = "목표는 500자 이내여야 합니다")
    private String goal;

    private Difficulty difficulty;

    @Size(max = 1000, message = "사전 지식은 1000자 이내여야 합니다")
    private String prerequisites;

    @Size(max = 1000, message = "진행 방식 상세는 1000자 이내여야 합니다")
    private String processDetail;

    @Size(max = 50, message = "대상 소속 타입은 50자 이내여야 합니다")
    private String targetOrgType;

    private Map<String, Object> targetOrgCriteria;

    /**
     * 기존 Study 엔티티에 변경사항 적용
     * null이 아닌 필드만 업데이트
     * ⚠️ Topic, Format은 Service에서 조회 후 별도 설정해야 함
     */
    public void updateEntity(Study study, Topic topic, Format format) {
        if (name != null) study.setName(name);
        if (description != null) study.setDescription(description);
        if (topic != null) study.setTopic(topic);
        if (format != null) study.setFormat(format);
        if (studyType != null) study.setStudyType(studyType);
        if (meetingType != null) study.setMeetingType(meetingType);
        if (regionId != null) study.setRegionId(regionId);
        if (locationDetail != null) study.setLocationDetail(locationDetail);
        if (scheduleSummary != null) study.setScheduleSummary(scheduleSummary);
        if (scheduleDays != null) study.setScheduleDays(scheduleDays);
        if (scheduleTime != null) study.setScheduleTime(scheduleTime);
        if (maxMembers != null) study.setMaxMembers(maxMembers);
        if (isPublic != null) study.setIsPublic(isPublic);
        if (penaltyPolicy != null) study.setPenaltyPolicy(penaltyPolicy);
        if (startDate != null) study.setStartDate(startDate);
        if (endDate != null) study.setEndDate(endDate);
        if (totalSessions != null) study.setTotalSessions(totalSessions);
        if (recruitStartDate != null) study.setRecruitStartDate(recruitStartDate);
        if (recruitEndDate != null) study.setRecruitEndDate(recruitEndDate);
        if (textbook != null) study.setTextbook(textbook);
        if (goal != null) study.setGoal(goal);
        if (difficulty != null) study.setDifficulty(difficulty);
        if (prerequisites != null) study.setPrerequisites(prerequisites);
        if (processDetail != null) study.setProcessDetail(processDetail);
        if (targetOrgType != null) study.setTargetOrgType(targetOrgType);
        if (targetOrgCriteria != null) study.setTargetOrgCriteria(targetOrgCriteria);
    }

    /**
     * 오프라인/혼합 스터디인 경우 지역 정보 검증
     */
    public boolean isValidLocation() {
        if (meetingType == MeetingType.OFFLINE || meetingType == MeetingType.HYBRID) {
            return regionId != null;
        }
        return true;
    }

    /**
     * 날짜 검증 (종료일이 시작일보다 늦어야 함)
     */
    public boolean isValidDateRange() {
        if (startDate != null && endDate != null) {
            return endDate.isAfter(startDate);
        }
        return true;
    }
}