package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.study.entity.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardPostSummaryResponse {
    private Long id;
    private Long studyId;
    private String studyName;
    private String topicName;
    private String title;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private int currentMembers;
    private Integer maxMembers;
    private int viewCount;
    private LocalDateTime createdAt;
    private Status studyStatus;

    public static BoardPostSummaryResponse from(
            BoardPost post,
            int currentMembers,
            Status studyStatus
    ) {
        return BoardPostSummaryResponse.builder()
                .id(post.getId())
                .studyId(post.getStudy().getId())
                .studyName(post.getStudy().getName())
                .topicName(post.getStudy().getTopicName())
                .title(post.getTitle())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getNickname())
                .authorProfileImage(post.getAuthor().getProfileImage())
                .currentMembers(currentMembers)
                .maxMembers(post.getStudy().getMaxMembers())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .studyStatus(studyStatus)
                .build();
    }
}
