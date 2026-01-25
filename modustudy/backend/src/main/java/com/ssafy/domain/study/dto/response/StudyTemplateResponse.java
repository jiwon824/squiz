package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.PenaltyPolicy;
import com.ssafy.domain.study.entity.StudyTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "템플릿 응답")
public class StudyTemplateResponse {

    @Schema(description = "템플릿 ID", example = "1")
    private Long id;

    @Schema(description = "템플릿 소유자 ID (null이면 시스템 템플릿)", example = "10")
    private Long userId;

    @Schema(description = "템플릿 이름", example = "알고리즘 스터디 템플릿")
    private String name;

    @Schema(description = "시스템 템플릿 여부", example = "false")
    private boolean isSystem;

    @Schema(description = "템플릿 타입", example = "ALGORITHM")
    private String templateType;

    @Schema(description = "주제", example = "백준 골드 달성")
    private String topic;

    @Schema(description = "형식", example = "문제 풀이 + 코드 리뷰")
    private String format;

    @Schema(description = "모임 방식", example = "ONLINE")
    private MeetingType meetingType;

    @Schema(description = "스터디 설명", example = "백준 골드 티어를 목표로 하는 알고리즘 스터디")
    private String description;

    @Schema(description = "교재/자료", example = "백준 온라인 저지")
    private String textbook;

    @Schema(description = "스터디 목표", example = "3개월 내 골드 티어 달성")
    private String goal;

    @Schema(description = "난이도", example = "INTERMEDIATE")
    private Difficulty difficulty;

    @Schema(description = "참가 조건", example = "실버 티어 이상")
    private String prerequisites;

    @Schema(description = "진행 방식 상세", example = "매주 3문제 풀이 후 코드 리뷰")
    private String processDetail;

    @Schema(description = "패널티 정책", example = "NORMAL")
    private PenaltyPolicy penaltyPolicy;

    @Schema(description = "생성일시", example = "2025-01-21T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-21T12:00:00")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static StudyTemplateResponse from(StudyTemplate template) {
        return StudyTemplateResponse.builder()
                .id(template.getId())
                .userId(template.getUserId())
                .name(template.getName())
                .isSystem(template.isSystem())
                .templateType(template.getTemplateType())
                .topic(template.getTopic())
                .format(template.getFormat())
                .meetingType(template.getMeetingType())
                .description(template.getDescription())
                .textbook(template.getTextbook())
                .goal(template.getGoal())
                .difficulty(template.getDifficulty())
                .prerequisites(template.getPrerequisites())
                .processDetail(template.getProcessDetail())
                .penaltyPolicy(template.getPenaltyPolicy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
