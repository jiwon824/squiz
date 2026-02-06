package com.ssafy.domain.retrospect.repository;

import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetrospectiveItemRepository extends JpaRepository<RetrospectiveItem, Long> {

    // 회고별 항목 목록 조회
    List<RetrospectiveItem> findByRetrospectiveId(Long retrospectiveId);

    // 회고별 카테고리별 항목 목록 조회
    List<RetrospectiveItem> findByRetrospectiveIdAndCategory(
            Long retrospectiveId, Category category);

    // 회고 + 항목 ID로 조회 (권한 체크용)
    Optional<RetrospectiveItem> findByIdAndRetrospectiveId(Long id, Long retrospectiveId);

    // 회고별 항목 개수 조회
    Long countByRetrospectiveId(Long retrospectiveId);

    // 회고별 사용자 수 조회 (참여자 수)
    @Query("SELECT COUNT(DISTINCT ri.userId) FROM RetrospectiveItem ri WHERE ri.retrospectiveId = :retrospectiveId")
    Long countDistinctUserByRetrospectiveId(@Param("retrospectiveId") Long retrospectiveId);

    // 회고에 특정 사용자가 작성한 항목 존재 여부
    boolean existsByRetrospectiveIdAndUserId(Long retrospectiveId, Long userId);

    // 회고별 특정 사용자의 항목 목록 조회
    List<RetrospectiveItem> findByRetrospectiveIdAndUserId(Long retrospectiveId, Long userId);

    // 회고별 항목 전체 삭제
    void deleteByRetrospectiveId(Long retrospectiveId);

}
