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
 * 스터디 신청 목록 응답 DTO (간략 버전)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationListResponse {

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
     * 신청자 닉네임
     */
    private String userNickname;

    /**
     * 신청자 프로필 이미지
     */
    private String profileImage;

    /**
     * AI 매칭 점수
     */
    private BigDecimal matchingScore;

    /**
     * 신청 상태
     */
    private ApplicationStatus status;

    /**
     * 신청 일시
     */
    private LocalDateTime createdAt;

    /**
     * StudyApplication Entity로부터 변환
     */
    public static ApplicationListResponse from(StudyApplication application) {
        return ApplicationListResponse.builder()
                .applicationId(application.getId())
                .studyId(application.getStudyId())
                .userId(application.getUserId())
                .matchingScore(application.getMatchingScore())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }

    /**
     * 스터디 이름 설정
     */
    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    /**
     * 신청자 닉네임 설정
     */
    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    /**
     * 신청자 프로필 이미지 설정
     */
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}