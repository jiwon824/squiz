package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.TemplateUsageLogRequest;
import com.ssafy.domain.study.dto.response.TemplateUsageLogResponse;
import com.ssafy.domain.study.service.TemplateUsageLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 템플릿 사용 로그 Controller
 * 사용자의 템플릿 사용 패턴 수집 (AI 파인튜닝용)
 */
@RestController
@RequestMapping("/api/v1/template-usage")
@RequiredArgsConstructor
@Slf4j
public class TemplateUsageLogController {

    private final TemplateUsageLogService templateUsageLogService;

    /**
     * 템플릿 사용 로그 저장
     * POST /api/v1/template-usage
     */
    @PostMapping
    public ResponseEntity<TemplateUsageLogResponse> logUsage(
            @Valid @RequestBody TemplateUsageLogRequest request,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 템플릿 사용 로그: userId={}, templateId={}", userId, request.getTemplateId());

        // TODO: 실제 구현 시 UserService에서 tech/schedule 조회
        Map<String, Object> userTechStack = null;
        Map<String, Object> userSchedule = null;

        TemplateUsageLogResponse response = templateUsageLogService.logUsage(
                request, userId, userTechStack, userSchedule);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 템플릿별 사용 통계
     * GET /api/v1/template-usage/stats/{templateId}
     */
    @GetMapping("/stats/{templateId}")
    public ResponseEntity<Map<String, Object>> getTemplateStats(
            @PathVariable Long templateId) {

        log.info("API 호출 - 템플릿 사용 통계: templateId={}", templateId);

        Map<String, Object> stats = templateUsageLogService.getTemplateStats(templateId);

        return ResponseEntity.ok(stats);
    }

    /**
     * 내 사용 로그 조회
     * GET /api/v1/template-usage/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<TemplateUsageLogResponse>> getMyLogs(
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 내 사용 로그: userId={}", userId);

        List<TemplateUsageLogResponse> response = templateUsageLogService.getUserLogs(userId);

        return ResponseEntity.ok(response);
    }
}
