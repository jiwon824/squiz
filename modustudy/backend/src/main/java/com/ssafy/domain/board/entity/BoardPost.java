package com.ssafy.domain.board.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "recruitment_field", length = 50)
    private String recruitmentField;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    private MeetingType meetingType;

    @Column(name = "target_members")
    private Integer targetMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status")
    private RecruitmentStatus recruitmentStatus;

    @Column(name = "view_count")
    private int viewCount;

    @Column(name = "like_count")
    private int likeCount;

    @Column(name = "comment_count")
    private int commentCount;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    public BoardPost(
            User author,
            Study study,
            BoardCategory category,
            String title,
            String content,
            String recruitmentField,
            MeetingType meetingType,
            Integer targetMembers,
            RecruitmentStatus recruitmentStatus
    ) {
        this.author = author;
        this.study = study;
        this.category = category;
        this.title = title;
        this.content = content;
        this.recruitmentField = recruitmentField;
        this.meetingType = meetingType;
        this.targetMembers = targetMembers;
        this.recruitmentStatus = recruitmentStatus;
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.isDeleted = false;
    }

    public void update(
            String title,
            String content,
            String recruitmentField,
            MeetingType meetingType,
            Integer targetMembers,
            RecruitmentStatus recruitmentStatus
    ) {
        this.title = title;
        this.content = content;
        this.recruitmentField = recruitmentField;
        this.meetingType = meetingType;
        this.targetMembers = targetMembers;
        this.recruitmentStatus = recruitmentStatus;
    }

    public void increaseViewCount() {
        this.viewCount += 1;
    }

    public void increaseCommentCount() {
        this.commentCount += 1;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount -= 1;
        }
    }

    public void delete() {
        this.isDeleted = true;
    }
}
