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
public class StudyCreateRequest {

    @NotBlank(message = "스터디명은 필수입니다")
    @Size(max = 100, message = "스터디명은 100자 이내여야 합니다")
    private String name;

    @Size(max = 200, message = "한줄 소개는 200자 이내여야 합니다")
    private String intro;

    @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
    private String description;

    // ========== 카테고리 변경: String → Long(ID) ==========
    @NotNull(message = "주제는 필수입니다")
    private Long topicId;

    private Long formatId;
    // ====================================================

    @NotNull(message = "스터디 타입은 필수입니다")
    private StudyType studyType;

    @NotNull(message = "미팅 타입은 필수입니다")
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
    private Integer maxMembers = 10;

    private Boolean isPublic = true;

    private PenaltyPolicy penaltyPolicy = PenaltyPolicy.NORMAL;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다")
    private LocalDate endDate;

    private Integer totalSessions;

    private LocalDate recruitStartDate;

    private LocalDate recruitEndDate;

    @Size(max = 500, message = "교재는 500자 이내여야 합니다")
    private String textbook;

    @Size(max = 500, message = "목표는 500자 이내여야 합니다")
    private String goal;

    private Difficulty difficulty = Difficulty.INTERMEDIATE;

    @Size(max = 1000, message = "사전 지식은 1000자 이내여야 합니다")
    private String prerequisites;

    @Size(max = 1000, message = "진행 방식 상세는 1000자 이내여야 합니다")
    private String processDetail;

    @Size(max = 50, message = "대상 소속 타입은 50자 이내여야 합니다")
    private String targetOrgType;

    private Map<String, Object> targetOrgCriteria;

    // 상태 (RECRUITING, PENDING 등) - 모집기간 기준으로 프론트에서 설정
    private Status status;

    /**
     * DTO를 Entity로 변환
     * ⚠️ Topic, Format은 Service에서 조회 후 설정해야 함
     */
    public Study toEntity(Long leaderId, Topic topic, Format format) {
        return Study.builder()
                .leaderId(leaderId)
                .name(name)
                .intro(intro)
                .description(description)
                .topic(topic)
                .format(format)
                .studyType(studyType)
                .meetingType(meetingType)
                .regionId(regionId)
                .locationDetail(locationDetail)
                .scheduleSummary(scheduleSummary)
                .scheduleDays(scheduleDays)
                .scheduleTime(scheduleTime)
                .maxMembers(maxMembers)
                .isPublic(isPublic)
                .status(status != null ? status : Status.DRAFT)
                .penaltyPolicy(penaltyPolicy)
                .startDate(startDate)
                .endDate(endDate)
                .totalSessions(totalSessions)
                .recruitStartDate(recruitStartDate)
                .recruitEndDate(recruitEndDate)
                .textbook(textbook)
                .goal(goal)
                .difficulty(difficulty)
                .prerequisites(prerequisites)
                .processDetail(processDetail)
                .targetOrgType(targetOrgType)
                .targetOrgCriteria(targetOrgCriteria)
                .build();
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
     * 날짜 검증 (종료일이 시작일과 같거나 늦어야 함)
     */
    public boolean isValidDateRange() {
        return !endDate.isBefore(startDate);
    }

    /**
     * 모집 기간 검증 (모집 종료일이 스터디 시작일보다 앞서야 함)
     */
    public boolean isValidRecruitmentPeriod() {
        if (recruitEndDate != null && startDate != null) {
            return !recruitEndDate.isAfter(startDate);
        }
        return true;
    }
}
