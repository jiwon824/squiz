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

                Page<RetrospectiveListResponse> response = retrospectiveService.getRetrospectives(studyId, userId, pageable);

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

                RetrospectiveDetailResponse response = retrospectiveService.getRetrospectiveDetail(studyId, retroId);

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

                RetrospectiveDetailResponse response = retrospectiveService.createRetrospective(studyId, request, userId);

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

                retrospectiveService.deleteRetrospective(studyId, retroId, userId);

                return ResponseEntity.noContent().build();
    }
}
