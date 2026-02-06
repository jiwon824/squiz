package com.ssafy.domain.retrospect.dto.request;

import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrospectiveCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    private RetrospectiveType retrospectiveType;

    private Long sessionId;

    public Retrospective toEntity(Long studyId, Long createdBy) {
        return Retrospective.builder()
                .studyId(studyId)
                .createdBy(createdBy)
                .title(title)
                .retrospectiveType(retrospectiveType != null ? retrospectiveType : RetrospectiveType.KPT)
                .sessionId(sessionId)
                .build();
    }
}
