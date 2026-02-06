package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.ApplicationStatus;
import com.ssafy.domain.study.entity.StudyApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyApplicationRepository extends JpaRepository<StudyApplication, Long> {

    // ============================================================
    // 스터디별 신청 목록 조회
    // ============================================================

    /**
     * 특정 스터디의 전체 신청 목록 조회 (페이징)
     */
    Page<StudyApplication> findByStudyId(Long studyId, Pageable pageable);

    /**
     * 특정 스터디의 특정 상태 신청 목록 조회 (페이징)
     */
    Page<StudyApplication> findByStudyIdAndStatus(Long studyId, ApplicationStatus status, Pageable pageable);

    /**
     * 특정 스터디의 신청 목록 조회 (리스트)
     */
    List<StudyApplication> findByStudyId(Long studyId);

    /**
     * 특정 스터디의 대기 중 신청 개수
     */
    Long countByStudyIdAndStatus(Long studyId, ApplicationStatus status);

    // ============================================================
    // 사용자별 신청 내역 조회
    // ============================================================

    /**
     * 특정 사용자의 전체 신청 내역 조회 (페이징)
     */
    Page<StudyApplication> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 특정 상태 신청 내역 조회 (페이징)
     */
    Page<StudyApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    /**
     * 특정 사용자의 신청 내역 조회 (리스트)
     */
    List<StudyApplication> findByUserId(Long userId);

    // ============================================================
    // 개별 신청 조회
    // ============================================================

    /**
     * 특정 스터디 + 특정 사용자의 신청 조회
     */
    Optional<StudyApplication> findByStudyIdAndUserId(Long studyId, Long userId);

    /**
     * 특정 스터디 + 특정 사용자의 신청 존재 여부
     */
    boolean existsByStudyIdAndUserId(Long studyId, Long userId);

    // ============================================================
    // 통계 조회
    // ============================================================

    /**
     * 특정 스터디의 전체 신청 개수
     */
    Long countByStudyId(Long studyId);

    /**
     * 특정 스터디의 승인된 신청 개수
     */
    @Query("SELECT COUNT(sa) FROM StudyApplication sa WHERE sa.studyId = :studyId AND sa.status = 'APPROVED'")
    Long countApprovedByStudyId(@Param("studyId") Long studyId);

    /**
     * 특정 사용자의 전체 신청 개수
     */
    Long countByUserId(Long userId);

    // ============================================================
    // 삭제
    // ============================================================

    /**
     * 특정 스터디의 모든 신청 삭제
     */
    void deleteByStudyId(Long studyId);

    /**
     * 특정 스터디 + 특정 사용자의 신청 삭제
     */
    void deleteByStudyIdAndUserId(Long studyId, Long userId);
}
