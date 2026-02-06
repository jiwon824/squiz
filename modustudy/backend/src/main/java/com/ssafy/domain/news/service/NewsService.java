package com.ssafy.domain.news.service;

import com.ssafy.domain.news.dto.response.NewsResponse;
import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final ItNewsRepository itNewsRepository;

    /**
     * 뉴스 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public Page<NewsResponse> getNewsList(Pageable pageable) {
        Page<ItNews> newsPage = itNewsRepository.findAllByOrderByPublishedAtDesc(pageable);
        return newsPage.map(NewsResponse::from);
    }

    /**
     * 뉴스 상세 조회 (조회수 증가)
     */
    @Transactional
    public NewsResponse getNewsDetail(Long newsId) {
        ItNews news = itNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다."));

        // 조회수 증가
        news.increaseViewCount();

        return NewsResponse.from(news);
    }

    /**
     * 인기 뉴스 조회 (조회수 기준)
     */
    @Transactional(readOnly = true)
    public Page<NewsResponse> getPopularNews(Pageable pageable) {
        Page<ItNews> popularNews = itNewsRepository.findAllByOrderByViewCountDescPublishedAtDesc(pageable);
        return popularNews.map(NewsResponse::from);
    }
}
