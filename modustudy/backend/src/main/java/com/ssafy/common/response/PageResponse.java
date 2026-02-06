package com.ssafy.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@Schema(description = "페이징 응답")
public class PageResponse<T> {

    @Schema(description = "응답 코드", example = "200")
    private final int status;

    @Schema(description = "응답 메시지", example = "Success")
    private final String message;

    @Schema(description = "데이터 목록")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private final int page;

    @Schema(description = "페이지 크기", example = "10")
    private final int size;

    @Schema(description = "전체 요소 수", example = "100")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "10")
    private final int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private final boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private final boolean last;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .status(200)
                .message("Success")
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public static <T, U> PageResponse<U> of(Page<T> page, List<U> content) {
        return PageResponse.<U>builder()
                .status(200)
                .message("Success")
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
