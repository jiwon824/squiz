package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.TopicResponse;
import com.ssafy.domain.study.service.TopicService;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicService topicService;

    private TopicResponse parentTopic1;
    private TopicResponse parentTopic2;
    private TopicResponse childTopic1;
    private TopicResponse childTopic2;

    @BeforeEach
    void setUp() {
        // 소분류
        childTopic1 = TopicResponse.builder()
                .id(11L)
                .name("백준")
                .icon("baekjoon-icon")
                .sortOrder(1)
                .children(null)
                .build();

        childTopic2 = TopicResponse.builder()
                .id(12L)
                .name("프로그래머스")
                .icon("programmers-icon")
                .sortOrder(2)
                .children(null)
                .build();

        // 대분류 (소분류 미포함)
        parentTopic1 = TopicResponse.builder()
                .id(1L)
                .name("알고리즘")
                .icon("algorithm-icon")
                .sortOrder(1)
                .children(null)
                .build();

        parentTopic2 = TopicResponse.builder()
                .id(2L)
                .name("CS")
                .icon("cs-icon")
                .sortOrder(2)
                .children(null)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/topics/parents - 대분류 목록 조회")
    class GetParentTopicsTest {

        @Test
        @DisplayName("성공 - 대분류 목록 반환")
        void success() throws Exception {
            // given
            given(topicService.getAllParentTopics())
                    .willReturn(Arrays.asList(parentTopic1, parentTopic2));

            // when & then
            mockMvc.perform(get("/api/v1/topics/parents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("알고리즘"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("CS"));
        }

        @Test
        @DisplayName("성공 - 빈 목록")
        void success_EmptyList() throws Exception {
            // given
            given(topicService.getAllParentTopics())
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/topics/parents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/topics - 전체 주제 목록 조회")
    class GetAllTopicsTest {

        @Test
        @DisplayName("성공 - 계층 구조로 반환")
        void success() throws Exception {
            // given
            TopicResponse parentWithChildren = TopicResponse.builder()
                    .id(1L)
                    .name("알고리즘")
                    .icon("algorithm-icon")
                    .sortOrder(1)
                    .children(Arrays.asList(childTopic1, childTopic2))
                    .build();

            given(topicService.getAllTopicsWithChildren())
                    .willReturn(Collections.singletonList(parentWithChildren));

            // when & then
            mockMvc.perform(get("/api/v1/topics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("알고리즘"))
                    .andExpect(jsonPath("$[0].children", hasSize(2)))
                    .andExpect(jsonPath("$[0].children[0].name").value("백준"))
                    .andExpect(jsonPath("$[0].children[1].name").value("프로그래머스"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/topics/{parentId}/children - 소분류 목록 조회")
    class GetChildTopicsTest {

        @Test
        @DisplayName("성공 - 소분류 목록 반환")
        void success() throws Exception {
            // given
            Long parentId = 1L;
            given(topicService.getChildTopics(parentId))
                    .willReturn(Arrays.asList(childTopic1, childTopic2));

            // when & then
            mockMvc.perform(get("/api/v1/topics/{parentId}/children", parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(11))
                    .andExpect(jsonPath("$[0].name").value("백준"))
                    .andExpect(jsonPath("$[1].id").value(12))
                    .andExpect(jsonPath("$[1].name").value("프로그래머스"));
        }

        @Test
        @DisplayName("성공 - 소분류 없음")
        void success_NoChildren() throws Exception {
            // given
            Long parentId = 99L;
            given(topicService.getChildTopics(parentId))
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/topics/{parentId}/children", parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/topics/{topicId} - 주제 단건 조회")
    class GetTopicTest {

        @Test
        @DisplayName("성공 - 대분류 조회")
        void success_ParentTopic() throws Exception {
            // given
            Long topicId = 1L;
            TopicResponse parentWithChildren = TopicResponse.builder()
                    .id(1L)
                    .name("알고리즘")
                    .icon("algorithm-icon")
                    .sortOrder(1)
                    .children(Arrays.asList(childTopic1, childTopic2))
                    .build();

            given(topicService.getTopic(topicId))
                    .willReturn(parentWithChildren);

            // when & then
            mockMvc.perform(get("/api/v1/topics/{topicId}", topicId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("알고리즘"))
                    .andExpect(jsonPath("$.children", hasSize(2)));
        }

        @Test
        @DisplayName("성공 - 소분류 조회")
        void success_ChildTopic() throws Exception {
            // given
            Long topicId = 11L;
            given(topicService.getTopic(topicId))
                    .willReturn(childTopic1);

            // when & then
            mockMvc.perform(get("/api/v1/topics/{topicId}", topicId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(11))
                    .andExpect(jsonPath("$.name").value("백준"))
                    .andExpect(jsonPath("$.children").doesNotExist());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주제")
        void fail_NotFound() throws Exception {
            // given
            Long topicId = 999L;
            given(topicService.getTopic(topicId))
                    .willThrow(new IllegalArgumentException("존재하지 않는 주제입니다: " + topicId));

            // when & then
            mockMvc.perform(get("/api/v1/topics/{topicId}", topicId))
                    .andExpect(status().isBadRequest());
        }
    }
}
