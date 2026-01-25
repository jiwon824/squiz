package com.ssafy.domain.dm.dto.response;

import com.ssafy.domain.dm.entity.DmConversation;

import java.time.LocalDateTime;

/**
 * DM 대화방 응답 DTO
 */
public record DmConversationResponse(
        Long conversationId,
        Long partnerId,
        String partnerNickname,
        String partnerProfileImage,
        Boolean partnerIsOnline,
        String lastMessage,
        Boolean lastMessageIsMine,
        Integer unreadCount,
        LocalDateTime lastMessageAt
) {
    public static DmConversationResponse from(DmConversation conversation, Long myId, int unreadCount) {
        return new DmConversationResponse(
                conversation.getId(),
                conversation.getPartnerId(myId),
                conversation.getPartnerNickname(myId),
                conversation.getPartnerProfileImage(myId),
                conversation.getPartnerIsOnline(myId),
                conversation.getLastMessageContent(),
                conversation.getLastMessageSenderId() != null && conversation.getLastMessageSenderId().equals(myId),
                unreadCount,
                conversation.getLastMessageAt()
        );
    }
}
