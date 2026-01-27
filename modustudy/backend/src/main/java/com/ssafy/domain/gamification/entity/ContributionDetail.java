package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contribution_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionDetail extends BaseEntity {

<<<<<<< HEAD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "reference_name", nullable = false, length = 200)
    private String referenceName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public ContributionDetail(User user, LocalDate contributionDate,
                              ActivityType activityType, Long referenceId, String referenceName) {
        this.user = user;
        this.contributionDate = contributionDate;
        this.activityType = activityType;
        this.referenceId = referenceId;
        this.referenceName = referenceName;
    }

    public enum ActivityType {
        STUDY_ATTENDANCE,
        QUIZ_SOLVED,
        MATERIAL_UPLOAD,
        RETROSPECT_WRITTEN
    }
}
=======
}
>>>>>>> 3389b2ff68a26273732372d6bd8e25e865e5c448
