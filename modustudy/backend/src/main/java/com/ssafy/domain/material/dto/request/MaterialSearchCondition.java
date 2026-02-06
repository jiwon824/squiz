package com.ssafy.domain.material.dto.request;

import com.ssafy.domain.material.entity.MaterialType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 자료 검색 조건 DTO
 */
 @Getter
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public class MaterialSearchCondition {

    /**
     * 주차 필터
     */
    private Integer weekNumber;

    /**
     * 자료 타입 필터 (LINK, FILE, IMAGE, VIDEO)
     */
    private MaterialType type;

    /**
     * 검색 키워드 (제목, 설명)
     */
    private String keyword;
}
