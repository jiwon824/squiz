package com.ssafy.domain.material.controller;

import com.ssafy.domain.material.dto.request.MaterialCommentCreateRequest;
import com.ssafy.domain.material.dto.response.MaterialCommentCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialCommentResponse;
import com.ssafy.domain.material.service.MaterialCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 자료 댓글 컨트롤러
 * Base URL: /api/v1/studies/{studyId}/materials/{materialId}/comments
 */
 @Slf4j
 @RestController
 @RequestMapping("/api/v1/studies/{studyId}/materials/{materialId}/comments")
 @RequiredArgsConstructor
 public class MaterialCommentController {

    private final MaterialCommentService commentService;

    /**
     * 댓글 목록 조회
     * GET /api/v1/studies/{studyId}/materials/{materialId}/comments
     */
    @GetMapping
    public ResponseEntity<List<MaterialCommentResponse>> getComments(
            @PathVariable Long studyId,
            @PathVariable Long materialId) {

                List<MaterialCommentResponse> result = commentService.getComments(materialId);

        return ResponseEntity.ok(result);
    }

    /**
     * 댓글 작성
     * POST /api/v1/studies/{studyId}/materials/{materialId}/comments
     */
    @PostMapping
    public ResponseEntity<MaterialCommentCreateResponse> createComment(
            @PathVariable Long studyId,
            @PathVariable Long materialId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody MaterialCommentCreateRequest request) {

                MaterialCommentCreateResponse result = commentService.createComment(materialId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 댓글 삭제
     * DELETE /api/v1/studies/{studyId}/materials/{materialId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long studyId,
            @PathVariable Long materialId,
            @PathVariable Long commentId,
            @RequestHeader("User-Id") Long userId) {

                commentService.deleteComment(materialId, commentId, userId);

        return ResponseEntity.noContent().build();
    }
}
