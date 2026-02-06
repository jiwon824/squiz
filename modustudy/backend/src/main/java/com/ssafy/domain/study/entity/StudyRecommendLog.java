package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_recommend_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecommendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommend_type", nullable = false)
    @Builder.Default
    private RecommendType recommendType = RecommendType.GENERAL;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "result_count", nullable = false)
    @Builder.Default
    private Integer resultCount = 0;

    @Column(name = "user_tech_snapshot", columnDefinition = "JSON")
    private String userTechSnapshot;

    @Column(name = "user_schedule_snapshot", columnDefinition = "JSON")
    private String userScheduleSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RecommendType {
        GENERAL, TOPIC
    }
}
