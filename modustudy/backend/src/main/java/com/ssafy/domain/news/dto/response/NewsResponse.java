package com.ssafy.domain.news.dto.response;

import com.ssafy.domain.news.entity.ItNews;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NewsResponse {

    private Long id;
    private String title;
    private String summary;
    private String sourceUrl;
    private String sourceName;
    private String thumbnailUrl;
    private String category;
    private LocalDateTime publishedAt;
    private Integer viewCount;

    /**
     * Entity → DTO 변환
     */
    public static NewsResponse from(ItNews news) {
        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .sourceUrl(news.getSourceUrl())
                .sourceName(news.getSourceName())
                .thumbnailUrl(news.getThumbnailUrl())
                .category(news.getCategory())
                .publishedAt(news.getPublishedAt())
                .viewCount(news.getViewCount())
                .build();
    }
}