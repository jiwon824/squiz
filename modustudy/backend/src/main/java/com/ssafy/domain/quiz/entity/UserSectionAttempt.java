package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.quiz.entity.enums.AttemptStatus;
import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자별 섹션 시도 기록 엔티티.
 *
 * 사용자가 섹션을 풀 때마다 기록되며, 점수와 통과 여부를 저장한다.
 * 한 섹션에 대해 여러 번 시도할 수 있다.
 *
 * FK: (quiz_course_id, section_number) references quiz_course_section
 * 
 * DDL: docs/sql/ERD.sql - user_section_attempt
 */
@Entity
@Table(name = "user_section_attempt", indexes = {
        @Index(name = "idx_attempt_user_section_status", columnList = "user_id, quiz_course_id, section_number, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSectionAttempt extends BaseEntity {

    /**
     * 낙관적 잠금 버전 (Optimistic Locking).
     * 동시 수정 충돌 방지를 위해 Hibernate가 자동 관리.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * 시도한 사용자.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 시도한 섹션 (복합 FK: quiz_course_id + section_number).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "quiz_course_id", referencedColumnName = "quiz_course_id", nullable = false),
            @JoinColumn(name = "section_number", referencedColumnName = "section_number", nullable = false)
    })
    private QuizCourseSection section;

    /**
     * 시도 상태 (IN_PROGRESS, SUBMITTED, ABANDONED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /**
     * 획득 점수 (%).
     */
    @Column
    private Integer score = 0;

    /**
     * 정답 개수.
     */
    @Column(name = "correct_count")
    private Integer correctCount = 0;

    /**
     * 총 문제 수 (이 시도에 할당된 문제 수).
     */
    @Column(name = "total_questions")
    private Integer totalQuestions = 0;

    /**
     * 통과 여부.
     */
    @Column(name = "is_passed")
    private Boolean isPassed = false;

    /**
     * 시도 완료 시각.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 이 시도에 할당된 문제 목록 (셔플된 순서로 저장).
     */
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<UserSectionAttemptQuestion> attemptQuestions = new ArrayList<>();

    @Builder
    public UserSectionAttempt(User user, QuizCourseSection section, Integer totalQuestions) {
        this.user = user;
        this.section = section;
        this.status = AttemptStatus.IN_PROGRESS;
        this.totalQuestions = totalQuestions != null ? totalQuestions : 0;
    }

    /**
     * 시도에 문제를 추가한다.
     */
    public void addAttemptQuestion(UserSectionAttemptQuestion attemptQuestion) {
        this.attemptQuestions.add(attemptQuestion);
        attemptQuestion.setAttempt(this);
    }

    /**
     * 시도를 완료 처리한다.
     *
     * @param correctCount 맞힌 문제 수
     * @param passScore    통과 기준 점수 (%)
     */
    public void complete(int correctCount, int passScore) {
        this.correctCount = correctCount;
        this.score = totalQuestions > 0 ? (correctCount * 100) / totalQuestions : 0;
        this.isPassed = this.score >= passScore;
        this.status = AttemptStatus.SUBMITTED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 시도를 포기 처리한다.
     */
    public void abandon() {
        this.status = AttemptStatus.ABANDONED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 진행 중인 시도인지 확인한다.
     */
    public boolean isInProgress() {
        return this.status == AttemptStatus.IN_PROGRESS;
    }
}
