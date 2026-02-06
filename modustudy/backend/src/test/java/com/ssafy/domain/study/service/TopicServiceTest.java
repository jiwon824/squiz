package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.TopicResponse;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private TopicService topicService;

    private Topic parentTopic1;
    private Topic parentTopic2;
    private Topic childTopic1;
    private Topic childTopic2;
    private Topic childTopic3;

    @BeforeEach
    void setUp() {
        // 대분류 1: 알고리즘
        parentTopic1 = Topic.builder()
                .name("알고리즘")
                .icon("algorithm-icon")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(parentTopic1, "id", 1L);

        // 대분류 2: CS
        parentTopic2 = Topic.builder()
                .name("CS")
                .icon("cs-icon")
                .sortOrder(2)
                .build();
        ReflectionTestUtils.setField(parentTopic2, "id", 2L);

        // 소분류 1: 백준 (알고리즘 하위)
        childTopic1 = Topic.builder()
                .name("백준")
                .parent(parentTopic1)
                .icon("baekjoon-icon")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(childTopic1, "id", 11L);

        // 소분류 2: 프로그래머스 (알고리즘 하위)
        childTopic2 = Topic.builder()
                .name("프로그래머스")
                .parent(parentTopic1)
                .icon("programmers-icon")
                .sortOrder(2)
                .build();
        ReflectionTestUtils.setField(childTopic2, "id", 12L);

        // 소분류 3: 운영체제 (CS 하위)
        childTopic3 = Topic.builder()
                .name("운영체제")
                .parent(parentTopic2)
                .icon("os-icon")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(childTopic3, "id", 21L);

        // 대분류에 소분류 연결
        ReflectionTestUtils.setField(parentTopic1, "children", Arrays.asList(childTopic1, childTopic2));
        ReflectionTestUtils.setField(parentTopic2, "children", Collections.singletonList(childTopic3));
    }

    @Nested
    @DisplayName("getAllParentTopics - 대분류 목록 조회")
    class GetAllParentTopicsTest {

        @Test
        @DisplayName("성공 - 대분류 목록 반환")
        void success() {
            // given
            given(topicRepository.findByParentIsNullOrderBySortOrderAsc())
                    .willReturn(Arrays.asList(parentTopic1, parentTopic2));

            // when
            List<TopicResponse> result = topicService.getAllParentTopics();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("알고리즘");
            assertThat(result.get(1).getName()).isEqualTo("CS");
            assertThat(result.get(0).getChildren()).isNull();  // children 미포함
            verify(topicRepository).findByParentIsNullOrderBySortOrderAsc();
        }

        @Test
        @DisplayName("성공 - 빈 목록 반환")
        void success_EmptyList() {
            // given
            given(topicRepository.findByParentIsNullOrderBySortOrderAsc())
                    .willReturn(Collections.emptyList());

            // when
            List<TopicResponse> result = topicService.getAllParentTopics();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllTopicsWithChildren - 전체 주제 계층 조회")
    class GetAllTopicsWithChildrenTest {

        @Test
        @DisplayName("성공 - 대분류 + 소분류 계층 구조 반환")
        void success() {
            // given
            given(topicRepository.findByParentIsNullOrderBySortOrderAsc())
                    .willReturn(Arrays.asList(parentTopic1, parentTopic2));

            // when
            List<TopicResponse> result = topicService.getAllTopicsWithChildren();

            // then
            assertThat(result).hasSize(2);

            // 알고리즘 대분류
            TopicResponse algorithmTopic = result.get(0);
            assertThat(algorithmTopic.getName()).isEqualTo("알고리즘");
            assertThat(algorithmTopic.getChildren()).hasSize(2);
            assertThat(algorithmTopic.getChildren().get(0).getName()).isEqualTo("백준");
            assertThat(algorithmTopic.getChildren().get(1).getName()).isEqualTo("프로그래머스");

            // CS 대분류
            TopicResponse csTopic = result.get(1);
            assertThat(csTopic.getName()).isEqualTo("CS");
            assertThat(csTopic.getChildren()).hasSize(1);
            assertThat(csTopic.getChildren().get(0).getName()).isEqualTo("운영체제");
        }
    }

    @Nested
    @DisplayName("getChildTopics - 소분류 목록 조회")
    class GetChildTopicsTest {

        @Test
        @DisplayName("성공 - 특정 대분류의 소분류 목록 반환")
        void success() {
            // given
            Long parentId = 1L;
            given(topicRepository.findByParentIdOrderBySortOrderAsc(parentId))
                    .willReturn(Arrays.asList(childTopic1, childTopic2));

            // when
            List<TopicResponse> result = topicService.getChildTopics(parentId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("백준");
            assertThat(result.get(1).getName()).isEqualTo("프로그래머스");
            verify(topicRepository).findByParentIdOrderBySortOrderAsc(parentId);
        }

        @Test
        @DisplayName("성공 - 소분류 없는 대분류")
        void success_NoChildren() {
            // given
            Long parentId = 99L;
            given(topicRepository.findByParentIdOrderBySortOrderAsc(parentId))
                    .willReturn(Collections.emptyList());

            // when
            List<TopicResponse> result = topicService.getChildTopics(parentId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTopic - 주제 단건 조회")
    class GetTopicTest {

        @Test
        @DisplayName("성공 - 대분류 조회 (소분류 포함)")
        void success_ParentTopic() {
            // given
            Long topicId = 1L;
            given(topicRepository.findById(topicId))
                    .willReturn(Optional.of(parentTopic1));

            // when
            TopicResponse result = topicService.getTopic(topicId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("알고리즘");
            assertThat(result.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 소분류 조회")
        void success_ChildTopic() {
            // given
            Long topicId = 11L;
            given(topicRepository.findById(topicId))
                    .willReturn(Optional.of(childTopic1));

            // when
            TopicResponse result = topicService.getTopic(topicId);

            // then
            assertThat(result.getId()).isEqualTo(11L);
            assertThat(result.getName()).isEqualTo("백준");
            assertThat(result.getChildren()).isNull();  // 소분류는 children 없음
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주제")
        void fail_NotFound() {
            // given
            Long topicId = 999L;
            given(topicRepository.findById(topicId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> topicService.getTopic(topicId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주제");
        }
    }

    @Nested
    @DisplayName("exists - 주제 존재 여부 확인")
    class ExistsTest {

        @Test
        @DisplayName("존재하는 주제 - true 반환")
        void exists_True() {
            // given
            Long topicId = 1L;
            given(topicRepository.existsById(topicId)).willReturn(true);

            // when
            boolean result = topicService.exists(topicId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 주제 - false 반환")
        void exists_False() {
            // given
            Long topicId = 999L;
            given(topicRepository.existsById(topicId)).willReturn(false);

            // when
            boolean result = topicService.exists(topicId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getTopicEntity - 엔티티 조회")
    class GetTopicEntityTest {

        @Test
        @DisplayName("성공 - 엔티티 반환")
        void success() {
            // given
            Long topicId = 1L;
            given(topicRepository.findById(topicId))
                    .willReturn(Optional.of(parentTopic1));

            // when
            Topic result = topicService.getTopicEntity(topicId);

            // then
            assertThat(result).isEqualTo(parentTopic1);
            assertThat(result.getName()).isEqualTo("알고리즘");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주제")
        void fail_NotFound() {
            // given
            Long topicId = 999L;
            given(topicRepository.findById(topicId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> topicService.getTopicEntity(topicId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주제");
        }
    }
}
