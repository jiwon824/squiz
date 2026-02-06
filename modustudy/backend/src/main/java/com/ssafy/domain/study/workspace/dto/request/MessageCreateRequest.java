package com.ssafy.domain.study.workspace.dto.request;

import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {

    @NotNull(message = "워크스페이스 ID는 필수입니다.")
    private Long workspaceId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 5000, message = "메시지는 5000자 이하여야 합니다.")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Size(max = 500, message = "파일 URL은 500자 이하여야 합니다.")
    private String fileUrl;

    /**
     * DTO -> Entity 변환
     */
    public Message toEntity(Long userId) {
        return Message.builder()
                .workspaceId(this.workspaceId)
                .userId(userId)
                .content(this.content)
                .messageType(this.messageType != null ? this.messageType : MessageType.TEXT)
                .fileUrl(this.fileUrl)
                .build();
    }

    /**
     * 텍스트 메시지 생성용 정적 팩토리
     */
    public static MessageCreateRequest text(Long workspaceId, String content) {
        return MessageCreateRequest.builder()
                .workspaceId(workspaceId)
                .content(content)
                .messageType(MessageType.TEXT)
                .build();
    }

    /**
     * 이미지 메시지 생성용 정적 팩토리
     */
    public static MessageCreateRequest image(Long workspaceId, String content, String fileUrl) {
        return MessageCreateRequest.builder()
                .workspaceId(workspaceId)
                .content(content)
                .messageType(MessageType.IMAGE)
                .fileUrl(fileUrl)
                .build();
    }

    /**
     * 파일 메시지 생성용 정적 팩토리
     */
    public static MessageCreateRequest file(Long workspaceId, String content, String fileUrl) {
        return MessageCreateRequest.builder()
                .workspaceId(workspaceId)
                .content(content)
                .messageType(MessageType.FILE)
                .fileUrl(fileUrl)
                .build();
    }
}
