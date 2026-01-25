package com.ssafy.domain.study.workspace.dto.response;

import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private String content;
    private MessageType messageType;
    private String fileUrl;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 작성자 정보
    private String userNickname;
    private String userProfileImage;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(message.getUserId())
                .content(message.getIsDeleted() ? "삭제된 메시지입니다." : message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getIsDeleted() ? null : message.getFileUrl())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    public static MessageResponse from(Message message, User user) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(message.getUserId())
                .content(message.getIsDeleted() ? "삭제된 메시지입니다." : message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getIsDeleted() ? null : message.getFileUrl())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .userNickname(user != null ? user.getNickname() : null)
                .userProfileImage(user != null ? user.getProfileImage() : null)
                .build();
    }

    public static MessageResponse fromSystemMessage(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .userId(0L)
                .content(message.getContent())
                .messageType(MessageType.SYSTEM)
                .isDeleted(false)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .userNickname("시스템")
                .build();
    }
}