package com.ssafy.domain.material.dto.response;

import com.ssafy.domain.material.entity.MaterialComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 자료 댓글 응답 DTO
 */
 @Getter
 @Builder
 public class MaterialCommentResponse {

    private Long id;
    private UploaderInfo user;
    private String content;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static MaterialCommentResponse from(MaterialComment comment, UploaderInfo user) {
        return MaterialCommentResponse.builder()
                .id(comment.getId())
                .user(user)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
