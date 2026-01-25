package com.ssafy.domain.study.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
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

    public void updateContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public boolean isAuthor(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public boolean isSystemMessage() {
        return this.messageType == MessageType.SYSTEM;
    }

    public boolean hasFile() {
        return this.messageType == MessageType.IMAGE ||
                this.messageType == MessageType.FILE;
    }

    // ============================================================
    // 정적 팩토리 메서드
    // ============================================================

    public static Message createSystemMessage(Long workspaceId, String content) {
        return Message.builder()
                .workspaceId(workspaceId)
                .userId(0L)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();
    }
}