package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.request.MessageUpdateRequest;
import com.ssafy.domain.study.workspace.dto.response.MessagePageResponse;
import com.ssafy.domain.study.workspace.dto.response.MessageResponse;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.service.MessageService;
import com.ssafy.domain.study.workspace.websocket.WorkspaceWebSocketEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 컨트롤러
 */
 @Slf4j
 @RestController
 @RequestMapping("/api/v1/workspaces/{workspaceId}/messages")
 @RequiredArgsConstructor
 public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 생성
     */
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long workspaceId,
            @Valid @RequestBody MessageCreateRequest request,
            @RequestHeader("User-Id") Long userId) {

// workspaceId 검증 (request에 있는 것과 일치하는지)
        if (!workspaceId.equals(request.getWorkspaceId())) {
            throw new IllegalArgumentException("경로의 workspaceId와 요청의 workspaceId가 일치하지 않습니다.");
        }

        MessageResponse response = messageService.createMessage(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 메시지 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<MessagePageResponse> getMessages(
            @PathVariable Long workspaceId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                MessagePageResponse response = messageService.getMessages(workspaceId, pageable);

                return ResponseEntity.ok(response);
    }

    /**
     * 최근 메시지 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<MessageResponse>> getRecentMessages(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "20") int limit) {

                List<MessageResponse> response = messageService.getRecentMessages(workspaceId, limit);

                return ResponseEntity.ok(response);
    }

    /**
     * 특정 시간 이후 메시지 조회 (폴링용)
     */
    @GetMapping("/after")
    public ResponseEntity<List<MessageResponse>> getMessagesAfter(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {

                List<MessageResponse> response = messageService.getMessagesAfter(workspaceId, after);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 검색
     */
    @GetMapping("/search")
    public ResponseEntity<MessagePageResponse> searchMessages(
            @PathVariable Long workspaceId,
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                MessagePageResponse response = messageService.searchMessages(workspaceId, keyword, pageable);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 타입별 조회
     */
    @GetMapping("/type/{messageType}")
    public ResponseEntity<MessagePageResponse> getMessagesByType(
            @PathVariable Long workspaceId,
            @PathVariable MessageType messageType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                MessagePageResponse response = messageService.getMessagesByType(workspaceId, messageType, pageable);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 상세 조회
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId) {

                MessageResponse response = messageService.getMessage(messageId);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 수정
     */
    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId,
            @Valid @RequestBody MessageUpdateRequest request,
            @RequestHeader("User-Id") Long userId) {

                MessageResponse response = messageService.updateMessage(messageId, request, userId);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 삭제
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId,
            @RequestHeader("User-Id") Long userId) {

                messageService.deleteMessage(messageId, userId);

                return ResponseEntity.noContent().build();
    }

    /**
     * 메시지 삭제 (관리자용)
     */
    @DeleteMapping("/{messageId}/admin")
    public ResponseEntity<Void> deleteMessageByAdmin(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId,
            @RequestHeader("User-Id") Long userId) {

                messageService.deleteMessageByAdmin(messageId);

                return ResponseEntity.noContent().build();
    }

    /**
     * 메시지 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getMessageCount(
            @PathVariable Long workspaceId) {

                long count = messageService.getMessageCount(workspaceId);

                return ResponseEntity.ok(count);
    }

    /**
     * 고정된 메시지 목록 조회
     */
    @GetMapping("/pinned")
    public ResponseEntity<List<MessageResponse>> getPinnedMessages(
            @PathVariable Long workspaceId) {

                List<MessageResponse> response = messageService.getPinnedMessages(workspaceId);

                return ResponseEntity.ok(response);
    }

    /**
     * 메시지 고정/해제 토글
     */
    @PatchMapping("/{messageId}/pin")
    public ResponseEntity<MessageResponse> togglePinMessage(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId,
            @RequestHeader("User-Id") Long userId) {

                MessageResponse response = messageService.togglePinMessage(messageId);

        // WebSocket으로 PIN 이벤트 브로드캐스트
        WorkspaceWebSocketEvent pinEvent = WorkspaceWebSocketEvent.pinMessage(response, userId);
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, pinEvent);

        return ResponseEntity.ok(response);
    }

    /**
     * 고정된 메시지 수 조회
     */
    @GetMapping("/pinned/count")
    public ResponseEntity<Long> getPinnedMessageCount(
            @PathVariable Long workspaceId) {

                long count = messageService.getPinnedMessageCount(workspaceId);

                return ResponseEntity.ok(count);
    }
}
