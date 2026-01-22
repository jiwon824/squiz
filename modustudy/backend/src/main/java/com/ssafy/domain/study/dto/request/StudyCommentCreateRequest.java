package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyCommentCreateRequest {

    /**
     * 부모 댓글 ID (대댓글인 경우)
     */
    private Long parentId;

    /**
     * 댓글 내용
     */
    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하로 작성해주세요")
    private String content;

    /**
     * 첨부 이미지 URL
     */
    @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다")
    private String imageUrl;
}