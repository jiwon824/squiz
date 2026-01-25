package com.ssafy.domain.study.workspace.dto.response;

import com.ssafy.domain.study.workspace.entity.Workspace;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WorkspaceResponse {

    private Long id;
    private Long studyId;
    private LocalDateTime createdAt;

    public static WorkspaceResponse from(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .studyId(workspace.getStudyId())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}