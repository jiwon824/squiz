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
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "level_name", length = 50)
    private String levelName = "새싹";

    @Column(name = "total_activity_days")
    private Integer totalActivityDays = 0;

    @Column(name = "current_streak")
    private Integer currentStreak = 0;

    @Column(name = "max_streak")
    private Integer maxStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_studies_joined")
    private Integer totalStudiesJoined = 0;

    @Column(name = "total_studies_led")
    private Integer totalStudiesLed = 0;

    @Column(name = "total_chat_count")
    private Integer totalChatCount = 0;

    @Column(name = "total_quiz_count")
    private Integer totalQuizCount = 0;

    @Column(name = "total_materials_uploaded")
    private Integer totalMaterialsUploaded = 0;

    @Column(name = "total_retrospectives")
    private Integer totalRetrospectives = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public UserStats(User user) {
        this.user = user;
        this.level = 1;
        this.levelName = "새싹";
        this.totalActivityDays = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
    }
}