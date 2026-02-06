package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 스터디 퀴즈 엔티티
 * 미팅/자료/수동 생성 퀴즈를 관리
 */
 @Entity
 @Table(name = "study_quiz")
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 public class StudyQuiz extends BaseEntity {

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 퀴즈 생성 소스 타입
     * MEETING: 화상회의 기반 (AI 자동 생성)
     * MATERIAL: 학습 자료 기반
     * MANUAL: 수동 생성
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    /**
     * 소스 ID (미팅인 경우 meetingId)
     */
    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StudyQuizStatus status = StudyQuizStatus.ACTIVE;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyQuizQuestion> questions = new ArrayList<>();

    public enum SourceType {
        MEETING, MATERIAL, MANUAL
    }

    public enum StudyQuizStatus {
        ACTIVE, DISABLED
    }

    /**
     * 문제 추가
     */
    public void addQuestion(StudyQuizQuestion question) {
        questions.add(question);
        question.setQuiz(this);
    }
}
