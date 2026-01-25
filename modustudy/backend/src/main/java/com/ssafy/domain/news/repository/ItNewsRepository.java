package com.ssafy.domain.news.repository;

import com.ssafy.domain.news.entity.ItNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItNewsRepository extends JpaRepository<ItNews, Long> {

    // 중복 체크용 (같은 URL이 이미 있는지)
    boolean existsBySourceUrl(String sourceUrl);

    // 최신 뉴스 조회 (활성화된 것만)
    List<ItNews> findTop20ByIsActiveTrueOrderByPublishedAtDesc();

    // 카테고리별 뉴스 조회
    List<ItNews> findByCategoryAndIsActiveTrueOrderByPublishedAtDesc(String category);
}