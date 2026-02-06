package com.ssafy.domain.material.dto.response;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 자료 상세 응답 DTO
 */
 @Getter
 @Builder
 public class MaterialDetailResponse {

    private Long id;
    private String title;
    private String description;
    private MaterialType materialType;

    // 링크 타입
    private String url;

    // 파일 타입
    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private Integer weekNumber;
    private Integer viewCount;
    private UploaderInfo uploader;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static MaterialDetailResponse from(Material material, UploaderInfo uploader) {
        return MaterialDetailResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType())
                .url(material.getUrl())
                .fileUrl(material.getFilePath())
                .fileName(material.getFileName())
                .fileSize(material.getFileSize())
                .weekNumber(material.getWeekNumber())
                .viewCount(material.getViewCount())
                .uploader(uploader)
                .createdAt(material.getCreatedAt())
                .build();
    }
}
