package com.ssafy.domain.gamification.entity;

import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.study.entity.Study;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id", nullable = false)
    private Penalty penalty;

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
    public UserPenalty(User user, Penalty penalty, Study study) {
        this.user = user;
        this.penalty = penalty;
        this.study = study;
        this.isActive = true;
        this.removalProgress = 0;
    }

    public void removeProgress() {
        this.removalProgress++;
    }

    public void remove() {
        this.isActive = false;
        this.removedAt = LocalDateTime.now();
    }
}