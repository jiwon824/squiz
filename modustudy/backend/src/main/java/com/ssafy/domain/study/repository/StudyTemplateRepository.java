package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 스터디 템플릿 Repository
 */
 @Repository
 public interface StudyTemplateRepository extends JpaRepository<StudyTemplate, Long> {

    /**
     * 특정 사용자의 템플릿 목록 조회
     */
    List<StudyTemplate> findByUserId(Long userId);

    /**
     * 시스템 템플릿 목록 조회
     */
    List<StudyTemplate> findByIsSystemTrue();

    /**
     * 특정 사용자의 특정 템플릿 조회 (권한 체크용)
     */
    Optional<StudyTemplate> findByIdAndUserId(Long id, Long userId);

    /**
     * 템플릿 타입별 시스템 템플릿 조회
     */
    List<StudyTemplate> findByIsSystemTrueAndTemplateType(String templateType);

    /**
     * 특정 사용자의 템플릿 개수
     */
    Long countByUserId(Long userId);

    /**
     * 템플릿 이름 중복 체크 (같은 사용자 내)
     */
    boolean existsByUserIdAndName(Long userId, String name);
}
