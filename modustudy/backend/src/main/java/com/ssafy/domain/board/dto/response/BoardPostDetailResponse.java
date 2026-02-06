package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.board.entity.RecruitmentStatus;
import com.ssafy.domain.study.entity.MeetingType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardPostDetailResponse {
    private Long id;
    private MeetingType meetingType;
    private String recruitmentField;
    private Integer targetMembers;
    private RecruitmentStatus recruitmentStatus;
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
            List<BoardCommentResponse> comments
    ) {
        return BoardPostDetailResponse.builder()
                .id(post.getId())
                .meetingType(post.getMeetingType())
                .recruitmentField(post.getRecruitmentField())
                .targetMembers(post.getTargetMembers())
                .recruitmentStatus(post.getRecruitmentStatus())
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
