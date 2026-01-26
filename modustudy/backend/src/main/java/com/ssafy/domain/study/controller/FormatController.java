package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.FormatResponse;
import com.ssafy.domain.study.service.FormatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 스터디 형식(Format) 조회 API
 */
@Tag(name = "Format", description = "스터디 형식 조회 API")
@RestController
@RequestMapping("/api/v1/formats")
@RequiredArgsConstructor
public class FormatController {

    private final FormatService formatService;

    /**
     * 전체 형식 목록 조회
     */
    @Operation(summary = "형식 목록 조회", description = "모든 스터디 형식을 조회합니다")
    @GetMapping
    public ResponseEntity<List<FormatResponse>> getAllFormats() {
        List<FormatResponse> formats = formatService.getAllFormats();
        return ResponseEntity.ok(formats);
    }

    /**
     * 형식 단건 조회
     */
    @Operation(summary = "형식 상세 조회", description = "특정 형식의 상세 정보를 조회합니다")
    @GetMapping("/{formatId}")
    public ResponseEntity<FormatResponse> getFormat(@PathVariable Long formatId) {
        FormatResponse format = formatService.getFormat(formatId);
        return ResponseEntity.ok(format);
    }
}