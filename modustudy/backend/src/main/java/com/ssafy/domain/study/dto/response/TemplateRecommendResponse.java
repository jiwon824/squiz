package com.ssafy.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI 템플릿 추천 응답")
public class TemplateRecommendResponse {

    @Schema(description = "추천 템플릿 타입", example = "ALGORITHM")
    private String templateType;

    @Schema(description = "추천 주제", example = "코딩테스트 준비")
    private String topic;

    @Schema(description = "추천 형식", example = "문제풀이 + 코드 리뷰")
    private String format;

    @Schema(description = "추천 난이도", example = "INTERMEDIATE")
    private String difficulty;

    @Schema(description = "추천 목표", example = "백준 골드 달성")
    private String goal;

    @Schema(description = "추천 교재", example = "백준 단계별 문제")
    private String textbook;

    @Schema(description = "추천 일정")
    private Map<String, Object> scheduleSuggestion;

    @Schema(description = "추천 이유", example = "Python 기술스택 기반 알고리즘 스터디 추천")
    private String reason;

    @Schema(description = "사용된 토큰 수")
    private int tokensUsed;

    // ========== 새로 추가: 상세 정보 + 커리큘럼 ==========

    @Schema(description = "스터디명", example = "React 심화 마스터")
    private String name;

    @Schema(description = "한줄 소개", example = "React 심화 학습을 위한 스터디입니다.")
    private String intro;

    @Schema(description = "상세 설명")
    private String description;

    @Schema(description = "선행 지식", example = "JavaScript 기초, HTML/CSS")
    private String prerequisites;

    @Schema(description = "진행 방식 상세")
    private String processDetail;

    @Schema(description = "주차별 커리큘럼")
    private List<Map<String, Object>> curriculum;
}
