package com.ssafy.domain.study.workspace.dto.request;

import com.ssafy.domain.study.workspace.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessageCreateRequest {

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 5000, message = "메시지는 5000자 이하여야 합니다.")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Size(max = 500, message = "파일 URL은 500자 이하여야 합니다.")
    private String fileUrl;
}