package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.StudyCommentCreateRequest;
import com.ssafy.domain.study.dto.request.StudyCommentUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyCommentPageResponse;
import com.ssafy.domain.study.dto.response.StudyCommentResponse;
import com.ssafy.domain.study.service.StudyCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/study/{studyId}/comments")
@RequiredArgsConstructor
@Slf4j
public class StudyCommentController {

    private final StudyCommentService commentService;

    // ============================================================
    // 댓글 생성
    // ============================================================

    /**
     * 댓글 생성 (최상위 댓글 또는 대댓글)
     * POST /api/v1/study/{studyId}/comments
     */
    @PostMapping
    public ResponseEntity<StudyCommentResponse> createComment(
            @PathVariable Long studyId,
            @Valid @RequestBody StudyCommentCreateRequest request,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 댓글 생성: studyId={}, userId={}, parentId={}",
                studyId, userId, request.getParentId());

        StudyCommentResponse response = commentService.createComment(studyId, request, userId);

        log.info("API 응답 - 댓글 생성 완료: commentId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // 댓글 목록 조회
    // ============================================================

    /**
     * 스터디별 댓글 목록 조회 (대댓글 포함)
     * GET /api/v1/study/{studyId}/comments?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<StudyCommentPageResponse> getCommentsByStudy(
            @PathVariable Long studyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("API 호출 - 스터디별 댓글 목록 조회: studyId={}, page={}, size={}",
                studyId, pageable.getPageNumber(), pageable.getPageSize());

        StudyCommentPageResponse response = commentService.getCommentsByStudy(studyId, pageable);

        log.info("API 응답 - 댓글 목록: studyId={}, totalElements={}, totalPages={}",
                studyId, response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디별 최상위 댓글만 조회 (대댓글 개수만 포함)
     * GET /api/v1/study/{studyId}/comments/parents?page=0&size=10
     */
    @GetMapping("/parents")
    public ResponseEntity<StudyCommentPageResponse> getParentCommentsOnly(
            @PathVariable Long studyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("API 호출 - 최상위 댓글만 조회: studyId={}, page={}, size={}",
                studyId, pageable.getPageNumber(), pageable.getPageSize());

        StudyCommentPageResponse response = commentService.getParentCommentsOnly(studyId, pageable);

        log.info("API 응답 - 최상위 댓글 목록: studyId={}, totalElements={}",
                studyId, response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 대댓글 목록 조회
     * GET /api/v1/study/{studyId}/comments/{commentId}/replies
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<StudyCommentResponse>> getReplies(
            @PathVariable Long studyId,
            @PathVariable Long commentId) {

        log.info("API 호출 - 대댓글 목록 조회: studyId={}, parentCommentId={}", studyId, commentId);

        List<StudyCommentResponse> response = commentService.getReplies(studyId, commentId);

        log.info("API 응답 - 대댓글 목록: parentCommentId={}, count={}", commentId, response.size());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 댓글 상세 조회
    // ============================================================

    /**
     * 댓글 상세 조회
     * GET /api/v1/study/{studyId}/comments/{commentId}
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<StudyCommentResponse> getComment(
            @PathVariable Long studyId,
            @PathVariable Long commentId) {

        log.info("API 호출 - 댓글 상세 조회: studyId={}, commentId={}", studyId, commentId);

        StudyCommentResponse response = commentService.getComment(studyId, commentId);

        log.info("API 응답 - 댓글 상세: commentId={}, userId={}",
                response.getId(), response.getUserId());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 댓글 수정
    // ============================================================

    /**
     * 댓글 수정 (작성자만 가능)
     * PUT /api/v1/study/{studyId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<StudyCommentResponse> updateComment(
            @PathVariable Long studyId,
            @PathVariable Long commentId,
            @Valid @RequestBody StudyCommentUpdateRequest request,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 댓글 수정: studyId={}, commentId={}, userId={}",
                studyId, commentId, userId);

        StudyCommentResponse response = commentService.updateComment(studyId, commentId, request, userId);

        log.info("API 응답 - 댓글 수정 완료: commentId={}", response.getId());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 댓글 삭제
    // ============================================================

    /**
     * 댓글 삭제 (작성자 또는 스터디장만 가능, Soft Delete)
     * DELETE /api/v1/study/{studyId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long studyId,
            @PathVariable Long commentId,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 댓글 삭제: studyId={}, commentId={}, userId={}",
                studyId, commentId, userId);

        commentService.deleteComment(studyId, commentId, userId);

        log.info("API 응답 - 댓글 삭제 완료: commentId={}", commentId);

        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 통계 조회
    // ============================================================

    /**
     * 스터디별 댓글 개수 조회
     * GET /api/v1/study/{studyId}/comments/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCommentCount(
            @PathVariable Long studyId) {

        log.info("API 호출 - 댓글 개수 조회: studyId={}", studyId);

        Long count = commentService.getCommentCount(studyId);

        log.info("API 응답 - 댓글 개수: studyId={}, count={}", studyId, count);

        return ResponseEntity.ok(count);
    }
}