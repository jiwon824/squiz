package com.ssafy.domain.news.controller;

import com.ssafy.domain.news.service.NewsScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/news")
@RequiredArgsConstructor
public class NewsTestController {

    private final NewsScraperService newsScraperService;

    /**
     * Google News IT 뉴스 크롤링
     */
    @PostMapping("/scrape")
    public String scrapeGoogleNews() {
        newsScraperService.scrapeGoogleNewsIT();
        return "Google News 크롤링 완료! DB를 확인해보세요.";
    }

    /**
     * 한겨레 IT 뉴스 크롤링
     */
    @PostMapping("/scrape/hankyoreh")
    public String scrapeHankyoreh() {
        newsScraperService.scrapeHankyorehIT();
        return "한겨레 크롤링 완료!";
    }

    /**
     * 전체 뉴스 크롤링
     */
    @PostMapping("/scrape/all")
    public String scrapeAll() {
        newsScraperService.scrapeAllNews();
        return "전체 크롤링 완료!";
    }
}