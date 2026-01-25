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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsScraperService {

    private final ItNewsRepository itNewsRepository;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * Google News IT 뉴스 RSS (차단 없음!)
     */
    @Transactional
    public void scrapeGoogleNewsIT() {
        log.info("=== Google News IT 뉴스 크롤링 시작 ===");

        try {
            // Google News IT 검색 RSS
            String url = "https://news.google.com/rss/search?q=IT+OR+인공지능+OR+개발자+OR+프로그래밍&hl=ko&gl=KR&ceid=KR:ko";

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .parser(org.jsoup.parser.Parser.xmlParser())
                    .get();

            log.info("Google News RSS 접속 성공");

            Elements items = doc.select("item");

            log.info("찾은 뉴스 개수: {}", items.size());

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

                    // 중복 체크
                    if (itNewsRepository.existsBySourceUrl(link)) {
                        log.debug("이미 존재: {}", title);
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

                    // 출처 추출 (Google News는 여러 언론사 통합)
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

                    log.info("뉴스 저장 [{}]: {}", savedCount, title);

                    // 10개 저장
                    if (savedCount >= 10) {
                        break;
                    }

                } catch (Exception e) {
                    log.error("개별 뉴스 처리 에러", e);
                }
            }

            log.info("=== Google News 크롤링 완료: {}개 저장 ===", savedCount);

        } catch (Exception e) {
            log.error("Google News RSS 크롤링 실패", e);
            throw new RuntimeException("뉴스 크롤링 중 오류 발생", e);
        }
    }

    /**
     * 한겨레 IT 뉴스 RSS (국내 언론사, 차단 없음)
     */
    @Transactional
    public void scrapeHankyorehIT() {
        log.info("=== 한겨레 IT 뉴스 크롤링 시작 ===");

        try {
            String url = "https://www.hani.co.kr/rss/";

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .parser(org.jsoup.parser.Parser.xmlParser())
                    .get();

            log.info("한겨레 RSS 접속 성공");

            Elements items = doc.select("item");

            log.info("찾은 뉴스 개수: {}", items.size());

            int savedCount = 0;

            for (Element item : items) {
                try {
                    String title = item.selectFirst("title").text();
                    String link = item.selectFirst("link").text();

                    // IT 관련 키워드 필터링
                    if (!isITRelated(title)) {
                        continue;
                    }

                    if (itNewsRepository.existsBySourceUrl(link)) {
                        continue;
                    }

                    Element descElement = item.selectFirst("description");
                    String description = descElement != null ? descElement.text() : "";

                    if (description.contains("<")) {
                        description = Jsoup.parse(description).text();
                    }

                    if (description.length() > 500) {
                        description = description.substring(0, 497) + "...";
                    }

                    ItNews news = ItNews.builder()
                            .title(title)
                            .summary(description)
                            .sourceUrl(link)
                            .sourceName("한겨레")
                            .category("IT")
                            .publishedAt(LocalDateTime.now())
                            .build();

                    itNewsRepository.save(news);
                    savedCount++;

                    log.info("뉴스 저장 [{}]: {}", savedCount, title);

                    if (savedCount >= 5) {
                        break;
                    }

                } catch (Exception e) {
                    log.error("뉴스 처리 에러", e);
                }
            }

            log.info("=== 한겨레 크롤링 완료: {}개 저장 ===", savedCount);

        } catch (Exception e) {
            log.error("한겨레 RSS 크롤링 실패", e);
        }
    }

    /**
     * IT 관련 키워드 체크
     */
    private boolean isITRelated(String title) {
        String[] keywords = {
                "IT", "AI", "인공지능", "개발", "프로그래밍", "소프트웨어",
                "앱", "데이터", "클라우드", "보안", "해킹", "스타트업",
                "테크", "기술", "디지털", "컴퓨터", "코딩", "웹"
        };

        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 다중 RSS 크롤링
     */
    @Transactional
    public void scrapeAllNews() {
        log.info("=== 전체 뉴스 크롤링 시작 ===");

        try {
            scrapeGoogleNewsIT();
        } catch (Exception e) {
            log.error("Google News 크롤링 실패", e);
        }

        try {
            scrapeHankyorehIT();
        } catch (Exception e) {
            log.error("한겨레 크롤링 실패", e);
        }

        log.info("=== 전체 뉴스 크롤링 완료 ===");
    }
}