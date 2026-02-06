package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.board.entity.RecruitmentStatus;
import com.ssafy.domain.study.entity.MeetingType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardPostSummaryResponse {
    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private String recruitmentField;
    private MeetingType meetingType;
    private Integer targetMembers;
    private int viewCount;
    private LocalDateTime createdAt;
    private RecruitmentStatus recruitmentStatus;

    public static BoardPostSummaryResponse from(BoardPost post) {
        return BoardPostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getNickname())
                .authorProfileImage(post.getAuthor().getProfileImage())
                .recruitmentField(post.getRecruitmentField())
                .meetingType(post.getMeetingType())
                .targetMembers(post.getTargetMembers())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .recruitmentStatus(post.getRecruitmentStatus())
                .build();
    }
}
