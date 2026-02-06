package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_study",
                columnNames = {"user_id", "study_id"}
        ))
        @Getter
        @Setter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        public class StudyBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============================================================
    // 정적 팩토리 메서드
    // ============================================================

    /**
     * 북마크 생성
     */
    public static StudyBookmark create(Long userId, Long studyId) {
        return StudyBookmark.builder()
                .userId(userId)
                .studyId(studyId)
                .build();
    }

    // ============================================================
    // 비즈니스 메서드
    // ============================================================

    /**
     * 본인 북마크인지 확인
     */
    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }
}
