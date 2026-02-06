package com.ssafy.domain.study.workspace.websocket;

import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.response.MessageResponse;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.service.MessageService;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 워크스페이스 WebSocket 컨트롤러
 *
 * 엔드포인트:
 * - /app/workspace/join/{workspaceId}     : 워크스페이스 입장
 * - /app/workspace/leave/{workspaceId}    : 워크스페이스 퇴장
 * - /app/workspace/send/{workspaceId}     : 메시지 전송
 * - /app/workspace/typing/{workspaceId}   : 입력 중 표시
 * - /app/workspace/presence/{workspaceId} : 상태 변경
 *
 * 구독:
 * - /topic/workspace/{workspaceId}        : 워크스페이스 메시지 수신 (브로드캐스트)
 */
 @Controller
 public class WorkspaceWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceWebSocketController.class);

    private final MessageService messageService;
    private final WorkspaceSessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public WorkspaceWebSocketController(MessageService messageService,
                                         WorkspaceSessionService sessionService,
                                         SimpMessagingTemplate messagingTemplate,
                                         UserRepository userRepository) {
        this.messageService = messageService;
        this.sessionService = sessionService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 워크스페이스 입장
     */
    @MessageMapping("/workspace/join/{workspaceId}")
    public void joinWorkspace(@DestinationVariable Long workspaceId,
                               @Header("userId") Long userId,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionService.joinWorkspace(workspaceId, userId, sessionId);

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";
        String profileImageUrl = user != null ? user.getProfileImage() : null;

        // 입장 이벤트 브로드캐스트
        WorkspaceWebSocketEvent joinEvent = WorkspaceWebSocketEvent.join(
                workspaceId, userId, nickname, profileImageUrl);
        broadcastToWorkspace(workspaceId, joinEvent);

}

    /**
     * 워크스페이스 퇴장
     */
    @MessageMapping("/workspace/leave/{workspaceId}")
    public void leaveWorkspace(@DestinationVariable Long workspaceId,
                                @Header("userId") Long userId,
                                @Header(value = "nickname", required = false) String nickname,
                                SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionService.leaveWorkspace(sessionId);

        // 퇴장 이벤트 브로드캐스트
        WorkspaceWebSocketEvent leaveEvent = WorkspaceWebSocketEvent.leave(workspaceId, userId, nickname);
        broadcastToWorkspace(workspaceId, leaveEvent);

}

    /**
     * 메시지 전송
     */
    @MessageMapping("/workspace/send/{workspaceId}")
    public void sendMessage(@DestinationVariable Long workspaceId,
                            @Header("userId") Long userId,
                            @Valid @Payload WorkspaceWebSocketMessage message) {
        try {
            // 메시지 타입 변환
            MessageType messageType = MessageType.TEXT;
            try {
                if (message.getMessageType() != null) {
                    messageType = MessageType.valueOf(message.getMessageType());
                }
            } catch (IllegalArgumentException e) {
}

            // 메시지 저장
            MessageCreateRequest request = MessageCreateRequest.builder()
                    .workspaceId(workspaceId)
                    .content(message.getContent())
                    .messageType(messageType)
                    .build();

            MessageResponse response = messageService.createMessage(request, userId);

            // 새 메시지 이벤트 브로드캐스트
            WorkspaceWebSocketEvent messageEvent = WorkspaceWebSocketEvent.newMessage(response);
            broadcastToWorkspace(workspaceId, messageEvent);

} catch (Exception e) {
// 에러 이벤트를 발신자에게 전송
            sendErrorToUser(userId, workspaceId, e.getMessage());
        }
    }

    /**
     * 입력 중 표시
     */
    @MessageMapping("/workspace/typing/{workspaceId}")
    public void typing(@DestinationVariable Long workspaceId,
                       @Header("userId") Long userId,
                       @Header(value = "nickname", required = false) String nickname) {
        try {
            // 닉네임이 없으면 조회
            if (nickname == null || nickname.isEmpty()) {
                User user = userRepository.findById(userId).orElse(null);
                nickname = user != null ? user.getNickname() : "알 수 없음";
            }

            // 입력 중 이벤트 브로드캐스트
            WorkspaceWebSocketEvent typingEvent = WorkspaceWebSocketEvent.typing(workspaceId, userId, nickname);
            broadcastToWorkspace(workspaceId, typingEvent);

        } catch (Exception e) {
}
    }

    /**
     * 상태 변경 (ACTIVE / IDLE)
     */
    @MessageMapping("/workspace/presence/{workspaceId}")
    public void presence(@DestinationVariable Long workspaceId,
                         @Header("userId") Long userId,
                         @Header(value = "nickname", required = false) String nickname,
                         @Valid @Payload WorkspacePresenceMessage message) {
        try {
            if (nickname == null || nickname.isEmpty()) {
                User user = userRepository.findById(userId).orElse(null);
                nickname = user != null ? user.getNickname() : "알 수 없음";
            }
            User user = userRepository.findById(userId).orElse(null);
            String profileImageUrl = user != null ? user.getProfileImage() : null;

            WorkspaceWebSocketEvent presenceEvent = WorkspaceWebSocketEvent.presence(
                    workspaceId, userId, nickname, profileImageUrl, message.getStatus());
            broadcastToWorkspace(workspaceId, presenceEvent);
        } catch (Exception e) {
}
    }

    /**
     * 워크스페이스에 이벤트 브로드캐스트
     */
    private void broadcastToWorkspace(Long workspaceId, Object payload) {
        String destination = "/topic/workspace/" + workspaceId;
        messagingTemplate.convertAndSend(destination, payload);
}

    /**
     * 특정 사용자에게 에러 메시지 전송
     */
    private void sendErrorToUser(Long userId, Long workspaceId, String errorMessage) {
        String destination = "/user/" + userId + "/queue/workspace/errors";
        messagingTemplate.convertAndSend(destination, errorMessage);
    }
}

