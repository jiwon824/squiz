package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.request.MessageUpdateRequest;
import com.ssafy.domain.study.workspace.dto.response.MessagePageResponse;
import com.ssafy.domain.study.workspace.dto.response.MessageResponse;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.repository.MessageRepository;
import com.ssafy.domain.study.workspace.repository.WorkspaceRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 메시지 서비스
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 @Transactional(readOnly = true)
 public class MessageService {

    private final MessageRepository messageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    /**
     * 메시지 생성
     */
    @Transactional
    public MessageResponse createMessage(MessageCreateRequest request, Long userId) {
        if (!workspaceRepository.existsById(request.getWorkspaceId())) {
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        Message message = messageRepository.save(request.toEntity(userId));
        return MessageResponse.of(message, user.getNickname(), null);
    }

    /**
     * 메시지 조회
     */
    public MessageResponse getMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        if (message.isDeleted()) {
            return MessageResponse.deletedMessage(message);
        }

        User user = userRepository.findById(message.getUserId()).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";

        return MessageResponse.of(message, nickname, null);
    }

    /**
     * 워크스페이스 내 메시지 목록 조회 (페이징)
     */
    public MessagePageResponse getMessages(Long workspaceId, Pageable pageable) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
        }

        Page<Message> messagePage = messageRepository.findByWorkspaceIdAndNotDeleted(workspaceId, pageable);

        List<Long> userIds = messagePage.getContent().stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<MessageResponse> responsePage = messagePage.map(message -> {
            User user = userMap.get(message.getUserId());
            String nickname = user != null ? user.getNickname() : "알 수 없음";
            return MessageResponse.of(message, nickname, null);
        });

        return MessagePageResponse.from(responsePage);
    }

    /**
     * 최근 메시지 조회
     */
    public List<MessageResponse> getRecentMessages(Long workspaceId, int limit) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
        }

        List<Message> messages = messageRepository.findRecentMessages(
                workspaceId, PageRequest.of(0, limit));

        List<Long> userIds = messages.stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return messages.stream()
                .map(message -> {
                    User user = userMap.get(message.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return MessageResponse.of(message, nickname, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 시간 이후의 새 메시지 조회 (폴링용)
     */
    public List<MessageResponse> getMessagesAfter(Long workspaceId, LocalDateTime after) {
        List<Message> messages = messageRepository.findMessagesAfter(workspaceId, after);

        List<Long> userIds = messages.stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return messages.stream()
                .map(message -> {
                    User user = userMap.get(message.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return MessageResponse.of(message, nickname, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 메시지 검색
     */
    public MessagePageResponse searchMessages(Long workspaceId, String keyword, Pageable pageable) {
        Page<Message> messagePage = messageRepository.searchByContent(workspaceId, keyword, pageable);

        List<Long> userIds = messagePage.getContent().stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<MessageResponse> responsePage = messagePage.map(message -> {
            User user = userMap.get(message.getUserId());
            String nickname = user != null ? user.getNickname() : "알 수 없음";
            return MessageResponse.of(message, nickname, null);
        });

        return MessagePageResponse.from(responsePage);
    }

    /**
     * 메시지 타입별 조회
     */
    public MessagePageResponse getMessagesByType(Long workspaceId, MessageType messageType, Pageable pageable) {
        Page<Message> messagePage = messageRepository.findByWorkspaceIdAndMessageType(
                workspaceId, messageType, pageable);

        List<Long> userIds = messagePage.getContent().stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<MessageResponse> responsePage = messagePage.map(message -> {
            User user = userMap.get(message.getUserId());
            String nickname = user != null ? user.getNickname() : "알 수 없음";
            return MessageResponse.of(message, nickname, null);
        });

        return MessagePageResponse.from(responsePage);
    }

    /**
     * 메시지 수정
     */
    @Transactional
    public MessageResponse updateMessage(Long messageId, MessageUpdateRequest request, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        if (!message.isAuthor(userId)) {
            throw new IllegalStateException("본인이 작성한 메시지만 수정할 수 있습니다.");
        }

        if (message.isDeleted()) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다.");
        }

        message.updateContent(request.getContent());
        User user = userRepository.findById(userId).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";

        return MessageResponse.of(message, nickname, null);
    }

    /**
     * 메시지 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        // 작성자 확인
        if (!message.isAuthor(userId)) {
            throw new IllegalStateException("본인이 작성한 메시지만 삭제할 수 있습니다.");
        }

        message.delete();
}

    /**
     * 메시지 삭제 (관리자용 - 권한 체크 없음)
     */
    @Transactional
    public void deleteMessageByAdmin(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        message.delete();
}

    /**
     * 워크스페이스 내 메시지 수 조회
     */
    public long getMessageCount(Long workspaceId) {
        return messageRepository.countByWorkspaceId(workspaceId);
    }

    /**
     * 고정된 메시지 목록 조회
     */
    public List<MessageResponse> getPinnedMessages(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
        }

        List<Message> messages = messageRepository.findPinnedMessages(workspaceId);

        List<Long> userIds = messages.stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return messages.stream()
                .map(message -> {
                    User user = userMap.get(message.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return MessageResponse.of(message, nickname, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 메시지 고정/해제 토글
     */
    @Transactional
    public MessageResponse togglePinMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        if (message.isDeleted()) {
            throw new IllegalStateException("삭제된 메시지는 고정할 수 없습니다.");
        }

        message.togglePin();
        User user = userRepository.findById(message.getUserId()).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";

        return MessageResponse.of(message, nickname, null);
    }

    /**
     * 고정된 메시지 수 조회
     */
    public long getPinnedMessageCount(Long workspaceId) {
        return messageRepository.countPinnedMessages(workspaceId);
    }
}
