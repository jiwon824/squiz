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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        log.info("API 호출 - 템플릿 생성: userId={}, name={}", userId, request.getName());

        StudyTemplateResponse response = studyTemplateService.createTemplate(request, userId);

        log.info("API 응답 - 템플릿 생성 완료: templateId={}", response.getId());

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

        log.info("API 호출 - 내 템플릿 목록 조회: userId={}", userId);

        List<StudyTemplateResponse> response = studyTemplateService.getMyTemplates(userId);

        log.info("API 응답 - 내 템플릿 목록: count={}", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 템플릿 목록 조회
     * GET /api/v1/study-templates/system?templateType=ALGORITHM
     */
    @GetMapping("/system")
    public ResponseEntity<List<StudyTemplateResponse>> getSystemTemplates(
            @RequestParam(required = false) String templateType) {

        log.info("API 호출 - 시스템 템플릿 조회: templateType={}", templateType);

        List<StudyTemplateResponse> response = studyTemplateService.getSystemTemplates(templateType);

        log.info("API 응답 - 시스템 템플릿 목록: count={}", response.size());

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

        log.info("API 호출 - AI 템플릿 추천: userId={}", userId);

        if (request == null) {
            request = new TemplateRecommendRequest();
        }

        TemplateRecommendResponse response = studyTemplateService.recommendTemplate(request, userId);

        log.info("API 응답 - AI 추천 완료: type={}, topic={}", response.getTemplateType(), response.getTopic());

        return ResponseEntity.ok(response);
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

        log.info("API 호출 - 템플릿 상세 조회: templateId={}, userId={}", templateId, userId);

        StudyTemplateResponse response = studyTemplateService.getTemplate(templateId, userId);

        log.info("API 응답 - 템플릿 상세: templateId={}, name={}", response.getId(), response.getName());

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

        log.info("API 호출 - 템플릿 수정: templateId={}, userId={}", templateId, userId);

        StudyTemplateResponse response = studyTemplateService.updateTemplate(templateId, request, userId);

        log.info("API 응답 - 템플릿 수정 완료: templateId={}", response.getId());

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

        log.info("API 호출 - 템플릿 삭제: templateId={}, userId={}", templateId, userId);

        studyTemplateService.deleteTemplate(templateId, userId);

        log.info("API 응답 - 템플릿 삭제 완료: templateId={}", templateId);

        return ResponseEntity.noContent().build();
    }
}