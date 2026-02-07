package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_comment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StudyComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스터디 ID
     */
    @Column(name = "study_id", nullable = false)
    private Long studyId;

    /**
     * 작성자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 부모 댓글 ID (대댓글인 경우)
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 댓글 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 첨부 이미지 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 삭제 여부 (Soft Delete)
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================================
    // 비즈니스 로직
    // ============================================================

    /**
     * 댓글 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }

    /**
     * 이미지 URL 수정
     */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 댓글 삭제 (Soft Delete)
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * 대댓글인지 확인
     */
    public boolean isReply() {
        return this.parentId != null;
    }

    /**
     * 삭제된 댓글인지 확인
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * 작성자 본인인지 확인
     */
    public boolean isAuthor(Long userId) {
        return this.userId.equals(userId);
    }
}
