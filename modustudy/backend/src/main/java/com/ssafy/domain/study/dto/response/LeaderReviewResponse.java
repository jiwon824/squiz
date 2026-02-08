package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.LeaderReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 스터디장 리뷰 응답 DTO
 */
 @Getter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class LeaderReviewResponse {

    /**
     * 리뷰 ID
     */
    private Long reviewId;

    /**
     * 평가자 ID
     */
    private Long reviewerId;

    /**
     * 평가자 이름
     */
    private String reviewerName;

    /**
     * 평가자 닉네임
     */
    private String reviewerNickname;

    /**
     * 스터디 ID
     */
    private Long studyId;

    /**
     * 스터디 이름
     */
    private String studyName;

    /**
     * 평점
     */
    private BigDecimal rating;

    /**
     * 리뷰 내용
     */
    private String comment;

    /**
     * 작성일
     */
    private LocalDateTime createdAt;

    /**
     * LeaderReview Entity로부터 변환
     * (추가 정보는 Service에서 채워줌)
     */
    public static LeaderReviewResponse from(LeaderReview review) {
        return LeaderReviewResponse.builder()
                .reviewId(review.getId())
                .reviewerId(review.getReviewerId())
                .studyId(review.getStudyId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * 평가자 정보 설정
     */
    public void setReviewerInfo(String name, String nickname) {
        this.reviewerName = name;
        this.reviewerNickname = nickname;
    }

    /**
     * 스터디 이름 설정
     */
    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }
}
