package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.TemplateUsageLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "템플릿 사용 로그 응답")
public class TemplateUsageLogResponse {

    private Long id;
    private Long userId;
    private Long templateId;
    private Long studyId;
    private boolean usedAsIs;
    private Map<String, Object> modifications;
    private LocalDateTime createdAt;

    public static TemplateUsageLogResponse from(TemplateUsageLog log) {
        return TemplateUsageLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .templateId(log.getTemplateId())
                .studyId(log.getStudyId())
                .usedAsIs(log.isUsedAsIs())
                .modifications(log.getModifications())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
