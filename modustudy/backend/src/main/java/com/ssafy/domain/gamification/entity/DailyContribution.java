package com.ssafy.domain.gamification.entity;

import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_contribution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_date",
                columnNames = {"user_id", "contribution_date"}
        )
        )
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        public class DailyContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Column(name = "activity_count")
    private Integer activityCount = 0;

    @Column(name = "has_activity")
    private Boolean hasActivity = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public DailyContribution(User user, LocalDate contributionDate, Integer activityCount) {
        this.user = user;
        this.contributionDate = contributionDate;
        this.activityCount = activityCount != null ? activityCount : 0;
        this.hasActivity = true;
    }

    public void incrementCount() {
        this.activityCount++;
    }
}
