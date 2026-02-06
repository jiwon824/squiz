package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.TopicResponse;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Topic 서비스
 */
 @Service
 @RequiredArgsConstructor
 @Transactional(readOnly = true)
 public class TopicService {

    private final TopicRepository topicRepository;

    /**
     * 모든 주제 조회 (대분류만)
     */
    public List<TopicResponse> getAllParentTopics() {
        List<Topic> parents = topicRepository.findByParentIsNullOrderBySortOrderAsc();
        return parents.stream()
                .map(TopicResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 주제 조회 (계층 구조)
     */
    public List<TopicResponse> getAllTopicsWithChildren() {
        List<Topic> parents = topicRepository.findByParentIsNullOrderBySortOrderAsc();
        return parents.stream()
                .map(TopicResponse::withChildren)
                .collect(Collectors.toList());
    }

    /**
     * 특정 대분류의 소분류 목록 조회
     */
    public List<TopicResponse> getChildTopics(Long parentId) {
        List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(parentId);
        return children.stream()
                .map(TopicResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 주제 단건 조회
     */
    public TopicResponse getTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제입니다: " + topicId));
        return TopicResponse.withChildren(topic);
    }

    /**
     * 주제 존재 여부 확인
     */
    public boolean exists(Long topicId) {
        return topicRepository.existsById(topicId);
    }

    /**
     * 주제 엔티티 조회 (내부용)
     */
    public Topic getTopicEntity(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제입니다: " + topicId));
    }
}
