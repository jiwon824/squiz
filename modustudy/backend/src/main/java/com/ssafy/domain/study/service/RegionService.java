package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    // 시/도 목록 조회
    public List<RegionResponse> getProvinces() {
        return regionRepository.findByLevelOrderBySortOrderAsc(1).stream()
                .map(RegionResponse::from)
                .toList();
    }

    // 시/도 드롭다운 옵션 조회
    public List<RegionResponse.Option> getProvinceOptions() {
        return regionRepository.findByLevelOrderBySortOrderAsc(1).stream()
                .map(RegionResponse.Option::from)
                .toList();
    }

    // 특정 시/도의 시/군/구 목록 조회
    public List<RegionResponse> getDistricts(Long provinceId) {
        return regionRepository.findByParentIdOrderBySortOrderAsc(provinceId).stream()
                .map(RegionResponse::from)
                .toList();
    }

    // 특정 시/도의 시/군/구 드롭다운 옵션 조회
    public List<RegionResponse.Option> getDistrictOptions(Long provinceId) {
        return regionRepository.findByParentIdOrderBySortOrderAsc(provinceId).stream()
                .map(RegionResponse.Option::from)
                .toList();
    }

    // 계층 구조 전체 조회
    public List<RegionResponse.Hierarchy> getHierarchy() {
        return regionRepository.findAllProvincesWithDistricts().stream()
                .map(RegionResponse.Hierarchy::from)
                .toList();
    }

    // ID로 상세 조회
    public RegionResponse getRegionById(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다: " + id));
        return RegionResponse.from(region);
    }

    // 코드로 상세 조회
    public RegionResponse getRegionByCode(String code) {
        Region region = regionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다: " + code));
        return RegionResponse.from(region);
    }

    // 검색
    public List<RegionResponse> search(String keyword) {
        return regionRepository.searchByKeyword(keyword).stream()
                .map(RegionResponse::from)
                .toList();
    }
}
