package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.TemplateUsageLogRequest;
import com.ssafy.domain.study.dto.response.TemplateUsageLogResponse;
import com.ssafy.domain.study.service.TemplateUsageLogService;
import com.ssafy.domain.user.entity.Profile;
import com.ssafy.domain.user.entity.UserSchedule;
import com.ssafy.domain.user.repository.ProfileRepository;
import com.ssafy.domain.user.repository.UserScheduleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    private final ProfileRepository profileRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final ObjectMapper objectMapper;

    /**
     * 템플릿 사용 로그 저장
     * POST /api/v1/template-usage
     */
    @PostMapping
    public ResponseEntity<TemplateUsageLogResponse> logUsage(
            @Valid @RequestBody TemplateUsageLogRequest request,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 템플릿 사용 로그: userId={}, templateId={}", userId, request.getTemplateId());

        // 사용자 기술 스택 스냅샷
        Map<String, Object> userTechStack = null;
        try {
            Profile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile != null && profile.getTech() != null) {
                List<String> techList = objectMapper.readValue(profile.getTech(), List.class);
                userTechStack = Map.of("tech", techList);
            }
        } catch (Exception e) {
            log.warn("기술 스택 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
        }

        // 사용자 가용 스케줄 스냅샷
        Map<String, Object> userSchedule = null;
        try {
            List<UserSchedule> schedules = userScheduleRepository.findByUserIdAndIsAvailableTrue(userId);
            if (!schedules.isEmpty()) {
                Map<String, Object> scheduleMap = new LinkedHashMap<>();
                for (UserSchedule s : schedules) {
                    scheduleMap.put(s.getDayOfWeek().name(),
                            Map.of("start", s.getStartTime().toString(), "end", s.getEndTime().toString()));
                }
                userSchedule = scheduleMap;
            }
        } catch (Exception e) {
            log.warn("스케줄 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
        }

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
