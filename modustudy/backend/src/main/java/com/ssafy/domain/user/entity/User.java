package com.ssafy.domain.user.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.quiz.entity.UserCourseProgress;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(unique = true)
    private String userId;

    private String password;

    private String name;
    private String department;
    private String position;

    // ========== 소셜 로그인용 ==========

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private Boolean isOnline = false;

    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private Boolean isSearchable = true;

    @Column(columnDefinition = "TEXT")
    private String bio;  // 자기소개

    @Column(columnDefinition = "JSON")
    private String interests;  // 관심 분야 (JSON 배열)

    @Column(columnDefinition = "JSON")
    private String techStacks;  // 기술 스택 (JSON 배열)

    // ========== 스터디 선호 설정 ==========

    @Column(columnDefinition = "JSON")
    private String availableDays;  // 가능한 요일 (JSON 배열, 예: ["MON","WED","FRI"])

    @Column(columnDefinition = "JSON")
    private String preferredTimeSlots;  // 선호 시간대 (JSON 배열, 예: ["EVENING","NIGHT"])

    @Column(name = "preferred_duration_weeks")
    private Integer preferredDurationWeeks;  // 선호 스터디 기간 (2~8주)

    // ========== 스터디장 관련 ==========

    /**
     * 스터디장 평점 (캐싱)
     */
    @Column(name = "leader_rating")
    @Builder.Default
    private Float leaderRating = 0.0f;

    /**
     * 스터디장 평가 수 (캐싱)
     */
    @Column(name = "leader_review_count")
    @Builder.Default
    private Integer leaderReviewCount = 0;

    // ========== 레벨/경험치 관련 ==========

    @Column(nullable = false)
    private Integer totalExp = 0;

    @Column(nullable = false)
    private Integer currentPoints = 0;

    @Column(nullable = false)
    private Integer currentLevel = 1;

    @Column(length = 50)
    private String levelName = "Bronze";

    @Column(length = 500)
    private String profileImage;  // 프로필 이미지 URL

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // ========== 소셜 계정 연동 ==========
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserSocialAccount> socialAccounts = new ArrayList<>();

    // ======= 퀴즈 관련 =======
    /**
     * 사용자의 코스 진행 목록.
     */
    @OneToMany(mappedBy = "user")
    private List<UserCourseProgress> courseProgresses = new ArrayList<>();

}
