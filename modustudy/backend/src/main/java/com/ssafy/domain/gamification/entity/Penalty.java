package com.ssafy.domain.gamification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(length = 10)
    private String icon;

    @Column(name = "grant_condition", length = 200)
    private String grantCondition;

    @Column(name = "removal_condition", length = 200)
    private String removalCondition;

    @Column(name = "removal_required")
    private Integer removalRequired;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}