package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 스터디장 리뷰 작성 요청 DTO
 */
 @Getter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class LeaderReviewCreateRequest {

    /**
     * 평점 (0.5 ~ 5.0, 0.5 단위)
     */
    @NotNull(message = "평점은 필수입니다")
    @DecimalMin(value = "0.5", message = "평점은 최소 0.5점입니다")
    @DecimalMax(value = "5.0", message = "평점은 최대 5.0점입니다")
    private BigDecimal rating;

    /**
     * 리뷰 내용
     */
    @Size(max = 1000, message = "리뷰는 1000자 이내로 작성해주세요")
    private String comment;
}
