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
@Table(name = "contribution_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionDetail {

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
        STUDY_ATTENDANCE,    // 스터디 출석
        QUIZ_SOLVED,         // 퀴즈 풀이
        STUDY_JOIN,          // 스터디 가입
        STUDY_CREATE,        // 스터디 생성
        MATERIAL_UPLOAD,     // 자료 업로드
        RETROSPECTIVE,       // 회고록 작성
        CHAT_MESSAGE         // 채팅 메시지
    }
}