package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "study_recommend_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecommendItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id", nullable = false)
    private Long logId;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "matching_score", precision = 7, scale = 2)
    private BigDecimal matchingScore;

    @Column(name = "tech_match_count")
    @Builder.Default
    private Integer techMatchCount = 0;

    @Column(name = "schedule_match_count")
    @Builder.Default
    private Integer scheduleMatchCount = 0;

    @Column(name = "topic_match_count")
    @Builder.Default
    private Integer topicMatchCount = 0;

    @Column(name = "match_reason", length = 500)
    private String matchReason;
}
