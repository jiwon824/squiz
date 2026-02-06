package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.ApplicationStatus;
import com.ssafy.domain.study.entity.StudyApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 스터디 신청 상세 응답 DTO
 */
 @Getter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class ApplicationResponse {

    /**
     * 신청 ID
     */
    private Long applicationId;

    /**
     * 스터디 ID
     */
    private Long studyId;

    /**
     * 스터디 이름
     */
    private String studyName;

    /**
     * 신청자 ID
     */
    private Long userId;

    /**
     * 신청자 이름
     */
    private String userName;

    /**
     * 신청자 닉네임
     */
    private String userNickname;

    /**
     * 신청자 이메일
     */
    private String userEmail;

    /**
     * 신청자 프로필 이미지
     */
    private String profileImage;

    /**
     * 신청 메시지
     */
    private String message;

    /**
     * AI 매칭 점수
     */
    private BigDecimal matchingScore;

    /**
     * 신청 상태
     */
    private ApplicationStatus status;

    /**
     * 거절 사유
     */
    private String rejectedReason;

    /**
     * 신청 일시
     */
    private LocalDateTime createdAt;

    /**
     * 처리 일시
     */
    private LocalDateTime processedAt;

    /**
     * StudyApplication Entity로부터 변환
     * (추가 정보는 Service에서 채워줌)
     */
    public static ApplicationResponse from(StudyApplication application) {
        return ApplicationResponse.builder()
                .applicationId(application.getId())
                .studyId(application.getStudyId())
                .userId(application.getUserId())
                .message(application.getMessage())
                .matchingScore(application.getMatchingScore())
                .status(application.getStatus())
                .rejectedReason(application.getRejectedReason())
                .createdAt(application.getCreatedAt())
                .processedAt(application.getProcessedAt())
                .build();
    }

    /**
     * 스터디 정보 설정
     */
    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    /**
     * 신청자 정보 설정
     */
    public void setUserInfo(String name, String nickname, String email, String profileImage) {
        this.userName = name;
        this.userNickname = nickname;
        this.userEmail = email;
        this.profileImage = profileImage;
    }
}
