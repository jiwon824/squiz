package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Topic;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicResponse {

    private Long id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private List<TopicResponse> children;  // 소분류 목록 (대분류인 경우)

    /**
     * Topic Entity → Response (소분류 포함)
     */
    public static TopicResponse from(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .icon(topic.getIcon())
                .sortOrder(topic.getSortOrder())
                .children(null)  // 필요 시 별도로 설정
                .build();
    }

    /**
     * Topic Entity → Response (대분류 + 소분류 포함)
     */
    public static TopicResponse fromWithChildren(Topic topic, List<Topic> children) {
        List<TopicResponse> childResponses = children != null
                ? children.stream().map(TopicResponse::from).toList()
                : null;

        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .icon(topic.getIcon())
                .sortOrder(topic.getSortOrder())
                .children(childResponses)
                .build();
    }

    /**
     * Topic Entity → Response (엔티티의 children 사용)
     */
    public static TopicResponse withChildren(Topic topic) {
        List<TopicResponse> childResponses = topic.getChildren() != null && !topic.getChildren().isEmpty()
                ? topic.getChildren().stream().map(TopicResponse::from).toList()
                : null;

        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .icon(topic.getIcon())
                .sortOrder(topic.getSortOrder())
                .children(childResponses)
                .build();
    }
}
