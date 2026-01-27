package com.ssafy.domain.retrospect.controller;

import com.ssafy.domain.retrospect.dto.response.RetrospectiveListResponse;
import com.ssafy.domain.retrospect.service.RetrospectiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
}