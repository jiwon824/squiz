package com.ssafy.domain.study.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyCommentPageResponse {

    /**
     * 댓글 목록
     */
    private List<StudyCommentResponse> comments;

    /**
     * 전체 댓글 개수
     */
    private Long totalElements;

    /**
     * 전체 페이지 수
     */
    private Integer totalPages;

    /**
     * 현재 페이지 번호
     */
    private Integer currentPage;

    /**
     * 다음 페이지 존재 여부
     */
    private Boolean hasNext;

    /**
     * 이전 페이지 존재 여부
     */
    private Boolean hasPrevious;

    /**
     * 페이징 정보와 함께 생성
     */
    public static StudyCommentPageResponse of(
            List<StudyCommentResponse> comments,
            Long totalElements,
            Integer totalPages,
            Integer currentPage,
            Boolean hasNext,
            Boolean hasPrevious
    ) {
        return StudyCommentPageResponse.builder()
                .comments(comments)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
