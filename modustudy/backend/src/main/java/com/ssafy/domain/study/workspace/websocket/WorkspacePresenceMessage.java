package com.ssafy.domain.study.workspace.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 워크스페이스 상태 전송용 DTO
 */
 public class WorkspacePresenceMessage {

    @NotNull
    private Long workspaceId;

    @NotBlank
    private String status; // ACTIVE | IDLE

    public WorkspacePresenceMessage() {}

    public WorkspacePresenceMessage(Long workspaceId, String status) {
        this.workspaceId = workspaceId;
        this.status = status;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
