package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 스터디 세션 Repository
 */
 @Repository
 public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /**
     * 특정 스터디의 모든 세션 조회
     */
    List<StudySession> findByStudyId(Long studyId);

    /**
     * 특정 스터디의 특정 회차 세션 조회
     */
    Optional<StudySession> findByStudyIdAndSessionNumber(Long studyId, Integer sessionNumber);

    /**
     * 특정 스터디의 특정 상태 세션 목록 조회
     */
    List<StudySession> findByStudyIdAndStatus(Long studyId, SessionStatus status);

    /**
     * 특정 스터디의 세션 개수 조회
     */
    long countByStudyId(Long studyId);

    /**
     * 특정 스터디의 완료된 세션 개수 조회
     */
    long countByStudyIdAndStatus(Long studyId, SessionStatus status);

    /**
     * 특정 기간 내 예정된 세션 조회
     */
    @Query("SELECT s FROM StudySession s WHERE s.studyId = :studyId " +
            "AND s.scheduledAt BETWEEN :startDateTime AND :endDateTime " +
            "ORDER BY s.scheduledAt ASC")
    List<StudySession> findByStudyIdAndScheduledAtBetween(
            @Param("studyId") Long studyId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    List<StudySession> findByScheduledAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * 특정 스터디의 다음/현재 진행 중인 세션 조회
     * 세션 종료 시간(scheduledAt + durationMinutes)이 현재보다 미래인 세션을 반환
     * 즉, 아직 끝나지 않았거나 미래에 시작되는 세션
     */
    @Query("SELECT s FROM StudySession s WHERE s.studyId = :studyId " +
            "AND s.status = 'SCHEDULED' " +
            "AND FUNCTION('TIMESTAMPADD', MINUTE, COALESCE(s.durationMinutes, 60), s.scheduledAt) > :now " +
            "ORDER BY s.scheduledAt ASC LIMIT 1")
    Optional<StudySession> findNextScheduledSession(
            @Param("studyId") Long studyId,
            @Param("now") LocalDateTime now);

    /**
     * 특정 스터디의 마지막 회차 번호 조회
     */
    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) FROM StudySession s WHERE s.studyId = :studyId")
    Integer findMaxSessionNumberByStudyId(@Param("studyId") Long studyId);

    /**
     * 특정 스터디의 모든 세션 삭제
     */
    @Modifying
    @Query("DELETE FROM StudySession s WHERE s.studyId = :studyId")
    void deleteAllByStudyId(@Param("studyId") Long studyId);

    /**
     * 특정 상태의 세션이 존재하는지 확인
     */
    boolean existsByStudyIdAndStatus(Long studyId, SessionStatus status);

    /**
     * 여러 스터디의 특정 기간 내 세션 조회
     */
    @Query("SELECT s FROM StudySession s WHERE s.studyId IN :studyIds " +
            "AND s.scheduledAt BETWEEN :startDateTime AND :endDateTime " +
            "ORDER BY s.scheduledAt ASC")
    List<StudySession> findByStudyIdInAndScheduledAtBetween(
            @Param("studyIds") List<Long> studyIds,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 특정 스터디의 모든 세션을 scheduledAt 기준 오름차순 조회
     * (세션 번호 재정렬용)
     */
    @Query("SELECT s FROM StudySession s WHERE s.studyId = :studyId ORDER BY s.scheduledAt ASC")
    List<StudySession> findByStudyIdOrderByScheduledAtAsc(@Param("studyId") Long studyId);
}
