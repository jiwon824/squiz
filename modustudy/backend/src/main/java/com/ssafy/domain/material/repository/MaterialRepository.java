package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long>, MaterialRepositoryCustom {

    /**
     * 스터디 ID로 자료 목록 조회 (페이징)
     */
    Page<Material> findByStudyId(Long studyId, Pageable pageable);

    /**
     * 스터디 ID로 자료 목록 조회 (전체)
     */
    List<Material> findByStudyId(Long studyId);

    /**
     * 스터디 ID + 자료 ID로 조회
     */
    Optional<Material> findByIdAndStudyId(Long id, Long studyId);

    /**
     * 스터디 ID + 주차로 조회
     */
    List<Material> findByStudyIdAndWeekNumber(Long studyId, Integer weekNumber);

    /**
     * 스터디 ID + 자료 타입으로 조회
     */
    Page<Material> findByStudyIdAndMaterialType(Long studyId, MaterialType materialType, Pageable pageable);

    /**
     * 스터디 ID로 자료 개수 조회
     */
    long countByStudyId(Long studyId);

    /**
     * 업로더 ID로 자료 목록 조회
     */
    List<Material> findByUploaderId(Long uploaderId);

    /**
     * 스터디 ID로 전체 삭제 (스터디 삭제 시)
     */
    void deleteByStudyId(Long studyId);
}
