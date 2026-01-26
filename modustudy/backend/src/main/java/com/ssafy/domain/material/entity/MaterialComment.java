package com.ssafy.domain.material.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MaterialComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 자료 ID
     */
    @Column(name = "material_id", nullable = false)
    private Long materialId;

    /**
     * 작성자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 댓글 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

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
     * 본인이 작성한 댓글인지 확인
     */
    public boolean isAuthor(Long userId) {
        return this.userId.equals(userId);
    }

    // ============================================================
    // 정적 팩토리 메서드
    // ============================================================

    /**
     * 댓글 생성
     */
    public static MaterialComment create(Long materialId, Long userId, String content) {
        return MaterialComment.builder()
                .materialId(materialId)
                .userId(userId)
                .content(content)
                .build();
    }
}
