package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Region;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionResponse {

    private Long id;
    private String code;
    private String name;
    private String fullName;
    private Integer level;
    private Long parentId;

    public static RegionResponse from(Region region) {
        return RegionResponse.builder()
                .id(region.getId())
                .code(region.getCode())
                .name(region.getName())
                .fullName(region.getFullName())
                .level(region.getLevel())
                .parentId(region.getParent() != null ? region.getParent().getId() : null)
                .build();
    }

    // 드롭다운 옵션용 간소화 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Option {
        private Long id;
        private String code;
        private String label;

        public static Option from(Region region) {
            return Option.builder()
                    .id(region.getId())
                    .code(region.getCode())
                    .label(region.getName())
                    .build();
        }
    }

    // 계층 구조 응답용 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Hierarchy {
        private Long id;
        private String code;
        private String name;
        private List<RegionResponse> districts;

        public static Hierarchy from(Region province) {
            List<RegionResponse> districtResponses = province.getChildren().stream()
                    .map(RegionResponse::from)
                    .toList();

            return Hierarchy.builder()
                    .id(province.getId())
                    .code(province.getCode())
                    .name(province.getName())
                    .districts(districtResponses)
                    .build();
        }
    }
}
