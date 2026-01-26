package com.ssafy.domain.news.service;

import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsScraperService {

    private final ItNewsRepository itNewsRepository;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * Google News IT 뉴스 크롤링 (여러 키워드)
     */
    public void scrapeGoogleNewsIT() {
        log.info("========================================");
        log.info("Google News IT 뉴스 크롤링 시작");
        log.info("========================================");

        // 다양한 IT 키워드 목록
        List<String> keywords = Arrays.asList(
                "IT+기술",
                "인공지능+AI",
                "빅데이터",
                "클라우드+컴퓨팅",
                "사이버보안",
                "소프트웨어+개발",
                "프론트엔드+개발",
                "백엔드+개발",
                "DevOps",
                "머신러닝",
                "블록체인",
                "코딩테스트",
                "개발자+채용",
                "스타트업+기술",
                "자바스크립트",
                "파이썬+프로그래밍",
                "쿠버네티스+Docker",
                "데이터사이언스",
                "모바일+앱개발",
                "웹개발"
        );

        int totalSaved = 0;
        int successCount = 0;
        int failCount = 0;

        for (String keyword : keywords) {
            try {
                int saved = scrapeByKeywordWithTransaction(keyword);
                totalSaved += saved;
                successCount++;

                log.info("[{}] 키워드로 {}개 뉴스 저장", keyword, saved);

                // 서버 부하 방지를 위한 대기 (1초)
                Thread.sleep(1000);

            } catch (Exception e) {
                failCount++;
                log.error("키워드 [{}] 크롤링 실패", keyword, e);
            }
        }

        log.info("========================================");
        log.info("크롤링 완료 - 성공: {}/{}, 실패: {}", successCount, keywords.size(), failCount);
        log.info("총 {}개의 새로운 뉴스 저장", totalSaved);
        log.info("========================================");
    }

    /**
     * 트랜잭션 래퍼 메서드
     * 각 키워드별로 독립적인 트랜잭션 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int scrapeByKeywordWithTransaction(String keyword) {
        return scrapeByKeyword(keyword);
    }

    /**
     * 특정 키워드로 Google News 크롤링
     */
    private int scrapeByKeyword(String keyword) {
        try {
            String url = String.format(
                    "https://news.google.com/rss/search?q=%s&hl=ko&gl=KR&ceid=KR:ko",
                    keyword
            );

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .parser(org.jsoup.parser.Parser.xmlParser())
                    .get();

            Elements items = doc.select("item");
            int savedCount = 0;

            for (Element item : items) {
                try {
                    // 제목
                    Element titleElement = item.selectFirst("title");
                    if (titleElement == null) continue;
                    String title = titleElement.text();

                    // 링크
                    Element linkElement = item.selectFirst("link");
                    if (linkElement == null) continue;
                    String link = linkElement.text();

                    // 중복 체크 - 이제 트랜잭션 안에서 정상 작동!
                    // 중복 체크 부분
                    if (itNewsRepository.existsBySourceUrl(link)) {
                        log.info("🔴 중복 뉴스 발견! URL: {}", link);
                        continue;
                    }

                    // 설명
                    Element descElement = item.selectFirst("description");
                    String description = descElement != null ? descElement.text() : "";

                    if (description.contains("<")) {
                        description = Jsoup.parse(description).text();
                    }

                    if (description.length() > 500) {
                        description = description.substring(0, 497) + "...";
                    }

                    // 발행일
                    LocalDateTime publishedAt = LocalDateTime.now();
                    Element pubDateElement = item.selectFirst("pubDate");
                    if (pubDateElement != null) {
                        try {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(
                                    pubDateElement.text(),
                                    DateTimeFormatter.RFC_1123_DATE_TIME
                            );
                            publishedAt = zonedDateTime.toLocalDateTime();
                        } catch (Exception e) {
                            log.debug("날짜 파싱 실패, 현재 시간 사용");
                        }
                    }

                    // 출처 추출
                    String sourceName = "Google News";
                    Element sourceElement = item.selectFirst("source");
                    if (sourceElement != null) {
                        sourceName = sourceElement.text();
                    }

                    // DB 저장
                    ItNews news = ItNews.builder()
                            .title(title)
                            .summary(description)
                            .sourceUrl(link)
                            .sourceName(sourceName)
                            .category("IT")
                            .publishedAt(publishedAt)
                            .build();

                    itNewsRepository.save(news);
                    savedCount++;

                    // 키워드당 최대 5개씩만 저장
                    if (savedCount >= 5) {
                        break;
                    }

                } catch (Exception e) {
                    log.error("개별 뉴스 처리 에러", e);
                }
            }

            return savedCount;

        } catch (Exception e) {
            log.error("키워드 [{}] RSS 크롤링 실패", keyword, e);
            return 0; // 예외 발생 시 0 반환 (throw 하지 않음)
        }
    }

}