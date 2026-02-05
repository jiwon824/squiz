package com.ssafy.domain.dm.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.dm.dto.request.DirectMessageRequest;
import com.ssafy.domain.dm.dto.response.DirectMessageResponse;
import com.ssafy.domain.dm.dto.response.DmConversationResponse;
import com.ssafy.domain.dm.entity.DirectMessage;
import com.ssafy.domain.dm.entity.DmConversation;
import com.ssafy.domain.dm.mapper.DirectMessageMapper;
import com.ssafy.domain.dm.mapper.DmConversationMapper;
import com.ssafy.domain.dm.websocket.DmRedisPublisher;
import com.ssafy.domain.dm.websocket.DmWebSocketEvent;
import com.ssafy.domain.friend.service.FriendService;
import com.ssafy.domain.gamification.event.FirstFriendChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

    private final DirectMessageMapper directMessageMapper;
    private final DmConversationMapper dmConversationMapper;
    private final FriendService friendService;
    private final ApplicationEventPublisher eventPublisher;
    private final DmRedisPublisher dmRedisPublisher;

    /**
     * DM 전송
     */
    @Transactional
    public DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request) {
        Long receiverId = request.receiverId();

        // 자기 자신에게 전송 불가
        if (senderId.equals(receiverId)) {
            throw new BusinessException("INVALID_REQUEST", "자기 자신에게 DM을 보낼 수 없습니다.");
        }

        // 차단 여부 확인
        if (friendService.isBlocked(senderId, receiverId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "차단된 사용자와는 DM을 주고받을 수 없습니다.");
        }

        // 친구 여부 확인 (친구만 DM 가능)
        if (!friendService.isFriend(senderId, receiverId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "친구만 DM을 보낼 수 있습니다.");
        }

        // 대화방 조회 또는 생성 (첫 대화 여부 확인을 위해 결과 객체 사용)
        ConversationResult conversationResult = getOrCreateConversationWithFlag(senderId, receiverId);
        DmConversation conversation = conversationResult.conversation();
        boolean isFirstChat = conversationResult.isNewlyCreated();

        // 상대방이 대화를 삭제했다면 복원
        if (conversation.isDeleted(receiverId)) {
            restoreConversation(conversation, receiverId);
        }

        // 메시지 생성
        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .content(request.content())
                .build();
        directMessageMapper.insert(message);

        // 대화방 마지막 메시지 시간 업데이트
        dmConversationMapper.updateLastMessageAt(conversation.getId(), LocalDateTime.now());

        // 첫 친구 채팅이면 게이미피케이션 이벤트 발행
        if (isFirstChat) {
            log.info("친구와 첫 채팅 이벤트 발행 - senderId: {}, receiverId: {}", senderId, receiverId);
            eventPublisher.publishEvent(new FirstFriendChatEvent(
                    senderId,
                    receiverId,
                    conversation.getId(),
                    LocalDate.now()
            ));
        }

        // 저장된 메시지 조회
        DirectMessage saved = directMessageMapper.findById(message.getId());
        DirectMessageResponse senderResponse = DirectMessageResponse.from(saved, senderId);

        // WebSocket으로 수신자에게 실시간 알림 (REST API 호출 시에도 실시간 전달)
        try {
            DirectMessageResponse receiverResponse = new DirectMessageResponse(
                    senderResponse.messageId(),
                    senderResponse.conversationId(),
                    senderResponse.senderId(),
                    senderResponse.senderNickname(),
                    senderResponse.senderProfileImage(),
                    senderResponse.content(),
                    senderResponse.isDeleted(),
                    false,  // 수신자 입장에서는 isMine = false
                    senderResponse.createdAt()
            );
            DmWebSocketEvent receiverEvent = DmWebSocketEvent.newMessage(receiverResponse);
            dmRedisPublisher.publishToUser(receiverId, "/queue/dm", receiverEvent);
            log.debug("REST API DM 전송 - WebSocket 알림: senderId={}, receiverId={}", senderId, receiverId);
        } catch (Exception e) {
            log.warn("WebSocket 알림 전송 실패 (메시지는 저장됨): {}", e.getMessage());
        }

        return senderResponse;
    }

    /**
     * 대화방 목록 조회
     */
    public List<DmConversationResponse> getConversations(Long userId) {
        List<DmConversation> conversations = dmConversationMapper.findByUserId(userId);

        return conversations.stream()
                .map(conv -> {
                    Long lastReadId = conv.getLastReadMessageId(userId);
                    int unreadCount = directMessageMapper.countUnreadMessages(conv.getId(), lastReadId);
                    return DmConversationResponse.from(conv, userId, unreadCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 대화 메시지 목록 조회
     */
    public List<DirectMessageResponse> getMessages(Long userId, Long conversationId, int page, int size) {
        // 대화방 참여자 확인
        if (!dmConversationMapper.existsByIdAndUserId(conversationId, userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "대화방에 접근할 수 없습니다.");
        }

        int offset = page * size;
        List<DirectMessage> messages = directMessageMapper.findByConversationId(conversationId, size, offset);

        return messages.stream()
                .map(msg -> DirectMessageResponse.from(msg, userId))
                .collect(Collectors.toList());
    }

    /**
     * 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long conversationId) {
        DmConversation conversation = dmConversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "대화방을 찾을 수 없습니다.");
        }

        // 참여자 확인
        boolean isUser1 = conversation.getUser1Id().equals(userId);
        boolean isUser2 = conversation.getUser2Id().equals(userId);
        if (!isUser1 && !isUser2) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "대화방에 접근할 수 없습니다.");
        }

        // 최신 메시지 ID 조회
        DirectMessage latestMessage = directMessageMapper.findLatestByConversationId(conversationId);
        if (latestMessage == null) {
            return;
        }

        // 마지막 읽은 메시지 ID 업데이트
        if (isUser1) {
            dmConversationMapper.updateUser1LastReadMessageId(conversationId, latestMessage.getId());
        } else {
            dmConversationMapper.updateUser2LastReadMessageId(conversationId, latestMessage.getId());
        }
    }

    /**
     * 대화방 삭제 (soft delete)
     */
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        DmConversation conversation = dmConversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "대화방을 찾을 수 없습니다.");
        }

        // 참여자 확인 및 삭제 처리
        if (conversation.getUser1Id().equals(userId)) {
            dmConversationMapper.markUser1Deleted(conversationId);
        } else if (conversation.getUser2Id().equals(userId)) {
            dmConversationMapper.markUser2Deleted(conversationId);
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "대화방에 접근할 수 없습니다.");
        }
    }

    /**
     * 안 읽은 메시지 총 개수 조회
     */
    public int getTotalUnreadCount(Long userId) {
        List<DmConversation> conversations = dmConversationMapper.findByUserId(userId);

        return conversations.stream()
                .mapToInt(conv -> {
                    Long lastReadId = conv.getLastReadMessageId(userId);
                    return directMessageMapper.countUnreadMessages(conv.getId(), lastReadId);
                })
                .sum();
    }

    /**
     * 대화 상대방 ID 조회 (WebSocket용)
     */
    public Long getOtherUserId(Long userId, Long conversationId) {
        DmConversation conversation = dmConversationMapper.findById(conversationId);
        if (conversation == null) {
            return null;
        }
        if (conversation.getUser1Id().equals(userId)) {
            return conversation.getUser2Id();
        } else if (conversation.getUser2Id().equals(userId)) {
            return conversation.getUser1Id();
        }
        return null;
    }

    /**
     * 대화방 조회 또는 생성 (첫 대화 여부 플래그 포함)
     */
    private ConversationResult getOrCreateConversationWithFlag(Long userId1, Long userId2) {
        // user1_id는 항상 작은 ID
        Long user1Id = Math.min(userId1, userId2);
        Long user2Id = Math.max(userId1, userId2);

        DmConversation existing = dmConversationMapper.findByUsers(user1Id, user2Id);
        if (existing != null) {
            return new ConversationResult(existing, false);
        }

        // 새 대화방 생성
        DmConversation newConversation = DmConversation.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
        dmConversationMapper.insert(newConversation);

        DmConversation created = dmConversationMapper.findById(newConversation.getId());
        return new ConversationResult(created, true);
    }

    /**
     * 대화방 조회/생성 결과 (첫 대화 여부 포함)
     */
    private record ConversationResult(DmConversation conversation, boolean isNewlyCreated) {}

    /**
     * 대화방 복원
     */
    private void restoreConversation(DmConversation conversation, Long userId) {
        if (conversation.isUser1(userId)) {
            dmConversationMapper.restoreUser1(conversation.getId());
        } else {
            dmConversationMapper.restoreUser2(conversation.getId());
        }
    }
}
