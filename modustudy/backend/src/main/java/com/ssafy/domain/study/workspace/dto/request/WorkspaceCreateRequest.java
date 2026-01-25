package com.ssafy.domain.study.workspace.dto.request;

import com.ssafy.domain.study.workspace.entity.WorkspaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkspaceCreateRequest {

    @NotBlank(message = "워크스페이스 이름은 필수입니다.")
    @Size(max = 100, message = "워크스페이스 이름은 100자 이하여야 합니다.")
    private String name;

    @NotNull(message = "워크스페이스 타입은 필수입니다.")
    private WorkspaceType type;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
    private String description;
}