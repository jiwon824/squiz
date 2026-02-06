package com.ssafy.domain.news.controller;

import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItNewsRepository itNewsRepository;

    @BeforeEach
    void setUp() {
        itNewsRepository.deleteAll();

        // 테스트 데이터 생성
        for (int i = 1; i <= 5; i++) {
            ItNews news = ItNews.builder()
                    .title("테스트 뉴스 " + i)
                    .sourceUrl("https://test.com/news/" + i)
                    .sourceName("테스트 언론사")
                    .category("IT")
                    .summary("테스트 요약 " + i)
                    .publishedAt(LocalDateTime.now().minusHours(i))
                    .build();

            // 조회수 설정
            for (int j = 0; j < i * 10; j++) {
                news.increaseViewCount();
            }

            itNewsRepository.save(news);
        }
    }

    @Test
    @DisplayName("최신 뉴스 목록 조회")
    void getLatestNews() throws Exception {
        mockMvc.perform(get("/api/news"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("테스트 뉴스 1"));
    }

    @Test
    @DisplayName("뉴스 상세 조회 및 조회수 증가")
    void getNewsDetail() throws Exception {
        // given
        ItNews news = itNewsRepository.findAll().get(0);
        int beforeViewCount = news.getViewCount();

        // when & then
        mockMvc.perform(get("/api/news/{newsId}", news.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(news.getId()))
                .andExpect(jsonPath("$.viewCount").value(beforeViewCount + 1));
    }

    @Test
    @DisplayName("인기 뉴스 조회")
    void getPopularNews() throws Exception {
        mockMvc.perform(get("/api/news/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("테스트 뉴스 5")) // 조회수 50
                .andExpect(jsonPath("$[0].viewCount").value(50));
    }

    @Test
    @DisplayName("뉴스 검색")
    void searchNews() throws Exception {
        mockMvc.perform(get("/api/news/search")
                        .param("keyword", "뉴스 3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("테스트 뉴스 3"));
    }
}
