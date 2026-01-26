package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.TopicResponse;
import com.ssafy.domain.study.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 스터디 주제(Topic) 조회 API
 */
@Tag(name = "Topic", description = "스터디 주제 조회 API")
@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    /**
     * 대분류 목록 조회
     */
    @Operation(summary = "대분류 목록 조회", description = "모든 대분류 주제를 조회합니다")
    @GetMapping("/parents")
    public ResponseEntity<List<TopicResponse>> getParentTopics() {
        List<TopicResponse> topics = topicService.getAllParentTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * 전체 주제 목록 조회 (계층 구조)
     */
    @Operation(summary = "전체 주제 목록 조회", description = "모든 주제를 계층 구조로 조회합니다")
    @GetMapping
    public ResponseEntity<List<TopicResponse>> getAllTopics() {
        List<TopicResponse> topics = topicService.getAllTopicsWithChildren();
        return ResponseEntity.ok(topics);
    }

    /**
     * 특정 대분류의 소분류 목록 조회
     */
    @Operation(summary = "소분류 목록 조회", description = "특정 대분류에 속한 소분류 목록을 조회합니다")
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<TopicResponse>> getChildTopics(@PathVariable Long parentId) {
        List<TopicResponse> topics = topicService.getChildTopics(parentId);
        return ResponseEntity.ok(topics);
    }

    /**
     * 주제 단건 조회
     */
    @Operation(summary = "주제 상세 조회", description = "특정 주제의 상세 정보를 조회합니다")
    @GetMapping("/{topicId}")
    public ResponseEntity<TopicResponse> getTopic(@PathVariable Long topicId) {
        TopicResponse topic = topicService.getTopic(topicId);
        return ResponseEntity.ok(topic);
    }
}