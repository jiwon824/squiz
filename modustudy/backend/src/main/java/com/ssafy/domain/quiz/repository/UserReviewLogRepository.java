package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReviewLogRepository extends JpaRepository<UserReviewLog, Long> {

    /**
     * 특정 복습 항목의 전체 학습 이력 조회 (최신순)
     */
    List<UserReviewLog> findByReviewItemIdOrderByReviewedAtDesc(Long reviewItemId);

    /**
     * 특정 복습 항목의 최근 10건 학습 이력 조회 (최신순)
     */
    List<UserReviewLog> findTop10ByReviewItemIdOrderByReviewedAtDesc(Long reviewItemId);

    /**
     * 특정 사용자의 특정 기간 동안의 학습 이력 조회 (오래된 순)
     */
    List<UserReviewLog> findAllByReviewItemUserIdAndReviewedAtBetweenOrderByReviewedAtAsc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
