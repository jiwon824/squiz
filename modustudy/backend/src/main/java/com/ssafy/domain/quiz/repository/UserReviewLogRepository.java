package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReviewLogRepository extends JpaRepository<UserReviewLog, Long> {

    /**
     * 특정 복습 항목의 학습 이력 조회
     */
    List<UserReviewLog> findByReviewItemIdOrderByReviewedAtDesc(Long reviewItemId);
}
