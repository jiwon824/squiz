package com.ssafy.domain.news.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.news-url}")
    private String newsApiUrl;

    /**
     * 네이버 뉴스 API 테스트
     * GET /api/news/test?keyword=Spring Boot
     */
    @GetMapping("/test")
    public ResponseEntity<String> testNaverNews(
            @RequestParam(defaultValue = "Spring Boot") String keyword
    ) {
        try {
            // 환경변수 확인 로그
            log.info("=== 네이버 뉴스 API 테스트 시작 ===");
            log.info("Client ID 앞 4자리: {}", clientId.substring(0, Math.min(4, clientId.length())));
            log.info("검색 키워드: {}", keyword);

            // 1. 키워드 인코딩
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            // 2. URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                    .queryParam("query", encodedKeyword)
                    .queryParam("display", 5)  // 5개만 조회
                    .queryParam("start", 1)
                    .queryParam("sort", "date")  // 최신순
                    .build(false)
                    .toUriString();

            log.info("API 호출 URL: {}", url);

            // 3. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 4. API 호출
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("응답 상태: {}", response.getStatusCode());
            log.info("=== 네이버 뉴스 API 테스트 성공 ===");

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("=== 네이버 뉴스 API 테스트 실패 ===");
            log.error("에러 메시지: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("API 호출 실패: " + e.getMessage());
        }
    }
}
