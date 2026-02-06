package com.ssafy.domain.gamification.entity;

import com.ssafy.domain.gamification.config.ExperienceConfig;
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

    /** 현재 레벨에서의 경험치 (레벨업 시 리셋) */
    @Column(name = "current_experience")
    private Integer currentExperience = 0;

    /** 누적 총 경험치 (통계용) */
    @Column(name = "total_experience")
    private Integer totalExperience = 0;

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

    @Column(name = "total_attendance")
    private Integer totalAttendance = 0;

    @Column(name = "total_chat_count")
    private Integer totalChatCount = 0;

    /** 오늘 채팅으로 얻은 경험치 (일일 제한용) */
    @Column(name = "today_chat_exp")
    private Integer todayChatExp = 0;

    @Column(name = "last_chat_date")
    private LocalDate lastChatDate;

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
        this.currentExperience = 0;
        this.totalExperience = 0;
        this.totalActivityDays = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.totalStudiesJoined = 0;
        this.totalStudiesLed = 0;
        this.totalAttendance = 0;
        this.totalChatCount = 0;
        this.todayChatExp = 0;
        this.totalQuizCount = 0;
        this.totalMaterialsUploaded = 0;
        this.totalRetrospectives = 0;
    }

    /**
     * 경험치 추가 및 레벨업 처리
     * @param exp 획득한 경험치
     * @return 레벨업 여부
     */
    public boolean addExperience(int exp) {
        this.currentExperience += exp;
        this.totalExperience += exp;

        boolean leveledUp = false;

        // 레벨업 체크 (연속 레벨업 가능)
        while (canLevelUp()) {
            levelUp();
            leveledUp = true;
        }

        return leveledUp;
    }

    /**
     * 레벨업 가능 여부 확인
     */
    private boolean canLevelUp() {
        if (this.level >= ExperienceConfig.MAX_LEVEL) {
            return false;
        }
        int requiredXp = ExperienceConfig.getRequiredXpForNextLevel(this.level);
        return this.currentExperience >= requiredXp;
    }

    /**
     * 레벨업 처리 (경험치 리셋)
     */
    private void levelUp() {
        int requiredXp = ExperienceConfig.getRequiredXpForNextLevel(this.level);
        this.currentExperience -= requiredXp; // 초과 경험치는 다음 레벨로 이월
        this.level++;
        this.levelName = ExperienceConfig.getLevelName(this.level);
    }

    /**
     * 다음 레벨까지 필요한 경험치
     */
    public int getRequiredExpForNextLevel() {
        return ExperienceConfig.getRequiredXpForNextLevel(this.level);
    }

    /**
     * 현재 레벨 진행률 (0-100)
     */
    public double getLevelProgressPercentage() {
        int required = getRequiredExpForNextLevel();
        if (required == 0) return 100.0; // 최대 레벨
        return Math.min(100.0, (double) this.currentExperience / required * 100);
    }

    /**
     * 활동일 기록 및 연속 출석 처리
     * @param date 활동 날짜
     * @return 연속 출석 보너스 (해당되면)
     */
    public int recordActivity(LocalDate date) {
        int streakBonus = 0;

        if (this.lastActivityDate == null) {
            // 첫 활동
            this.currentStreak = 1;
            this.totalActivityDays = 1;
        } else if (date.equals(this.lastActivityDate)) {
            // 같은 날 중복 활동 - 연속 출석 변경 없음
            return 0;
        } else if (date.equals(this.lastActivityDate.plusDays(1))) {
            // 연속 출석
            this.currentStreak++;
            this.totalActivityDays++;

            // 연속 출석 마일스톤 보너스
            streakBonus = ExperienceConfig.getStreakBonus(this.currentStreak);
        } else {
            // 연속 출석 끊김
            this.currentStreak = 1;
            this.totalActivityDays++;
        }

        // 최대 연속 출석 갱신
        if (this.currentStreak > this.maxStreak) {
            this.maxStreak = this.currentStreak;
        }

        this.lastActivityDate = date;
        return streakBonus;
    }

    /**
     * 채팅 경험치 추가 (일일 제한 적용)
     * @param date 채팅 날짜
     * @return 실제 획득한 경험치
     */
    public int addChatExperience(LocalDate date) {
        // 날짜가 바뀌면 일일 채팅 경험치 리셋
        if (this.lastChatDate == null || !date.equals(this.lastChatDate)) {
            this.todayChatExp = 0;
            this.lastChatDate = date;
        }

        // 일일 제한 확인
        if (this.todayChatExp >= ExperienceConfig.CHAT_DAILY_MAX) {
            return 0;
        }

        int expGain = Math.min(
                ExperienceConfig.CHAT_MESSAGE,
                ExperienceConfig.CHAT_DAILY_MAX - this.todayChatExp
        );

        this.todayChatExp += expGain;
        this.totalChatCount++;

        return expGain;
    }

    // ========== 통계 증가 메서드 ==========

    public void incrementStudiesJoined() {
        this.totalStudiesJoined++;
    }

    public void incrementStudiesLed() {
        this.totalStudiesLed++;
    }

    public void incrementAttendance() {
        this.totalAttendance++;
    }

    public void incrementQuizCount() {
        this.totalQuizCount++;
    }

    public void incrementMaterialsUploaded() {
        this.totalMaterialsUploaded++;
    }

    public void incrementRetrospectives() {
        this.totalRetrospectives++;
    }
}
