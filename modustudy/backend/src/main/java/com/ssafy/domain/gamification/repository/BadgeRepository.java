package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 배지 조회 레포지토리.
 * 호출자: {@link com.ssafy.domain.quiz.service.QuizCourseService}
 *
 */
 public interface BadgeRepository extends JpaRepository<Badge, Long> {
    /**
     * 배지 코드를 기준으로 배지 목록을 조회한다.
     *
     * @param codes 배지 코드 목록
     * @return 배지 엔티티 목록
     */
    List<Badge> findByCodeIn(Iterable<String> codes);

    /**
     * 배지 코드로 단일 배지를 조회한다.
     *
     * @param code 배지 코드
     * @return 배지 엔티티
     */
    Optional<Badge> findByCode(String code);

    /**
     * 모든 배지를 카테고리와 정렬 순서대로 조회한다.
     *
     * @return 정렬된 배지 목록
     */
    List<Badge> findAllByOrderByCategoryAscSortOrderAsc();
}
