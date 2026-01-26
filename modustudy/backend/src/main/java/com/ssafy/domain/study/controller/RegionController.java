package com.ssafy.domain.study.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Region", description = "지역 관리 API")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "모든 시/도 목록 조회", description = "전국 시/도 목록을 조회합니다.")
    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<RegionResponse.Detail>>> getAllProvinces() {
        List<RegionResponse.Detail> provinces = regionService.getAllProvinces();
        return ResponseEntity.ok(ApiResponse.success(provinces));
    }

    @Operation(summary = "시/도 드롭다운 옵션 조회", description = "드롭다운 선택용 시/도 목록을 조회합니다.")
    @GetMapping("/provinces/options")
    public ResponseEntity<ApiResponse<List<RegionResponse.SelectOption>>> getProvinceOptions() {
        List<RegionResponse.SelectOption> options = regionService.getProvinceOptions();
        return ResponseEntity.ok(ApiResponse.success(options));
    }

    @Operation(summary = "시/도 + 하위 지역 전체 조회", description = "모든 시/도와 하위 시/군/구를 계층 구조로 조회합니다.")
    @GetMapping("/hierarchy")
    public ResponseEntity<ApiResponse<List<RegionResponse.ProvinceWithDistricts>>> getAllProvincesWithDistricts() {
        List<RegionResponse.ProvinceWithDistricts> hierarchy = regionService.getAllProvincesWithDistricts();
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    @Operation(summary = "특정 시/도의 시/군/구 목록 조회", description = "특정 시/도에 속한 시/군/구 목록을 조회합니다.")
    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<ApiResponse<List<RegionResponse.Detail>>> getDistrictsByProvinceId(
            @Parameter(description = "시/도 ID") @PathVariable Long provinceId) {
        List<RegionResponse.Detail> districts = regionService.getDistrictsByProvinceId(provinceId);
        return ResponseEntity.ok(ApiResponse.success(districts));
    }

    @Operation(summary = "특정 시/도의 시/군/구 드롭다운 옵션 조회", description = "드롭다운 선택용 시/군/구 목록을 조회합니다.")
    @GetMapping("/provinces/{provinceId}/districts/options")
    public ResponseEntity<ApiResponse<List<RegionResponse.SelectOption>>> getDistrictOptions(
            @Parameter(description = "시/도 ID") @PathVariable Long provinceId) {
        List<RegionResponse.SelectOption> options = regionService.getDistrictOptions(provinceId);
        return ResponseEntity.ok(ApiResponse.success(options));
    }

    @Operation(summary = "지역 상세 조회 (ID)", description = "지역 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegionResponse.Detail>> getRegionById(
            @Parameter(description = "지역 ID") @PathVariable Long id) {
        RegionResponse.Detail region = regionService.getRegionById(id);
        return ResponseEntity.ok(ApiResponse.success(region));
    }

    @Operation(summary = "지역 상세 조회 (코드)", description = "지역 코드로 상세 정보를 조회합니다.")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<RegionResponse.Detail>> getRegionByCode(
            @Parameter(description = "지역 코드 (예: SEOUL, GYEONGBUK_GUMI)") @PathVariable String code) {
        RegionResponse.Detail region = regionService.getRegionByCode(code);
        return ResponseEntity.ok(ApiResponse.success(region));
    }

    @Operation(summary = "지역 검색", description = "지역명으로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RegionResponse.Detail>>> searchRegions(
            @Parameter(description = "검색 키워드") @RequestParam String keyword) {
        List<RegionResponse.Detail> results = regionService.searchRegions(keyword);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}