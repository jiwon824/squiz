package com.ssafy.domain.quiz.entity;

import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 코스 진행 기록 엔티티.
 *
 * 사용자의 코스 진행 상태를 추적한다.
 * 
 * PK: (user_id, course_id) - 복합 키
 * 
 * DDL: docs/sql/ERD.sql - user_course_progress
 */
@Entity
@Table(name = "user_course_progress")
@IdClass(UserCourseProgressId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourseProgress {

    /**
     * 사용자 ID (복합 PK).
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * 코스 ID (복합 PK).
     */
    @Id
    @Column(name = "course_id")
    private Long courseId;

    /**
     * 사용자 참조 (읽기 전용, insert/update에 포함되지 않음).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 코스 참조 (읽기 전용, insert/update에 포함되지 않음).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private QuizCourse course;

    /**
     * 사용자가 현재 진행 중인 섹션 번호.
     * 
     * @Builder 패턴이 기본값을 무시해서 @Builder.Default 필요
     */
    @Builder.Default
    @Column(name = "current_section")
    private Integer currentSection = 1;

    /**
     * 완료된 섹션 수.
     */
    @Builder.Default
    @Column(name = "completed_sections")
    private Integer completedSections = 0;

    /**
     * 코스가 완료되었는지 여부.
     */
    @Builder.Default
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    /**
     * When the course was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 사용자가 코스를 시작한 시각.
     */
    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    /**
     * 마지막 업데이트 시각.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 다음 섹션으로 진행
     */
    public void advanceToSection(Integer nextSectionNumber) {
        this.currentSection = nextSectionNumber;
        this.completedSections++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 코스를 완료 처리
     */
    public void completeCourse() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }
}