package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.board.entity.BoardComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardCommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private Long parentId;
    private String content;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardCommentResponse from(BoardComment comment) {
        return BoardCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getNickname())
                .authorProfileImage(comment.getAuthor().getProfileImage())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .isDeleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
