package com.ssafy.domain.study.entity;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.converter.JsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "study")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String intro;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ========== 카테고리 변경: String → 연관관계 ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id")
    private Format format;
    // ====================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "study_type", nullable = false)
    private StudyType studyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    @Builder.Default
    private MeetingType meetingType = MeetingType.ONLINE;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "location_detail", length = 200)
    private String locationDetail;

    @Column(name = "schedule_summary", length = 100)
    private String scheduleSummary;

    @Column(name = "schedule_days", length = 50)
    private String scheduleDays;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 10;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_policy")
    @Builder.Default
    private PenaltyPolicy penaltyPolicy = PenaltyPolicy.NORMAL;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "recruit_start_date")
    private LocalDate recruitStartDate;

    @Column(name = "recruit_end_date")
    private LocalDate recruitEndDate;

    @Column(name = "extension_count")
    @Builder.Default
    private Integer extensionCount = 0;

    @Column(length = 500)
    private String textbook;

    @Column(length = 500)
    private String goal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Difficulty difficulty = Difficulty.INTERMEDIATE;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "process_detail", columnDefinition = "TEXT")
    private String processDetail;

    @Column(name = "target_org_type", length = 50)
    private String targetOrgType;

    @Convert(converter = JsonConverter.class)
    @Column(name = "target_org_criteria", columnDefinition = "JSON")
    private Map<String, Object> targetOrgCriteria;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== 편의 메서드 ==========

    /**
     * 주제명 반환 (null 안전)
     */
    public String getTopicName() {
        return topic != null ? topic.getName() : null;
    }

    /**
     * 형식명 반환 (null 안전)
     */
    public String getFormatName() {
        return format != null ? format.getName() : null;
    }

    // ========== 비즈니스 로직 ==========

    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

    public void extendRecruitment(LocalDate newEndDate) {
        if (this.extensionCount == null) {
            this.extensionCount = 0;
        }

        if (this.extensionCount >= 1) {
            throw new StudyException.MaxExtensionReachedException();
        }
        this.recruitEndDate = newEndDate;
        this.extensionCount++;
    }

    /**
     * 스터디 시작 가능 여부 확인
     * - 모집완료/시작대기 또는 확정대기 상태에서만 시작 가능
     */
    public boolean canStart() {
        return this.status == Status.RECRUIT_CLOSED || this.status == Status.PENDING;
    }

    /**
     * 모집 연장 가능 여부 확인
     * - 모집중 또는 확정대기 상태에서만 연장 가능
     * - 최대 1회 연장 가능
     */
    public boolean canExtendRecruitment() {
        boolean validStatus = this.status == Status.RECRUITING || this.status == Status.PENDING;
        int currentExtensionCount = this.extensionCount != null ? this.extensionCount : 0;
        return validStatus && currentExtensionCount < 1;
    }

    /**
     * 스터디 시작 처리
     */
    public void start() {
        if (!canStart()) {
            throw new StudyException.CannotStartStudyException();
        }
        this.status = Status.IN_PROGRESS;
    }
}