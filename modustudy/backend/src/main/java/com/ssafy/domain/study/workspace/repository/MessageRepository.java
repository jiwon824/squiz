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

/**
 * 메시지 Repository
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 워크스페이스 ID로 메시지 목록 조회 (삭제되지 않은 것만, 생성일 내림차순)
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByWorkspaceIdAndNotDeleted(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);

    /**
     * 워크스페이스 ID로 메시지 목록 조회 (삭제 포함, 생성일 내림차순)
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);

    /**
     * 워크스페이스 ID로 최근 메시지 N개 조회
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    List<Message> findRecentMessages(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);

    /**
     * 특정 시간 이후의 메시지 조회 (새 메시지 폴링용)
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false " +
            "AND m.createdAt > :after " +
            "ORDER BY m.createdAt ASC")
    List<Message> findMessagesAfter(
            @Param("workspaceId") Long workspaceId,
            @Param("after") LocalDateTime after);

    /**
     * 사용자가 작성한 메시지 목록 조회
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.userId = :userId " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * 워크스페이스 내 메시지 타입별 조회
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.messageType = :messageType " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByWorkspaceIdAndMessageType(
            @Param("workspaceId") Long workspaceId,
            @Param("messageType") MessageType messageType,
            Pageable pageable);

    /**
     * 워크스페이스 내 메시지 검색 (내용 기준)
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false " +
            "AND m.content LIKE %:keyword% " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchByContent(
            @Param("workspaceId") Long workspaceId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 워크스페이스 내 메시지 수 조회
     */
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.workspaceId = :workspaceId " +
            "AND m.isDeleted = false")
    long countByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 워크스페이스 ID로 모든 메시지 삭제 (워크스페이스 삭제 시)
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.workspaceId = :workspaceId")
    void deleteAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 사용자 ID로 모든 메시지 삭제 (회원 탈퇴 시)
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}