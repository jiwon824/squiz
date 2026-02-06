package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    // 시/도 목록 (level=1)
    List<Region> findByLevelOrderBySortOrderAsc(Integer level);

    // 특정 시/도의 시/군/구 목록
    List<Region> findByParentIdOrderBySortOrderAsc(Long parentId);

    // 코드로 조회
    Optional<Region> findByCode(String code);

    // 이름 검색 (시/도명 또는 시/군/구명)
    @Query("SELECT r FROM Region r WHERE r.name LIKE %:keyword% OR r.fullName LIKE %:keyword% ORDER BY r.level ASC, r.sortOrder ASC")
    List<Region> searchByKeyword(String keyword);

    // 시/도 + 하위 시/군/구 전체 조회 (N+1 방지)
    @Query("SELECT r FROM Region r LEFT JOIN FETCH r.children c WHERE r.level = 1 ORDER BY r.sortOrder ASC")
    List<Region> findAllProvincesWithDistricts();
}
