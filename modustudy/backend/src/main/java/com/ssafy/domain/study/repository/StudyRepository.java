package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Study 기본 Repository
 */
 @Repository
 public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryCustom {

    /**
     * 공개된 스터디 조회 (DRAFT 제외, is_public = true)
     */
    @Query("SELECT s FROM Study s " +
            "WHERE s.status != :excludeStatus " +
            "AND s.isPublic = true " +
            "ORDER BY s.createdAt DESC")
    Page<Study> findAllPublicStudies(@Param("excludeStatus") Status excludeStatus, Pageable pageable);

    /**
     * 모집중인 스터디만 조회
     */
    @Query("SELECT s FROM Study s " +
            "WHERE s.status = 'RECRUITING' " +
            "AND s.isPublic = true " +
            "ORDER BY s.createdAt DESC")
    Page<Study> findRecruitingStudies(Pageable pageable);

    /**
     * 특정 스터디장의 스터디 목록 조회
     */
    Page<Study> findByLeaderId(Long leaderId, Pageable pageable);

    /**
     * 특정 스터디장이 생성한 스터디 개수 조회
     */
    long countByLeaderId(Long leaderId);

    /**
     * 특정 스터디장의 스터디 존재 여부 확인
     */
    boolean existsByLeaderId(Long leaderId);

    /**
     * 특정 상태의 스터디 개수 조회
     */
    Long countByStatus(Status status);

    /**
     * 특정 스터디장의 특정 상태 스터디 목록
     */
    Page<Study> findByLeaderIdAndStatus(Long leaderId, Status status, Pageable pageable);

    /**
     * 특정 스터디장의 특정 상태 스터디 목록 (전체)
     */
    List<Study> findByLeaderIdAndStatus(Long leaderId, Status status);

    /**
     * 특정 스터디장의 여러 상태 스터디 목록 (전체)
     */
    List<Study> findByLeaderIdAndStatusIn(Long leaderId, List<Status> statuses);

    /**
     * 특정 상태이고 모집 종료일이 특정 날짜 이전인 스터디 목록 조회
     * - 모집 기간 종료 스케줄러에서 사용
     */
    List<Study> findByStatusAndRecruitEndDateBefore(Status status, LocalDate date);

    // ============================================
    // 스케줄러용 메서드 (StudyStatusScheduler)
    // ============================================

    /**
     * 특정 상태의 스터디 목록 조회
     */
    List<Study> findByStatus(Status status);

    /**
     * 특정 상태이고 모집 시작일이 특정 날짜 이하인 스터디 목록 조회
     * - SCHEDULED → RECRUITING 전이용
     */
    List<Study> findByStatusAndRecruitStartDateLessThanEqual(Status status, LocalDate date);

    /**
     * 특정 상태이고 모집 종료일이 특정 날짜 미만인 스터디 목록 조회
     * - RECRUITING → RECRUIT_CLOSED 전이용
     */
    List<Study> findByStatusAndRecruitEndDateLessThan(Status status, LocalDate date);

    /**
     * 특정 상태이고 시작일이 특정 날짜 이하인 스터디 목록 조회
     * - RECRUIT_CLOSED → IN_PROGRESS 전이용
     */
    List<Study> findByStatusAndStartDateLessThanEqual(Status status, LocalDate date);

    /**
     * 특정 상태이고 종료일이 특정 날짜 미만인 스터디 목록 조회
     * - IN_PROGRESS → COMPLETED 전이용
     */
    List<Study> findByStatusAndEndDateLessThan(Status status, LocalDate date);
}
