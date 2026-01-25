package com.ssafy.domain.news.repository;

import com.ssafy.domain.news.entity.ItNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItNewsRepository extends JpaRepository<ItNews, Long> {

    // 중복 체크
    boolean existsBySourceUrl(String sourceUrl);

    // 최신 뉴스 20개 (활성화된 것만)
    List<ItNews> findTop20ByIsActiveTrueOrderByPublishedAtDesc();

    // 카테고리별 뉴스
    List<ItNews> findByCategoryAndIsActiveTrueOrderByPublishedAtDesc(String category);

    // 제목 검색
    List<ItNews> findByTitleContainingAndIsActiveTrueOrderByPublishedAtDesc(String keyword);
}