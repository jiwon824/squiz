package com.ssafy.domain.material.dto.response;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 자료 목록 응답 DTO
 */
@Getter
@Builder
public class MaterialListResponse {

    private Long id;
    private String title;
    private String description;
    private MaterialType materialType;

    // 링크 타입
    private String url;

    // 파일 타입
    private String fileUrl;
    private Long fileSize;

    private Integer weekNumber;
    private Integer viewCount;
    private Long commentCount;
    private UploaderInfo uploader;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static MaterialListResponse from(Material material, UploaderInfo uploader, Long commentCount) {
        return MaterialListResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType())
                .url(material.getUrl())
                .fileUrl(material.getFilePath())
                .fileSize(material.getFileSize())
                .weekNumber(material.getWeekNumber())
                .viewCount(material.getViewCount())
                .commentCount(commentCount)
                .uploader(uploader)
                .createdAt(material.getCreatedAt())
                .build();
    }
}