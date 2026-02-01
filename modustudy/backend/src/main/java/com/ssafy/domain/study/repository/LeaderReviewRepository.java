package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.LeaderReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaderReviewRepository extends JpaRepository<LeaderReview, Long> {

    /**
     * 특정 스터디장의 리뷰 목록 조회 (페이징)
     */
    Page<LeaderReview> findByLeaderId(Long leaderId, Pageable pageable);

    /**
     * 특정 스터디장의 리뷰 개수
     */
    Long countByLeaderId(Long leaderId);

    /**
     * 특정 스터디장의 평균 평점 계산
     */
    @Query("SELECT AVG(lr.rating) FROM LeaderReview lr WHERE lr.leaderId = :leaderId")
    Double calculateAverageRating(@Param("leaderId") Long leaderId);

    /**
     * 특정 스터디의 리뷰 조회
     */
    List<LeaderReview> findByStudyId(Long StudyId);

    /**
     * 특정 스터디 + 평가자로 리뷰 존재 여부 확인 (중복 방지)
     */
    boolean existsByStudyIdAndReviewerId(Long studyId, Long reviewerId);

    /**
     * 특정 스터디 + 평가자로 리뷰 조회 (내 리뷰 조회)
     */
    java.util.Optional<LeaderReview> findByStudyIdAndReviewerId(Long studyId, Long reviewerId);
}
