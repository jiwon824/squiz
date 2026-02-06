package com.ssafy.domain.study.dto.request;

import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.PenaltyPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 템플릿 생성 요청 DTO
 */
 @Data
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 @Schema(description = "템플릿 생성 요청")
 public class CreateTemplateRequest {

    @Schema(description = "템플릿 이름", example = "알고리즘 스터디 템플릿", required = true)
    @NotBlank(message = "템플릿 이름은 필수입니다")
    @Size(max = 100, message = "템플릿 이름은 100자 이내여야 합니다")
    private String name;

    @Schema(description = "한줄 소개", example = "백준 골드를 향해 달려가는 스터디!")
    @Size(max = 200, message = "한줄 소개는 200자 이내여야 합니다")
    private String intro;

    @Schema(description = "템플릿 타입", example = "ALGORITHM")
    private String templateType;

    @Schema(description = "주제", example = "백준 골드 달성")
    @Size(max = 50, message = "주제는 50자 이내여야 합니다")
    private String topic;

    @Schema(description = "형식", example = "문제 풀이 + 코드 리뷰")
    @Size(max = 50, message = "형식은 50자 이내여야 합니다")
    private String format;

    @Schema(description = "모임 방식", example = "ONLINE")
    private MeetingType meetingType;

    @Schema(description = "스터디 설명", example = "백준 골드 티어를 목표로 하는 알고리즘 스터디")
    private String description;

    @Schema(description = "교재/자료", example = "백준 온라인 저지")
    @Size(max = 500, message = "교재는 500자 이내여야 합니다")
    private String textbook;

    @Schema(description = "스터디 목표", example = "3개월 내 골드 티어 달성")
    @Size(max = 500, message = "목표는 500자 이내여야 합니다")
    private String goal;

    @Schema(description = "난이도", example = "INTERMEDIATE")
    private Difficulty difficulty;

    @Schema(description = "참가 조건", example = "실버 티어 이상")
    private String prerequisites;

    @Schema(description = "진행 방식 상세", example = "매주 3문제 풀이 후 코드 리뷰")
    private String processDetail;

    @Schema(description = "패널티 정책", example = "NORMAL")
    private PenaltyPolicy penaltyPolicy;
}
