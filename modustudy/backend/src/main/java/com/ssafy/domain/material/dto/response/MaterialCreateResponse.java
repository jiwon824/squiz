package com.ssafy.domain.material.dto.response;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 자료 생성 응답 DTO
 */
 @Getter
 @Builder
 public class MaterialCreateResponse {

    private Long id;
    private String title;
    private MaterialType materialType;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static MaterialCreateResponse from(Material material) {
        return MaterialCreateResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType())
                .createdAt(material.getCreatedAt())
                .build();
    }
}
