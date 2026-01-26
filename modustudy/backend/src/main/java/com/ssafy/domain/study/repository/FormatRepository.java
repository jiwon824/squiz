package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Format;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormatRepository extends JpaRepository<Format, Long> {

    /**
     * 전체 형식 목록 조회 (정렬순)
     */
    List<Format> findAllByOrderBySortOrderAsc();
}