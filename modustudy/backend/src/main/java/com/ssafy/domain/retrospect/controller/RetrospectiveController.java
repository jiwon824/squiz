package com.ssafy.domain.retrospect.controller;

import com.ssafy.domain.retrospect.dto.request.RetrospectiveCreateRequest;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveDetailResponse;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveListResponse;
import com.ssafy.domain.retrospect.service.RetrospectiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/studies/{studyId}/retrospectives")
@RequiredArgsConstructor
@Slf4j
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    /**
     * 스터디별 회고 목록 조회
     * GET /api/v1/studies/{studyId}/retrospectives?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<RetrospectiveListResponse>> getRetrospectives(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 회고 목록 조회: studyId={}, userId={}, page={}, size={}",
                studyId, userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<RetrospectiveListResponse> response = retrospectiveService.getRetrospectives(studyId, userId, pageable);

        log.info("API 응답 - 회고 목록: studyId={}, count={}, totalElements={}",
                studyId, response.getContent().size(), response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 회고 상세 조회
     * GET /api/v1/studies/{studyId}/retrospectives/{retroId}
     */
    @GetMapping("/{retroId}")
    public ResponseEntity<RetrospectiveDetailResponse> getRetrospectiveDetail(
            @PathVariable Long studyId,
            @PathVariable Long retroId) {

        log.info("API 호출 - 회고 상세 조회: studyId={}, retroId={}", studyId, retroId);

        RetrospectiveDetailResponse response = retrospectiveService.getRetrospectiveDetail(studyId, retroId);

        log.info("API 응답 - 회고 상세: retroId={}, title={}", response.getId(), response.getTitle());

        return ResponseEntity.ok(response);
    }

    /**
     * 회고 생성 (스터디 멤버 누구나)
     * POST /api/v1/studies/{studyId}/retrospectives
     */
    @PostMapping
    public ResponseEntity<RetrospectiveDetailResponse> createRetrospective(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody RetrospectiveCreateRequest request) {

        log.info("API 호출 - 회고 생성: studyId={}, userId={}, title={}", studyId, userId, request.getTitle());

        RetrospectiveDetailResponse response = retrospectiveService.createRetrospective(studyId, request, userId);

        log.info("API 응답 - 회고 생성 완료: retroId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 회고 삭제 (스터디장 또는 생성자)
     * DELETE /api/v1/studies/{studyId}/retrospectives/{retroId}
     */
    @DeleteMapping("/{retroId}")
    public ResponseEntity<Void> deleteRetrospective(
            @PathVariable Long studyId,
            @PathVariable Long retroId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 회고 삭제: studyId={}, retroId={}, userId={}", studyId, retroId, userId);

        retrospectiveService.deleteRetrospective(studyId, retroId, userId);

        log.info("API 응답 - 회고 삭제 완료: retroId={}", retroId);

        return ResponseEntity.noContent().build();
    }
}