package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badge")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge extends BaseEntity {
    /**
     * 배지 고유 코드.
     * 퀴즈 코스의 badgeCode와 매칭된다.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 배지 표시 이름.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 배지 설명.
     */
    @Column(nullable = false, length = 200)
    private String description;

    /**
     * 배지 아이콘 (이모지 등).
     */
    @Column(length = 10)
    private String icon;

    /**
     * 배지 카테고리.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeCategory category;

    /**
     * 배지 획득 조건 타입.
     */
    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    /**
     * 배지 획득 조건 값.
     */
    @Column(name = "condition_value", nullable = false)
    private Integer conditionValue;

    /**
     * 목록 정렬 우선순위.
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
