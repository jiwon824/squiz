package com.ssafy.domain.dm.dto.response;

import com.ssafy.domain.dm.entity.DirectMessage;

import java.time.LocalDateTime;

/**
 * DM 메시지 응답 DTO
 */
 public record DirectMessageResponse(
        Long messageId,
        Long conversationId,
        Long senderId,
        String senderNickname,
        String senderProfileImage,
        String content,
        Boolean isDeleted,
        Boolean isMine,
        LocalDateTime createdAt
        ) {
    public static DirectMessageResponse from(DirectMessage message, Long myId) {
        return new DirectMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getSenderNickname(),
                message.getSenderProfileImage(),
                message.getContent(),
                message.getIsDeleted(),
                message.getSenderId().equals(myId),
                message.getCreatedAt()
        );
    }
}
