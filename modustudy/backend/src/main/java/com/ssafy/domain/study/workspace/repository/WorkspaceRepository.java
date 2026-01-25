package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 워크스페이스 Repository
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /**
     * 스터디 ID로 워크스페이스 조회
     */
    Optional<Workspace> findByStudyId(Long studyId);

    /**
     * 스터디 ID로 워크스페이스 존재 여부 확인
     */
    boolean existsByStudyId(Long studyId);

    /**
     * 스터디 ID로 워크스페이스 삭제
     */
    @Modifying
    @Query("DELETE FROM Workspace w WHERE w.studyId = :studyId")
    void deleteByStudyId(@Param("studyId") Long studyId);
}