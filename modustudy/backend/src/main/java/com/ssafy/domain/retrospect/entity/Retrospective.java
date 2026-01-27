package com.ssafy.domain.retrospect.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "retrospective")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(length = 200, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrospective_type")
    @Builder.Default
    private RetrospectiveType retrospectiveType = RetrospectiveType.KPT;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
