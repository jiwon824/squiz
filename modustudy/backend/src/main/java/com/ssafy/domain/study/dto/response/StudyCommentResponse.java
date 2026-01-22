package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.StudyComment;
import com.ssafy.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyCommentResponse {

    private Long id;
    private Long studyId;
    private Long userId;
    private String userNickname;
    private String userProfileImage;
    private Long parentId;
    private String content;
    private String imageUrl;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 대댓글 목록 (최상위 댓글인 경우에만)
     */
    private List<StudyCommentResponse> replies;

    /**
     * 대댓글 개수
     */
    private Long replyCount;

    /**
     * Entity를 DTO로 변환 (기본)
     */
    public static StudyCommentResponse from(StudyComment comment) {
        return StudyCommentResponse.builder()
                .id(comment.getId())
                .studyId(comment.getStudyId())
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .content(comment.getIsDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .imageUrl(comment.getIsDeleted() ? null : comment.getImageUrl())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 DTO로 변환 (작성자 정보 포함)
     */
    public static StudyCommentResponse from(StudyComment comment, User user) {
        return StudyCommentResponse.builder()
                .id(comment.getId())
                .studyId(comment.getStudyId())
                .userId(comment.getUserId())
                .userNickname(user.getNickname())
                .userProfileImage(user.getProfileImage())
                .parentId(comment.getParentId())
                .content(comment.getIsDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .imageUrl(comment.getIsDeleted() ? null : comment.getImageUrl())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 DTO로 변환 (작성자 정보 + 대댓글 목록 포함)
     */
    public static StudyCommentResponse from(StudyComment comment, User user, List<StudyCommentResponse> replies) {
        return StudyCommentResponse.builder()
                .id(comment.getId())
                .studyId(comment.getStudyId())
                .userId(comment.getUserId())
                .userNickname(user.getNickname())
                .userProfileImage(user.getProfileImage())
                .parentId(comment.getParentId())
                .content(comment.getIsDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .imageUrl(comment.getIsDeleted() ? null : comment.getImageUrl())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .replyCount((long) replies.size())
                .build();
    }

    /**
     * Entity를 DTO로 변환 (작성자 정보 + 대댓글 개수만)
     */
    public static StudyCommentResponse from(StudyComment comment, User user, Long replyCount) {
        return StudyCommentResponse.builder()
                .id(comment.getId())
                .studyId(comment.getStudyId())
                .userId(comment.getUserId())
                .userNickname(user.getNickname())
                .userProfileImage(user.getProfileImage())
                .parentId(comment.getParentId())
                .content(comment.getIsDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .imageUrl(comment.getIsDeleted() ? null : comment.getImageUrl())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(replyCount)
                .build();
    }
}