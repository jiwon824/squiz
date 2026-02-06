package com.ssafy.domain.study.workspace.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private MessageType messageType;
    private String fileUrl;
    @JsonProperty("isDeleted")
    private Boolean isDeleted;
    @JsonProperty("isPinned")
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(message.getUserId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .isDeleted(message.getIsDeleted())
                .isPinned(message.getIsPinned())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (사용자 정보 포함)
     */
    public static MessageResponse of(Message message, String nickname, String profileImageUrl) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(message.getUserId())
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .isDeleted(message.getIsDeleted())
                .isPinned(message.getIsPinned())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * 삭제된 메시지용 응답 (내용 숨김)
     */
    public static MessageResponse deletedMessage(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(message.getUserId())
                .content("삭제된 메시지입니다.")
                .messageType(message.getMessageType())
                .fileUrl(null)
                .isDeleted(true)
                .isPinned(false)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
