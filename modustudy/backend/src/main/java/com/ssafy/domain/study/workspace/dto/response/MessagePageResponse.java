package com.ssafy.domain.study.workspace.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessagePageResponse {

    private List<MessageResponse> messages;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private boolean hasNext;
    private boolean hasPrevious;

    public static MessagePageResponse from(Page<MessageResponse> page) {
        return MessagePageResponse.builder()
                .messages(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}