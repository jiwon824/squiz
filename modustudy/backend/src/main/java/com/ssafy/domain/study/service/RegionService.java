package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    /**
     * 모든 시/도 목록 조회
     */
    public List<RegionResponse.Detail> getAllProvinces() {
        return regionRepository.findAllProvinces().stream()
                .map(RegionResponse.Detail::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 시/도 목록 조회 (드롭다운용)
     */
    public List<RegionResponse.SelectOption> getProvinceOptions() {
        return regionRepository.findAllProvinces().stream()
                .map(RegionResponse.SelectOption::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시/도의 하위 지역 목록 조회
     */
    public List<RegionResponse.Detail> getDistrictsByProvinceId(Long provinceId) {
        return regionRepository.findByParentId(provinceId).stream()
                .map(RegionResponse.Detail::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시/도 코드의 하위 지역 목록 조회
     */
    public List<RegionResponse.Detail> getDistrictsByProvinceCode(String provinceCode) {
        return regionRepository.findByParentCode(provinceCode).stream()
                .map(RegionResponse.Detail::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시/도의 하위 지역 목록 조회 (드롭다운용)
     */
    public List<RegionResponse.SelectOption> getDistrictOptions(Long provinceId) {
        return regionRepository.findByParentId(provinceId).stream()
                .map(RegionResponse.SelectOption::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 시/도 + 하위 지역 계층 조회
     */
    public List<RegionResponse.ProvinceWithDistricts> getAllProvincesWithDistricts() {
        return regionRepository.findAllProvincesWithChildren().stream()
                .map(RegionResponse.ProvinceWithDistricts::from)
                .collect(Collectors.toList());
    }

    /**
     * 지역 ID로 조회
     */
    public RegionResponse.Detail getRegionById(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new RegionNotFoundException(id));
        return RegionResponse.Detail.from(region);
    }

    /**
     * 지역 코드로 조회
     */
    public RegionResponse.Detail getRegionByCode(String code) {
        Region region = regionRepository.findByCode(code)
                .orElseThrow(() -> new RegionNotFoundException(code));
        return RegionResponse.Detail.from(region);
    }

    /**
     * 지역명 검색
     */
    public List<RegionResponse.Detail> searchRegions(String keyword) {
        return regionRepository.searchByKeyword(keyword).stream()
                .map(RegionResponse.Detail::from)
                .collect(Collectors.toList());
    }

    /**
     * 지역 엔티티 조회 (내부용)
     */
    public Region getRegionEntityById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new RegionNotFoundException(id));
    }

    /**
     * 지역 엔티티 조회 (내부용, 코드로)
     */
    public Region getRegionEntityByCode(String code) {
        return regionRepository.findByCode(code)
                .orElseThrow(() -> new RegionNotFoundException(code));
    }

    // ===== Exception =====

    public static class RegionNotFoundException extends NotFoundException {
        public RegionNotFoundException(Long id) {
            super("REGION_NOT_FOUND", "지역을 찾을 수 없습니다: " + id);
        }

        public RegionNotFoundException(String code) {
            super("REGION_NOT_FOUND", "지역을 찾을 수 없습니다: " + code);
        }
    }
}