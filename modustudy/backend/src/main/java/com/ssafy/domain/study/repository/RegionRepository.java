package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    /**
     * 코드로 지역 조회
     */
    Optional<Region> findByCode(String code);

    /**
     * 모든 시/도 조회 (Level 1)
     */
    @Query("SELECT r FROM Region r WHERE r.level = 1 AND r.isActive = true ORDER BY r.sortOrder")
    List<Region> findAllProvinces();

    /**
     * 특정 시/도의 하위 지역 조회 (Level 2)
     */
    @Query("SELECT r FROM Region r WHERE r.parent.id = :parentId AND r.isActive = true ORDER BY r.sortOrder")
    List<Region> findByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 시/도 코드의 하위 지역 조회
     */
    @Query("SELECT r FROM Region r WHERE r.parent.code = :parentCode AND r.isActive = true ORDER BY r.sortOrder")
    List<Region> findByParentCode(@Param("parentCode") String parentCode);

    /**
     * 레벨별 지역 조회
     */
    List<Region> findByLevelAndIsActiveTrueOrderBySortOrder(Integer level);

    /**
     * 지역명 검색 (LIKE)
     */
    @Query("SELECT r FROM Region r WHERE r.isActive = true AND " +
            "(r.name LIKE %:keyword% OR r.fullName LIKE %:keyword%) " +
            "ORDER BY r.level, r.sortOrder")
    List<Region> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 시/도와 함께 하위 지역 페치 조인
     */
    @Query("SELECT DISTINCT r FROM Region r " +
            "LEFT JOIN FETCH r.children c " +
            "WHERE r.level = 1 AND r.isActive = true " +
            "ORDER BY r.sortOrder")
    List<Region> findAllProvincesWithChildren();

    /**
     * ID 목록으로 지역 조회
     */
    List<Region> findByIdIn(List<Long> ids);

    /**
     * 활성화된 모든 지역 조회 (계층 순서대로)
     */
    @Query("SELECT r FROM Region r WHERE r.isActive = true ORDER BY r.level, r.sortOrder")
    List<Region> findAllActiveOrderByLevelAndSortOrder();

    /**
     * 코드 존재 여부 확인
     */
    boolean existsByCode(String code);
}