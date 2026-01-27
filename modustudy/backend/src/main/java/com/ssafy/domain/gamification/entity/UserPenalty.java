package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
<<<<<<< HEAD
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ⭐ 변경: Penalty 엔티티 대신 Enum 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 50)
    private PenaltyType penaltyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "removal_progress")
    private Integer removalProgress = 0;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }

    @Builder
    public UserPenalty(User user, PenaltyType penaltyType, Study study) {
        this.user = user;
        this.penaltyType = penaltyType;
        this.study = study;
        this.isActive = true;
        this.removalProgress = 0;
    }

    public void increaseProgress() {
        this.removalProgress++;
    }

    public void remove() {
        this.isActive = false;
        this.removedAt = LocalDateTime.now();
    }
}
=======
public class UserPenalty extends BaseEntity {
}
>>>>>>> 0bd01c520deee4e3af4151c34385aedf281e4bbd
