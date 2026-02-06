package com.ssafy.domain.recruitment.repository;

import com.ssafy.domain.recruitment.entity.TeamRecruit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRecruitRepository extends JpaRepository<TeamRecruit, Long> {

    /**
     * 전체 모집글 목록 (최신순)
     */
    Page<TeamRecruit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 카테고리별 모집글 목록
     */
    Page<TeamRecruit> findByCategoryOrderByCreatedAtDesc(TeamRecruit.RecruitCategory category, Pageable pageable);

    /**
     * 모집 상태별 목록
     */
    Page<TeamRecruit> findByStatusOrderByCreatedAtDesc(TeamRecruit.RecruitStatus status, Pageable pageable);

    /**
     * 내가 작성한 모집글
     */
    List<TeamRecruit> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 모집중인 글만 조회
     */
    Page<TeamRecruit> findByStatusAndCategoryOrderByCreatedAtDesc(
            TeamRecruit.RecruitStatus status,
            TeamRecruit.RecruitCategory category,
            Pageable pageable
    );
}
