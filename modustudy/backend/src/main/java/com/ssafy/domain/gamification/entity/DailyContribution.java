package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_contribution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
<<<<<<< HEAD
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
=======
public class DailyContribution extends BaseEntity {
}
>>>>>>> 3389b2ff68a26273732372d6bd8e25e865e5c448
