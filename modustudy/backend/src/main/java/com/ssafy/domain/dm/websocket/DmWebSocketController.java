package com.ssafy.domain.dm.websocket;

import com.ssafy.domain.dm.dto.request.DirectMessageRequest;
import com.ssafy.domain.dm.dto.response.DirectMessageResponse;
import com.ssafy.domain.dm.service.DirectMessageService;
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
 * DM WebSocket 컨트롤러
 *
 * 엔드포인트:
 * - /app/dm/connect        : DM 연결 (userId 등록)
 * - /app/dm/send           : 메시지 전송
 * - /app/dm/typing/{id}    : 입력 중 표시
 * - /app/dm/read/{id}      : 읽음 처리
 *
 * 구독:
 * - /user/queue/dm         : 개인 DM 메시지 수신
 * - /user/queue/dm/events  : DM 이벤트 (typing, read 등)
 */
@Controller
public class DmWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(DmWebSocketController.class);

    private final DirectMessageService directMessageService;
    private final DmSessionService dmSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    public DmWebSocketController(DirectMessageService directMessageService,
                                  DmSessionService dmSessionService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.directMessageService = directMessageService;
        this.dmSessionService = dmSessionService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * DM 연결 - 사용자 세션 등록
     */
    @MessageMapping("/dm/connect")
    public void connect(@Header("userId") Long userId,
                        SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        dmSessionService.registerSession(userId, sessionId);
        log.info("DM connected: userId={}, sessionId={}", userId, sessionId);

        // 온라인 상태 알림 (추후 친구 목록에 활용 가능)
        DmWebSocketEvent onlineEvent = DmWebSocketEvent.online(userId);
        // 필요시 친구들에게 온라인 상태 브로드캐스트
    }

    /**
     * DM 메시지 전송
     */
    @MessageMapping("/dm/send")
    public void sendMessage(@Header("userId") Long senderId,
                            @Valid @Payload DmWebSocketMessage message) {
        try {
            // 기존 서비스를 통해 메시지 저장 + 검증
            DirectMessageRequest request = new DirectMessageRequest(
                    message.getReceiverId(),
                    message.getContent()
            );
            DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

            // 발신자에게 전송 (본인 화면에 표시)
            DmWebSocketEvent senderEvent = DmWebSocketEvent.newMessage(response);
            sendToUser(senderId, "/queue/dm", senderEvent);

            // 수신자에게 전송
            DirectMessageResponse receiverResponse = new DirectMessageResponse(
                    response.messageId(),
                    response.conversationId(),
                    response.senderId(),
                    response.senderNickname(),
                    response.senderProfileImage(),
                    response.content(),
                    response.isDeleted(),
                    false,  // 수신자 입장에서는 isMine = false
                    response.createdAt()
            );
            DmWebSocketEvent receiverEvent = DmWebSocketEvent.newMessage(receiverResponse);
            sendToUser(message.getReceiverId(), "/queue/dm", receiverEvent);

            log.debug("DM sent: from={} to={} messageId={}", senderId, message.getReceiverId(), response.messageId());

        } catch (Exception e) {
            log.error("Failed to send DM: from={} to={}", senderId, message.getReceiverId(), e);
            // 에러 이벤트를 발신자에게 전송
            sendErrorToUser(senderId, e.getMessage());
        }
    }

    /**
     * 입력 중 표시
     */
    @MessageMapping("/dm/typing/{conversationId}")
    public void typing(@Header("userId") Long userId,
                       @Header("nickname") String nickname,
                       @DestinationVariable Long conversationId) {
        try {
            // 대화 상대방 조회
            Long otherUserId = directMessageService.getOtherUserId(userId, conversationId);
            if (otherUserId != null) {
                DmWebSocketEvent typingEvent = DmWebSocketEvent.typing(conversationId, userId, nickname);
                sendToUser(otherUserId, "/queue/dm/events", typingEvent);
            }
        } catch (Exception e) {
            log.warn("Failed to send typing indicator: userId={}, conversationId={}", userId, conversationId);
        }
    }

    /**
     * 읽음 처리 알림
     */
    @MessageMapping("/dm/read/{conversationId}")
    public void markAsRead(@Header("userId") Long userId,
                           @DestinationVariable Long conversationId) {
        try {
            // DB 읽음 처리
            directMessageService.markAsRead(userId, conversationId);

            // 대화 상대방에게 읽음 알림
            Long otherUserId = directMessageService.getOtherUserId(userId, conversationId);
            if (otherUserId != null) {
                DmWebSocketEvent readEvent = DmWebSocketEvent.read(conversationId, userId);
                sendToUser(otherUserId, "/queue/dm/events", readEvent);
            }

            log.debug("DM read: userId={}, conversationId={}", userId, conversationId);
        } catch (Exception e) {
            log.warn("Failed to mark as read: userId={}, conversationId={}", userId, conversationId);
        }
    }

    /**
     * 특정 사용자에게 메시지 전송
     */
    private void sendToUser(Long userId, String destination, Object payload) {
        for (String sessionId : dmSessionService.getSessionIds(userId)) {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    destination,
                    payload,
                    createHeaders(sessionId)
            );
        }
    }

    /**
     * 에러 메시지 전송
     */
    private void sendErrorToUser(Long userId, String errorMessage) {
        DmWebSocketEvent errorEvent = new DmWebSocketEvent();
        errorEvent.setType(null);  // ERROR type 추가 가능
        sendToUser(userId, "/queue/dm/errors", errorMessage);
    }

    private org.springframework.messaging.MessageHeaders createHeaders(String sessionId) {
        org.springframework.messaging.simp.SimpMessageHeaderAccessor accessor =
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(
                        org.springframework.messaging.simp.SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}
