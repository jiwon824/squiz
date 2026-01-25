package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.entity.WorkspaceType;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    // ============================================================
    // 스터디별 조회
    // ============================================================
    List<Workspace> findByStudyId(Long studyId);

    List<Workspace> findByStudyIdAndType(Long studyId, WorkspaceType type);

    // ============================================================
    // 개별 조회
    // ============================================================
    Optional<Workspace> findByIdAndStudyId(Long id, Long studyId);

    Optional<Workspace> findByStudyIdAndName(Long studyId, String name);

// ============================================================
    // 존재 여부
    // ============================================================

    boolean existsByStudyId(Long studyId);

    boolean existsByStudyIdAndName(Long studyId, String name);

    // ============================================================
    // 통계
    // ============================================================

    long countByStudyId(Long studyId);

    long countByStudyIdAndType(Long studyId, WorkspaceType type);

    // ============================================================
    // 삭제
    // ============================================================

    void deleteByStudyId(Long studyId);
}
