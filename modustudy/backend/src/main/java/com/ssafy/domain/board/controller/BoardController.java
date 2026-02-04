package com.ssafy.domain.board.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.board.dto.request.BoardCommentCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostUpdateRequest;
import com.ssafy.domain.board.dto.request.BoardReportRequest;
import com.ssafy.domain.board.dto.response.BoardCommentResponse;
import com.ssafy.domain.board.dto.response.BoardPostDetailResponse;
import com.ssafy.domain.board.dto.response.BoardPostSummaryResponse;
import com.ssafy.domain.board.dto.response.BoardRecruitingStudyResponse;
import com.ssafy.domain.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/recruitments/studies")
    public ResponseEntity<ApiResponse<List<BoardRecruitingStudyResponse>>> getRecruitingStudies(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        return ResponseEntity.ok(ApiResponse.success(boardService.getRecruitingStudies(userId)));
    }

    @PostMapping("/recruitments")
    public ResponseEntity<ApiResponse<BoardPostDetailResponse>> createPost(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody BoardPostCreateRequest request
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        BoardPostDetailResponse response = boardService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/recruitments")
    public ResponseEntity<ApiResponse<Page<BoardPostSummaryResponse>>> getPosts(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getPosts(pageable)));
    }

    @GetMapping("/recruitments/{postId}")
    public ResponseEntity<ApiResponse<BoardPostDetailResponse>> getPostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getPostDetail(postId)));
    }

    @PutMapping("/recruitments/{postId}")
    public ResponseEntity<ApiResponse<BoardPostDetailResponse>> updatePost(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long postId,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody BoardPostUpdateRequest request
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        return ResponseEntity.ok(ApiResponse.success(boardService.updatePost(userId, postId, request)));
    }

    @DeleteMapping("/recruitments/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long postId,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        boardService.deletePost(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(null, "삭제되었습니다."));
    }

    @PostMapping("/recruitments/{postId}/comments")
    public ResponseEntity<ApiResponse<BoardCommentResponse>> addComment(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long postId,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody BoardCommentCreateRequest request
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(boardService.addComment(userId, postId, request)));
    }

    @PostMapping("/recruitments/{postId}/report")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long postId,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody BoardReportRequest request
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        boardService.reportPost(userId, postId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신고가 접수되었습니다."));
    }

    @DeleteMapping("/recruitments/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader(value = "User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userDetails, userIdHeader);
        boardService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null, "삭제되었습니다."));
    }

    private Long resolveUserId(SsafyUserDetails userDetails, Long userIdHeader) {
        if (userDetails != null && userDetails.getUser() != null) {
            return userDetails.getUser().getId();
        }
        if (userIdHeader != null) {
            return userIdHeader;
        }
        throw new BusinessException("UNAUTHORIZED", "로그인이 필요합니다.");
    }
}
