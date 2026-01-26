package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

public class RegionResponse {

    /**
     * 기본 지역 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String code;
        private String name;
        private String fullName;
        private Integer level;
        private Long parentId;

        public static Detail from(Region region) {
            return Detail.builder()
                    .id(region.getId())
                    .code(region.getCode())
                    .name(region.getName())
                    .fullName(region.getFullName())
                    .level(region.getLevel())
                    .parentId(region.getParent() != null ? region.getParent().getId() : null)
                    .build();
        }
    }

    /**
     * 시/도 + 하위 지역 포함 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvinceWithDistricts {
        private Long id;
        private String code;
        private String name;
        private List<District> districts;

        public static ProvinceWithDistricts from(Region province) {
            return ProvinceWithDistricts.builder()
                    .id(province.getId())
                    .code(province.getCode())
                    .name(province.getName())
                    .districts(province.getChildren().stream()
                            .filter(Region::getIsActive)
                            .map(District::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    /**
     * 시/군/구 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class District {
        private Long id;
        private String code;
        private String name;
        private String fullName;

        public static District from(Region district) {
            return District.builder()
                    .id(district.getId())
                    .code(district.getCode())
                    .name(district.getName())
                    .fullName(district.getFullName())
                    .build();
        }
    }

    /**
     * 드롭다운/선택용 간단 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectOption {
        private Long id;
        private String code;
        private String label;

        public static SelectOption from(Region region) {
            return SelectOption.builder()
                    .id(region.getId())
                    .code(region.getCode())
                    .label(region.getName())
                    .build();
        }

        public static SelectOption fromWithFullName(Region region) {
            return SelectOption.builder()
                    .id(region.getId())
                    .code(region.getCode())
                    .label(region.getFullName() != null ? region.getFullName() : region.getName())
                    .build();
        }
    }
}