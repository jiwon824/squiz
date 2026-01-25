package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 스터디 템플릿 엔티티
 * 사용자가 자주 사용하는 스터디 설정을 템플릿으로 저장
 */
@Entity
@Table(name = "study_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 템플릿 소유자 (User ID)
     * NULL이면 시스템 제공 템플릿
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 템플릿 이름 (예: "알고리즘 스터디 템플릿")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 시스템 기본 템플릿 여부
     */
    @Column(name = "is_system")
    @Builder.Default
    private boolean isSystem = false;

    /**
     * 템플릿 타입 (ALGORITHM, CS, INTERVIEW, PROJECT, CERTIFICATE, READING)
     */
    @Column(name = "template_type", length = 50)
    private String templateType;

    /**
     * 주제
     */
    @Column(length = 50)
    private String topic;

    /**
     * 형식 (예: "문제 풀이 + 코드 리뷰")
     */
    @Column(length = 50)
    private String format;

    /**
     * 모임 방식 (ONLINE, OFFLINE, HYBRID)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    private MeetingType meetingType;

    /**
     * 스터디 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 교재/자료
     */
    @Column(length = 500)
    private String textbook;

    /**
     * 스터디 목표
     */
    @Column(length = 500)
    private String goal;

    /**
     * 난이도 (BEGINNER, ELEMENTARY, INTERMEDIATE, ADVANCED)
     */
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    /**
     * 참가 조건/사전 요구사항
     */
    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    /**
     * 진행 방식 상세
     */
    @Column(columnDefinition = "TEXT", name = "process_detail")
    private String processDetail;

    /**
     * 패널티 정책 (STRICT, NORMAL, LENIENT, RATIO, NONE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_policy")
    private PenaltyPolicy penaltyPolicy;

    /**
     * 생성일시
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}