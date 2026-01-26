package com.ssafy.domain.material.dto.request;

import com.ssafy.domain.material.entity.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 링크 자료 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이내여야 합니다")
    private String description;

    @NotNull(message = "자료 타입은 필수입니다")
    private MaterialType materialType;

    @NotBlank(message = "URL은 필수입니다")
    @Size(max = 500, message = "URL은 500자 이내여야 합니다")
    private String url;

    private Integer weekNumber;
}