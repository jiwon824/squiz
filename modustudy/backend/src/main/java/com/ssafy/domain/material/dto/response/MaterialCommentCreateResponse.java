package com.ssafy.domain.material.dto.response;

import com.ssafy.domain.material.entity.MaterialComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 자료 댓글 생성 응답 DTO
 */
 @Getter
 @Builder
 public class MaterialCommentCreateResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static MaterialCommentCreateResponse from(MaterialComment comment) {
        return MaterialCommentCreateResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
