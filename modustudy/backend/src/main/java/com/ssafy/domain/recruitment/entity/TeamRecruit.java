package com.ssafy.domain.recruitment.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * 팀원 모집 글 엔티티
 */
 @Entity
 @Table(name = "team_recruit")
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 public class TeamRecruit extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_roles", columnDefinition = "json")
    private String requiredRoles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_stack", columnDefinition = "json")
    private String techStack;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 5;

    @Column(name = "current_members")
    @Builder.Default
    private Integer currentMembers = 1;

    @Column
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecruitStatus status = RecruitStatus.RECRUITING;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    public enum RecruitCategory {
        HACKATHON, PROJECT, COMPETITION
    }

    public enum RecruitStatus {
        RECRUITING, CLOSED, COMPLETED
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateStatus(RecruitStatus status) {
        this.status = status;
    }

    public void update(String title, String content, String requiredRoles, String techStack,
                       Integer maxMembers, LocalDate deadline, RecruitCategory category) {
        this.title = title;
        this.content = content;
        this.requiredRoles = requiredRoles;
        this.techStack = techStack;
        this.maxMembers = maxMembers;
        this.deadline = deadline;
        this.category = category;
    }
}
