package com.ssafy.domain.dm.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DM 메시지 엔티티
 */
 @Getter
 @Setter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class DirectMessage {

    private Long id;

    /**
     * 소속 대화 ID
     */
    private Long conversationId;

    /**
     * 발신자 ID
     */
    private Long senderId;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 삭제 여부
     */
    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN된 발신자 정보
    private String senderNickname;
    private String senderProfileImage;
}
