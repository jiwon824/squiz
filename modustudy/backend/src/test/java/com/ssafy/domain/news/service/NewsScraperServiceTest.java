package com.ssafy.domain.news.service;

import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NewsScraperServiceTest {

    @Autowired
    private NewsScraperService newsScraperService;

    @Autowired
    private ItNewsRepository itNewsRepository;

    @BeforeEach
    void setUp() {
        itNewsRepository.deleteAll();
    }

    @Test
    @DisplayName("Google News IT 뉴스 크롤링 테스트")
    void scrapeGoogleNewsIT() {
        // given
        long beforeCount = itNewsRepository.count();
        assertThat(beforeCount).isEqualTo(0);

        // when
        newsScraperService.scrapeGoogleNewsIT();

        // then
        long afterCount = itNewsRepository.count();
        assertThat(afterCount).isGreaterThan(0);

        List<ItNews> newsList = itNewsRepository.findAll();
        assertThat(newsList).isNotEmpty();

        ItNews firstNews = newsList.get(0);
        assertThat(firstNews.getTitle()).isNotNull();
        assertThat(firstNews.getSourceUrl()).isNotNull();
        assertThat(firstNews.getCategory()).isEqualTo("IT");

    }

    @Test
    @DisplayName("중복 뉴스 저장 방지 테스트")
    void preventDuplicateNews() {
        // given - 테스트용 뉴스 생성
        ItNews testNews = ItNews.builder()
                .title("테스트 뉴스")
                .sourceUrl("https://test-url-12345.com")
                .sourceName("테스트")
                .category("IT")
                .publishedAt(LocalDateTime.now())
                .build();

        itNewsRepository.save(testNews);

        // when - 중복 체크
        boolean exists = itNewsRepository.existsBySourceUrl("https://test-url-12345.com");

        // then
        assertThat(exists).isTrue();
        assertThat(itNewsRepository.count()).isEqualTo(1);

}
}

