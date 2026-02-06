package com.ssafy.domain.friend.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.friend.dto.request.FriendRequest;
import com.ssafy.domain.friend.dto.response.*;
import com.ssafy.domain.friend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 API")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 친구 목록 조회
     */
    @Operation(summary = "친구 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.getFriends(userId)));
    }

    /**
     * 사용자 검색
     */
    @Operation(summary = "사용자 검색", description = "닉네임으로 사용자를 검색합니다. 검색 허용 설정한 사용자만 표시됩니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchUsers(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestParam String keyword) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.searchUsers(userId, keyword)));
    }

    /**
     * 친구 요청 보내기
     */
    @Operation(summary = "친구 요청 보내기")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody FriendRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(friendService.sendFriendRequest(userId, request.userId())));
    }

    /**
     * 받은 친구 요청 목록
     */
    @Operation(summary = "받은 친구 요청 목록")
    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getReceivedRequests(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.getReceivedRequests(userId)));
    }

    /**
     * 보낸 친구 요청 목록
     */
    @Operation(summary = "보낸 친구 요청 목록")
    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getSentRequests(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.getSentRequests(userId)));
    }

    /**
     * 친구 요청 수락
     */
    @Operation(summary = "친구 요청 수락")
    @PutMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptFriendRequest(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long requestId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.acceptFriendRequest(userId, requestId)));
    }

    /**
     * 친구 요청 거절
     */
    @Operation(summary = "친구 요청 거절")
    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<MessageResponse>> rejectFriendRequest(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long requestId) {
        Long userId = userDetails.getUser().getId();
        friendService.rejectFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 거절했습니다."));
    }

    /**
     * 친구 삭제
     */
    @Operation(summary = "친구 삭제")
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteFriend(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long friendshipId) {
        Long userId = userDetails.getUser().getId();
        friendService.deleteFriend(userId, friendshipId);
        return ResponseEntity.ok(ApiResponse.success("친구가 삭제되었습니다."));
    }

    /**
     * 사용자 차단
     */
    @Operation(summary = "사용자 차단", description = "차단된 사용자는 친구 요청, DM을 보낼 수 없습니다.")
    @PostMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<MessageResponse>> blockUser(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long userId) {
        Long myId = userDetails.getUser().getId();
        friendService.blockUser(myId, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자를 차단했습니다."));
    }

    /**
     * 차단 해제
     */
    @Operation(summary = "차단 해제")
    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<MessageResponse>> unblockUser(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long userId) {
        Long myId = userDetails.getUser().getId();
        friendService.unblockUser(myId, userId);
        return ResponseEntity.ok(ApiResponse.success("차단을 해제했습니다."));
    }

    /**
     * 차단 목록 조회
     */
    @Operation(summary = "차단 목록 조회")
    @GetMapping("/block")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(friendService.getBlockedUsers(userId)));
    }
}
