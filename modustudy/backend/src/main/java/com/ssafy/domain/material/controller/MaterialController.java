package com.ssafy.domain.material.controller;

import com.ssafy.domain.material.dto.request.MaterialCreateRequest;
import com.ssafy.domain.material.dto.request.MaterialSearchCondition;
import com.ssafy.domain.material.dto.request.MaterialUpdateRequest;
import com.ssafy.domain.material.dto.response.MaterialCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialDetailResponse;
import com.ssafy.domain.material.dto.response.MaterialListResponse;
import com.ssafy.domain.material.entity.MaterialType;
import com.ssafy.domain.material.service.MaterialService;
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

/**
 * 자료 컨트롤러
 * Base URL: /api/v1/studies/{studyId}/materials
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/studies/{studyId}/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /**
     * 자료 목록 조회
     * GET /api/v1/studies/{studyId}/materials?page=0&size=20&weekNumber=1&type=FILE&keyword=DP
     */
    @GetMapping
    public ResponseEntity<Page<MaterialListResponse>> getMaterials(
            @PathVariable Long studyId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("자료 목록 조회 요청 - studyId: {}, weekNumber: {}, type: {}, keyword: {}",
                studyId, weekNumber, type, keyword);

        MaterialSearchCondition condition = MaterialSearchCondition.builder()
                .weekNumber(weekNumber)
                .type(type)
                .keyword(keyword)
                .build();

        Page<MaterialListResponse> result = materialService.getMaterials(studyId, condition, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * 자료 상세 조회
     * GET /api/v1/studies/{studyId}/materials/{materialId}
     */
    @GetMapping("/{materialId}")
    public ResponseEntity<MaterialDetailResponse> getMaterialDetail(
            @PathVariable Long studyId,
            @PathVariable Long materialId) {

        log.info("자료 상세 조회 요청 - studyId: {}, materialId: {}", studyId, materialId);

        MaterialDetailResponse result = materialService.getMaterialDetail(studyId, materialId);

        return ResponseEntity.ok(result);
    }

    /**
     * 링크 자료 생성
     * POST /api/v1/studies/{studyId}/materials
     * Content-Type: application/json
     */
    @PostMapping
    public ResponseEntity<MaterialCreateResponse> createLinkMaterial(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody MaterialCreateRequest request) {

        log.info("링크 자료 생성 요청 - studyId: {}, userId: {}, title: {}", studyId, userId, request.getTitle());

        MaterialCreateResponse result = materialService.createLinkMaterial(studyId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // TODO: 파일 업로드 API - FileStorageService 구현 후 추가
    // @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<MaterialCreateResponse> uploadFileMaterial(...)

    /**
     * 자료 수정
     * PUT /api/v1/studies/{studyId}/materials/{materialId}
     */
    @PutMapping("/{materialId}")
    public ResponseEntity<Void> updateMaterial(
            @PathVariable Long studyId,
            @PathVariable Long materialId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody MaterialUpdateRequest request) {

        log.info("자료 수정 요청 - studyId: {}, materialId: {}, userId: {}", studyId, materialId, userId);

        materialService.updateMaterial(studyId, materialId, userId, request);

        return ResponseEntity.ok().build();
    }

    /**
     * 자료 삭제
     * DELETE /api/v1/studies/{studyId}/materials/{materialId}
     */
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long studyId,
            @PathVariable Long materialId,
            @RequestHeader("User-Id") Long userId,
            @RequestHeader(value = "Is-Leader", defaultValue = "false") Boolean isLeader) {

        log.info("자료 삭제 요청 - studyId: {}, materialId: {}, userId: {}, isLeader: {}",
                studyId, materialId, userId, isLeader);

        materialService.deleteMaterial(studyId, materialId, userId, isLeader);

        return ResponseEntity.noContent().build();
    }
}