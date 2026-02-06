package com.ssafy.domain.news.entity;

import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_bookmark",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_news", columnNames = {"user_id", "news_id"})
        })
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @EntityListeners(AuditingEntityListener.class)
        public class NewsBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private ItNews news;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public NewsBookmark(User user, ItNews news) {
        this.user = user;
        this.news = news;
    }
}
