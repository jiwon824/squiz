package com.ssafy.domain.study.workspace.dto.response;

import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.entity.WorkspaceType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkspaceResponse {

    private Long id;
    private Long studyId;
    private String name;
    private WorkspaceType type;
    private String description;
    private LocalDateTime createdAt;

    public static WorkspaceResponse from(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .studyId(workspace.getStudyId())
                .name(workspace.getName())
                .type(workspace.getType())
                .description(workspace.getDescription())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}