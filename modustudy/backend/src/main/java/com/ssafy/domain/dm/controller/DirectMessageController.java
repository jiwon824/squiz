package com.ssafy.domain.dm.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.dm.dto.request.DirectMessageRequest;
import com.ssafy.domain.dm.dto.response.DirectMessageResponse;
import com.ssafy.domain.dm.dto.response.DmConversationResponse;
import com.ssafy.domain.dm.service.DirectMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Direct Message", description = "DM API")
@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    /**
     * DM 전송
     */
    @Operation(summary = "DM 전송", description = "친구에게 DM을 전송합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<DirectMessageResponse>> sendMessage(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody DirectMessageRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        DirectMessageResponse response = directMessageService.sendMessage(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 대화방 목록 조회
     */
    @Operation(summary = "대화방 목록 조회")
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<DmConversationResponse>>> getConversations(
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        List<DmConversationResponse> conversations = directMessageService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * 대화 메시지 목록 조회
     */
    @Operation(summary = "대화 메시지 목록 조회")
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<DirectMessageResponse>>> getMessages(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long userId = userDetails.getUser().getId();
        List<DirectMessageResponse> messages = directMessageService.getMessages(userId, conversationId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 읽음 처리
     */
    @Operation(summary = "읽음 처리", description = "대화방의 메시지를 읽음 처리합니다.")
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markAsRead(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long conversationId
    ) {
        Long userId = userDetails.getUser().getId();
        directMessageService.markAsRead(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("읽음 처리되었습니다."));
    }

    /**
     * 대화방 삭제
     */
    @Operation(summary = "대화방 삭제")
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteConversation(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long conversationId
    ) {
        Long userId = userDetails.getUser().getId();
        directMessageService.deleteConversation(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("대화방이 삭제되었습니다."));
    }

    /**
     * 안 읽은 메시지 총 개수 조회
     */
    @Operation(summary = "안 읽은 메시지 총 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount(
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        int count = directMessageService.getTotalUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }
}
