package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.ApplicationCreateRequest;
import com.ssafy.domain.study.dto.request.ApplicationProcessRequest;
import com.ssafy.domain.study.dto.response.ApplicationResponse;
import com.ssafy.domain.study.entity.ApplicationStatus;
import com.ssafy.domain.study.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;

    // ============================================================
    // 신청 생성
    // ============================================================

    /**
     * 스터디 신청 생성
     */
    @PostMapping("/study/{studyId}/applications")
    public ResponseEntity<ApplicationResponse> createApplication(
            @PathVariable Long studyId,
            @Valid @RequestBody ApplicationCreateRequest request,
            @RequestHeader("user-id") Long userId) {

                ApplicationResponse response = applicationService.createApplication(studyId, request, userId);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // 신청 목록 조회
    // ============================================================

    /**
     * 스터디별 신청 목록 조회
     */
    @GetMapping("/study/{studyId}/applications")
    public ResponseEntity<Page<ApplicationResponse>> getApplicationsByStudy(
            @PathVariable Long studyId,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<ApplicationResponse> response = applicationService.getApplicationByStudy(studyId, status, pageable);

                return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 신청 내역 조회
     */
    @GetMapping("/user/{userId}/applications")
    public ResponseEntity<Page<ApplicationResponse>> getApplicationsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<ApplicationResponse> response = applicationService.getApplicationByUser(userId, status, pageable);

                return ResponseEntity.ok(response);
    }

    /**
     * 신청 상세 조회
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplication(
            @PathVariable Long applicationId) {

                ApplicationResponse response = applicationService.getApplication(applicationId);

                return ResponseEntity.ok(response);
    }

    // ============================================================
    // 신청 승인/거절
    // ============================================================

    /**
     * 신청 승인
     */
    @PatchMapping("/study/{studyId}/applications/{applicationId}/approve")
    public ResponseEntity<ApplicationResponse> approveApplication(
            @PathVariable Long studyId,
            @PathVariable Long applicationId,
            @RequestHeader("user-id") Long leaderId) {

                ApplicationResponse response = applicationService.approveApplication(studyId, applicationId, leaderId);

                return ResponseEntity.ok(response);
    }

    /**
     * 신청 거절
     */
    @PatchMapping("/study/{studyId}/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable Long studyId,
            @PathVariable Long applicationId,
            @RequestBody ApplicationProcessRequest request,
            @RequestHeader("user-id") Long leaderId) {

                ApplicationResponse response = applicationService.rejectApplication(
                studyId, applicationId, leaderId, request.getRejectedReason());

                return ResponseEntity.ok(response);
    }

    // ============================================================
    // 내 신청 내역 조회 (편의 기능)
    // ============================================================

    /**
     * 내 신청 내역 조회
     */
    @GetMapping("/my/applications")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            @RequestHeader("user-id") Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<ApplicationResponse> response = applicationService.getApplicationByUser(userId, status, pageable);

                return ResponseEntity.ok(response);
    }
}
