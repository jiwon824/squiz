package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 계층형 지역 엔티티.
 * Level 1: 시/도 (서울특별시, 경기도 등)
 * Level 2: 시/군/구 (강남구, 구미시 등)
 */
@Entity
@Table(name = "region", indexes = {
        @Index(name = "idx_region_parent", columnList = "parent_id"),
        @Index(name = "idx_region_level", columnList = "level"),
        @Index(name = "idx_region_code", columnList = "code"),
        @Index(name = "idx_region_active", columnList = "is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상위 지역 (NULL이면 시/도)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    /**
     * 하위 지역 목록
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Region> children = new ArrayList<>();

    /**
     * 지역 코드 (SEOUL, SEOUL_GANGNAM 등)
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * 지역명 (서울특별시, 강남구 등)
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 계층 레벨 (1: 시/도, 2: 시/군/구)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    /**
     * 전체 지역명 (서울특별시 강남구)
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * 정렬 순서
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 비즈니스 메서드 =====

    /**
     * 시/도인지 확인
     */
    public boolean isProvince() {
        return this.level == 1;
    }

    /**
     * 시/군/구인지 확인
     */
    public boolean isDistrict() {
        return this.level == 2;
    }

    /**
     * 하위 지역 추가
     */
    public void addChild(Region child) {
        this.children.add(child);
        child.assignParent(this);
    }

    /**
     * 부모 설정 (내부용)
     */
    private void assignParent(Region parent) {
        this.parent = parent;
    }

    /**
     * 활성화/비활성화
     */
    public void updateActive(boolean active) {
        this.isActive = active;
    }

    /**
     * 전체 지역명 반환 (없으면 name 반환)
     */
    public String getDisplayName() {
        return fullName != null ? fullName : name;
    }
}