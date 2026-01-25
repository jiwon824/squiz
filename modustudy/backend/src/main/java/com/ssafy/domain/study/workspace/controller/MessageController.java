package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.request.MessageUpdateRequest;
import com.ssafy.domain.study.workspace.dto.response.MessagePageResponse;
import com.ssafy.domain.study.workspace.dto.response.MessageResponse;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * 메시지 생성
     */
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long workspaceId,
            @Valid @RequestBody MessageCreateRequest request,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 메시지 생성: workspaceId={}, userId={}, type={}",
                workspaceId, userId, request.getMessageType());

        // workspaceId 검증 (request에 있는 것과 일치하는지)
        if (!workspaceId.equals(request.getWorkspaceId())) {
            throw new IllegalArgumentException("경로의 workspaceId와 요청의 workspaceId가 일치하지 않습니다.");
        }

        MessageResponse response = messageService.createMessage(request, userId);

        log.info("API 응답 - 메시지 생성 완료: messageId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 메시지 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<MessagePageResponse> getMessages(
            @PathVariable Long workspaceId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("API 호출 - 메시지 목록 조회: workspaceId={}, page={}, size={}",
                workspaceId, pageable.getPageNumber(), pageable.getPageSize());

        MessagePageResponse response = messageService.getMessages(workspaceId, pageable);

        log.info("API 응답 - 메시지 목록: totalElements={}", response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 메시지 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<MessageResponse>> getRecentMessages(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("API 호출 - 최근 메시지 조회: workspaceId={}, limit={}", workspaceId, limit);

        List<MessageResponse> response = messageService.getRecentMessages(workspaceId, limit);

        log.info("API 응답 - 최근 메시지: count={}", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 시간 이후 메시지 조회 (폴링용)
     */
    @GetMapping("/after")
    public ResponseEntity<List<MessageResponse>> getMessagesAfter(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {

        log.info("API 호출 - 특정 시간 이후 메시지 조회: workspaceId={}, after={}", workspaceId, after);

        List<MessageResponse> response = messageService.getMessagesAfter(workspaceId, after);

        log.info("API 응답 - 새 메시지: count={}", response.size());

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

        log.info("API 호출 - 메시지 검색: workspaceId={}, keyword={}", workspaceId, keyword);

        MessagePageResponse response = messageService.searchMessages(workspaceId, keyword, pageable);

        log.info("API 응답 - 검색 결과: totalElements={}", response.getTotalElements());

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

        log.info("API 호출 - 메시지 타입별 조회: workspaceId={}, type={}", workspaceId, messageType);

        MessagePageResponse response = messageService.getMessagesByType(workspaceId, messageType, pageable);

        log.info("API 응답 - 타입별 메시지: totalElements={}", response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 메시지 상세 조회
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable Long workspaceId,
            @PathVariable Long messageId) {

        log.info("API 호출 - 메시지 상세 조회: workspaceId={}, messageId={}", workspaceId, messageId);

        MessageResponse response = messageService.getMessage(messageId);

        log.info("API 응답 - 메시지 조회 완료: userId={}", response.getUserId());

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

        log.info("API 호출 - 메시지 수정: workspaceId={}, messageId={}, userId={}",
                workspaceId, messageId, userId);

        MessageResponse response = messageService.updateMessage(messageId, request, userId);

        log.info("API 응답 - 메시지 수정 완료");

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

        log.info("API 호출 - 메시지 삭제: workspaceId={}, messageId={}, userId={}",
                workspaceId, messageId, userId);

        messageService.deleteMessage(messageId, userId);

        log.info("API 응답 - 메시지 삭제 완료");

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

        log.info("API 호출 - 관리자 메시지 삭제: workspaceId={}, messageId={}, adminId={}",
                workspaceId, messageId, userId);

        messageService.deleteMessageByAdmin(messageId);

        log.info("API 응답 - 관리자 메시지 삭제 완료");

        return ResponseEntity.noContent().build();
    }

    /**
     * 메시지 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getMessageCount(
            @PathVariable Long workspaceId) {

        log.info("API 호출 - 메시지 수 조회: workspaceId={}", workspaceId);

        long count = messageService.getMessageCount(workspaceId);

        log.info("API 응답 - 메시지 수: {}", count);

        return ResponseEntity.ok(count);
    }
}