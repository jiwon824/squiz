package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 메시지 Repository
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ============================================================
    // 워크스페이스별 조회
    // ============================================================

    /**
     * 워크스페이스의 메시지 목록 (삭제 제외)
     */
    Page<Message> findByWorkspaceIdAndIsDeletedFalse(Long workspaceId, Pageable pageable);

    /**
     * 워크스페이스의 최근 메시지 N개
     */
    List<Message> findTop50ByWorkspaceIdAndIsDeletedFalse(Long workspaceId);

    /**
     * 특정 시간 이후 메시지 조회 (실시간 동기화용)
     */
    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findMessagesSince(@Param("workspaceId") Long workspaceId,
                                    @Param("since") LocalDateTime since);

    // ============================================================
    // 개별 조회
    // ============================================================

    Optional<Message> findByIdAndWorkspaceIdAndIsDeletedFalse(Long id, Long workspaceId);

    Optional<Message> findByIdAndIsDeletedFalse(Long id);

    // ============================================================
    // 사용자별 조회
    // ============================================================

    Page<Message> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    List<Message> findByWorkspaceIdAndUserIdAndIsDeletedFalse(Long workspaceId, Long userId);

    // ============================================================
    // 검색
    // ============================================================

    @Query("SELECT m FROM Message m WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false AND m.content LIKE %:keyword% " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchByKeyword(@Param("workspaceId") Long workspaceId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    // ============================================================
    // 통계
    // ============================================================

    long countByWorkspaceIdAndIsDeletedFalse(Long workspaceId);

    long countByWorkspaceIdAndMessageTypeAndIsDeletedFalse(Long workspaceId, MessageType messageType);

    // ============================================================
    // 삭제
    // ============================================================

    @Modifying
    @Query("UPDATE Message m SET m.isDeleted = true WHERE m.workspaceId = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") Long workspaceId);

    void deleteByWorkspaceId(Long workspaceId);
}