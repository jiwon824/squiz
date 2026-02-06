package com.ssafy.domain.gamification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "level_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer level;

    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName;

    @Column(name = "required_exp", nullable = false)
    private Integer requiredExp;

    @Column(name = "level_icon_url", length = 500)
    private String levelIconUrl;

    @Column(name = "level_color", length = 20)
    private String levelColor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
