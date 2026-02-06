package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Region", description = "지역 조회 API")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "시/도 목록 조회")
    @GetMapping("/provinces")
    public ResponseEntity<List<RegionResponse>> getProvinces() {
        return ResponseEntity.ok(regionService.getProvinces());
    }

    @Operation(summary = "시/도 드롭다운 옵션 조회")
    @GetMapping("/provinces/options")
    public ResponseEntity<List<RegionResponse.Option>> getProvinceOptions() {
        return ResponseEntity.ok(regionService.getProvinceOptions());
    }

    @Operation(summary = "시/도 + 하위 지역 계층 구조 조회")
    @GetMapping("/hierarchy")
    public ResponseEntity<List<RegionResponse.Hierarchy>> getHierarchy() {
        return ResponseEntity.ok(regionService.getHierarchy());
    }

    @Operation(summary = "특정 시/도의 시/군/구 목록 조회")
    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<List<RegionResponse>> getDistricts(@PathVariable Long provinceId) {
        return ResponseEntity.ok(regionService.getDistricts(provinceId));
    }

    @Operation(summary = "특정 시/도의 시/군/구 드롭다운 옵션 조회")
    @GetMapping("/provinces/{provinceId}/districts/options")
    public ResponseEntity<List<RegionResponse.Option>> getDistrictOptions(@PathVariable Long provinceId) {
        return ResponseEntity.ok(regionService.getDistrictOptions(provinceId));
    }

    @Operation(summary = "지역 상세 조회 (ID)")
    @GetMapping("/{id}")
    public ResponseEntity<RegionResponse> getRegionById(@PathVariable Long id) {
        return ResponseEntity.ok(regionService.getRegionById(id));
    }

    @Operation(summary = "지역 상세 조회 (코드)")
    @GetMapping("/code/{code}")
    public ResponseEntity<RegionResponse> getRegionByCode(@PathVariable String code) {
        return ResponseEntity.ok(regionService.getRegionByCode(code));
    }

    @Operation(summary = "지역 검색")
    @GetMapping("/search")
    public ResponseEntity<List<RegionResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(regionService.search(keyword));
    }
}
