package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "study_application")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StudyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스터디 ID
     */
    @Column(name = "study_id", nullable = false)
    private Long studyId;

    /**
     * 신청자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 신청 메시지
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * AI 매칭 점수
     */
    @Column(name = "matching_score", precision = 5, scale = 2)
    private BigDecimal matchingScore;

    /**
     * 신청 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /**
     * 거절 사유
     */
    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    /**
     * 신청 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 처리 일시 (승인/거절 시점)
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // ============================================================
    // 비즈니스 로직
    // ============================================================

    /**
     * 신청 승인
     */
    public void approve() {
        this.status = ApplicationStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 신청 거절
     */
    public void reject(String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectedReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 대기 중인지 확인
     */
    public boolean isPending() {
        return this.status == ApplicationStatus.PENDING;
    }

    /**
     * 승인된 신청인지 확인
     */
    public boolean isApproved() {
        return this.status == ApplicationStatus.APPROVED;
    }

    /**
     * 거절된 신청인지 확인
     */
    public boolean isRejected() {
        return this.status == ApplicationStatus.REJECTED;
    }
}
