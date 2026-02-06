package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.MaterialComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialCommentRepository extends JpaRepository<MaterialComment, Long> {

    /**
     * 자료 ID로 댓글 목록 조회
     */
    List<MaterialComment> findByMaterialId(Long materialId);

    /**
     * 자료 ID로 댓글 개수 조회
     */
    long countByMaterialId(Long materialId);

    /**
     * 자료 ID로 댓글 전체 삭제 (자료 삭제 시)
     */
    @Modifying
    @Query("DELETE FROM MaterialComment mc WHERE mc.materialId = :materialId")
    void deleteByMaterialId(@Param("materialId") Long materialId);

    /**
     * 사용자 ID로 댓글 목록 조회
     */
    List<MaterialComment> findByUserId(Long userId);

    /**
     * 자료 ID + 댓글 ID로 존재 여부 확인
     */
    boolean existsByIdAndMaterialId(Long Id, Long materialId);
}
