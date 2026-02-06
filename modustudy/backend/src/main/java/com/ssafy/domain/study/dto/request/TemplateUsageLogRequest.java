package com.ssafy.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "템플릿 사용 로그 요청")
public class TemplateUsageLogRequest {

    @Schema(description = "사용한 템플릿 ID", example = "1", required = true)
    @NotNull(message = "템플릿 ID는 필수입니다")
    private Long templateId;

    @Schema(description = "생성된 스터디 ID (미생성 시 null)", example = "10")
    private Long studyId;

    @Schema(description = "템플릿 그대로 사용 여부", example = "false")
    private boolean usedAsIs;

    @Schema(description = "수정된 필드와 값", example = "{\"difficulty\": \"ADVANCED\", \"textbook\": \"백준 골드\"}")
    private Map<String, Object> modifications;
}
