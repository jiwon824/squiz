package com.ssafy.domain.daily.repository;

import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyItemRepository extends JpaRepository<DailyItem, Long> {

    /**
     * 특정 데일리 리포트의 모든 항목 조회
     */
    List<DailyItem> findByDailyReportIdOrderByCreatedAtAsc(Long dailyReportId);

    /**
     * 특정 데일리 리포트에서 특정 사용자의 항목 조회
     */
    List<DailyItem> findByDailyReportIdAndUserIdOrderByCreatedAtAsc(Long dailyReportId, Long userId);

    /**
     * 특정 데일리 리포트에서 특정 사용자의 특정 카테고리 항목 조회
     */
    List<DailyItem> findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(
            Long dailyReportId, Long userId, DailyCategory category);

    /**
     * 특정 데일리 리포트에서 특정 카테고리의 모든 항목 조회 (블로커 조회용)
     */
    List<DailyItem> findByDailyReportIdAndCategoryOrderByCreatedAtAsc(
            Long dailyReportId, DailyCategory category);

    /**
     * 특정 데일리 리포트의 항목 개수
     */
    long countByDailyReportId(Long dailyReportId);

    /**
     * 특정 데일리 리포트에서 특정 사용자의 항목 개수
     */
    long countByDailyReportIdAndUserId(Long dailyReportId, Long userId);

    /**
     * 특정 데일리 리포트의 모든 항목 삭제
     */
    void deleteByDailyReportId(Long dailyReportId);
}
