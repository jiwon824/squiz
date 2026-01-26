package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * 대분류 목록 조회 (parent가 null인 것)
     */
    List<Topic> findByParentIsNullOrderBySortOrderAsc();

    /**
     * 특정 대분류의 소분류 목록 조회
     */
    List<Topic> findByParentIdOrderBySortOrderAsc(Long parentId);

    /**
     * 대분류와 소분류 한번에 조회 (N+1 방지)
     */
    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.parent ORDER BY t.sortOrder")
    List<Topic> findAllWithParent();
}