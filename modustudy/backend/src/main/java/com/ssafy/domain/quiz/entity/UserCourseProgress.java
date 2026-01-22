package com.ssafy.domain.quiz.entity;

import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_course_progress")
@IdClass(UserCourseProgressId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourseProgress {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "course_id")
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private QuizCourse course;

    // @Builder 패턴이 기본값을 무시해서 @Builder.Default 필요
    @Builder.Default
    @Column(name = "current_section")
    private Integer currentSection = 1;

    // @Builder 패턴이 기본값을 무시해서 @Builder.Default 필요
    @Builder.Default
    @Column(name = "completed_sections")
    private Integer completedSections = 0;

    // @Builder 패턴이 기본값을 무시해서 @Builder.Default 필요
    @Builder.Default
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void advanceToSection(Integer nextSectionNumber) {
        this.currentSection = nextSectionNumber;
        this.completedSections++;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeCourse() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }
}