package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Format;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormatResponse {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;

    /**
     * Format Entity → Response
     */
    public static FormatResponse from(Format format) {
        return FormatResponse.builder()
                .id(format.getId())
                .name(format.getName())
                .description(format.getDescription())
                .icon(format.getIcon())
                .sortOrder(format.getSortOrder())
                .build();
    }
}
