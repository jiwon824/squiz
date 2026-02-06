package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Topic;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EntityManager entityManager;

    private Topic parentTopic1;
    private Topic parentTopic2;
    private Topic childTopic1;
    private Topic childTopic2;
    private Topic childTopic3;

    @BeforeEach
    void setUp() {
        // 대분류 생성
        parentTopic1 = topicRepository.save(Topic.builder()
                .name("알고리즘/코딩테스트")
                .icon("code")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        parentTopic2 = topicRepository.save(Topic.builder()
                .name("CS 기초")
                .icon("cpu")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        // 소분류 생성 (알고리즘 하위)
        childTopic1 = topicRepository.save(Topic.builder()
                .name("백준")
                .parent(parentTopic1)
                .sortOrder(1)
                .build());

        childTopic2 = topicRepository.save(Topic.builder()
                .name("프로그래머스")
                .parent(parentTopic1)
                .sortOrder(2)
                .build());

        // 소분류 생성 (CS 기초 하위)
        childTopic3 = topicRepository.save(Topic.builder()
                .name("운영체제")
                .parent(parentTopic2)
                .sortOrder(1)
                .build());

        topicRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("대분류 목록 조회 - parent가 null인 주제만 조회")
    void findByParentIsNull_Success() {
        // when
        List<Topic> parents = topicRepository.findByParentIsNullOrderBySortOrderAsc();

        // then
        assertThat(parents).hasSize(2);
        assertThat(parents.get(0).getName()).isEqualTo("알고리즘/코딩테스트");
        assertThat(parents.get(1).getName()).isEqualTo("CS 기초");
        assertThat(parents).allMatch(Topic::isParent);
    }

    @Test
    @DisplayName("소분류 목록 조회 - 특정 대분류의 하위 주제 조회")
    void findByParentId_Success() {
        // when
        List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(parentTopic1.getId());

        // then
        assertThat(children).hasSize(2);
        assertThat(children.get(0).getName()).isEqualTo("백준");
        assertThat(children.get(1).getName()).isEqualTo("프로그래머스");
        assertThat(children).allMatch(Topic::isChild);
    }

    @Test
    @DisplayName("소분류 목록 조회 - 다른 대분류의 소분류")
    void findByParentId_OtherParent() {
        // when
        List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(parentTopic2.getId());

        // then
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("운영체제");
    }

    @Test
    @DisplayName("소분류 목록 조회 - 존재하지 않는 대분류")
    void findByParentId_NotExists() {
        // when
        List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(999L);

        // then
        assertThat(children).isEmpty();
    }

    @Test
    @DisplayName("전체 주제 조회 (부모 포함)")
    void findAllWithParent_Success() {
        // when
        List<Topic> all = topicRepository.findAllWithParent();

        // then
        assertThat(all).hasSize(5);
    }

    @Test
    @DisplayName("대분류인지 확인")
    void isParent_Success() {
        // when
        Topic topic = topicRepository.findById(parentTopic1.getId()).orElseThrow();

        // then
        assertThat(topic.isParent()).isTrue();
        assertThat(topic.isChild()).isFalse();
    }

    @Test
    @DisplayName("소분류인지 확인")
    void isChild_Success() {
        // when
        Topic topic = topicRepository.findById(childTopic1.getId()).orElseThrow();

        // then
        assertThat(topic.isParent()).isFalse();
        assertThat(topic.isChild()).isTrue();
    }
}
