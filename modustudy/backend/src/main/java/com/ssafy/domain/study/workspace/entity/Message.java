package com.ssafy.domain.study.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_message_user_id", columnList = "user_id"),
        @Index(name = "idx_message_created_at", columnList = "created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================================
    // 비즈니스 로직
    // ============================================================

    /**
     * 메시지 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }

    /**
     * 메시지 삭제 (Soft Delete)
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * 작성자 확인
     */
    public boolean isAuthor(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * 시스템 메시지 여부 확인
     */
    public boolean isSystemMessage() {
        return this.messageType == MessageType.SYSTEM;
    }

    /**
     * 파일 첨부 여부 확인
     */
    public boolean hasFile() {
        return this.messageType == MessageType.IMAGE ||
                this.messageType == MessageType.FILE;
    }

    // ============================================================
    // 정적 팩토리 메서드
    // ============================================================

    /**
     * 일반 텍스트 메시지 생성
     */
    public static Message createTextMessage(Long workspaceId, Long userId, String content) {
        return Message.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .content(content)
                .messageType(MessageType.TEXT)
                .build();
    }

    /**
     * 이미지 메시지 생성
     */
    public static Message createImageMessage(Long workspaceId, Long userId, String content, String fileUrl) {
        return Message.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .content(content)
                .messageType(MessageType.IMAGE)
                .fileUrl(fileUrl)
                .build();
    }

    /**
     * 파일 메시지 생성
     */
    public static Message createFileMessage(Long workspaceId, Long userId, String content, String fileUrl) {
        return Message.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .content(content)
                .messageType(MessageType.FILE)
                .fileUrl(fileUrl)
                .build();
    }

    /**
     * 시스템 메시지 생성 (입장/퇴장 알림 등)
     */
    public static Message createSystemMessage(Long workspaceId, String content) {
        return Message.builder()
                .workspaceId(workspaceId)
                .userId(0L)  // 시스템 메시지는 userId = 0
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();
    }
}