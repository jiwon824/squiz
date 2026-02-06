package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.CreateTemplateRequest;
import com.ssafy.domain.study.dto.request.TemplateRecommendRequest;
import com.ssafy.domain.study.dto.request.UpdateTemplateRequest;
import com.ssafy.domain.study.dto.response.StudyTemplateResponse;
import com.ssafy.domain.study.dto.response.TemplateRecommendResponse;
import com.ssafy.domain.study.service.StudyTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 스터디 템플릿 Controller
 */
 @RestController
 @RequestMapping("/api/v1/study-templates")
 @RequiredArgsConstructor
 @Slf4j
 public class StudyTemplateController {

    private final StudyTemplateService studyTemplateService;

    // ============================================================
    // 템플릿 생성
    // ============================================================

    /**
     * 템플릿 생성
     * POST /api/v1/study-templates
     */
    @PostMapping
    public ResponseEntity<StudyTemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @RequestHeader("user-id") Long userId) {

                StudyTemplateResponse response = studyTemplateService.createTemplate(request, userId);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // 템플릿 목록 조회
    // ============================================================

    /**
     * 내 템플릿 목록 조회
     * GET /api/v1/study-templates/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<StudyTemplateResponse>> getMyTemplates(
            @RequestHeader("user-id") Long userId) {

                List<StudyTemplateResponse> response = studyTemplateService.getMyTemplates(userId);

                return ResponseEntity.ok(response);
    }

    /**
     * 시스템 템플릿 목록 조회
     * GET /api/v1/study-templates/system?templateType=ALGORITHM
     */
    @GetMapping("/system")
    public ResponseEntity<List<StudyTemplateResponse>> getSystemTemplates(
            @RequestParam(required = false) String templateType) {

                List<StudyTemplateResponse> response = studyTemplateService.getSystemTemplates(templateType);

                return ResponseEntity.ok(response);
    }

    // ============================================================
    // AI 추천
    // ============================================================

    /**
     * AI 템플릿 추천
     * POST /api/v1/study-templates/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<TemplateRecommendResponse> recommendTemplate(
            @RequestBody(required = false) TemplateRecommendRequest request,
            @RequestHeader("user-id") Long userId) {

                if (request == null) {
            request = new TemplateRecommendRequest();
        }

        TemplateRecommendResponse response = studyTemplateService.recommendTemplate(request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * AI 템플릿 추천 (스트리밍)
     * GET /api/v1/study-templates/recommend/stream
     * - SSE(Server-Sent Events)로 실시간 토큰 전송
     * - 완료 시 파싱된 JSON 결과 전송
     */
    @GetMapping(value = "/recommend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter recommendTemplateStream(
            @RequestParam(required = false) String topicInput,
            @RequestParam(required = false, defaultValue = "4") Integer durationWeeks,
            @RequestParam(required = false) Integer totalSessions,
            @RequestHeader(value = "user-id", required = false) Long userId) {

// 타임아웃 2분 (AI 생성 최대 시간 고려)
        SseEmitter emitter = new SseEmitter(120000L);

        // 에러/타임아웃 핸들러
// 비동기로 AI 서버 스트리밍 호출
        CompletableFuture.runAsync(() -> {
            try {
                studyTemplateService.streamRecommendTemplate(
                        topicInput, durationWeeks, totalSessions, userId, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\": \"" + e.getMessage() + "\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ============================================================
    // 템플릿 상세 조회
    // ============================================================

    /**
     * 템플릿 상세 조회
     * GET /api/v1/study-templates/{templateId}
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<StudyTemplateResponse> getTemplate(
            @PathVariable Long templateId,
            @RequestHeader("user-id") Long userId) {

                StudyTemplateResponse response = studyTemplateService.getTemplate(templateId, userId);

                return ResponseEntity.ok(response);
    }

    // ============================================================
    // 템플릿 수정
    // ============================================================

    /**
     * 템플릿 수정
     * PUT /api/v1/study-templates/{templateId}
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<StudyTemplateResponse> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdateTemplateRequest request,
            @RequestHeader("user-id") Long userId) {

                StudyTemplateResponse response = studyTemplateService.updateTemplate(templateId, request, userId);

                return ResponseEntity.ok(response);
    }

    // ============================================================
    // 템플릿 삭제
    // ============================================================

    /**
     * 템플릿 삭제
     * DELETE /api/v1/study-templates/{templateId}
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long templateId,
            @RequestHeader("user-id") Long userId) {

                studyTemplateService.deleteTemplate(templateId, userId);

                return ResponseEntity.noContent().build();
    }
}

