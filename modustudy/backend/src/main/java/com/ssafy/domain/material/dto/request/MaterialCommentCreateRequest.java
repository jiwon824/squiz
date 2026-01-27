package com.ssafy.domain.material.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 자료 댓글 작성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 1000, message = "댓글은 1000자 이내여야 합니다")
    private String content;
}