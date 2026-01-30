package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserReviewItemRepository extends JpaRepository<UserReviewItem, Long> {

    /**
     * 유니크 키 (userId, contentType, contentId) 기반 조회
     */
    Optional<UserReviewItem> findByUserIdAndContentTypeAndContentId(
            Long userId, ReviewContentType contentType, Long contentId);

    /**
     * 특정 사용자의 전체 복습 항목 조회
     */
    List<UserReviewItem> findAllByUserId(Long userId);

    /**
     * 복습 예정 항목 조회 (nextReviewAt <= 현재 시각)
     */
    @Query("SELECT r FROM UserReviewItem r " +
           "WHERE r.userId = :userId AND r.nextReviewAt <= :now " +
           "ORDER BY r.nextReviewAt ASC")
    List<UserReviewItem> findDueItems(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * 복습 예정 항목 수 조회 (nextReviewAt < 현재 시각)
     */
    long countByUserIdAndNextReviewAtBefore(Long userId, LocalDateTime now);
}
