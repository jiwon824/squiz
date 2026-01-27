package com.ssafy.domain.gamification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 패널티 마스터 데이터 (정의)
 */
@Entity
@Table(name = "penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;  // THREE_DAY_QUIT, LATE_KING 등

    @Column(nullable = false, length = 100)
    private String name;  // 작심삼일, 지각왕 등

    @Column(nullable = false, length = 200)
    private String description;  // 설명

    @Column(length = 10)
    private String icon;  // 😅, ⏰ 등

    @Column(length = 200)
    private String grantCondition;  // 부여 조건 설명

    @Column(length = 200)
    private String removalCondition;  // 해소 조건 설명

    private Integer removalRequired;  // 해소에 필요한 횟수 (3, 5 등)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}