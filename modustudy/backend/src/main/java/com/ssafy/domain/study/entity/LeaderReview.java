package com.ssafy.domain.study.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "study_leader_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_study_reviewer",
                columnNames = {"study_id", "reviewer_id"}
        ))
        @Getter
        @Setter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        public class LeaderReview extends BaseEntity {

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
