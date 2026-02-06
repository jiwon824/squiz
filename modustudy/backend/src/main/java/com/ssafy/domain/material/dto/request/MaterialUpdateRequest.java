package com.ssafy.domain.material.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 자료 수정 요청 DTO
 */
 @Getter
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public class MaterialUpdateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이내여야 합니다")
    private String description;

    private Integer weekNumber;
}
