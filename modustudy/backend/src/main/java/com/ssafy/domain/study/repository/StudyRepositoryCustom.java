package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Study Custom Repository 인터페이스
 * 동적 쿼리용
 */
 public interface StudyRepositoryCustom {

    /**
     * 동적 조건으로 스터디 검색/필터링
     */
    Page<Study> searchStudies(StudySearchCondition condition, Pageable pageable);
}
