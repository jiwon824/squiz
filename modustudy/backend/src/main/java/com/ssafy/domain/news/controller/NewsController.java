package com.ssafy.domain.news.controller;

import com.ssafy.domain.news.dto.response.NewsResponse;
import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final ItNewsRepository itNewsRepository;

    /**
     * 최신 뉴스 목록 조회 (20개)
     */
    @GetMapping
    public ResponseEntity<List<NewsResponse>> getLatestNews() {
        List<ItNews> newsList = itNewsRepository.findTop20ByIsActiveTrueOrderByPublishedAtDesc();

        List<NewsResponse> response = newsList.stream()
                .map(NewsResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 뉴스 상세 조회 (조회수 증가)
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNewsDetail(@PathVariable Long newsId) {
        ItNews news = itNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다."));

        // 조회수 증가
        news.increaseViewCount();
        itNewsRepository.save(news);

        return ResponseEntity.ok(NewsResponse.from(news));
    }

    /**
     * 카테고리별 뉴스 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<NewsResponse>> getNewsByCategory(@PathVariable String category) {
        List<ItNews> newsList = itNewsRepository
                .findByCategoryAndIsActiveTrueOrderByPublishedAtDesc(category);

        List<NewsResponse> response = newsList.stream()
                .map(NewsResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 인기 뉴스 조회 (조회수 높은 순 10개)
     */
    @GetMapping("/popular")
    public ResponseEntity<List<NewsResponse>> getPopularNews() {
        PageRequest pageRequest = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "viewCount"));

        List<ItNews> newsList = itNewsRepository.findAll(pageRequest).getContent();

        List<NewsResponse> response = newsList.stream()
                .map(NewsResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 뉴스 검색 (제목 기준)
     */
    @GetMapping("/search")
    public ResponseEntity<List<NewsResponse>> searchNews(@RequestParam String keyword) {
        List<ItNews> newsList = itNewsRepository
                .findByTitleContainingAndIsActiveTrueOrderByPublishedAtDesc(keyword);

        List<NewsResponse> response = newsList.stream()
                .map(NewsResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
