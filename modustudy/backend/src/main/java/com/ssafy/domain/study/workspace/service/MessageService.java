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
        log.info("메시지 생성 요청 - workspaceId: {}, userId: {}", request.getWorkspaceId(), userId);

        if (!workspaceRepository.existsById(request.getWorkspaceId())) {
            log.warn("워크스페이스를 찾을 수 없습니다 - workspaceId: {}", request.getWorkspaceId());
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        Message message = messageRepository.save(request.toEntity(userId));
        log.info("메시지 생성 완료 - messageId: {}", message.getId());

        return MessageResponse.of(message, user.getNickname(), null);
    }

    /**
     * 메시지 조회
     */
    public MessageResponse getMessage(Long messageId) {
        log.info("메시지 조회 요청 - messageId: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("메시지를 찾을 수 없습니다 - messageId: {}", messageId);
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
        log.info("메시지 목록 조회 요청 - workspaceId: {}, page: {}, size: {}",
                workspaceId, pageable.getPageNumber(), pageable.getPageSize());

        if (!workspaceRepository.existsById(workspaceId)) {
            log.warn("워크스페이스를 찾을 수 없습니다 - workspaceId: {}", workspaceId);
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
        log.info("최근 메시지 조회 요청 - workspaceId: {}, limit: {}", workspaceId, limit);

        if (!workspaceRepository.existsById(workspaceId)) {
            log.warn("워크스페이스를 찾을 수 없습니다 - workspaceId: {}", workspaceId);
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
        log.info("새 메시지 조회 요청 - workspaceId: {}, after: {}", workspaceId, after);

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
        log.info("메시지 검색 요청 - workspaceId: {}, keyword: {}", workspaceId, keyword);

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
        log.info("메시지 타입별 조회 요청 - workspaceId: {}, type: {}", workspaceId, messageType);

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
        log.info("메시지 수정 요청 - messageId: {}, userId: {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("메시지를 찾을 수 없습니다 - messageId: {}", messageId);
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        if (!message.isAuthor(userId)) {
            log.warn("메시지 수정 권한이 없습니다 - messageId: {}, userId: {}", messageId, userId);
            throw new IllegalStateException("본인이 작성한 메시지만 수정할 수 있습니다.");
        }

        if (message.isDeleted()) {
            log.warn("삭제된 메시지입니다 - messageId: {}", messageId);
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다.");
        }

        message.updateContent(request.getContent());
        log.info("메시지 수정 완료 - messageId: {}", messageId);

        User user = userRepository.findById(userId).orElse(null);
        String nickname = user != null ? user.getNickname() : "알 수 없음";

        return MessageResponse.of(message, nickname, null);
    }

    /**
     * 메시지 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        log.info("메시지 삭제 요청 - messageId: {}, userId: {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("메시지를 찾을 수 없습니다 - messageId: {}", messageId);
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        // 작성자 확인
        if (!message.isAuthor(userId)) {
            log.warn("메시지 삭제 권한이 없습니다 - messageId: {}, userId: {}", messageId, userId);
            throw new IllegalStateException("본인이 작성한 메시지만 삭제할 수 있습니다.");
        }

        message.delete();
        log.info("메시지 삭제 완료 - messageId: {}", messageId);
    }

    /**
     * 메시지 삭제 (관리자용 - 권한 체크 없음)
     */
    @Transactional
    public void deleteMessageByAdmin(Long messageId) {
        log.info("관리자 메시지 삭제 요청 - messageId: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("메시지를 찾을 수 없습니다 - messageId: {}", messageId);
                    return new IllegalArgumentException("메시지를 찾을 수 없습니다.");
                });

        message.delete();
        log.info("관리자 메시지 삭제 완료 - messageId: {}", messageId);
    }

    /**
     * 워크스페이스 내 메시지 수 조회
     */
    public long getMessageCount(Long workspaceId) {
        return messageRepository.countByWorkspaceId(workspaceId);
    }
}