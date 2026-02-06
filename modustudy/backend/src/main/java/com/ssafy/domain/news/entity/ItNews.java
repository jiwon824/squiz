package com.ssafy.domain.news.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "it_news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ItNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(length = 100)
    private String sourceName;  // velog, tistory, ZDNet 등

    @Lob
    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(length = 50)
    private String category;  // AI, 백엔드, 프론트엔드 등

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ItNews(String title, String summary, String sourceUrl,
                  String sourceName, String thumbnailUrl, String category,
                  LocalDateTime publishedAt) {
        this.title = title;
        this.summary = summary;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
        this.thumbnailUrl = thumbnailUrl;
        this.category = category;
        this.publishedAt = publishedAt;
    }
    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }

    // 비활성화
    public void deactivate() {
        this.isActive = false;
    }
}
