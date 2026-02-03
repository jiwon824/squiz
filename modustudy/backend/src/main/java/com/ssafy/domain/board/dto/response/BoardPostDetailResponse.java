package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.StudyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardPostDetailResponse {
    private Long id;
    private Long studyId;
    private String studyName;
    private String topicName;
    private StudyType studyType;
    private MeetingType meetingType;
    private Integer maxMembers;
    private int currentMembers;
    private Status studyStatus;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BoardCommentResponse> comments;

    public static BoardPostDetailResponse from(
            BoardPost post,
            int currentMembers,
            Status studyStatus,
            List<BoardCommentResponse> comments
    ) {
        return BoardPostDetailResponse.builder()
                .id(post.getId())
                .studyId(post.getStudy().getId())
                .studyName(post.getStudy().getName())
                .topicName(post.getStudy().getTopicName())
                .studyType(post.getStudy().getStudyType())
                .meetingType(post.getStudy().getMeetingType())
                .maxMembers(post.getStudy().getMaxMembers())
                .currentMembers(currentMembers)
                .studyStatus(studyStatus)
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getNickname())
                .authorProfileImage(post.getAuthor().getProfileImage())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .build();
    }
}
