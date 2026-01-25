package com.ssafy.domain.study.workspace.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkspaceUpdateRequest {

    @Size(max = 100, message = "워크스페이스 이름은 100자 이하여야 합니다.")
    private String name;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
    private String description;
}