package com.ssafy.domain.gamification.entity;

import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_badge",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_badge",
                columnNames = {"user_id", "badge_id"}
        ),
        indexes = @Index(
                name = "idx_user_badge_type",
                columnList = "user_id, badge_type, reference_type"
        )
        )
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type")
    private BadgeType badgeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "earned_at")
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        earnedAt = LocalDateTime.now();
    }

    @Builder
    public UserBadge(User user, Badge badge, BadgeType badgeType,
                     ReferenceType referenceType, Long referenceId, Integer rank) {
        this.user = user;
        this.badge = badge;
        this.badgeType = badgeType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.rank = rank;
    }

    public enum BadgeType {
        ACTIVITY,
        STREAK,
        STUDY,
        QUIZ_KING,
        SPECIAL
    }

    public enum ReferenceType {
        CONTEST,
        STUDY,
        GENERAL
    }
}
