package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyCommentRepository extends JpaRepository<StudyComment, Long> {

    // ============================================================
    // 댓글 조회 (스터디별)
    // ============================================================

    /**
     * 특정 스터디의 최상위 댓글 목록 조회 (삭제되지 않은 것만, 페이징)
     */
    @Query("SELECT sc FROM StudyComment sc " +
            "WHERE sc.studyId = :studyId " +
            "AND sc.parentId IS NULL " +
            "AND sc.isDeleted = false " +
            "ORDER BY sc.createdAt DESC")
    Page<StudyComment> findParentCommentsByStudyId(@Param("studyId") Long studyId, Pageable pageable);

    /**
     * 특정 스터디의 최상위 댓글 목록 조회 (삭제된 것 포함, 페이징)
     * - 대댓글이 있는 삭제된 댓글은 "삭제된 댓글입니다"로 표시하기 위함
     */
    @Query("SELECT sc FROM StudyComment sc " +
            "WHERE sc.studyId = :studyId " +
            "AND sc.parentId IS NULL " +
            "ORDER BY sc.createdAt DESC")
    Page<StudyComment> findAllParentCommentsByStudyId(@Param("studyId") Long studyId, Pageable pageable);

    /**
     * 특정 스터디의 전체 댓글 목록 조회 (리스트)
     */
    List<StudyComment> findByStudyIdAndIsDeletedFalseOrderByCreatedAtAsc(Long studyId);

    // ============================================================
    // 대댓글 조회
    // ============================================================

    /**
     * 특정 부모 댓글의 대댓글 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT sc FROM StudyComment sc " +
            "WHERE sc.parentId = :parentId " +
            "AND sc.isDeleted = false " +
            "ORDER BY sc.createdAt ASC")
    List<StudyComment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 부모 댓글의 대댓글 목록 조회 (삭제된 것 포함)
     */
    List<StudyComment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    /**
     * 특정 부모 댓글의 대댓글 개수 (삭제되지 않은 것만)
     */
    @Query("SELECT COUNT(sc) FROM StudyComment sc " +
            "WHERE sc.parentId = :parentId " +
            "AND sc.isDeleted = false")
    Long countRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 부모 댓글에 대댓글이 존재하는지 확인
     */
    boolean existsByParentIdAndIsDeletedFalse(Long parentId);

    // ============================================================
    // 사용자별 댓글 조회
    // ============================================================

    /**
     * 특정 사용자의 댓글 목록 조회 (페이징)
     */
    Page<StudyComment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 댓글 목록 조회 (리스트)
     */
    List<StudyComment> findByUserIdAndIsDeletedFalse(Long userId);

    // ============================================================
    // 개별 댓글 조회
    // ============================================================

    /**
     * 삭제되지 않은 댓글 조회
     */
    Optional<StudyComment> findByIdAndIsDeletedFalse(Long id);

    /**
     * 특정 스터디의 특정 댓글 조회
     */
    Optional<StudyComment> findByIdAndStudyIdAndIsDeletedFalse(Long id, Long studyId);

    // ============================================================
    // 통계
    // ============================================================

    /**
     * 특정 스터디의 전체 댓글 개수 (삭제되지 않은 것만)
     */
    Long countByStudyIdAndIsDeletedFalse(Long studyId);

    /**
     * 특정 스터디의 최상위 댓글 개수 (삭제되지 않은 것만)
     */
    @Query("SELECT COUNT(sc) FROM StudyComment sc " +
            "WHERE sc.studyId = :studyId " +
            "AND sc.parentId IS NULL " +
            "AND sc.isDeleted = false")
    Long countParentCommentsByStudyId(@Param("studyId") Long studyId);

    /**
     * 특정 사용자의 댓글 개수
     */
    Long countByUserIdAndIsDeletedFalse(Long userId);

    // ============================================================
    // 삭제
    // ============================================================

    /**
     * 특정 스터디의 모든 댓글 삭제 (Hard Delete - 스터디 삭제 시 사용)
     */
    void deleteByStudyId(Long studyId);
}