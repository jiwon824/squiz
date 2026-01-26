package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.FormatResponse;
import com.ssafy.domain.study.service.FormatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class FormatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FormatService formatService;

    private FormatResponse format1;
    private FormatResponse format2;
    private FormatResponse format3;

    @BeforeEach
    void setUp() {
        format1 = FormatResponse.builder()
                .id(1L)
                .name("문제 풀이")
                .description("알고리즘 문제를 함께 풀어요")
                .icon("problem-solving-icon")
                .sortOrder(1)
                .build();

        format2 = FormatResponse.builder()
                .id(2L)
                .name("강의 수강")
                .description("온라인 강의를 함께 수강해요")
                .icon("lecture-icon")
                .sortOrder(2)
                .build();

        format3 = FormatResponse.builder()
                .id(3L)
                .name("책 스터디")
                .description("책을 함께 읽고 토론해요")
                .icon("book-icon")
                .sortOrder(3)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/formats - 전체 형식 목록 조회")
    class GetAllFormatsTest {

        @Test
        @DisplayName("성공 - 형식 목록 반환")
        void success() throws Exception {
            // given
            given(formatService.getAllFormats())
                    .willReturn(Arrays.asList(format1, format2, format3));

            // when & then
            mockMvc.perform(get("/api/v1/formats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("문제 풀이"))
                    .andExpect(jsonPath("$[0].description").value("알고리즘 문제를 함께 풀어요"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("강의 수강"))
                    .andExpect(jsonPath("$[2].id").value(3))
                    .andExpect(jsonPath("$[2].name").value("책 스터디"));
        }

        @Test
        @DisplayName("성공 - 빈 목록")
        void success_EmptyList() throws Exception {
            // given
            given(formatService.getAllFormats())
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/formats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/formats/{formatId} - 형식 단건 조회")
    class GetFormatTest {

        @Test
        @DisplayName("성공 - 형식 정보 반환")
        void success() throws Exception {
            // given
            Long formatId = 1L;
            given(formatService.getFormat(formatId))
                    .willReturn(format1);

            // when & then
            mockMvc.perform(get("/api/v1/formats/{formatId}", formatId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("문제 풀이"))
                    .andExpect(jsonPath("$.description").value("알고리즘 문제를 함께 풀어요"))
                    .andExpect(jsonPath("$.icon").value("problem-solving-icon"))
                    .andExpect(jsonPath("$.sortOrder").value(1));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 형식")
        void fail_NotFound() throws Exception {
            // given
            Long formatId = 999L;
            given(formatService.getFormat(formatId))
                    .willThrow(new IllegalArgumentException("존재하지 않는 형식입니다: " + formatId));

            // when & then
            mockMvc.perform(get("/api/v1/formats/{formatId}", formatId))
                    .andExpect(status().isBadRequest());
        }
    }
}